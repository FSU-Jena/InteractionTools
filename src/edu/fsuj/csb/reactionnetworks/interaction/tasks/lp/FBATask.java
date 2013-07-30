package edu.fsuj.csb.reactionnetworks.interaction.tasks.lp;

import java.util.TreeSet;

public class FBATask extends LinearProgrammingTask {

  private static final long serialVersionUID = 2619391610630359556L;

	public FBATask(int compartmentId, TreeSet<Integer> consume, TreeSet<Integer> produce, TreeSet<Integer> noConsume, TreeSet<Integer> noProduce, TreeSet<Integer> ignoredSubstances, boolean useMILP, boolean ignoreUnbalancedReactions) {
	  super(compartmentId, consume, produce, noConsume, noProduce, ignoredSubstances, useMILP, ignoreUnbalancedReactions);
	  // TODO Auto-generated constructor stub
  }

}
