package edu.fsuj.csb.reactionnetworks.interaction;

import java.io.Serializable;
import java.util.TreeSet;

import edu.fsuj.csb.tools.xml.ObjectComparator;

public class SeedOptimizationSolution implements Serializable {

  private static final long serialVersionUID = -3138802826107790526L;
  private TreeSet<Integer> inflows=new TreeSet<Integer>(); // ids of substances with an inflow into the compartment
  private TreeSet<Integer> outflows=new TreeSet<Integer>(); // ids of substances with an outflow from the compartment
  private TreeSet<Integer> forward=new TreeSet<Integer>(); // forward reactions
  private TreeSet<Integer> backward=new TreeSet<Integer>(); // backward reactions
  private int compartmentID;
  
  public SeedOptimizationSolution(int compartmentId) {
  	this.compartmentID=compartmentId;
  }
  
  public int compartmentId(){
  	return compartmentID;
  }

	public void addInflow(int substanceId) {
		inflows.add(substanceId);
  }

	public void outInflow(int substanceId) {
		outflows.add(substanceId);
  }

	public void addForwardReaction(int reactionId) {
		forward.add(reactionId);	  
  }

	public void addBackwardReaction(int reactionId) {
		backward.add(reactionId);	  
  }

	public TreeSet<Integer> inflows() {
	  return inflows;
  }

	public TreeSet<Integer> outflows() {
	  return outflows;
  }

	public TreeSet<Integer> forwardReactions() {
	  return forward;
  }

	public TreeSet<Integer> backwardReactions() {
		return backward;
  }
	
	@Override
	public String toString() {
	  return "SeedOptimizationSolution:\nin: "+inflows()+"\nout: "+outflows()+"\nforward: "+forwardReactions()+"\nbackward: "+backwardReactions();
	}

	public static TreeSet<SeedOptimizationSolution> set() {
	  return new TreeSet<SeedOptimizationSolution>(ObjectComparator.get());
  }
}
