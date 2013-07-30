package edu.fsuj.csb.reactionnetworks.interaction.tasks;

import java.io.Serializable;
import java.util.TreeSet;

import edu.fsuj.csb.tools.xml.Tools;

public class SubstanceSet implements Serializable{

  private static final long serialVersionUID = -9036588405259981961L;
	private TreeSet<Integer> degradeList,produceList,noInflowList,noOutflowList,ignoreList;

	public SubstanceSet(TreeSet<Integer> degradeList, TreeSet<Integer> produceList, TreeSet<Integer> noInflowList, TreeSet<Integer> noOutflowList, TreeSet<Integer> ignoreList) {
		this.degradeList=Tools.nonNullSet(degradeList);
		this.produceList=Tools.nonNullSet(produceList);
		this.noOutflowList=Tools.nonNullSet(noOutflowList);
		this.noInflowList=Tools.nonNullSet(noInflowList);
		this.ignoreList=Tools.nonNullSet(ignoreList);
  }

	public TreeSet<Integer> consume() {
	  return new TreeSet<Integer>(degradeList);
  }

	public TreeSet<Integer> produce() {
		return new TreeSet<Integer>( produceList);
  }

	public TreeSet<Integer> noProduce() {
		return new TreeSet<Integer>( noOutflowList);
  }

	public TreeSet<Integer> noConsume() {
		return new TreeSet<Integer>( noInflowList);
  }

	public TreeSet<Integer> ignoredSubstances() {
		return new TreeSet<Integer>( ignoreList);
  }
}
