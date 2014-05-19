package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.sql.SQLException;

import de.srsoftware.gui.treepanel.TreeNode;
import de.srsoftware.gui.treepanel.TreePanel;
import edu.fsuj.csb.reactionnetworks.organismtools.DbReaction;
import edu.fsuj.csb.tools.organisms.ReactionSet;
import edu.fsuj.csb.tools.xml.Tools;

public class MetabolicNetworkPanel extends TreePanel {

	private static final long serialVersionUID = -6376374837687130039L;
	private ReactionSet reactions;
	private Point center;
	
	public MetabolicNetworkPanel() {
		super();
		LeveledTreeNode.setCentered(true);
		reactions = new ReactionSet();
		// organizerThread.setTreeMapper(this);
	}

	public void jumpToSubatance(Integer substanceId) throws SQLException {
		SubstanceTreeNode stn = SubstanceTreeNode.get(substanceId);
		loadReactions(stn);
		setTree(stn);
		sendActionEvent(new ActionEvent(this, 0, "activate"));
	}

	private void loadReactions(SubstanceTreeNode stn) throws SQLException {
		for (int rid : reactions.get()) {
			DbReaction reaction = DbReaction.load(rid);
			if (reaction.hasProduct(stn.id())) {
				stn.addProducingReaction(ReactionTreeNode.get(rid));
			}
			if (reaction.hasReactant(stn.id())) {
				stn.addConsumingReaction(ReactionTreeNode.get(rid));
			}
		}
	}

	@Override
	protected TreeNode getNodeAt(Point point) {
		Tools.startMethod("MetabolicNetworkPanel.getNodeAt");
		TreeNode closest = null;
		double minDist = Double.MAX_VALUE;
		for (LeveledTreeNode node : LeveledTreeNode.visibleNodes()) {
			double d = point.distance(node.getOrigin());
			node.resetDimension();
			if (d < minDist) {
				closest = node;
				minDist = d;
			}
		}
		Tools.endMethod(closest.getText());
		return closest;
	}

	@Override
	public void paint(Graphics g) {
		// System.out.println("MetabolicNetworkPanel.paint...");
		super.paint(g);
		Dimension dim = this.getSize();
		center=new Point(dim.width / 2, dim.height / 2);
		if (tree==null) return;
		tree.moveTowards(center);
		LeveledTreeNode.clearPainted();
		((LeveledTreeNode) tree).paint(g, this, 3);
		// System.out.println("...MetabolicNetworkPanel.paint");
	}

	public void addReactions(ReactionSet rs) {
		reactions.addAll(rs);
	}

	protected void setTreeTo(TreeNode node) {
		//System.out.println(node.getClass());
		try {
			if (node instanceof ReactionTreeNode) {
				((ReactionTreeNode) node).loadSubstances();
			}
			if (node instanceof SubstanceTreeNode) {
				loadReactions(((SubstanceTreeNode) node));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		super.setTreeTo(node);
	}

	@Override
  public void toogleFold() {
	  // TODO Auto-generated method stub
  }
}
