package edu.fsuj.csb.reactionnetworks.interaction;

import java.io.Serializable;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.fsuj.csb.tools.xml.ObjectComparator;

public class OptimizationSolution implements Serializable{

  private static final long serialVersionUID = -1066667680804531598L;
	private TreeMap<Integer, Double> inflows;
	private TreeMap<Integer, Double> outflows;
	private TreeMap<Integer, Double> backward;
	private TreeMap<Integer, Double> forward;

	public OptimizationSolution() {
		inflows=new TreeMap<Integer, Double>();
		outflows=new TreeMap<Integer, Double>();
		forward=new TreeMap<Integer, Double>();
		backward=new TreeMap<Integer, Double>();
	}

	public TreeMap<Integer,Double> inflows() {
	  return inflows;
  }

	public TreeMap<Integer,Double> forwardReactions() {
	  // TODO Auto-generated method stub
	  return forward;
  }

	public TreeMap<Integer,Double> backwardReactions() {
	  // TODO Auto-generated method stub
	  return backward;
  }

	public TreeMap<Integer,Double> outflows() {
	  return outflows;
  }
	
	public TreeMap<Integer,Double> reactions(){
		TreeMap<Integer,Double> result = new TreeMap<Integer,Double>();
		result.putAll(forwardReactions());
		result.putAll(backwardReactions());
		return result;
	}
	
	@Override
	public String toString() {
	  return "SoptimizationSolution("+inflows()+"⇒"+reactions()+"⇒"+outflows()+")";
	}

	public static TreeSet<OptimizationSolution> set() {
	  return new TreeSet<OptimizationSolution>(ObjectComparator.get());
	  }

	public void addOutflow(Integer key, double value) {
		outflows.put(key, value);
	  
  }

	public void addInflow(Integer key, double value) {
		inflows.put(key, value);	  
  }

	public void addForwardReaction(Integer key, double value) {
		forward.put(key, value);
	  
  }

	public void addBackwardReaction(Integer key, double value) {
		backward.put(key, value);	  
  }
	
}
