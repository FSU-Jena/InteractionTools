package edu.fsuj.csb.reactionnetworks.interaction.tasks;

import java.io.Serializable;
import java.util.TreeSet;

import edu.fsuj.csb.tools.xml.Tools;

public class SubstanceSet implements Serializable{

  private static final long serialVersionUID = -9036588405259981961L;
	private TreeSet<Integer> desiredInflows,desiredOutflows,forbiddenInflows,forbiddenOutflows,ignoredSubstances;

	public SubstanceSet(TreeSet<Integer> desiredInflows, TreeSet<Integer> desiredOutflows, TreeSet<Integer> forbiddenInflows, TreeSet<Integer> forbiddenOutflows, TreeSet<Integer> ignoredSubstances) {
		this.desiredInflows=Tools.nonNullSet(desiredInflows);
		this.desiredOutflows=Tools.nonNullSet(desiredOutflows);
		this.forbiddenOutflows=Tools.nonNullSet(forbiddenOutflows);
		this.forbiddenInflows=Tools.nonNullSet(forbiddenInflows);
		this.ignoredSubstances=Tools.nonNullSet(ignoredSubstances);
  }

	public TreeSet<Integer> desiredInflows() {
	  return new TreeSet<Integer>(desiredInflows);
  }

	public TreeSet<Integer> desiredOutFlows() {
		return new TreeSet<Integer>( desiredOutflows);
  }

	public TreeSet<Integer> forbiddenOutflows() {
		return new TreeSet<Integer>( forbiddenOutflows);
  }

	public TreeSet<Integer> forbiddenInflows() {
		return new TreeSet<Integer>( forbiddenInflows);
  }

	public TreeSet<Integer> ignoredSubstances() {
		return new TreeSet<Integer>( ignoredSubstances);
  }

	public TreeSet<Integer> calculateAuxiliaryInflows(TreeSet<Integer> allSubstances) {
		TreeSet<Integer> result=possibleInflows(allSubstances);
		result.removeAll(desiredInflows);
	  return result;
  }
	
	public TreeSet<Integer> calculateAuxiliaryOutflows(TreeSet<Integer> allSubstances){
		TreeSet<Integer> result=possibleOutflows(allSubstances);
		result.removeAll(desiredOutflows);
		return result;
	}

	public TreeSet<Integer> possibleInflows(TreeSet<Integer> allSubstances) {
		TreeSet<Integer> result=new TreeSet<Integer>(allSubstances);
		result.removeAll(forbiddenInflows);
		result.removeAll(desiredOutflows);
		result.removeAll(ignoredSubstances);
	  return result;
  }
	
	public TreeSet<Integer> possibleOutflows(TreeSet<Integer> allSubstances) {
		TreeSet<Integer> result=new TreeSet<Integer>(allSubstances);
		result.removeAll(forbiddenOutflows);
		result.removeAll(desiredInflows);
		result.removeAll(ignoredSubstances);
	  return result;
  }
}
