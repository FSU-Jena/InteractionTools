package edu.fsuj.csb.reactionnetworks.interaction.tasks;

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

public class OptimizationTask extends CalculationTask {

	private static final long serialVersionUID = -8309620489674531282L;
	private static final boolean INFLOW = true;
	private static final boolean OUTFLOW = false;
	private TreeSet<Integer> substancesThatShallBeConsumed;
	private TreeSet<Integer> substancesThatShallBeProduced;
	private TreeSet<Integer> substancesThatShallNotBeProduced;
	private TreeSet<Integer> substancesThatShallNotBeConsumed;
	private TreeSet<Integer> substancesThatShallNotBeBalanced;
	private int compartmentId;
	private boolean useMILP = true;
	private boolean ignoreUnbalanced;
	private Double inflowWeight=1.0;
	private Double outflowWeight=1.0;
	private Double reactionWeight=1.0;
	private static Double limit = 100000.0;

	/**
	 * @param cid the id of the compartment, on which calculations are done
	 * @param consume the set of ids of substances, that shall be degraded/consumed
	 * @param produce the set of ids of substances, that shall be produced
	 * @param noConsume the set of ids of substances, that shall not be taken up
	 * @param noProduce the set of ids of substances, that shall not be produced
	 * @param ignoredSubstances the set of ids that shall not be used at all
	 * @param useMILP determines, whether the slower MILP shall be used
	 * @param ignoreUnbalancedReactions determines, whether unbalanced reactions are allowed
	 */
	public OptimizationTask(int cid, TreeSet<Integer> consume, TreeSet<Integer> produce, TreeSet<Integer> noConsume, TreeSet<Integer> noProduce, TreeSet<Integer> ignoredSubstances, boolean useMILP, boolean ignoreUnbalancedReactions) {
		substancesThatShallBeConsumed = nonNullSet(consume);
		substancesThatShallBeProduced = nonNullSet(produce);
		substancesThatShallNotBeConsumed = nonNullSet(noConsume);
		substancesThatShallNotBeProduced = nonNullSet(noProduce);
		substancesThatShallNotBeBalanced = nonNullSet(ignoredSubstances);
		compartmentId = cid;
		this.useMILP = useMILP;
		ignoreUnbalanced = ignoreUnbalancedReactions;
	}

