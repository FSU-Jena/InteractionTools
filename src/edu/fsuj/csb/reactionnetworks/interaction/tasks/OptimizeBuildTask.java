package edu.fsuj.csb.reactionnetworks.interaction.tasks;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.CalculationClient;
import edu.fsuj.csb.reactionnetworks.interaction.SeedOptimizationSolution;
import edu.fsuj.csb.reactionnetworks.interaction.SubstanceListNode;
import edu.fsuj.csb.reactionnetworks.interaction.gui.OptimizationParametersTab.OptimizationParameterSet;
import edu.fsuj.csb.reactionnetworks.interaction.results.SeedOptimizationResult;
import edu.fsuj.csb.reactionnetworks.organismtools.DbCompartment;
import edu.fsuj.csb.tools.LPSolverWrapper.CplexWrapper;
import edu.fsuj.csb.tools.LPSolverWrapper.LPCondition;
import edu.fsuj.csb.tools.LPSolverWrapper.LPDiff;
import edu.fsuj.csb.tools.LPSolverWrapper.LPSum;
import edu.fsuj.csb.tools.LPSolverWrapper.LPTerm;
import edu.fsuj.csb.tools.LPSolverWrapper.LPVariable;
import edu.fsuj.csb.tools.organisms.Compartment;
import edu.fsuj.csb.tools.organisms.Reaction;
import edu.fsuj.csb.tools.organisms.gui.ComponentNode;
import edu.fsuj.csb.tools.xml.NoTokenException;
import edu.fsuj.csb.tools.xml.ObjectComparator;

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
	 */
	public OptimizeBuildTask(int cid, TreeSet<Integer> decompose, TreeSet<Integer> build, TreeSet<Integer> ignore, OptimizationParameterSet optimizationParameterSet, boolean ignoreUnbalanced) {
		super(cid, decompose, build,ignore);
		parameters=optimizationParameterSet;
		this.ignoreUnbalanced=ignoreUnbalanced;
	}

	public void run(CalculationClient calculationClient) throws IOException, NoTokenException, AlreadyBoundException {
		System.out.println("substances that shall be decomposed: " + substancesThatShallBeDecomposed());
		System.out.println("substances that shall be produced: " + substancesThatShallBeBuilt());
		try {
			TreeMap<TreeSet<Integer>, TreeSet<Integer>> solutions = new TreeMap<TreeSet<Integer>, TreeSet<Integer>>(ObjectComparator.get()); // set of (set of substances, that must be supplied)
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
				solutions.put(solution.inflows(), solution.outflows()); // don't allow inflow of the substances in the solution in the next turn
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
	private SeedOptimizationSolution runInternal(TreeMap<TreeSet<Integer>, TreeSet<Integer>> solutions) throws SQLException, IOException, InterruptedException, DataFormatException {
		System.out.println("running seed optimization:");
		System.out.println("  forbidden solutions: " + solutions.toString().replace("=", "=>"));
		System.out.print("  Creating solver input file...");

		CplexWrapper cpw = new CplexWrapper(); // create solver instance
		int cid = getCompartmentId(); // get compartment id
		Compartment compartment = DbCompartment.load(cid); // get compartment
		TreeMap<Integer, LPTerm> balances = createBasicBalances(cpw,ignoreSubstancesList, ignoreUnbalanced,compartment); // create balances for all substances in the compartment
		TreeSet<Integer> utilizedSubstances = compartment.utilizedSubstances(); // get the list of all substances utilized by this compartment
		utilizedSubstances.removeAll(ignoreSubstancesList); 
		addInflowReactionsFor(utilizedSubstances, balances, cpw); // add inflow reactions for all substances
		addOutflowReactionsFor(utilizedSubstances, balances, cpw); // add outflow reactions for all substances
		writeBalances(balances, cpw); // write the balance equations to the solver input file

		LPTerm termToMinimize = null; // start creating the directive

		if (parameters.getNumberOfAllReactions()>0) {
			System.out.println("numberOfAllReactionsImportance>0");
			for (Iterator<Integer> rit = compartment.reactions().iterator(); rit.hasNext();) { // iterate through all the compartments reactions
				int rid = rit.next(); // reaction id
				Reaction reaction = Reaction.get(rid); // get the respective reaction
				if (reaction.hasUnchangedSubstances()) continue; // skip "magic" reactions
				if (reaction.firesForwardIn(compartment)) { // if reaction can fire forward in the compartment:
					LPVariable var = new LPVariable("sRf_" + rid); // add forward switch to termToMinimize, so the optimizer tries to suppress the reaction
					if (termToMinimize == null) {
						termToMinimize = var;
					} else termToMinimize = new LPSum(termToMinimize, var);
				}
				if (reaction.firesBackwardIn(compartment)) { // if reaction can fire backward in the compartment:
					LPVariable var = new LPVariable("sRb_" + rid); // add backward switch to termToMinimize, so the optimizer tries to suppress the backward reaction
					if (termToMinimize == null) {
						termToMinimize = var;
					} else termToMinimize = new LPSum(termToMinimize, var);
				}
			}
			if (termToMinimize!=null) termToMinimize=new LPSum(null, 0.0+parameters.getNumberOfAllReactions(),termToMinimize); // 
		}

		int numberOfInflowReactionsImportance=Math.max(parameters.getNumberOfInflows(), parameters.getNumberOfAllReactions());
		int numberOfOutflowReactionsImportance=Math.max(parameters.getNumberOfOutflows(), parameters.getNumberOfAllReactions());
		for (Iterator<Integer> it = utilizedSubstances.iterator(); it.hasNext();) { // iterate through all the compartment's substances:
			int sid = it.next(); // get substance id

			LPVariable inflowSwitch = new LPVariable("sRi_" + sid);
			LPVariable outflowSwitch = new LPVariable("sRo_" + sid);
			LPVariable inflowVelocity = new LPVariable("vRi_" + sid);
			LPVariable outflowVelocity = new LPVariable("vRo_" + sid);

			addCondition(sid, cpw, INFLOW); // connect inflow switch and velocity
			addCondition(sid, cpw, OUTFLOW); // connect outflow switch and velocity

			if (substancesThatShallBeDecomposed().contains(sid)) { // activate inflow for all substances, that shall be decomposed
				cpw.setEqual(inflowSwitch, 1.0, "force inflow for substance to be degraded");
				cpw.setEqual(outflowSwitch, 0.0, "forbid outflow of substances to be degraded");
				if (parameters.getRateOfInflows()>0)	{
					termToMinimize=new LPDiff(termToMinimize, parameters.getRateOfInflows()+0.0,inflowVelocity);
					LPCondition lpc=new LPCondition(inflowVelocity, 10000);
					lpc.setComment("Test");
					cpw.addCondition(lpc);
				}
			} else {
				if (numberOfInflowReactionsImportance>0) termToMinimize = new LPSum(termToMinimize, 0.0+numberOfInflowReactionsImportance, inflowSwitch); // minimize other inflows
			}

			if (substancesThatShallBeBuilt().contains(sid)) { // activate outflow for all substances, that shall be produced
				cpw.setEqual(outflowSwitch, 1.0, "force outflow for substance to be built");
				cpw.setEqual(inflowSwitch, 0.0, "forbid inflow of substances to be built");
				if (parameters.getRateOfOutflows()>0)	{
					termToMinimize=new LPDiff(termToMinimize, parameters.getRateOfOutflows()+0.0,outflowVelocity);
					LPCondition lpc=new LPCondition(outflowVelocity, 10000);
					lpc.setComment("Test");
					cpw.addCondition(lpc);
				}
			} else {
				if (numberOfOutflowReactionsImportance>0)	termToMinimize = new LPSum(termToMinimize, 0.0+numberOfOutflowReactionsImportance, outflowSwitch); // minimize other outflows
			}

		}

		int solutionNumber = 0;
		for (Iterator<TreeSet<Integer>> solutionIterator = solutions.keySet().iterator(); solutionIterator.hasNext();) {
			solutionNumber++;
			int sum = 0;
			TreeSet<Integer> solution = solutionIterator.next();
			LPTerm inflowSwitchSum = null;
			for (Iterator<Integer> inflowIterator = solution.iterator(); inflowIterator.hasNext();) {
				if (inflowSwitchSum == null) inflowSwitchSum = new LPVariable("sRi_" + inflowIterator.next());
				else inflowSwitchSum = new LPSum(inflowSwitchSum, new LPVariable("sRi_" + inflowIterator.next()));
				sum++;
			}
			LPCondition lpc = new LPCondition(inflowSwitchSum, sum - 1);
			lpc.setComment("Forbid solution " + solutionNumber);
			cpw.addCondition(lpc);
		}

		cpw.minimize(termToMinimize); // set the optimization directive

		System.out.print("done.\n  Starting solver: ");
		SimpleDateFormat formatter = new SimpleDateFormat("yy-MM-dd HH.mm.ss");

		String filename=("seedOptimization " + getNumber() + " " + formatter.format(new Date()) + ".lp").replace(" ", "_").replace(":", ".");
		cpw.setTaskfileName(filename);
		
		cpw.start("sR*");

		TreeMap<LPVariable, Double> solution = cpw.getSolution();
		if (solution == null) return null;
		SeedOptimizationSolution result = new SeedOptimizationSolution(compartmentId);
		for (Iterator<Entry<LPVariable, Double>> values = solution.entrySet().iterator(); values.hasNext();) {
			Entry<LPVariable, Double> entry = values.next();
			String name = entry.getKey().toString();
			if (entry.getValue() > 0) {
				if (name.startsWith("sRi")) result.addInflow(Integer.parseInt(name.substring(4))); // get values of the switches
				if (name.startsWith("sRo")) result.outInflow(Integer.parseInt(name.substring(4))); // get values of the switches
				if (name.startsWith("sRf")) result.addForwardReaction(Integer.parseInt(name.substring(4)));
				if (name.startsWith("sRb")) result.addBackwardReaction(Integer.parseInt(name.substring(4)));
			}
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