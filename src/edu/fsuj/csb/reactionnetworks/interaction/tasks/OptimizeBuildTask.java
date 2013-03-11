package edu.fsuj.csb.reactionnetworks.interaction.tasks;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.Balances;
import edu.fsuj.csb.reactionnetworks.interaction.CalculationClient;
import edu.fsuj.csb.reactionnetworks.interaction.SeedOptimizationSolution;
import edu.fsuj.csb.reactionnetworks.interaction.SubstanceListNode;
import edu.fsuj.csb.reactionnetworks.interaction.gui.OptimizationParametersTab.OptimizationParameterSet;
import edu.fsuj.csb.reactionnetworks.interaction.results.SeedOptimizationResult;
import edu.fsuj.csb.reactionnetworks.organismtools.DbCompartment;
import edu.fsuj.csb.tools.LPSolverWrapper.LPCondition;
import edu.fsuj.csb.tools.LPSolverWrapper.LPConditionEqual;
import edu.fsuj.csb.tools.LPSolverWrapper.LPConditionEqualOrGreater;
import edu.fsuj.csb.tools.LPSolverWrapper.LPConditionLessThan;
import edu.fsuj.csb.tools.LPSolverWrapper.LPSolveWrapper;
import edu.fsuj.csb.tools.LPSolverWrapper.LPSum;
import edu.fsuj.csb.tools.LPSolverWrapper.LPTerm;
import edu.fsuj.csb.tools.LPSolverWrapper.LPVariable;
import edu.fsuj.csb.tools.organisms.gui.ComponentNode;
import edu.fsuj.csb.tools.xml.NoTokenException;

public class OptimizeBuildTask extends OptimizationTask {

	private static final long serialVersionUID = 3199686545941979280L;
	private OptimizationParameterSet parameters;
	private boolean ignoreUnbalanced;
	
	/**
	 * A task, which calculates for a given set of target substances (build) which shall be formed, the optimal set of inflows with the side condition, that the set of substances to be decomposed (decompose) is used.
	 * 
	 * @param cid the organism, for which the network shall be optimized
	 * @param decompose the set of substances, that shall be decomposed
	 * @param build the set of substances, for which the production shall be optimized
	 * @param ignore the set of substances, which shall not be taken into account during calculation
	 * @param optimizationParameterSet a set of optimization parameters
	 * @param ignoreUnbalanced 
	 * @param noOutflow 
	 */
	public OptimizeBuildTask(int cid, TreeSet<Integer> decompose, TreeSet<Integer> build, TreeSet<Integer> ignore, OptimizationParameterSet optimizationParameterSet, boolean ignoreUnbalanced, TreeSet<Integer> noOutflow) {
		super(cid, decompose, build,ignore,noOutflow);
		parameters=optimizationParameterSet;
		this.ignoreUnbalanced=ignoreUnbalanced;
	}

