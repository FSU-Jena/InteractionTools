package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.util.TreeSet;

import de.srsoftware.gui.treepanel.TreeNode;

public class SubstanceTreeNode extends TreeNode {	
	public SubstanceTreeNode(int id, TreeSet<String> names, String formula) {
		super("\\small{"+id+"}\\n "+names.first()+"\\n "+formula);
  }
}
