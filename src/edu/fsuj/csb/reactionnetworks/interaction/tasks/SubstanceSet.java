package edu.fsuj.csb.reactionnetworks.interaction.tasks;

import java.util.TreeSet;

import edu.fsuj.csb.tools.xml.Tools;

public class SubstanceSet {

	private TreeSet<Integer> degradeList,produceList,noInflowList,noOutflowList,ignoreList;

	public SubstanceSet(TreeSet<Integer> degradeList, TreeSet<Integer> produceList, TreeSet<Integer> noInflowList, TreeSet<Integer> noOutflowList, TreeSet<Integer> ignoreList) {
		this.degradeList=Tools.nonNullSet(degradeList);
		this.produceList=Tools.nonNullSet(produceList);
		this.noOutflowList=Tools.nonNullSet(noOutflowList);
		this.noInflowList=Tools.nonNullSet(noInflowList);
		this.ignoreList=Tools.nonNullSet(ignoreList);
  }

	public TreeSet<Integer> consume() {
	  return degradeList;
  }

	public TreeSet<Integer> produce() {
	  return produceList;
  }

	public TreeSet<Integer> noProduce() {
	  return noOutflowList;
  }

	public TreeSet<Integer> noConsume() {
	  return noInflowList;
  }

	public TreeSet<Integer> ignoredSubstances() {
	  return ignoreList;
  }
}