	public void run(CalculationClient calculationClient) throws IOException, NoTokenException, AlreadyBoundException {
		System.out.println("substances that shall be decomposed: " + substancesThatShallBeDecomposed());
		System.out.println("substances that shall be produced: " + substancesThatShallBeBuilt());
		try {
			TreeSet<SeedOptimizationSolution> solutions = SeedOptimizationSolution.set(); // set of (set of substances, that must be supplied)
			int solutionSize = Integer.MAX_VALUE; // for monitoring the size of the solutions
			int number=1;
			while (true) {
				SeedOptimizationSolution solution = runInternal(solutions); // start the actual calculation

				if (solution == null) {
					System.out.println("Solution #"+number+": no solution found.");
					break;
				}
				int inflowSize = solution.inflows().size();
				if (inflowSize > solutionSize) { // if the size of the solution increases, we're getting suboptimal solutions. so: quit!
					System.out.println("found no more solution with size " + solutionSize);
					System.out.println("next solution: #"+(number++)+": Inflows "+solution.inflows()+" / Outflows "+solution.outflows());
					System.out.println("Reactions: "+solution.forwardReactions()+" - "+solution.backwardReactions());
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
				System.out.println("solution #"+(number++)+": Inflows "+solution.inflows()+" / Outflows "+solution.outflows());
				System.out.println("Reactions: "+solution.forwardReactions()+" - "+solution.backwardReactions());
				solutions.add(solution); // don't allow inflow of the substances in the solution in the next turn
				calculationClient.sendObject(new SeedOptimizationResult(this, solution));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
	    e.printStackTrace();
    } catch (DataFormatException e) {
	    e.printStackTrace();
    }
	}

	private Collection<Integer> substancesThatShallBeDecomposed() {
		return substanceIds;
	}

	private Collection<Integer> substancesThatShallBeBuilt() {
		return buildSubstancesList;
	}

	/**
	 * calculates a minimal seed set for the given target substances, excluding solutions given by parameter
	 * 
	 * idea: we formulate an linear optimization problem, where
	 * <ul>
	 * <li>the outflow of the targets shall be maximized</li>
	 * <li>the inflow of the substances to be decomposed shall be maximized</li>
	 * <li>the inflow of all other substances shall be minimized</li>
	 * </ul>
	 * Therefor, the network is mapped as follows:
	 * <ul>
	 * <li>for all chemical species in the network there is a balance equation</li>
	 * <li>for all possible reactions, the products have positive influence on their balance</li>
	 * <li>for all possible reactions, the substrates have negative influence on their balance</li>
	 * <li>all balances are zero (steady state)</li>
	 * <li>for all reactions, the flux is is coupled to a switch, so that
	 * <ul>
	 * <li>the switch is 1 (true), if the flux is greater than zero</li>
	 * <li>the flux is zero, if the switch is 0 (false)</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * 
	 * @param solutions those solutions shall be forbidden during the search for new solutions
	 * @return a mapping from substance ids (keys) to solution values. The values should all be greater then zero, the respective substances are those needed to form the target substances.
	 * @throws SQLException if any database error occurs
	 * @throws IOException
	 * @throws InterruptedException 
	 * @throws DataFormatException 
	 */
	private SeedOptimizationSolution runInternal(TreeSet<SeedOptimizationSolution> solutions) throws SQLException, IOException, InterruptedException, DataFormatException {
		DbCompartment compartment=DbCompartment.load(compartmentId);
		Balances balances=new Balances(ignoreSubstancesList, ignoreUnbalanced, compartment);
		
		LPTerm reactionTerm=null;
		for (LPVariable reaction:balances.reactionSet())	reactionTerm=new LPSum(reactionTerm,reaction);
		
		LPTerm inflowTerm=null;
		LPSum outflowTerm=null;
		for (Integer substanceId:balances.substanceSet()) {
			inflowTerm=new LPSum(inflowTerm, Balances.inflow(substanceId));
			outflowTerm=new LPSum(outflowTerm, Balances.outflow(substanceId));
		}
		
		TreeSet<LPCondition> conditions=LPCondition.set();
		
		for (Integer sid:substancesThatShallBeBuilt()) {
			conditions.add(new LPConditionEqualOrGreater(Balances.outflow(sid), 5.0));
			conditions.add(new LPConditionEqual(Balances.inflow(sid), 0.0));
		}
		for (Integer sid:substancesThatShallBeDecomposed())	{
			conditions.add(new LPConditionEqualOrGreater(Balances.inflow(sid), 5.0));
			conditions.add(new LPConditionEqual(Balances.outflow(sid), 0.0));
		}
		
		for (Integer sid:noOutflowList){
			conditions.add(new LPConditionEqual(new LPVariable("O"+sid),0.0));
		}
		
		conditions.addAll(supressPreviousSolutions(solutions));

		
		LPTerm termToMinimize=new LPSum(reactionTerm, new LPSum(inflowTerm, outflowTerm));
		
		LPSolveWrapper solver=new LPSolveWrapper();
		balances.writeToSolver(solver);
		for (LPCondition condition:conditions) solver.addCondition(condition);
		solver.minimize(termToMinimize);
		
		System.out.print("done.\n  Starting solver: ");
		SimpleDateFormat formatter = new SimpleDateFormat("yy-MM-dd HH.mm.ss");

		String filename=("seedOptimization " + getNumber() + " " + formatter.format(new Date()) + ".lp").replace(" ", "_").replace(":", ".");
		solver.setTaskfileName(filename);
		
		solver.start();
		TreeMap<LPVariable, Double> solution = solver.getSolution();
		System.out.println();
		
		if (solution == null) return null;
		SeedOptimizationSolution result = new SeedOptimizationSolution(compartmentId);
		for (Entry<LPVariable, Double> entry:solution.entrySet()) {
			String name = entry.getKey().toString();
			Double value=entry.getValue();			
			if (value != 0) {
				if (name.startsWith("I")) result.addInflow(Integer.parseInt(name.substring(1)),value); // get values of the switches
				if (name.startsWith("O")) result.addOutflow(Integer.parseInt(name.substring(1)),value); // get values of the switches
				if (name.startsWith("F")) result.addForwardReaction(Integer.parseInt(name.substring(1)),value);
				if (name.startsWith("B")) result.addBackwardReaction(Integer.parseInt(name.substring(1)),value);
			}
		}
		return result;
	}

	private TreeSet<LPCondition> supressPreviousSolutions(TreeSet<SeedOptimizationSolution> solutions) {
		TreeSet<LPCondition> result=LPCondition.set();
		for (SeedOptimizationSolution solution: solutions){
			double sum=-0.1;
			LPTerm term=null;
			for (Entry<Integer, Double> inflow:solution.inflows().entrySet()){
				term=new LPSum(term, new LPVariable("I"+inflow.getKey()));
				sum+=inflow.getValue();				
			}
			for (Entry<Integer, Double> outflow:solution.outflows().entrySet()){
				term=new LPSum(term, new LPVariable("O"+outflow.getKey()));
				sum+=outflow.getValue();				
			}
			for (Entry<Integer, Double> forward:solution.forwardReactions().entrySet()){
				term=new LPSum(term, new LPVariable("F"+forward.getKey()));
				sum+=forward.getValue();				
			}
			for (Entry<Integer, Double> backward:solution.backwardReactions().entrySet()){
				term=new LPSum(term, new LPVariable("B"+backward.getKey()));
				sum+=backward.getValue();				
			}
			LPCondition c = new LPConditionLessThan(term, sum);
			System.out.println(c);
			try {
	      Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
			result.add(c);
		}
	  return result;
  }

	public MutableTreeNode treeRepresentation() throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		DefaultMutableTreeNode result = new DefaultMutableTreeNode("Task: Calculate additionals with MILP ["+this.getClass().getSimpleName()+"]");
		result.add(ComponentNode.create(compartmentId));
		DefaultMutableTreeNode inputs = inputTree();
		result.add(inputs);
		result.add(new SubstanceListNode("ignored substances", ignoreSubstancesList));
		DefaultMutableTreeNode outputs = outputTree();
		result.add(outputs);
		result.add(parameters.tree());
		return result;
	}
}