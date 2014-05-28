package edu.fsuj.csb.reactionnetworks.interaction.tasks;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.CalculationClient;
import edu.fsuj.csb.reactionnetworks.interaction.OptimizationSolution;
import edu.fsuj.csb.reactionnetworks.interaction.SeedOptimizationSolution;
import edu.fsuj.csb.reactionnetworks.interaction.SubstanceListNode;
import edu.fsuj.csb.reactionnetworks.interaction.gui.OptimizationParametersTab.OptimizationParameterSet;
import edu.fsuj.csb.reactionnetworks.interaction.results.SeedOptimizationResult;
import edu.fsuj.csb.reactionnetworks.organismtools.gui.DbComponentNode;
import edu.fsuj.csb.tools.LPSolverWrapper.LPSolveWrapper;
import edu.fsuj.csb.tools.LPSolverWrapper.LPVariable;
import edu.fsuj.csb.tools.xml.NoTokenException;

public class OptimizeBuildTask extends OptimizationTask {

	private static final long serialVersionUID = 3199686545941979280L;
	
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
	 * @param useMilp 
	 */
	public OptimizeBuildTask(int cid, TreeSet<Integer> decompose, TreeSet<Integer> build, TreeSet<Integer> ignore, OptimizationParameterSet optimizationParameterSet, boolean ignoreUnbalanced, TreeSet<Integer> noOutflow, boolean useMilp) {
		super(cid, decompose, build, null, noOutflow, ignore, useMilp, ignoreUnbalanced);
	}

	protected String filename(){
  	SimpleDateFormat formatter = new SimpleDateFormat("yy-MM-dd HH.mm.ss");
		return "OptimizeBuildTask " + formatter.format(new Date()) + "-"+ getNumber()  + ".lp";
	}
	
	public void run(CalculationClient calculationClient) throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		try {
			TreeSet<OptimizationSolution> solutions = OptimizationSolution.set(); // set of (set of substances, that must be supplied)
			int solutionSize = Integer.MAX_VALUE; // for monitoring the size of the solutions
			int number = 1;
			while (true) {

				SeedOptimizationSolution solution = (SeedOptimizationSolution) runInternal(solutions); // start the actual calculation

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
				calculationClient.sendObject(new SeedOptimizationResult(this, solution));
				solutions.add(solution); // don't allow inflow of the substances in the solution in the next turn
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DataFormatException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
	    e.printStackTrace();
    }
	}
	
	/**
	 * actually create a solution object. this method may be overwritten
	 * @param solver the solver handle
	 * @return the solution object
	 */
	protected SeedOptimizationSolution createSolution(LPSolveWrapper solver) {
		SeedOptimizationSolution solution = new SeedOptimizationSolution(getCompartmentId());
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
	public MutableTreeNode treeRepresentation() throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		DefaultMutableTreeNode result = new DefaultMutableTreeNode("Task: Calculate additionals with MILP ["+this.getClass().getSimpleName()+"]");
		result.add(DbComponentNode.create(getCompartmentId()));
		DefaultMutableTreeNode inputs = inputTree();
		result.add(inputs);
		result.add(new SubstanceListNode("ignored substances", ignoredSubstances()));
		DefaultMutableTreeNode outputs = outputTree();
		result.add(outputs);
		//result.add(parameters.tree());
		return result;
	}
}