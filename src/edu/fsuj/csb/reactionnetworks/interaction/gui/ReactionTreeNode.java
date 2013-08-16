package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.util.TreeMap;
import java.util.TreeSet;

import de.srsoftware.gui.treepanel.TreeNode;
import edu.fsuj.csb.tools.xml.ObjectComparator;
import edu.fsuj.csb.tools.xml.Tools;

public class ReactionTreeNode extends TreeNode {
	static TreeMap<Integer,ReactionTreeNode> rtns=new TreeMap<Integer, ReactionTreeNode>();

	public ReactionTreeNode(int id) {
		super(id+"\n "+names(id).first());
  }

	private static TreeSet<String> names(int id) {
		TreeSet<String> result = Tools.StringSet();
		result.add("reaction_"+id);
		return result;
  }

	public static TreeSet<ReactionTreeNode> set() {
	  return new TreeSet<ReactionTreeNode>(ObjectComparator.get());
  }

	public static ReactionTreeNode get(int id) {
		ReactionTreeNode result = rtns.get(id);
		if (result==null) result=new ReactionTreeNode(id);
	  return result;
  }

}
