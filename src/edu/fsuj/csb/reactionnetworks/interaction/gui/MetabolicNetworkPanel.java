package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.sql.SQLException;
import java.util.TreeSet;

import de.srsoftware.gui.treepanel.TreeNode;
import de.srsoftware.gui.treepanel.TreePanel;
import de.srsoftware.gui.treepanel.TreeThread;
import edu.fsuj.csb.reactionnetworks.organismtools.DbReaction;
import edu.fsuj.csb.tools.organisms.ReactionSet;
import edu.fsuj.csb.tools.xml.ObjectComparator;
import edu.fsuj.csb.tools.xml.Tools;

public class MetabolicNetworkPanel extends TreePanel {

  private static final long serialVersionUID = -6376374837687130039L;
  private ReactionSet reactions;

  public MetabolicNetworkPanel() {
		super();		
		LeveledTreeNode.setCentered(true);
		reactions=new ReactionSet();
		//organizerThread.setTreeMapper(this);	
  }
  
  public void jumpToSubatance(Integer substanceId) throws SQLException{
  	ReactionSet relatedReaction = new ReactionSet();
  	SubstanceTreeNode stn = SubstanceTreeNode.get(substanceId);
  	for (int rid:reactions.get()){
  		DbReaction reaction=DbReaction.load(rid);
  		if (reaction.hasReactant(substanceId) || reaction.hasProduct(substanceId)) {
  			ReactionTreeNode rtn = ReactionTreeNode.get(rid);
  			stn.addReaction(rtn);
  		}
  	}  	
  	setTree(stn);  	
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
	
	public void addReactions(ReactionSet rs) {
		reactions.addAll(rs);
  }
}
