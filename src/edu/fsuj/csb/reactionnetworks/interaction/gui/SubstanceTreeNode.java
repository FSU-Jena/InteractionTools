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

public class SubstanceTreeNode extends TreeNode {

	static TreeMap<Integer, SubstanceTreeNode> stns = new TreeMap<Integer, SubstanceTreeNode>();

	private int dist = 5;

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

	public Dimension paint(Graphics g, ImageObserver obs, boolean draw) {
		System.err.println("SubstanceTreeNode.paint(g,obs)");
		Dimension ownDim = super.paint(g, obs, draw);
		if (draw) {
			Vector<ReactionTreeNode> reactions = new Vector<ReactionTreeNode>();
			int height = 0;
			TreeNode child = this.firstChild();
			while (child != null) {
				if (child instanceof ReactionTreeNode) {
					reactions.add((ReactionTreeNode) child);
					Dimension dim = child.nodeDimension(g, obs);
					height += dim.height + dist;
				}
				child = child.next();
			}
			if (height>0){
				int y = getOrigin().y + ((ownDim.height - height) / 2);
				int x = getOrigin().x + ownDim.width + 100;
				for (ReactionTreeNode reaction : reactions) {
					reaction.moveTowards(x, y);
					Dimension dim = reaction.paint(g, obs, true);
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
}