	private static TreeSet<Integer> nonNullSet(TreeSet<Integer> set) {
		if (set==null) return new TreeSet<Integer>();
	  return set;
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
				break; // TODO: remove
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
		Tools.startMethod("runInternal("+solutions+")");
		
		/* prepare */
		LPSolveWrapper solver = new LPSolveWrapper();
		DbCompartment compartment = DbCompartment.load(compartmentId);
		
		/* create reaction balances */

		TreeMap<Integer, LPTerm> balances = createReactionBalances(compartment);

		TreeSet<Integer> allSubstances = compartment.utilizedSubstances();
		
		/* add inflows and outflows */

		TreeSet<Binding> inflowBindings = addInflows(allSubstances, balances);
		TreeSet<Binding> outflowBindings = addOutflows(allSubstances, balances);
		
		LPTerm inflowsTerm=null;
		LPTerm reactionsTerm=null;
		LPTerm outflowsTerm=null;
		LPTerm termToMinimize=null;
				
		if (useMILP) {

			for (Binding reactionBinding:reactionBindings(compartment)){ /* create terms for substances according to reactions */
				solver.addCondition(reactionBinding.lowerLimit());
				solver.addCondition(reactionBinding.upperLimit());
				solver.addBinVar(reactionBinding.switchVar());
				reactionsTerm=new LPSum(reactionsTerm, reactionBinding.switchVar());
			}
			

			for (Binding inflowBinding:inflowBindings){
				solver.addCondition(inflowBinding.lowerLimit());
				solver.addCondition(inflowBinding.upperLimit());
				solver.addBinVar(inflowBinding.switchVar());
				inflowsTerm=new LPSum(inflowsTerm, inflowBinding.switchVar());
			}
			

			for (Binding outflowBinding:outflowBindings){
				solver.addCondition(outflowBinding.lowerLimit());
				solver.addCondition(outflowBinding.upperLimit());
				solver.addBinVar(outflowBinding.switchVar());
				outflowsTerm=new LPSum(inflowsTerm, outflowBinding.switchVar());
			}
			
			/* force desired inflows and outflows */
			for (Integer sid : substancesThatShallBeConsumed)	solver.addCondition(new LPCondition(inflow(sid), LPCondition.GREATER_THEN, 1.0));
			for (Integer sid : substancesThatShallBeProduced)	solver.addCondition(new LPCondition(outflow(sid), LPCondition.GREATER_THEN, 1.0));
			
			
			
		} else { // not using MILP:
			for (Integer rid:compartment.reactions()){ /* create terms for substances according to reactions */
				Reaction reaction = Reaction.get(rid);
				if (ignoreUnbalanced && !reaction.isBalanced()) continue;

				if (reaction.firesForwardIn(compartment)) reactionsTerm=new LPSum(reactionsTerm, forward(rid));
				if (reaction.firesBackwardIn(compartment)) reactionsTerm=new LPSum(reactionsTerm, backward(rid));
			}
			
			for (Integer sid : allSubstances)	{
				LPVariable inflow = inflow(sid);
				LPVariable outflow = outflow(sid);

				if (substancesThatShallBeConsumed.contains(sid)){
					solver.addCondition(new LPCondition(inflow, LPCondition.GREATER_THEN, 1.0));				
					inflowsTerm=new LPDiff(inflowsTerm, inflow);
				} else {
					if (!substancesThatShallNotBeConsumed.contains(sid)) inflowsTerm=new LPSum(inflowsTerm, inflow);
				}
				
				if (substancesThatShallBeProduced.contains(sid)){
					solver.addCondition(new LPCondition(outflow, LPCondition.GREATER_THEN, 1.0));
					outflowsTerm=new LPDiff(outflowsTerm, outflow);
				} else {
					if (!substancesThatShallNotBeProduced.contains(sid)) outflowsTerm=new LPSum(outflowsTerm, outflow);
				}
			}
		}
		
		termToMinimize=new LPSum(new LPSum(inflowWeight,inflowsTerm, outflowWeight,outflowsTerm), reactionWeight,reactionsTerm);
		
		int number=0;
		for (OptimizationSolution solution:solutions){
			if (useMILP){
				double sum=0;
				LPTerm sumTerm=null;
				for (Entry<Integer, Double> entry:solution.inflows().entrySet()){
					sum+=entry.getValue();
					sumTerm=new LPSum(sumTerm, inflowSwitch(entry.getKey()));					
				}
				for (Entry<Integer, Double> entry:solution.outflows().entrySet()){
					sum+=entry.getValue();
					sumTerm=new LPSum(sumTerm, inflowSwitch(entry.getKey()));					
				}
				for (Entry<Integer, Double> entry:solution.forwardReactions().entrySet()){
					sum+=entry.getValue();
					sumTerm=new LPSum(sumTerm, forwardSwitch(entry.getKey()));					
				}
				for (Entry<Integer, Double> entry:solution.backwardReactions().entrySet()){
					sum+=entry.getValue();
					sumTerm=new LPSum(sumTerm, backwardSwitch(entry.getKey()));					
				}
				LPCondition solutionExclusion = new LPCondition(sumTerm,LPCondition.LESS_THEN,sum);
				solutionExclusion.setComment("Exclude Solution #"+(++number));
				solver.addCondition(solutionExclusion);
			} else {
				double sum=-0.0001;
				LPTerm sumTerm=null;
				for (Entry<Integer, Double> entry:solution.inflows().entrySet()){
					sum+=entry.getValue();
					sumTerm=new LPSum(sumTerm, inflow(entry.getKey()));					
				}
				for (Entry<Integer, Double> entry:solution.outflows().entrySet()){
					sum+=entry.getValue();
					sumTerm=new LPSum(sumTerm, outflow(entry.getKey()));					
				}
				for (Entry<Integer, Double> entry:solution.forwardReactions().entrySet()){
					sum+=entry.getValue();
					sumTerm=new LPSum(sumTerm, forward(entry.getKey()));					
				}
				for (Entry<Integer, Double> entry:solution.backwardReactions().entrySet()){
					sum+=entry.getValue();
					sumTerm=new LPSum(sumTerm, backward(entry.getKey()));					
				}
				LPCondition solutionExclusion = new LPCondition(sumTerm,LPCondition.LESS_THEN,sum);
				solutionExclusion.setComment("Exclude Solution #"+(++number));
				solver.addCondition(solutionExclusion);
			}
		}

		for (Entry<Integer, LPTerm> balance:balances.entrySet()) {
			LPTerm term = balance.getValue();
			int var=balance.getKey();
			LPCondition cond=new LPCondition(term, LPCondition.EQUAL, 0.0);
			cond.setComment("Balance for Substance "+var);
			solver.addCondition(cond);
		}
		solver.minimize(termToMinimize);
		solver.setTaskfileName(filename().replace(" ", "_").replace(":", "."));
		solver.start();

		OptimizationSolution solution = createSolution(solver);
		Tools.endMethod(solution);
		return solution;
	}

