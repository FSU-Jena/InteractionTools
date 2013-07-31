package edu.fsuj.csb.reactionnetworks.interaction.tasks.lp;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.CalculationClient;
import edu.fsuj.csb.reactionnetworks.interaction.OptimizationSolution;
import edu.fsuj.csb.reactionnetworks.interaction.SubstanceListNode;
import edu.fsuj.csb.reactionnetworks.interaction.results.OptimizationResult;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.CalculationTask;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.ParameterSet;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.SubstanceSet;
import edu.fsuj.csb.reactionnetworks.organismtools.DbCompartment;
import edu.fsuj.csb.reactionnetworks.organismtools.DbReaction;
import edu.fsuj.csb.tools.LPSolverWrapper.LPCondition;
import edu.fsuj.csb.tools.LPSolverWrapper.LPDiff;
import edu.fsuj.csb.tools.LPSolverWrapper.LPSolveWrapper;
import edu.fsuj.csb.tools.LPSolverWrapper.LPSum;
import edu.fsuj.csb.tools.LPSolverWrapper.LPTerm;
import edu.fsuj.csb.tools.LPSolverWrapper.LPVariable;
import edu.fsuj.csb.tools.organisms.Compartment;
import edu.fsuj.csb.tools.organisms.Reaction;
import edu.fsuj.csb.tools.organisms.ReactionSet;
import edu.fsuj.csb.tools.xml.NoTokenException;
import edu.fsuj.csb.tools.xml.ObjectComparator;
import edu.fsuj.csb.tools.xml.Tools;

public class LinearProgrammingTask extends CalculationTask {

	private static final long serialVersionUID = -8309620489674531282L;
	private static final boolean INFLOW = true;
	private static final boolean OUTFLOW = false;

	private int compartmentId;
	private SubstanceSet substances;
	private ParameterSet parameters;

	public LinearProgrammingTask(Integer compartmentId, SubstanceSet substanceSet, ParameterSet parameterSet) {
		this.compartmentId = compartmentId;
		this.substances = substanceSet;
		this.parameters = parameterSet;
	}

