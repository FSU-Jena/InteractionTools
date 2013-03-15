package edu.fsuj.csb.reactionnetworks.interaction.tasks;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeSet;

public class SeedCalculationTask extends OptimizationTask {

	private static final long serialVersionUID = -8131331284620004914L;
	/**
	 * This clas represents a Task for calculation the seedset of a certain set of substances in a given compartment 
	 * @param compartmentId the database id of the compartment
	 * @param targetSubstanceIds the set of (database related) substance ids, which shall be built
	 * @param ignoreUnbalanced 
	 */
	public SeedCalculationTask(int compartmentId, TreeSet<Integer> targetSubstanceIds,TreeSet<Integer> ignoredSubstances, boolean ignoreUnbalanced) {
		super(compartmentId, null, targetSubstanceIds, null, null, ignoredSubstances, true, ignoreUnbalanced);
  }
	
	@Override
	protected String filename(){
  	SimpleDateFormat formatter = new SimpleDateFormat("yy-MM-dd HH.mm.ss");
		return ("seedCalculation " + getNumber() + " " + formatter.format(new Date()) + ".lp").replace(" ", "_").replace(":", ".");
	}
}
