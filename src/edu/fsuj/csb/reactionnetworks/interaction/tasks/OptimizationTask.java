package edu.fsuj.csb.reactionnetworks.interaction.tasks;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.SubstanceListNode;
import edu.fsuj.csb.tools.LPSolverWrapper.CplexWrapper;
import edu.fsuj.csb.tools.LPSolverWrapper.LPCondition;
import edu.fsuj.csb.tools.LPSolverWrapper.LPConditionEqual;
import edu.fsuj.csb.tools.LPSolverWrapper.LPConditionEqualOrLess;
import edu.fsuj.csb.tools.LPSolverWrapper.LPDiff;
import edu.fsuj.csb.tools.LPSolverWrapper.LPSolveWrapper;
import edu.fsuj.csb.tools.LPSolverWrapper.LPSum;
import edu.fsuj.csb.tools.LPSolverWrapper.LPTerm;
import edu.fsuj.csb.tools.LPSolverWrapper.LPVariable;
import edu.fsuj.csb.tools.organisms.Compartment;
import edu.fsuj.csb.tools.organisms.Reaction;
import edu.fsuj.csb.tools.organisms.Substance;
import edu.fsuj.csb.tools.xml.ObjectComparator;

public abstract class OptimizationTask extends TaskContainingCompartmentAndSubtances {

	private static final long serialVersionUID = -7358978173891618447L;
	protected TreeSet<Integer> buildSubstancesList,ignoreSubstancesList,noOutflowList;
	protected static double limit = 1000.0;
	public final static int FORWARD = 0;
	public final static int BACKWARD = 1;
	public final static int INFLOW = 2;
	public final static int OUTFLOW = 3;

	public OptimizationTask(int cid, TreeSet<Integer> decompose, TreeSet<Integer> build, TreeSet<Integer> ignore, TreeSet<Integer> noOutflow) {
		super(cid, decompose);
		buildSubstancesList = build;
		ignoreSubstancesList=ignore;
		noOutflowList=noOutflow;
	}

	/**
	 * creates a cplex wrapper object and adds the following information:
	 * <ul>
	 * <li>for each substance a balance term</li>
	 * <li>for each reaction and backward reaction a velocity variable and a switch variable (separate for backward reaction</li>
	 * <li>a coulpling for reversible reactions, that prevents the reaction from firing in both directions simultaneously</li>
	 * </ul>
	 * 
	 * @return a mapping from the substance ids to their balance terms
	 * @throws SQLException
	 * @throws DataFormatException 
	 */
	public static TreeMap<Integer, LPTerm> createBasicBalances(CplexWrapper cpw, TreeSet<Integer> ignoredSubstances, boolean ignoreUnbalanced, Compartment compartment) throws SQLException, DataFormatException {

		TreeMap<Integer, LPTerm> mappingFromSIDtoBalanceTerm = new TreeMap<Integer, LPTerm>(ObjectComparator.get());

		for (Iterator<Integer> reactions = compartment.reactions().iterator(); reactions.hasNext();) { // iterate through all reactions
			int reactionID = reactions.next();
			Reaction reaction = Reaction.get(reactionID);
			if (ignoreUnbalanced && !reaction.isBalanced()) continue;
			//if (reaction.hasUnchangedSubstances()) continue; // skip "magic" reactions

			/* in the following, basic reaction parameters are set */
			LPVariable forwardVelocity = null;
			if (reaction.firesForwardIn(compartment)) {
				forwardVelocity = addCondition(cpw,reactionID, FORWARD);
				addSubstrateBalances(forwardVelocity, reaction.substrates(), mappingFromSIDtoBalanceTerm,ignoredSubstances);
				addProductBalances(forwardVelocity, reaction.products(), mappingFromSIDtoBalanceTerm,ignoredSubstances);
			}

			if (reaction.firesBackwardIn(compartment)) {
				LPVariable backwardVelocity = addCondition(cpw,reactionID, BACKWARD);
				addSubstrateBalances(backwardVelocity, reaction.products(), mappingFromSIDtoBalanceTerm,ignoredSubstances);
				addProductBalances(backwardVelocity, reaction.substrates(), mappingFromSIDtoBalanceTerm,ignoredSubstances);

				if (forwardVelocity != null) { // if reaction can fire in both directions: allow only one at a time
					LPCondition condition = new LPConditionEqual(new LPSum(reactionSwitch(reactionID, FORWARD), reactionSwitch(reactionID, BACKWARD)), 1.0);
					condition.setComment("forbid forward and backward to occur simultanously");
					cpw.addCondition(condition);
				}
			}
		}

		return mappingFromSIDtoBalanceTerm;
	}

	/**
	 * adds a production term related to the current reaction to the balance of all the reactions products
	 * 
	 * @param velocity the velocity of the reaction
	 * @param products the mapping from the product substance ids to the respective stochiometries
	 * @param mappingFromSIDtoBalanceTerm
	 */
	private static void addProductBalances(LPVariable velocity, Map<Integer, Integer> products, TreeMap<Integer, LPTerm> mappingFromSIDtoBalanceTerm,TreeSet<Integer> ignoredSubstances) {
		for (Iterator<Entry<Integer, Integer>> entries = products.entrySet().iterator(); entries.hasNext();) {
			Entry<Integer, Integer> entry = entries.next(); // map from substance id to its stochiometry in the reaction
			int substanceId = entry.getKey();
			if (ignoredSubstances.contains(substanceId)) continue; // don't create balances for ignored substances
			double stochiometry = entry.getValue() + .0;
			LPTerm balance = mappingFromSIDtoBalanceTerm.get(substanceId);
			balance = new LPSum(balance, stochiometry, velocity);
			mappingFromSIDtoBalanceTerm.put(substanceId, balance);
		}
	}