	/**
	 * actually create a solution object. this method may be overwritten
	 * @param solver the solver handle
	 * @return the solution object
	 */
	protected OptimizationSolution createSolution(LPSolveWrapper solver) {
		OptimizationSolution solution = new OptimizationSolution();
		
		if (solver.getSolution().isEmpty()) return null;
		for (Entry<LPVariable, Double> entry:solver.getSolution().entrySet()){
			double val=entry.getValue();
			if (val!=0.0) {
				String key=entry.getKey().toString();
				if (key.startsWith("O")) solution.addOutflow(Integer.parseInt(key.substring(1)),val);
				if (key.startsWith("I")) solution.addInflow(Integer.parseInt(key.substring(1)),val);
				if (key.startsWith("F")) solution.addForwardReaction(Integer.parseInt(key.substring(1)),val);
				if (key.startsWith("B")) solution.addBackwardReaction(Integer.parseInt(key.substring(1)),val);
			}
		}
	  return solution; 
  }

	private TreeSet<Binding> reactionBindings(Compartment compartment) throws SQLException {
	  TreeSet<Binding> reactionBindings=Binding.set();		
		for (int reactionId:compartment.reactions()){
			DbReaction reaction=DbReaction.load(reactionId);
			if (reaction.firesForwardIn(compartment)) reactionBindings.add(new Binding(forward(reactionId), forwardSwitch(reactionId)));
			if (reaction.firesBackwardIn(compartment)) reactionBindings.add(new Binding(backward(reactionId), backwardSwitch(reactionId)));
		}
		return reactionBindings;
  }

	static class Binding {
		
		private LPCondition lowerLimit;
		private LPCondition upperLimit;
		private LPVariable switchVar;
		
		public Binding(LPVariable flow, LPVariable flowSwitch) {
			Tools.startMethod("new Binding("+flow+", "+flowSwitch+")");
			lowerLimit = new LPCondition(new LPDiff(flowSwitch, flow),LPCondition.LESS_OR_EQUAL, 0.0);
			lowerLimit.setComment("force velocity>1 if switch=1");

			upperLimit = new LPCondition(new LPDiff(flow, limit,flowSwitch),LPCondition.LESS_OR_EQUAL,0.0);
			upperLimit.setComment("force velocity=0 if switch==0 ");
			switchVar=flowSwitch;
			Tools.endMethod();
		}

		public LPCondition upperLimit() {
	    return upperLimit;
    }

		public LPCondition lowerLimit() {
	    return lowerLimit;
    }
		
		public LPVariable switchVar(){
			return switchVar;
		}

		public static TreeSet<Binding> set() {
			return new TreeSet<Binding>(ObjectComparator.get());
		}
	}

	private TreeSet<Binding> addOutflows(TreeSet<Integer> allSubstances, TreeMap<Integer, LPTerm> balances) {
		Tools.startMethod("addOutflows(...)");
		TreeSet<Binding> bindings = Binding.set();
		for (Integer sid : allSubstances) {
			if (substancesThatShallBeConsumed.contains(sid)){
				Tools.indent("substancesThatShallBeConsumed contains "+sid);
				continue;
			}
			if (substancesThatShallNotBeProduced.contains(sid)){
				System.out.println("substancesThatShallNotBeProduced contains "+sid);
				continue;	 // do not add inflow for substance that (shall be produced) or (shall not be consumed)
			} 
			LPVariable outflow = addBoundaryFlow(balances, sid, OUTFLOW);
			if (useMILP) {
				LPVariable outflowSwitch = outflowSwitch(sid);
				bindings.add(new Binding(outflow, outflowSwitch));
			}
		}
		Tools.endMethod(bindings);
		return bindings;
	}

	private TreeSet<Binding> addInflows(TreeSet<Integer> allSubstances, TreeMap<Integer, LPTerm> balances) {
		Tools.startMethod("addInflows(...)");
		TreeSet<Binding> bindings = Binding.set();
		for (Integer sid : allSubstances) {
			if (substancesThatShallBeProduced.contains(sid)){
				Tools.indent("substancesThatShallBeProduced contains "+sid);
				continue;
			}
			if (substancesThatShallNotBeConsumed.contains(sid)){
				System.out.println("substancesThatShallNotBeConsumed contains "+sid);
				 // do not add inflow for substance that (shall be produced) or (shall not be consumed)
				continue;
			}			
			LPVariable inflow = addBoundaryFlow(balances, sid, INFLOW);
			if (useMILP) {
				LPVariable inflowSwitch = inflowSwitch(sid);
				bindings.add(new Binding(inflow, inflowSwitch));
			}
		}
		Tools.endMethod(bindings);
		return bindings;
	}

