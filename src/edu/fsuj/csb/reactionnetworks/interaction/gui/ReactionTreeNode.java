package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.util.TreeSet;

import de.srsoftware.gui.treepanel.TreeNode;
import edu.fsuj.csb.tools.xml.ObjectComparator;

public class ReactionTreeNode extends TreeNode {

	public ReactionTreeNode(int id, TreeSet<String> names) {
		super(id+"\n "+names.first());
  }

	public static TreeSet<ReactionTreeNode> set() {
	  return new TreeSet<ReactionTreeNode>(ObjectComparator.get());
  }

}
