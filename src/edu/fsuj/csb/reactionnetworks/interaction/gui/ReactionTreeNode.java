package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.util.TreeSet;

import de.srsoftware.gui.treepanel.TreeNode;

public class ReactionTreeNode extends TreeNode {

	public ReactionTreeNode(int id, TreeSet<String> names) {
		super(id+"\n "+names.first());
  }

}
