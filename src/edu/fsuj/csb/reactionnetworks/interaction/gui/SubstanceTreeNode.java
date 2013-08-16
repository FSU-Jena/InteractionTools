package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.ImageObserver;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import de.srsoftware.gui.treepanel.TreeNode;
import edu.fsuj.csb.tools.xml.ObjectComparator;
import edu.fsuj.csb.tools.xml.Tools;

public class SubstanceTreeNode extends LeveledTreeNode {

	static TreeMap<Integer, SubstanceTreeNode> stns = new TreeMap<Integer, SubstanceTreeNode>();
TreeSet<ReactionTreeNode> reactions=ReactionTreeNode.set();

	public SubstanceTreeNode(int id) {
		super("\\small{" + id + "}\\n " + names(id).first() + "\\n " + formula(id));
	}

	private static String formula(int id) {
		return "H2O";
	}

	@Override
	public TreeNode getRoot() {
	  return this;
	}
	
	private static TreeSet<String> names(int id) {
		TreeSet<String> result = Tools.StringSet();
		result.add("Sustance "+id);
		return result;
	}

	public Dimension paint(Graphics g, ImageObserver obs, int levels) {
		super.paint(g, obs,levels);
		System.out.println("SubstanceTreeNode.paint(g,obs,"+levels+")");
		Dimension ownDim = super.paint(g, obs, levels>0);
		if (levels>0) {
			int height = 0;
			for (ReactionTreeNode reaction:reactions){
				if (LeveledTreeNode.hasBeenPainted(reaction)) continue;
				Dimension dim = reaction.nodeDimension(g, obs);
				height += dim.height + dist;
			}
			if (height>0){
				int y = getOrigin().y + ((ownDim.height - height) / 2);
				int x = getOrigin().x + ownDim.width + 100;
				for (ReactionTreeNode reaction : reactions) {
					if (LeveledTreeNode.hasBeenPainted(reaction)) continue;
					reaction.moveTowards(x, y);
					Dimension dim = reaction.paint(g, obs, levels-1);
					y += dim.height + dist;
				}
			}
		}
		return ownDim;
	}

	public static SubstanceTreeNode get(int id) {
		SubstanceTreeNode result = stns.get(id);
		if (result == null) result = new SubstanceTreeNode(id);
		return result;
	}

	public static TreeSet<SubstanceTreeNode> set() {
	  return new TreeSet<SubstanceTreeNode>(ObjectComparator.get());
  }

	public void addReaction(ReactionTreeNode r) {
	  reactions.add(r);
	  
  }
}
