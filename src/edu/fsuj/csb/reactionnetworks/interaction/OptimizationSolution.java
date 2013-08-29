package edu.fsuj.csb.reactionnetworks.interaction;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.reactionnetworks.organismtools.DbReaction;
import edu.fsuj.csb.reactionnetworks.organismtools.DbSubstance;
import edu.fsuj.csb.tools.organisms.gui.ReactionNode;
import edu.fsuj.csb.tools.organisms.gui.SubstanceNode;
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
	  return "OptimizationSolution("+inflows()+"⇒"+reactions()+"⇒"+outflows()+")";
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

	public DefaultMutableTreeNode tree() throws SQLException {
		DefaultMutableTreeNode result=new DefaultMutableTreeNode("Solution");
		result.add(inflowTree());
		result.add(reactionTree());
		result.add(outflowTree());
	  return result;
  }

	private DefaultMutableTreeNode reactionTree() throws SQLException {
		TreeMap<Integer, Double> reactions = reactions();
		DefaultMutableTreeNode reactionTree=new DefaultMutableTreeNode("Reactions ("+reactions.size()+")");
	  for (Entry<Integer, Double> reaction:reactions.entrySet()){
	  	ReactionNode rn=new ReactionNode(DbReaction.load(reaction.getKey()));
	  	rn.add(new DefaultMutableTreeNode("Rate: "+reaction.getValue()));
	  	reactionTree.add(rn);	  	
	  }
	  return reactionTree;
  }
	private DefaultMutableTreeNode outflowTree() throws SQLException {
		DefaultMutableTreeNode outflowTree=new DefaultMutableTreeNode("Outflows ("+outflows.size()+")");
		for (Entry<Integer, Double> outflow:outflows.entrySet()){
			SubstanceNode sn=new SubstanceNode(DbSubstance.load(outflow.getKey()));
			sn.add(new DefaultMutableTreeNode("Rate: "+outflow.getValue()));
			outflowTree.add(sn);
		}
		return outflowTree;
  }
	private DefaultMutableTreeNode inflowTree() throws SQLException {
		DefaultMutableTreeNode inflowTree=new DefaultMutableTreeNode("Inflows ("+inflows.size()+")");
		for (Entry<Integer, Double> inflow:inflows.entrySet()){
			SubstanceNode sn=new SubstanceNode(DbSubstance.load(inflow.getKey()));
			sn.add(new DefaultMutableTreeNode("Rate: "+inflow.getValue()));
			inflowTree.add(sn);
		}
		return inflowTree;
  }	
}
