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
import edu.fsuj.csb.reactionnetworks.interaction.results.lp.LinearProgrammingResult;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.CalculationTask;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.ParameterSet;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.SubstanceSet;
import edu.fsuj.csb.reactionnetworks.organismtools.DbCompartment;
import edu.fsuj.csb.reactionnetworks.organismtools.DbReaction;
import edu.fsuj.csb.reactionnetworks.organismtools.DbSubstance;
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
	private int nextSolutionNumber = 1;
	private SubstanceSet substances;
	private ParameterSet parameters;
	TreeSet<OptimizationSolution> solutions = null; // set of (set of substances, that must be supplied)
	Integer solutionSize = null; // for monitoring the size of the solutions
	private ReactionSet additionalReactions;

	public LinearProgrammingTask(Integer compartmentId, SubstanceSet substanceSet, ReactionSet additionalReactions, ParameterSet parameterSet) {
		Tools.startMethod("new LinearProgramminTask("+compartmentId+","+substanceSet+", "+parameterSet+")");
		this.compartmentId = compartmentId;
		this.substances = substanceSet;
		this.parameters = parameterSet;
		this.additionalReactions=additionalReactions;
		Tools.endMethod();
	}

	@Override
	public void run(CalculationClient calculationClient) throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		Tools.startMethod("LinearProgramminTask.run("+calculationClient+")");
		try {
			while (true) {
				solutions = OptimizationSolution.set();
				OptimizationSolution solution = runInternal(); // start the actual calculation
				calculationClient.sendObject(new LinearProgrammingResult(this, solution));
				if (!addNewSolution(solution)) break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DataFormatException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Tools.endMethod();
	}

	protected OptimizationSolution runInternal() throws DataFormatException, SQLException, IOException, InterruptedException {
		Tools.startMethod("LinearProgramminTask.runInternal()");

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
			internalReactionSum = buildInternalReactionSwitchSum(solver, compartment,additionalReactions);

			// add inflows //
			desiredInflowSum = buildInflowSwitchSum(solver, balances, substances.desiredInflows(), true);
			auxiliaryInflowSum = buildInflowSwitchSum(solver, balances, auxiliaryInflows);
			// add outflows //
			desiredOutflowSum = buildOutflowSwitchSum(solver, balances, substances.desiredOutFlows(), true);
			auxiliaryOutflowSum = buildOutflowSwitchSum(solver, balances, auxiliaryOutflows);

		} else { // not using MILP:

			// create sum of internal reactions //
			internalReactionSum = buildInternalReactionSum(compartment,additionalReactions);

			// add inflows //
			desiredInflowSum = buildInflowSum(solver, balances, substances.desiredInflows(), true);
			auxiliaryInflowSum = buildInflowSum(solver, balances, auxiliaryInflows);

			// add outflows //
			desiredOutflowSum = buildOutflowSum(solver, balances, substances.desiredOutFlows(), true);
			auxiliaryOutflowSum = buildOutflowSum(solver, balances, auxiliaryOutflows);
		}

		LPTerm boundaryFlows = new LPSum(parameters.auxiliaryInflowWeight(), auxiliaryInflowSum, parameters.auxiliaryOutflowWeight(), auxiliaryOutflowSum);
		LPTerm auxiliaryFlows = new LPSum(parameters.reactionWeight(), internalReactionSum, boundaryFlows);

		LPTerm desiredFlows = new LPSum(parameters.desiredInflowWeight(), desiredInflowSum, parameters.desiredOutflowWeight(), desiredOutflowSum);
		termToMinimize = new LPDiff(auxiliaryFlows, desiredFlows);

		if (solutions!=null && !solutions.isEmpty()) excludeEarlierSolutions(solver);

		setBalancesToZero(solver, balances);

		solver.minimize(termToMinimize);
		solver.setTaskfileName(filename().replace(" ", "_").replace(":", "."));
		solver.start();

		OptimizationSolution solution = createSolution(solver);
		Tools.endMethod(solution);
		return solution;
	}

	public boolean addNewSolution(OptimizationSolution solution) {
		Tools.startMethod("LinearProgramminTask.addNewSolution("+solution+")");
		if (solution == null) {
			System.out.println("Solution #" + nextSolutionNumber + ": no solution found.");
			Tools.endMethod(false);
			return false;
		}
		int inflowSize = solution.inflows().size();
		if (solutionSize == null) {
			solutionSize=inflowSize;
		}else {
			if (inflowSize > solutionSize) { // if the size of the solution increases, we're getting suboptimal solutions. so: quit!
				System.out.println("found no more solution with size " + solutions.size());
				System.out.println("next solution: #" + (nextSolutionNumber) + ": Inflows " + solution.inflows() + " / Outflows " + solution.outflows());
				System.out.println("Reactions: " + solution.forwardReactions() + " - " + solution.backwardReactions());
				Tools.endMethod(false);
				return false;
			}
			if (inflowSize < solutionSize) { // normaly the size of the solutions should not decrease, as this would mean, we have found suboptimal solutions before...
				System.err.println("uh oh! found smaller solution (" + solution + "). this was not expected.");
				Tools.endMethod(false);
				return false;
			}
		}
		System.out.println("solution #" + (nextSolutionNumber++) + ": Inflows " + solution.inflows() + " / Outflows " + solution.outflows());
		System.out.println("Reactions: " + solution.forwardReactions() + " - " + solution.backwardReactions());
		solutions.add(solution); // don't allow inflow of the substances in the solution in the next turn
		Tools.endMethod(true);
		return true;
	}

	private void setBalancesToZero(LPSolveWrapper solver, TreeMap<Integer, LPTerm> balances) throws SQLException {
		Tools.startMethod("LinearProgrammingTask.setBalancesToZero("+solver+", "+balances.toString().substring(0,10)+")");
		for (Entry<Integer, LPTerm> balance : balances.entrySet()) {
			LPTerm balanceTerm = balance.getValue();
			int substanceId = balance.getKey();
			LPCondition cond = new LPCondition(balanceTerm, LPCondition.EQUAL, 0.0,"Balance for Substance " + DbSubstance.load(substanceId));
			solver.addCondition(cond);
		}
		Tools.endMethod();
	}

	private void excludeEarlierSolutions(LPSolveWrapper solver) {
		Tools.startMethod("LinearProgrammingTask.excludeEarlierSolutions("+solver+")");
		Tools.notImplemented("LinearProgrammingTask.excludeEarlierSolutions(solver)");
		Tools.endMethod();
	}

	LPTerm simpleSum(LPTerm term1, LPTerm term2) {		
		if (term1 == null) return term2;
		if (term2 == null) return term1;
		return new LPSum(term1, term2);
	}

	private LPTerm buildInflowSum(LPSolveWrapper solver, TreeMap<Integer, LPTerm> balances, TreeSet<Integer> inflows) throws SQLException {
		return buildInflowSum(solver, balances, inflows, false);
	}

	private LPTerm buildInflowSum(LPSolveWrapper solver, TreeMap<Integer, LPTerm> balances, TreeSet<Integer> inflows, boolean isDesired) throws SQLException {
		Tools.startMethod("LinearProgrammingTask.buildInflowSum("+solver+", {balances}, [inflows], "+isDesired+")");
		LPTerm result = null;

		for (Integer sid : inflows) {
			LPVariable inflow = addBoundaryFlow(balances, sid, INFLOW); // adds the inflow to the balance of the substance
			result = simpleSum(result, inflow); // adds the inflow to the sum of inflows
			if (isDesired) solver.addCondition(new LPCondition(inflow, LPCondition.GREATER_THEN, 1.0,"force inflow of "+DbSubstance.load(sid))); // force this substance to be consumed
		}
		Tools.endMethod(result);
		return result;
	}

	private LPTerm buildInflowSwitchSum(LPSolveWrapper solver, TreeMap<Integer, LPTerm> balances, TreeSet<Integer> inflows) throws SQLException {
		return buildInflowSwitchSum(solver, balances, inflows, false);
	}

	private LPTerm buildInflowSwitchSum(LPSolveWrapper solver, TreeMap<Integer, LPTerm> balances, TreeSet<Integer> inflows, boolean isDesired) throws SQLException {
		Tools.startMethod("LinearProgrammingTask.buildInflowSwitchSum("+solver+", "+balances+", "+inflows+", "+isDesired+")");
		LPTerm result = null;
		for (Integer sid : inflows) {
			LPVariable inflow = addBoundaryFlow(balances, sid, INFLOW); // adds the inflow to the balance of the substance
			Binding inflowBinding = new Binding(inflow, solver); // create switch for the flow and connect it to the flow
			result = simpleSum(result, inflowBinding.switchVar()); // add the switch to the sum of inflows
			if (isDesired) solver.addCondition(new LPCondition(inflow, LPCondition.GREATER_THEN, 1.0,"force inflow of "+DbSubstance.load(sid))); // force this susbtance to be consumed
		}
		Tools.endMethod(result);
		return result;
	}

	private LPTerm buildOutflowSum(LPSolveWrapper solver, TreeMap<Integer, LPTerm> balances, TreeSet<Integer> outflows) throws SQLException {
		return buildOutflowSum(solver, balances, outflows, false);
	}

	private LPTerm buildOutflowSum(LPSolveWrapper solver, TreeMap<Integer, LPTerm> balances, TreeSet<Integer> outflows, boolean addCondition) throws SQLException {
		Tools.startMethod("LinearProgrammingTask.buildOutflowSum("+solver+", "+balances+", "+outflows+", "+addCondition+")");
		LPTerm result = null;
		for (Integer sid : outflows) {
			LPVariable outflow = addBoundaryFlow(balances, sid, OUTFLOW); // adds the outflow to the balance of the substance
			result = simpleSum(result, outflow); // adds the outflow to the sum of outflows
			if (addCondition) solver.addCondition(new LPCondition(outflow, LPCondition.GREATER_THEN, 1.0,"force outflow of "+DbSubstance.load(sid))); // force this substance to be produced
		}
		Tools.endMethod(result);
		return result;
	}

	private LPTerm buildOutflowSwitchSum(LPSolveWrapper solver, TreeMap<Integer, LPTerm> balances, TreeSet<Integer> outflows) throws SQLException {
		return buildOutflowSwitchSum(solver, balances, outflows, false);
	}

	private LPTerm buildOutflowSwitchSum(LPSolveWrapper solver, TreeMap<Integer, LPTerm> balances, TreeSet<Integer> outflows, boolean isDesired) throws SQLException {

		Tools.startMethod("LinearProgrammingTask.buildOutflowSwitchSum("+solver+", "+balances+", "+outflows+", "+isDesired+")");
		LPTerm result = null;
		for (Integer sid : outflows) {
			LPVariable outflow = addBoundaryFlow(balances, sid, OUTFLOW); // adds the inflow to the balance of the substance
			Binding outflowBinding = new Binding(outflow, solver); // create switch for the flow and connect it to the flow
			result = simpleSum(result, outflowBinding.switchVar()); // add the switch to the sum of inflows
			if (isDesired) solver.addCondition(new LPCondition(outflow, LPCondition.GREATER_THEN, 1.0,"force outflow of "+DbSubstance.load(sid))); // force this susbtance to be consumed
		}
		Tools.endMethod(result);
		return result;
	}

	private TreeMap<Integer, LPTerm> createSubatanceBalancesFromReactions(Compartment compartment) throws DataFormatException, SQLException {
		Tools.startMethod("LinearProgrammingTask.createSubatanceBalancesFromReactions("+compartment+")");
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

	private LPTerm buildInternalReactionSum(DbCompartment compartment,ReactionSet additionalReactions) throws DataFormatException, SQLException {
		Tools.startMethod("LinearProgrammingTask.buildInternalReactionSum("+compartment+")");
		LPTerm reactionSum = null;
		ReactionSet reactions = compartment.reactions().clone();
		reactions.addAll(additionalReactions);
		for (Integer rid : reactions) { /* create terms for substances according to reactions */
			DbReaction reaction = DbReaction.load(rid);
			if (parameters.ignoreUnbalanced() && !reaction.isBalanced()) continue;
			if (reaction.firesForwardIn(compartment)) reactionSum = simpleSum(reactionSum, forward(rid));
			if (reaction.firesBackwardIn(compartment)) reactionSum = simpleSum(reactionSum, backward(rid));
		}
		Tools.endMethod(reactionSum);
		return reactionSum;
	}

	private LPTerm buildInternalReactionSwitchSum(LPSolveWrapper solver, Compartment compartment, ReactionSet additionalReactions) throws SQLException, DataFormatException {
		Tools.startMethod("LinearProgrammingTask.buildInternalReactionSwitchSum("+solver+", "+compartment+")");
		LPTerm result = null;
		
		ReactionSet reactions = compartment.reactions().clone();
		reactions.addAll(additionalReactions.get());
		for (int rid : reactions.get()) {
			DbReaction reaction = DbReaction.load(rid);
			if (parameters.ignoreUnbalanced() && !reaction.isBalanced()) continue;
			if (reaction.firesForwardIn(compartment)) {
				Binding binding = new Binding(forward(rid), solver);
				result = simpleSum(result, binding.switchVar());
			}

			if (reaction.firesBackwardIn(compartment)) {
				Binding binding = new Binding(backward(rid), solver);
				result = simpleSum(result, binding.switchVar());
			}
		}
		Tools.endMethod(result);
		return result;
	}

	/**
	 * actually create a solution object. this method may be overwritten
	 * 
	 * @param solver the solver handle
	 * @return the solution object
	 */
	protected OptimizationSolution createSolution(LPSolveWrapper solver) {
		Tools.startMethod("LinearProgrammingTask.createSolution("+solver+")");
		OptimizationSolution result = null;
		TreeMap<LPVariable, Double> solution = solver.getSolution();
		if (solution!=null){
			result=new OptimizationSolution();
			for (Entry<LPVariable, Double> entry : solution.entrySet()) {
				double val = entry.getValue();
				if (val != 0.0) {
					String key = entry.getKey().toString();
					if (key.startsWith("O")) result.addOutflow(Integer.parseInt(key.substring(1)), val);
					if (key.startsWith("I")) result.addInflow(Integer.parseInt(key.substring(1)), val);
					if (key.startsWith("F")) result.addForwardReaction(Integer.parseInt(key.substring(1)), val);
					if (key.startsWith("B")) result.addBackwardReaction(Integer.parseInt(key.substring(1)), val);
				}
			}
		}
		Tools.endMethod(result==null?null:result.toString().substring(0,40));
		return result;
	}

	private LPVariable addBoundaryFlow(TreeMap<Integer, LPTerm> balances, Integer sid, boolean direction) {
		//Tools.startMethod("addBoundaryFlow(...," + sid + ", " + ((direction == INFLOW) ? "inflow" : "outflow") + ")");
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
		//Tools.endMethod(flowVariable);
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