	private LPVariable addBoundaryFlow(TreeMap<Integer, LPTerm> balances, Integer sid, boolean direction) {
		Tools.startMethod("addBoundaryFlow(...,"+sid+", "+((direction==INFLOW)?"inflow":"outflow")+")");
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

	private TreeMap<Integer, LPTerm> createReactionBalances(Compartment compartment) throws DataFormatException {
		Tools.startMethod("createBasicBalances("+compartment+")");
		TreeMap<Integer, LPTerm> balances = new TreeMap<Integer, LPTerm>(ObjectComparator.get());
		ReactionSet reactions = compartment.reactions();
		for (Integer reactionID : reactions) { /* create terms for substances according to reactions */
			Reaction reaction = Reaction.get(reactionID);
			if (ignoreUnbalanced && !reaction.isBalanced()) continue;

			if (reaction.firesForwardIn(compartment)) addForwardVelocity(reaction, balances);
			if (reaction.firesBackwardIn(compartment)) addBackwardVelocity(reaction, balances);
		}
		Tools.endMethod(balances);
		return balances;
	}

	private void addBackwardVelocity(Reaction reaction, TreeMap<Integer, LPTerm> mappingFromSubstancesToTerms) {
		Tools.startMethod("addBackwardVelocity("+reaction+")");
		LPVariable backwardVelocity = backward(reaction.id());
		for (Entry<Integer, Integer> entry : reaction.substrates().entrySet()) {
			int substanceId = entry.getKey();
			double stoich = entry.getValue();
			if (substancesThatShallNotBeBalanced.contains(substanceId)) continue;
			LPTerm balanceForSubstance = mappingFromSubstancesToTerms.get(substanceId);
			mappingFromSubstancesToTerms.put(substanceId, new LPSum(balanceForSubstance, stoich, backwardVelocity));
			//Tools.indent("adding +"+substanceId);
		}
		for (Entry<Integer, Integer> entry : reaction.products().entrySet()) {
			int substanceId = entry.getKey();
			double stoich = entry.getValue();
			if (substancesThatShallNotBeBalanced.contains(substanceId)) continue;
			LPTerm balanceForSubstance = mappingFromSubstancesToTerms.get(substanceId);
			mappingFromSubstancesToTerms.put(substanceId, new LPDiff(balanceForSubstance, stoich, backwardVelocity));
			//Tools.indent("adding -"+substanceId);
		}
		Tools.endMethod();
	}

	private void addForwardVelocity(Reaction reaction, TreeMap<Integer, LPTerm> mappingFromSubstancesToTerms) {
		Tools.startMethod("addForwardVelocity("+reaction+")");
		LPVariable forwardVelocity = forward(reaction.id());
		for (Entry<Integer, Integer> entry : reaction.products().entrySet()) {
			int substanceId = entry.getKey();
			double stoich = entry.getValue();
			if (substancesThatShallNotBeBalanced.contains(substanceId)) continue;
			LPTerm balanceForSubstance = mappingFromSubstancesToTerms.get(substanceId);
			mappingFromSubstancesToTerms.put(substanceId, new LPSum(balanceForSubstance, stoich, forwardVelocity));
			//Tools.indent("adding +"+substanceId);

		}
		for (Entry<Integer, Integer> entry : reaction.substrates().entrySet()) {
			int substanceId = entry.getKey();
			double stoich = entry.getValue();
			if (substancesThatShallNotBeBalanced.contains(substanceId)) continue;
			LPTerm balanceForSubstance = mappingFromSubstancesToTerms.get(substanceId);
			mappingFromSubstancesToTerms.put(substanceId, new LPDiff(balanceForSubstance, stoich, forwardVelocity));
			//Tools.indent("adding -"+substanceId);
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

	public TreeSet<Integer> ignoredSubstances() {
	  return substancesThatShallNotBeBalanced;
  }
	
	public int getCompartmentId() {
	  return compartmentId;
  }
	
	protected String filename(){
  	SimpleDateFormat formatter = new SimpleDateFormat("yy-MM-dd HH.mm.ss");
		return "OptimizationTask " + formatter.format(new Date()) + "."+ getNumber()  + ".lp";
	}
	
	public DefaultMutableTreeNode outputTree() throws SQLException {
		return new SubstanceListNode("desired output substances",substancesThatShallBeProduced);
  }
	public DefaultMutableTreeNode inputTree() throws SQLException {
		return new SubstanceListNode("desired input substances",substancesThatShallBeConsumed);
  }
}