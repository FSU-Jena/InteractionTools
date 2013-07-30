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

		/* create reaction balances */

		TreeMap<Integer, LPTerm> balances = createSubatanceBalancesFromReactions(compartment);

		TreeSet<Integer> allSubstances = compartment.utilizedSubstances();

		TreeSet<Integer> auxiliaryInflows = substances.calculateAuxiliaryInflows(allSubstances);
		TreeSet<Integer> auxiliaryOutflows = substances.calculateAuxiliaryOutflows(allSubstances);
		TreeSet<Integer> possibleInflows = substances.possibleInflows(allSubstances);
		TreeSet<Integer> possibleOutFlows = substances.possibleOutflows(allSubstances);

		/* add inflows */
		
		TreeSet<Binding> inflowBindings = addInflows(possibleInflows,balances);

		/* add outflows */

		TreeSet<Binding> outflowBindings = addOutflows(possibleOutFlows, balances);

		LPTerm desiredInflowSum = null;
		LPTerm auxiliaryInflowSum = null;
		LPTerm desiredOutflowSum = null;
		LPTerm auxiliaryOutflowSum = null;
		LPTerm internalReactionSum = null;
		LPTerm termToMinimize = null;

		if (parameters.useMILP()) { // TODO: Dieser Abschnitt muss überarbeitet werden

			
			/* bind reaction velocities to reaction switches and create sum of reaction switches */
			internalReactionSum = buildReactionSwitchSum(solver, compartment);

			/* bin inflow reaction velocities to their switches and create sum of inflow reaction switches */
			desiredInflowSum = buildInflowSwitchSum(solver, inflowBindings);

			/* bin outflow reaction velocities to their switches and create sum of outflow reaction switches */
			desiredOutflowSum = buildOutflowSwitchSum(solver, outflowBindings);



		} else { // not using MILP:
			internalReactionSum = buildInternalReactionSum(solver, compartment);
			desiredInflowSum = buildInflowSum(solver, substances.desiredInflows(),true);
			auxiliaryInflowSum = buildInflowSum(solver, auxiliaryInflows);			
			desiredOutflowSum = buildOutflowSum(solver,substances.desiredOutFlows(),true);
			auxiliaryOutflowSum = buildOutflowSum(solver, auxiliaryOutflows);
		}

		LPTerm auxiliaryBoundaryFlows=new LPSum(parameters.auxiliaryInflowWeight(), auxiliaryInflowSum, parameters.auxiliaryOutflowWeight(), auxiliaryOutflowSum);
		LPTerm auxiliaryFlows=new LPSum(parameters.reactionWeight(), internalReactionSum, auxiliaryBoundaryFlows);
		
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
					sumTerm = new LPSum(sumTerm, inflowSwitch(entry.getKey()));
				}
				for (Entry<Integer, Double> entry : solution.outflows().entrySet()) {
					sum += entry.getValue();
					sumTerm = new LPSum(sumTerm, inflowSwitch(entry.getKey()));
				}
				for (Entry<Integer, Double> entry : solution.forwardReactions().entrySet()) {
					sum += entry.getValue();
					sumTerm = new LPSum(sumTerm, forwardSwitch(entry.getKey()));
				}
				for (Entry<Integer, Double> entry : solution.backwardReactions().entrySet()) {
					sum += entry.getValue();
					sumTerm = new LPSum(sumTerm, backwardSwitch(entry.getKey()));
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

	private LPTerm buildOutflowSum(LPSolveWrapper solver, TreeSet<Integer> allSubstances) {
		return buildOutflowSum(solver,allSubstances,false);
	}
	private LPTerm buildOutflowSum(LPSolveWrapper solver, TreeSet<Integer> allSubstances,boolean addCondition) {
		LPTerm result = null;
		for (Integer sid : allSubstances) {
			LPVariable outflow = outflow(sid);
			if (addCondition) solver.addCondition(new LPCondition(outflow, LPCondition.GREATER_THEN, 1.0)); // force this substance to be consumed
			result = simpleSum(result, outflow);
		}
		return result;
	}
	
	private LPTerm buildInflowSum(LPSolveWrapper solver, TreeSet<Integer> inflows) {
		return buildInflowSum(solver,inflows,false);
	}

	private LPTerm buildInflowSum(LPSolveWrapper solver, TreeSet<Integer> inflows, boolean addCondition) {
		LPTerm result = null;
		for (Integer sid : inflows) {
			LPVariable inflow = inflow(sid);
			if (addCondition) solver.addCondition(new LPCondition(inflow, LPCondition.GREATER_THEN, 1.0)); // force this substance to be consumed
			result = simpleSum(result, inflow);
		}
		return result;
	}

	LPTerm simpleSum(LPTerm term1, LPTerm term2) {
		if (term1 == null) return term2;
		if (term2 == null) return term1;
		return new LPSum(term1, term2);
	}

	private LPTerm buildInternalReactionSum(LPSolveWrapper solver, DbCompartment compartment) throws DataFormatException {
		LPTerm result = null;
		for (Integer rid : compartment.reactions()) { /* create terms for substances according to reactions */
			Reaction reaction = Reaction.get(rid);
			if (parameters.ignoreUnbalanced() && !reaction.isBalanced()) continue;
			if (reaction.firesForwardIn(compartment)) result = simpleSum(result, forward(rid));
			if (reaction.firesBackwardIn(compartment)) result = simpleSum(result, backward(rid));
		}
		return result;
	}

	private LPTerm buildReactionSwitchSum(LPSolveWrapper solver, Compartment compartment) throws SQLException {
		LPTerm result = null;
		for (Binding reactionBinding : reactionBindings(compartment)) { /* create terms for substances according to reactions */
			solver.addCondition(reactionBinding.lowerLimit());
			solver.addCondition(reactionBinding.upperLimit());
			solver.addBinVar(reactionBinding.switchVar());
			result = simpleSum(result, reactionBinding.switchVar());
		}
		return result;
	}

	private LPTerm buildOutflowSwitchSum(LPSolveWrapper solver, TreeSet<Binding> outflowBindings) {
		LPTerm result = null;
		for (Binding outflowBinding : outflowBindings) {
			solver.addCondition(outflowBinding.lowerLimit());
			solver.addCondition(outflowBinding.upperLimit());
			solver.addBinVar(outflowBinding.switchVar());
			result = simpleSum(result, outflowBinding.switchVar());
		}
		/* force desired outflows */
		for (Integer sid : substances.desiredOutFlows())
			solver.addCondition(new LPCondition(outflow(sid), LPCondition.GREATER_THEN, 1.0));

		return result;
	}

	private LPTerm buildInflowSwitchSum(LPSolveWrapper solver, TreeSet<Binding> inflowBindings) {
		LPTerm result = null;
		for (Binding inflowBinding : inflowBindings) {
			solver.addCondition(inflowBinding.lowerLimit());
			solver.addCondition(inflowBinding.upperLimit());
			solver.addBinVar(inflowBinding.switchVar());
			result = simpleSum(result, inflowBinding.switchVar());
		}
		/* force desired inflows */
		for (Integer sid : substances.desiredInflows()) solver.addCondition(new LPCondition(inflow(sid), LPCondition.GREATER_THEN, 1.0));
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

	private TreeSet<Binding> reactionBindings(Compartment compartment) throws SQLException {
		TreeSet<Binding> reactionBindings = Binding.set();
		for (int reactionId : compartment.reactions()) {
			DbReaction reaction = DbReaction.load(reactionId);
			if (reaction.firesForwardIn(compartment)) reactionBindings.add(new Binding(forward(reactionId), forwardSwitch(reactionId)));
			if (reaction.firesBackwardIn(compartment)) reactionBindings.add(new Binding(backward(reactionId), backwardSwitch(reactionId)));
		}
		return reactionBindings;
	}

	private TreeSet<Binding> addOutflows(TreeSet<Integer> possibleOutflows, TreeMap<Integer, LPTerm> balances) {
		Tools.startMethod("addOutflows(...)");
		TreeSet<Binding> bindings = Binding.set();
		for (Integer sid : possibleOutflows) {
			LPVariable outflow = addBoundaryFlow(balances, sid, OUTFLOW);
			if (parameters.useMILP()) {
				LPVariable outflowSwitch = outflowSwitch(sid);
				bindings.add(new Binding(outflow, outflowSwitch));
			}
		}
		Tools.endMethod(bindings);
		return bindings;
	}

	private TreeSet<Binding> addInflows(TreeSet<Integer> possibleInflows, TreeMap<Integer, LPTerm> balances) {
		Tools.startMethod("addInflows(...)");
		TreeSet<Binding> bindings = Binding.set();
		for (Integer sid : possibleInflows) {
			LPVariable inflow = addBoundaryFlow(balances, sid, INFLOW);
			if (parameters.useMILP()) {
				LPVariable inflowSwitch = inflowSwitch(sid);
				bindings.add(new Binding(inflow, inflowSwitch));
			}
		}
		Tools.endMethod(bindings);
		return bindings;
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

	private TreeMap<Integer, LPTerm> createSubatanceBalancesFromReactions(Compartment compartment) throws DataFormatException {
		Tools.startMethod("createBasicBalances(" + compartment + ")");
		TreeMap<Integer, LPTerm> substanceBalances = new TreeMap<Integer, LPTerm>(ObjectComparator.get());
		ReactionSet reactions = compartment.reactions();
		for (Integer reactionID : reactions) { /* create terms for substances according to reactions */
			Reaction reaction = Reaction.get(reactionID);
			if (parameters.ignoreUnbalanced() && !reaction.isBalanced()) continue;

			if (reaction.firesForwardIn(compartment)) addForwardVelocity(reaction, substanceBalances);
			if (reaction.firesBackwardIn(compartment)) addBackwardVelocity(reaction, substanceBalances);
		}
		Tools.endMethod(substanceBalances);
		return substanceBalances;
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
			if (substances.ignoredSubstances().contains(substanceId)) continue;
			LPTerm balanceForSubstance = mappingFromSubstancesToTerms.get(substanceId);
			mappingFromSubstancesToTerms.put(substanceId, new LPSum(balanceForSubstance, stoich, forwardVelocity));
			// Tools.indent("adding +"+substanceId);

		}
		for (Entry<Integer, Integer> entry : reaction.substrates().entrySet()) {
			int substanceId = entry.getKey();
			double stoich = entry.getValue();
			if (substances.ignoredSubstances().contains(substanceId)) continue;
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

	private LPVariable forwardSwitch(int reactionid) {
		return new LPVariable("sF" + reactionid);
	}

	private LPVariable backwardSwitch(int reactionid) {
		return new LPVariable("sB" + reactionid);
	}

	public static LPVariable inflow(Integer substanceId) {
		return new LPVariable("I" + substanceId);
	}

	public static LPVariable outflow(Integer substanceId) {
		return new LPVariable("O" + substanceId);
	}

	public static LPVariable inflowSwitch(Integer substanceId) {
		return new LPVariable("sI" + substanceId);
	}

	public static LPVariable outflowSwitch(Integer substanceId) {
		return new LPVariable("sO" + substanceId);
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