	@Override
	public void run(CalculationClient calculationClient) throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		try {
			TreeSet<OptimizationSolution> solutions = OptimizationSolution.set(); // set of (set of substances, that must be supplied)
			int solutionSize = Integer.MAX_VALUE; // for monitoring the size of the solutions
			int number = 1;
			while (true) {

				OptimizationSolution solution = runInternal(solutions); // start the actual calculation

				if (solution == null) {
					System.out.println("Solution #" + number + ": no solution found.");
					break;
				}
				int inflowSize = solution.inflows().size();
				if (inflowSize > solutionSize) { // if the size of the solution increases, we're getting suboptimal solutions. so: quit!
					System.out.println("found no more solution with size " + solutionSize);
					System.out.println("next solution: #" + (number++) + ": Inflows " + solution.inflows() + " / Outflows " + solution.outflows());
					System.out.println("Reactions: " + solution.forwardReactions() + " - " + solution.backwardReactions());
					break;
				}
				if (inflowSize < solutionSize) { // normaly the size of the solutions should not decrease, as this would mean, we have found suboptimal solutions before...
					if (solutionSize == Integer.MAX_VALUE) {
						solutionSize = inflowSize;
					} else {
						System.err.println("uh oh! found smaller solution (" + solution + "). this was not expected.");
						break;
					}
				}
				System.out.println("solution #" + (number++) + ": Inflows " + solution.inflows() + " / Outflows " + solution.outflows());
				System.out.println("Reactions: " + solution.forwardReactions() + " - " + solution.backwardReactions());
				solutions.add(solution); // don't allow inflow of the substances in the solution in the next turn
				calculationClient.sendObject(new OptimizationResult(this, solution));
				break; // TODO: add break condition
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DataFormatException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	protected OptimizationSolution runInternal(TreeSet<OptimizationSolution> solutions) throws DataFormatException, SQLException, IOException, InterruptedException {
		Tools.startMethod("runInternal(" + solutions + ")");

		/* prepare */
		LPSolveWrapper solver = new LPSolveWrapper();
		DbCompartment compartment = DbCompartment.load(compartmentId);
		TreeSet<Integer> allSubstances = compartment.utilizedSubstances();
		TreeSet<Integer> auxiliaryInflows = substances.calculateAuxiliaryInflows(allSubstances);
		TreeSet<Integer> auxiliaryOutflows = substances.calculateAuxiliaryOutflows(allSubstances);

		LPTerm desiredInflowSum = null;
		LPTerm auxiliaryInflowSum = null;
		LPTerm desiredOutflowSum = null;
		LPTerm auxiliaryOutflowSum = null;
		LPTerm internalReactionSum = null;
		LPTerm termToMinimize = null;

		// create substance balances from internal reactions //
		TreeMap<Integer, LPTerm> balances = createSubatanceBalancesFromReactions(compartment);

		if (parameters.useMILP()) {
			
			// bind internal reaction velocities to switches and create sum of internal reaction switches //
			internalReactionSum = buildInternalReactionSwitchSum(solver, compartment);

			// add inflows //
			desiredInflowSum = buildInflowSwitchSum(solver, balances, substances.desiredInflows(), true);
			auxiliaryInflowSum = buildInflowSwitchSum(solver, balances, auxiliaryInflows);
			// add outflows //
			desiredOutflowSum = buildOutflowSwitchSum(solver, balances, substances.desiredOutFlows(),true);
			auxiliaryOutflowSum = buildOutflowSwitchSum(solver, balances, auxiliaryOutflows);			

		} else { // not using MILP:
			
			// create sum of internal reactions //
			internalReactionSum = buildInternalReactionSum(compartment);
			
			// add inflows //	
			desiredInflowSum = buildInflowSum(solver,balances, substances.desiredInflows(),true);
			auxiliaryInflowSum = buildInflowSum(solver,balances, auxiliaryInflows);			
			
			// add outflows //
			desiredOutflowSum = buildOutflowSum(solver,balances,substances.desiredOutFlows(),true);
			auxiliaryOutflowSum = buildOutflowSum(solver,balances, auxiliaryOutflows);						
		}

		LPTerm boundaryFlows=new LPSum(parameters.auxiliaryInflowWeight(), auxiliaryInflowSum, parameters.auxiliaryOutflowWeight(), auxiliaryOutflowSum);
		LPTerm auxiliaryFlows=new LPSum(parameters.reactionWeight(), internalReactionSum, boundaryFlows);
		
		LPTerm desiredFlows=new LPSum(parameters.desiredInflowWeight(), desiredInflowSum, parameters.desiredOutflowWeight(), desiredOutflowSum);
		termToMinimize = new LPDiff(auxiliaryFlows, desiredFlows);

		// TODO: von hier an überarbeiten

		int number = 0;
		for (OptimizationSolution solution : solutions) {
			if (parameters.useMILP()) {
				double sum = 0;
				LPTerm sumTerm = null;
				for (Entry<Integer, Double> entry : solution.inflows().entrySet()) {
					sum += entry.getValue();
					sumTerm = new LPSum(sumTerm, Binding.inflowSwitch(entry.getKey()));
				}
				for (Entry<Integer, Double> entry : solution.outflows().entrySet()) {
					sum += entry.getValue();
					sumTerm = new LPSum(sumTerm, Binding.inflowSwitch(entry.getKey()));
				}
				for (Entry<Integer, Double> entry : solution.forwardReactions().entrySet()) {
					sum += entry.getValue();
					sumTerm = new LPSum(sumTerm, Binding.forwardSwitch(entry.getKey()));
				}
				for (Entry<Integer, Double> entry : solution.backwardReactions().entrySet()) {
					sum += entry.getValue();
					sumTerm = new LPSum(sumTerm, Binding.backwardSwitch(entry.getKey()));
				}
				LPCondition solutionExclusion = new LPCondition(sumTerm, LPCondition.LESS_THEN, sum);
				solutionExclusion.setComment("Exclude Solution #" + (++number));
				solver.addCondition(solutionExclusion);
			} else {
				double sum = -0.0001;
				LPTerm sumTerm = null;
				for (Entry<Integer, Double> entry : solution.inflows().entrySet()) {
					sum += entry.getValue();
					sumTerm = new LPSum(sumTerm, inflow(entry.getKey()));
				}
				for (Entry<Integer, Double> entry : solution.outflows().entrySet()) {
					sum += entry.getValue();
					sumTerm = new LPSum(sumTerm, outflow(entry.getKey()));
				}
				for (Entry<Integer, Double> entry : solution.forwardReactions().entrySet()) {
					sum += entry.getValue();
					sumTerm = new LPSum(sumTerm, forward(entry.getKey()));
				}
				for (Entry<Integer, Double> entry : solution.backwardReactions().entrySet()) {
					sum += entry.getValue();
					sumTerm = new LPSum(sumTerm, backward(entry.getKey()));
				}
				LPCondition solutionExclusion = new LPCondition(sumTerm, LPCondition.LESS_THEN, sum);
				solutionExclusion.setComment("Exclude Solution #" + (++number));
				solver.addCondition(solutionExclusion);
			}
		}

		for (Entry<Integer, LPTerm> balance : balances.entrySet()) {
			LPTerm term = balance.getValue();
			int var = balance.getKey();
			LPCondition cond = new LPCondition(term, LPCondition.EQUAL, 0.0);
			cond.setComment("Balance for Substance " + var);
			solver.addCondition(cond);
		}
		solver.minimize(termToMinimize);
		solver.setTaskfileName(filename().replace(" ", "_").replace(":", "."));
		solver.start();

		OptimizationSolution solution = createSolution(solver);
		Tools.endMethod(solution);
		return solution;
	}
	
	LPTerm simpleSum(LPTerm term1, LPTerm term2) {
		if (term1 == null) return term2;
		if (term2 == null) return term1;
		return new LPSum(term1, term2);
	}

	private LPTerm buildInflowSum(LPSolveWrapper solver, TreeMap<Integer, LPTerm> balances, TreeSet<Integer> inflows) {
		return buildInflowSum(solver,balances,inflows,false);
	}
	private LPTerm buildInflowSum(LPSolveWrapper solver, TreeMap<Integer, LPTerm> balances, TreeSet<Integer> inflows, boolean isDesired) {
		Tools.startMethod("addInflows(...)"); // TODO: correct startMethod calls (ALL!)
		LPTerm result = null;

		for (Integer sid : inflows) {
			LPVariable inflow = addBoundaryFlow(balances, sid, INFLOW); // adds the inflow to the balance of the substance
			result = simpleSum(result, inflow); // adds the inflow to the sum of inflows
			if (isDesired) solver.addCondition(new LPCondition(inflow, LPCondition.GREATER_THEN, 1.0)); // force this substance to be consumed
		}
		Tools.endMethod(result);

		return result;
	}
	private LPTerm buildInflowSwitchSum(LPSolveWrapper solver, TreeMap<Integer, LPTerm> balances, TreeSet<Integer> inflows) {
		return buildInflowSwitchSum(solver, balances, inflows, false);
	}	
	private LPTerm buildInflowSwitchSum(LPSolveWrapper solver, TreeMap<Integer, LPTerm> balances, TreeSet<Integer> inflows,boolean desired) {
		Tools.startMethod("addInflows(...)"); // TODO: correct startMethod calls (ALL!)
		LPTerm result = null;
		for (Integer sid : inflows) {
			LPVariable inflow = addBoundaryFlow(balances, sid, INFLOW); // adds the inflow to the balance of the substance
			Binding inflowBinding=new Binding(inflow, solver); // create switch for the flow and connect it to the flow 
			result = simpleSum(result, inflowBinding.switchVar()); // add the switch to the sum of inflows
			if (desired) solver.addCondition(new LPCondition(inflow, LPCondition.GREATER_THEN, 1.0)); // force this susbtance to be consumed
		}
		Tools.endMethod(result);
		return result;
	}



  private LPTerm buildOutflowSum(LPSolveWrapper solver, TreeMap<Integer, LPTerm> balances, TreeSet<Integer> outflows) {
		return buildOutflowSum(solver,balances, outflows,false);
	}  
	private LPTerm buildOutflowSum(LPSolveWrapper solver, TreeMap<Integer, LPTerm> balances, TreeSet<Integer> outflows,boolean addCondition) {
		Tools.startMethod("addOutflows(...)");
		LPTerm result = null;
		for (Integer sid : outflows) {
			LPVariable outflow = addBoundaryFlow(balances, sid, OUTFLOW); // adds the outflow to the balance of the substance			
			result = simpleSum(result, outflow); // adds the outflow to the sum of outflows
			if (addCondition) solver.addCondition(new LPCondition(outflow, LPCondition.GREATER_THEN, 1.0)); // force this substance to be produced
		}
		Tools.endMethod(result);
		return result;
	}
	private LPTerm buildOutflowSwitchSum(LPSolveWrapper solver, TreeMap<Integer, LPTerm> balances, TreeSet<Integer> outflows) {
		return buildOutflowSwitchSum(solver, balances, outflows, false);
	}
	private LPTerm buildOutflowSwitchSum(LPSolveWrapper solver, TreeMap<Integer, LPTerm> balances, TreeSet<Integer> outflows,boolean desired) {

		Tools.startMethod("addInflows(...)"); // TODO: correct startMethod calls (ALL!)
		LPTerm result = null;
		for (Integer sid : outflows) {
			LPVariable outflow = addBoundaryFlow(balances, sid, OUTFLOW); // adds the inflow to the balance of the substance
			Binding outflowBinding=new Binding(outflow, solver); // create switch for the flow and connect it to the flow 
			result = simpleSum(result, outflowBinding.switchVar()); // add the switch to the sum of inflows
			if (desired) solver.addCondition(new LPCondition(outflow, LPCondition.GREATER_THEN, 1.0)); // force this susbtance to be consumed
		}
		Tools.endMethod(result);
		return result;
	}

	private TreeMap<Integer, LPTerm> createSubatanceBalancesFromReactions(Compartment compartment) throws DataFormatException, SQLException {
		Tools.startMethod("createBasicBalances(" + compartment + ")");
		TreeMap<Integer, LPTerm> substanceBalances = new TreeMap<Integer, LPTerm>(ObjectComparator.get());
		for (Integer reactionID : compartment.reactions()) { /* create terms for substances according to reactions */
			DbReaction reaction = DbReaction.load(reactionID);
			if (parameters.ignoreUnbalanced() && !reaction.isBalanced()) continue;
			if (reaction.firesForwardIn(compartment)) addForwardVelocity(reaction, substanceBalances);
			if (reaction.firesBackwardIn(compartment)) addBackwardVelocity(reaction, substanceBalances);
		}
		Tools.endMethod(substanceBalances);
		return substanceBalances;
	}
		
private LPTerm buildInternalReactionSum(DbCompartment compartment) throws DataFormatException, SQLException {
		LPTerm reactionSum = null;
		for (Integer rid : compartment.reactions()) { /* create terms for substances according to reactions */
			DbReaction reaction = DbReaction.load(rid);
			if (parameters.ignoreUnbalanced() && !reaction.isBalanced()) continue;
			if (reaction.firesForwardIn(compartment)) reactionSum = simpleSum(reactionSum, forward(rid));
			if (reaction.firesBackwardIn(compartment)) reactionSum = simpleSum(reactionSum, backward(rid));
		}
		return reactionSum;
	}
	private LPTerm buildInternalReactionSwitchSum(LPSolveWrapper solver, Compartment compartment) throws SQLException, DataFormatException {		
		LPTerm result = null;
		
		for (int rid : compartment.reactions()) {			
			DbReaction reaction = DbReaction.load(rid);
			if (parameters.ignoreUnbalanced() && !reaction.isBalanced()) continue;
			if (reaction.firesForwardIn(compartment)){
				Binding binding = new Binding(forward(rid),solver);
				result = simpleSum(result, binding.switchVar());
			}
			
			if (reaction.firesBackwardIn(compartment)) {
				Binding binding=new Binding(backward(rid),solver);
				result = simpleSum(result, binding.switchVar());
			}		
		}
		return result;
	}
	


	
	/**
	 * actually create a solution object. this method may be overwritten
	 * 
	 * @param solver the solver handle
	 * @return the solution object
	 */
	protected OptimizationSolution createSolution(LPSolveWrapper solver) {
		OptimizationSolution solution = new OptimizationSolution();

		for (Entry<LPVariable, Double> entry : solver.getSolution().entrySet()) {
			double val = entry.getValue();
			if (val != 0.0) {
				String key = entry.getKey().toString();
				if (key.startsWith("O")) solution.addOutflow(Integer.parseInt(key.substring(1)), val);
				if (key.startsWith("I")) solution.addInflow(Integer.parseInt(key.substring(1)), val);
				if (key.startsWith("F")) solution.addForwardReaction(Integer.parseInt(key.substring(1)), val);
				if (key.startsWith("B")) solution.addBackwardReaction(Integer.parseInt(key.substring(1)), val);
			}
		}
		return solution;
	}





	private LPVariable addBoundaryFlow(TreeMap<Integer, LPTerm> balances, Integer sid, boolean direction) {
		Tools.startMethod("addBoundaryFlow(...," + sid + ", " + ((direction == INFLOW) ? "inflow" : "outflow") + ")");
		LPTerm balanceForSubstance = balances.get(sid);
		LPVariable flowVariable;
		if (direction == INFLOW) {
			flowVariable = inflow(sid);
			balanceForSubstance = new LPSum(flowVariable, balanceForSubstance);
		} else {
			flowVariable = outflow(sid);
			balanceForSubstance = new LPDiff(balanceForSubstance, flowVariable);
		}
		balances.put(sid, balanceForSubstance);
		Tools.endMethod(flowVariable);
		return flowVariable;
	}



	private void addBackwardVelocity(Reaction reaction, TreeMap<Integer, LPTerm> mappingFromSubstancesToTerms) {
		Tools.startMethod("addBackwardVelocity(" + reaction + ")");
		LPVariable backwardVelocity = backward(reaction.id());
		for (Entry<Integer, Integer> entry : reaction.substrates().entrySet()) {
			int substanceId = entry.getKey();
			double stoich = entry.getValue();
			if (substances.ignoredSubstances().contains(substanceId)) continue;
			LPTerm balanceForSubstance = mappingFromSubstancesToTerms.get(substanceId);
			mappingFromSubstancesToTerms.put(substanceId, new LPSum(balanceForSubstance, stoich, backwardVelocity));
			// Tools.indent("adding +"+substanceId);
		}
		for (Entry<Integer, Integer> entry : reaction.products().entrySet()) {
			int substanceId = entry.getKey();
			double stoich = entry.getValue();
			if (substances.ignoredSubstances().contains(substanceId)) continue;
			LPTerm balanceForSubstance = mappingFromSubstancesToTerms.get(substanceId);
			mappingFromSubstancesToTerms.put(substanceId, new LPDiff(balanceForSubstance, stoich, backwardVelocity));
			// Tools.indent("adding -"+substanceId);
		}
		Tools.endMethod();
	}

	private void addForwardVelocity(Reaction reaction, TreeMap<Integer, LPTerm> mappingFromSubstancesToTerms) {
		Tools.startMethod("addForwardVelocity(" + reaction + ")");
		LPVariable forwardVelocity = forward(reaction.id());
		for (Entry<Integer, Integer> entry : reaction.products().entrySet()) {
			int substanceId = entry.getKey();
			double stoich = entry.getValue();
			LPTerm balanceForSubstance = mappingFromSubstancesToTerms.get(substanceId);
			mappingFromSubstancesToTerms.put(substanceId, new LPSum(balanceForSubstance, stoich, forwardVelocity));
			// Tools.indent("adding +"+substanceId);

		}
		for (Entry<Integer, Integer> entry : reaction.substrates().entrySet()) {
			int substanceId = entry.getKey();
			double stoich = entry.getValue();
			LPTerm balanceForSubstance = mappingFromSubstancesToTerms.get(substanceId);
			mappingFromSubstancesToTerms.put(substanceId, new LPDiff(balanceForSubstance, stoich, forwardVelocity));
			// Tools.indent("adding -"+substanceId);
		}
		Tools.endMethod();
	}

	private LPVariable forward(int reactionid) {
		return new LPVariable("F" + reactionid);
	}

	private LPVariable backward(int reactionid) {
		return new LPVariable("B" + reactionid);
	}



	public static LPVariable inflow(Integer substanceId) {
		return new LPVariable("I" + substanceId);
	}

	public static LPVariable outflow(Integer substanceId) {
		return new LPVariable("O" + substanceId);
	}


	public int getCompartmentId() {
		return compartmentId;
	}

	protected String filename() {
		SimpleDateFormat formatter = new SimpleDateFormat("yy-MM-dd HH.mm.ss");
		return "OptimizationTask " + formatter.format(new Date()) + "." + getNumber() + ".lp";
	}

	public DefaultMutableTreeNode outputTree() throws SQLException {
		return new SubstanceListNode("desired output substances", substances.desiredOutFlows());
	}

	public DefaultMutableTreeNode inputTree() throws SQLException {
		return new SubstanceListNode("desired input substances", substances.desiredInflows());
	}
}