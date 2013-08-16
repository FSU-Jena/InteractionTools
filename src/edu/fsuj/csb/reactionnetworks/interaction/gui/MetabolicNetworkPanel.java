package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.util.TreeSet;

import de.srsoftware.gui.treepanel.TreeNode;
import de.srsoftware.gui.treepanel.TreePanel;
import edu.fsuj.csb.tools.xml.Tools;

public class MetabolicNetworkPanel extends TreePanel {

  private static final long serialVersionUID = -6376374837687130039L;
	private int dist = 10;

  public MetabolicNetworkPanel() {
  	super();
  	setTree(createTestNode());
  }

	private TreeNode createTestNode() {
		SubstanceTreeNode testNode=waterNode();
	  return testNode;
  }

	private SubstanceTreeNode waterNode() {
		TreeSet<String> sNames = Tools.StringSet();
		sNames.add("Water");
		sNames.add("Aqua");
		TreeSet<String> rNames = Tools.StringSet();
		rNames.add("Reaction 1");
		rNames.add("first reaction");
		SubstanceTreeNode node = new SubstanceTreeNode(123,sNames,"H2O");
		node.addChild(new ReactionTreeNode(345,rNames));
	  return node;
  }
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		Dimension dim=this.getSize();
		moveNodeTowards(tree, new Point(dim.width/2,dim.height/2));
		tree.paint(g, this);
	}
}
