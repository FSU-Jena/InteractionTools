package edu.fsuj.csb.reactionnetworks.interaction.tasks;

public class ParameterSet {

	private boolean MILP,ignoreUnbalanced;
	
	public ParameterSet(boolean useMilp, boolean ignoreUnbalanced) {
	 MILP=useMilp;
	 this.ignoreUnbalanced=ignoreUnbalanced;
  }

	public boolean useMILP() {
	  return MILP;
  }

	public boolean ignoreUnbalanced() {
	  return ignoreUnbalanced;
  }

}
