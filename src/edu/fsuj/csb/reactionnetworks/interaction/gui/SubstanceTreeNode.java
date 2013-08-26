package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.ImageObserver;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import de.srsoftware.gui.treepanel.TreeNode;
import edu.fsuj.csb.reactionnetworks.organismtools.DbSubstance;
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
		Tools.startMethod("SubstanceTreeNode.paint(g,obs,"+levels+")");
		super.paint(g, obs,levels);
		
		Dimension ownDim = super.paint(g, obs, false);
		
		if (levels>0) {
			Font oldFont = g.getFont();
			float oldSize = oldFont.getSize();
  		g.setFont(oldFont.deriveFont(oldSize * 5 / 6));
			int height = 0;
			for (ReactionTreeNode reaction:reactions){
				if (LeveledTreeNode.hasBeenPainted(reaction)) continue;
				reaction.setParent(this);
				Dimension dim = reaction.nodeDimension(g, obs);
				height += dim.height + dist;
			}			
			if (height>0){				
				int y = getOrigin().y + ((ownDim.height - height) / 2);
				int x = getOrigin().x + ownDim.width + 50*levels;
				for (ReactionTreeNode reaction : reactions) {
					if (LeveledTreeNode.hasBeenPainted(reaction)) continue;
					reaction.moveTowards(x, y);
					if (levels>1)	g.drawLine(getOrigin().x, getOrigin().y, reaction.getOrigin().x, reaction.getOrigin().y);
					Dimension dim = reaction.paint(g, obs, levels-1);
					y += dim.height + dist;
				}
			}
			g.setFont(oldFont);
			super.paint(g, obs,true);
		}
		Tools.endMethod();
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
