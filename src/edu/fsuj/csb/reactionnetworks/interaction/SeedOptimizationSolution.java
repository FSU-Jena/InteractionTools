package edu.fsuj.csb.reactionnetworks.interaction;


public class SeedOptimizationSolution extends OptimizationSolution {
  private static final long serialVersionUID = -2236576165318596834L;
	private int cid;
  
  public SeedOptimizationSolution(int compartmentId) {
  	cid=compartmentId;
  }
	public String toString() {
	  return "SeedOptimizationSolution:\nin: "+inflows()+"\nout: "+outflows()+"\nforward: "+forwardReactions()+"\nbackward: "+backwardReactions();
	}
	public int compartmentId() {
	  return cid;
  }
}
