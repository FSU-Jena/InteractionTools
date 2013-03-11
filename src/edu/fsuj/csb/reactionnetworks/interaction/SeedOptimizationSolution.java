package edu.fsuj.csb.reactionnetworks.interaction;

import java.io.Serializable;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.fsuj.csb.tools.xml.ObjectComparator;

public class SeedOptimizationSolution implements Serializable {

  private static final long serialVersionUID = -3138802826107790526L;
  private TreeMap<Integer,Double> inflows=new TreeMap<Integer,Double>(); // ids of substances with an inflow into the compartment
  private TreeMap<Integer,Double> outflows=new TreeMap<Integer,Double>(); // ids of substances with an outflow from the compartment
  private TreeMap<Integer,Double> forward=new TreeMap<Integer,Double>(); // forward reactions
  private TreeMap<Integer,Double> backward=new TreeMap<Integer,Double>(); // backward reactions
  private int compartmentID;
  
  public SeedOptimizationSolution(int compartmentId) {
  	this.compartmentID=compartmentId;
  }
  
  public int compartmentId(){
  	return compartmentID;
  }

	public void addInflow(int substanceId,double vel) {
		inflows.put(substanceId,vel);
  }

	public void addOutflow(int substanceId, double vel) {
		outflows.put(substanceId,vel);
  }

	public void addForwardReaction(int reactionId, double vel) {
		forward.put(reactionId,vel);	  
  }

	public void addBackwardReaction(int reactionId, double vel) {
		backward.put(reactionId,vel);	  
  }

	public TreeMap<Integer, Double> inflows() {
	  return inflows;
  }

	public TreeMap<Integer, Double> outflows() {
	  return outflows;
  }

	public TreeMap<Integer, Double> forwardReactions() {
	  return forward;
  }

	public TreeMap<Integer, Double> backwardReactions() {
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
