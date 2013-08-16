package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.util.TreeSet;

import de.srsoftware.gui.treepanel.TreeNode;
import de.srsoftware.gui.treepanel.TreePanel;
import de.srsoftware.gui.treepanel.TreeThread;
import edu.fsuj.csb.tools.xml.ObjectComparator;
import edu.fsuj.csb.tools.xml.Tools;

public class MetabolicNetworkPanel extends TreePanel {

  private static final long serialVersionUID = -6376374837687130039L;

  public MetabolicNetworkPanel() {
		super();		
		LeveledTreeNode.setCentered(true);
		//organizerThread.setTreeMapper(this);	
  	setTree(createTestNode());
  }

	private TreeNode createTestNode() {
		SubstanceTreeNode testNode=testTree();
	  return testNode;
  }
	
	private SubstanceTreeNode testTree() {
		SubstanceTreeNode s1 = SubstanceTreeNode.get(1);
		SubstanceTreeNode s2 = SubstanceTreeNode.get(2);
		SubstanceTreeNode s3 = SubstanceTreeNode.get(3);
		SubstanceTreeNode s4 = SubstanceTreeNode.get(4);
		SubstanceTreeNode s5 = SubstanceTreeNode.get(5);
	  ReactionTreeNode r1 = ReactionTreeNode.get(1);
	  ReactionTreeNode r2 = ReactionTreeNode.get(2);
	  ReactionTreeNode r3 = ReactionTreeNode.get(3);
	  
	  r1.addSubstrate(s1);
	  r1.addSubstrate(s2);
	  r1.addProduct(s4);
	  r2.addSubstrate(s1);
	  r2.addSubstrate(s3);
	  r2.addProduct(s5);
	  r3.addSubstrate(s4);
	  r3.addProduct(s5);
	  
	  s1.addReaction(r1);
	  s2.addReaction(r1);
	  s2.addReaction(r2);
	  s3.addReaction(r2);
	  s4.addReaction(r1);
	  s4.addReaction(r3);
	  s5.addReaction(r2);
	  s5.addReaction(r3);
	  
	  return s2;
  }
	
	@Override
	protected TreeNode getNodeAt(Point point) {
		Tools.startMethod("MetabolicNetworkPanel.getNodeAt");
		TreeNode closest=null;
		double minDist=Double.MAX_VALUE;
		for (LeveledTreeNode node:LeveledTreeNode.visibleNodes()){
			double d = point.distance(node.getOrigin());
			node.resetDimension();
			if (d<minDist){
				closest=node;
				minDist=d;				
			}
		}
		Tools.endMethod(closest.getText());		
		return closest;
	}

	@Override
	public void paint(Graphics g) {
		System.out.println("MetabolicNetworkPanel.paint...");
		super.paint(g);
		g.setFont(g.getFont().deriveFont(20f));
		Dimension dim=this.getSize();
		tree.moveTowards(dim.width/2,dim.height/2);
		LeveledTreeNode.clearPainted();
		((LeveledTreeNode)tree).paint(g, this,3);
		System.out.println("...MetabolicNetworkPanel.paint");
	}
}
