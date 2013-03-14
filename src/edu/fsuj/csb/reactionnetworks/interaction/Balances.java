package edu.fsuj.csb.reactionnetworks.interaction;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.zip.DataFormatException;

import edu.fsuj.csb.tools.LPSolverWrapper.LPCondition;
import edu.fsuj.csb.tools.LPSolverWrapper.LPConditionEqual;
import edu.fsuj.csb.tools.LPSolverWrapper.LPDiff;
import edu.fsuj.csb.tools.LPSolverWrapper.LPSolveWrapper;
import edu.fsuj.csb.tools.LPSolverWrapper.LPSum;
import edu.fsuj.csb.tools.LPSolverWrapper.LPTerm;
import edu.fsuj.csb.tools.LPSolverWrapper.LPVariable;
import edu.fsuj.csb.tools.organisms.Compartment;
import edu.fsuj.csb.tools.organisms.Reaction;
import edu.fsuj.csb.tools.xml.ObjectComparator;

public class Balances {
	public final static int FORWARD = 0;
	public final static int BACKWARD = 1;
	public final static int INFLOW = 2;
	public final static int OUTFLOW = 3;
	private TreeSet<Integer> ignoredSubstances;	
	private TreeSet<LPCondition> conditions=LPCondition.set();
	private Vector<LPVariable> reactions=new Vector<LPVariable>();
	private Set<Integer> substances=new TreeSet<Integer>();
	
	/**
	 * creates for all substances the balances with respect of consumption and production in reactions and inflows and outflows
	 * @param ignoredSubstances
	 * @param ignoreUnbalanced
	 * @param compartment
	 * @throws DataFormatException
	 */
	public Balances(TreeSet<Integer> ignoredSubstances, boolean ignoreUnbalanced, Compartment compartment) throws DataFormatException {
		this.ignoredSubstances=ignoredSubstances;
		
		TreeMap<Integer,LPTerm> mappingFromSubstancesToTerms=new TreeMap<Integer, LPTerm>(ObjectComparator.get());
		
		for (Integer reactionID:compartment.reactions()){ /* create terms for substances according to reactions */
			Reaction reaction = Reaction.get(reactionID);
			if (ignoreUnbalanced && !reaction.isBalanced()) continue;
			
			if (reaction.firesForwardIn(compartment))	addForwardVelocity(reaction,mappingFromSubstancesToTerms);
			if (reaction.firesBackwardIn(compartment))addBackwardVelocity(reaction,mappingFromSubstancesToTerms);
		}
		
		for(Entry<Integer,LPTerm> entry:mappingFromSubstancesToTerms.entrySet()){	/* add inflows and outflows */
			int substanceId=entry.getKey();
			LPTerm newBalance=new LPSum(entry.getValue(),new LPDiff(inflow(substanceId), outflow(substanceId)));
			mappingFromSubstancesToTerms.put(substanceId, newBalance);
		}
		
		substances=mappingFromSubstancesToTerms.keySet();
		
		for (LPTerm term:mappingFromSubstancesToTerms.values()){ /* set all substance terms to be zero */
			conditions.add(new LPConditionEqual(term, 0.0));
		}
	}


	private void addBackwardVelocity(Reaction reaction, TreeMap<Integer, LPTerm> mappingFromSubstancesToTerms) {
		LPVariable velocity =new LPVariable("B"+reaction.id());
		reactions.add(velocity);
		for (Entry<Integer,Integer> entry:reaction.substrates().entrySet()){
			int substanceId=entry.getKey();
			double stoich=entry.getValue();
			if (ignoredSubstances.contains(substanceId)) continue;
			LPTerm balanceForSubstance=mappingFromSubstancesToTerms.get(substanceId);
			mappingFromSubstancesToTerms.put(substanceId, new LPSum(balanceForSubstance, stoich, velocity));
		}
		for (Entry<Integer,Integer> entry:reaction.products().entrySet()){
			int substanceId=entry.getKey();
			double stoich=entry.getValue();
			if (ignoredSubstances.contains(substanceId)) continue;
			LPTerm balanceForSubstance=mappingFromSubstancesToTerms.get(substanceId);
			mappingFromSubstancesToTerms.put(substanceId, new LPDiff(balanceForSubstance, stoich, velocity));
		}
  }


	private void addForwardVelocity(Reaction reaction, TreeMap<Integer, LPTerm> mappingFromSubstancesToTerms) {
		LPVariable velocity =new LPVariable("F"+reaction.id());
		reactions.add(velocity);
		for (Entry<Integer,Integer> entry:reaction.products().entrySet()){
			int substanceId=entry.getKey();
			double stoich=entry.getValue();
			if (ignoredSubstances.contains(substanceId)) continue;
			LPTerm balanceForSubstance=mappingFromSubstancesToTerms.get(substanceId);
			mappingFromSubstancesToTerms.put(substanceId, new LPSum(balanceForSubstance, stoich, velocity));
		}
		for (Entry<Integer,Integer> entry:reaction.substrates().entrySet()){
			int substanceId=entry.getKey();
			double stoich=entry.getValue();
			if (ignoredSubstances.contains(substanceId)) continue;
			LPTerm balanceForSubstance=mappingFromSubstancesToTerms.get(substanceId);
			mappingFromSubstancesToTerms.put(substanceId, new LPDiff(balanceForSubstance, stoich, velocity));
		}
  }


	public Collection<LPVariable> reactionSet() {
	  return reactions;
  }


	public Set<Integer> substanceSet() {
	  return substances;
  }


	public static LPVariable inflow(Integer substanceId) {
	  return new LPVariable("I"+substanceId);
  }


	public static LPVariable outflow(Integer substanceId) {
	  return new LPVariable("O"+substanceId);
	  }


	public void writeToSolver(LPSolveWrapper solver) {
		for (LPCondition condition:conditions) solver.addCondition(condition);
  }
}