	/**
	 * adds a consumption term related to the current reaction to the balance of all the reactions substrates
	 * 
	 * @param velocity the velocity of the reaction
	 * @param substrates the mapping from the substrate substance ids to the respective stochiometries
	 * @param mappingFromSIDtoBalanceTerm
	 */
	private static void addSubstrateBalances(LPVariable velocity, Map<Integer, Integer> substrates, TreeMap<Integer, LPTerm> mappingFromSIDtoBalanceTerm,TreeSet<Integer> ignoredSubstances) {
		for (Iterator<Entry<Integer, Integer>> entries = substrates.entrySet().iterator(); entries.hasNext();) {
			Entry<Integer, Integer> entry = entries.next(); // map from substance id to its stochiometry in the reaction
			int substanceId = entry.getKey();
			if (ignoredSubstances.contains(substanceId)) continue; // don't create balances for ignored substances
			double stochiometry = entry.getValue() + .0;
			LPTerm balance = mappingFromSIDtoBalanceTerm.get(substanceId);
			balance = new LPDiff(balance, stochiometry, velocity);
			mappingFromSIDtoBalanceTerm.put(substanceId, balance);
		}
	}

	private static LPVariable reactionSwitch(int id, int dir) {
		switch (dir) {
		case FORWARD:
			return new LPVariable("sRf_" + id);
		case BACKWARD:
			return new LPVariable("sRb_" + id);
		case INFLOW:
			return new LPVariable("sRi_" + id);
		case OUTFLOW:
			return new LPVariable("sRo_" + id);
		}
		return null;
	}

	private static LPVariable reactionVariable(int id, int dir) {
		switch (dir) {
		case FORWARD:
			return new LPVariable("vRf_" + id);
		case BACKWARD:
			return new LPVariable("vRb_" + id);
		case INFLOW:
			return new LPVariable("vRi_" + id);
		case OUTFLOW:
			return new LPVariable("vRo_" + id);
		}
		return null;
	}

	protected static LPVariable addCondition(LPSolveWrapper solver, int reactionID, int direction) {
		LPVariable forwardSwitch = reactionSwitch(reactionID, direction);
		LPVariable forwardVelocity = reactionVariable(reactionID, direction);
		couple(forwardSwitch, forwardVelocity, solver);
		return forwardVelocity;
	}

	private static void couple(LPVariable reactionSwitch, LPVariable velocity, LPSolveWrapper solver) {
		LPCondition condition = new LPConditionEqualOrLess(new LPDiff(reactionSwitch, velocity), 0.0);
		condition.setComment("force velocity>1 if switch=1");
		solver.addCondition(condition);
		condition = new LPConditionEqualOrLess(new LPDiff(velocity, limit,reactionSwitch),0.0);
		condition.setComment("force velocity=0 if switch==0 ");
		solver.addCondition(condition);
		solver.addBinVar(reactionSwitch);
	}

	public static void addInflowReactionsFor(TreeSet<Integer> utilizedSubstances, TreeMap<Integer, LPTerm> balances, CplexWrapper solver) {
		for (Iterator<Integer> it = utilizedSubstances.iterator(); it.hasNext();) {
			int sid = it.next();
			LPVariable inflowVelocity = new LPVariable("vRi_" + sid);
			LPVariable inflowSwitch = new LPVariable("sRi_" + sid);
			couple(inflowSwitch, inflowVelocity, solver);
			LPTerm balance = balances.get(sid);
			balances.put(sid, new LPSum(balance, inflowVelocity));
		}
	}

	public static void addOutflowReactionsFor(TreeSet<Integer> utilizedSubstances, TreeMap<Integer, LPTerm> balances, CplexWrapper solver) {
		for (Iterator<Integer> it = utilizedSubstances.iterator(); it.hasNext();) {
			int sid = it.next();
			LPVariable outflowVelocity = new LPVariable("vRo_" + sid);
			LPVariable outflowSwitch = new LPVariable("sRo_" + sid);
			couple(outflowSwitch, outflowVelocity, solver);
			LPTerm balance = balances.get(sid);
			balances.put(sid, new LPDiff(balance, outflowVelocity));
		}
	}

	public static void writeBalances(TreeMap<Integer, LPTerm> balances, CplexWrapper solver) {
		for (Iterator<Entry<Integer, LPTerm>> it = balances.entrySet().iterator(); it.hasNext();) {
			Entry<Integer, LPTerm> entry = it.next();
			int sid = entry.getKey();
			LPTerm balance = entry.getValue();
			solver.setEqual(balance, .0, "Balance for " +Substance.get(sid));
		}
	}
	
	public DefaultMutableTreeNode outputTree() throws SQLException {
		return new SubstanceListNode("desired output substances",buildSubstancesList);
  }
	
	public TreeSet<Integer> ignoredSubstances(){
		return new TreeSet<Integer>(ignoreSubstancesList);
	}
}
