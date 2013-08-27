package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.image.ImageObserver;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import de.srsoftware.gui.treepanel.TreeNode;
import edu.fsuj.csb.reactionnetworks.organismtools.DbSubstance;
import edu.fsuj.csb.tools.organisms.Formula;
import edu.fsuj.csb.tools.xml.ObjectComparator;
import edu.fsuj.csb.tools.xml.Tools;

public class SubstanceTreeNode extends LeveledTreeNode {

	static TreeMap<Integer, SubstanceTreeNode> stns = new TreeMap<Integer, SubstanceTreeNode>();
	private DbSubstance dbs;
	TreeSet<ReactionTreeNode> producingReactions = ReactionTreeNode.set();
	TreeSet<ReactionTreeNode> consuminggReactions = ReactionTreeNode.set();
	private int id;
	private static Stroke arrowStroke = new BasicStroke(2);

	public SubstanceTreeNode(int id) throws SQLException {
		super("" + id);
		dbs = DbSubstance.load(id);
		Iterator<String> nit = names().iterator();
		setText("\\small{Substance " + id + "}\\n " + nit.next() + "\\n " + formula());

		while (nit.hasNext())
			nameNodes.add(new TreeNode(nit.next()));
		this.id = id;
		stns.put(id, this);
	}

	private String formula() {
		Formula formula = dbs.formula();
		return (formula != null) ? formula.code() : "no formula";
	}

	@Override
	public TreeNode getRoot() {
		return this;
	}

	private TreeSet<String> names() throws SQLException {
		return dbs.names();
	}

	public Dimension paint(Graphics g, ImageObserver obs, int level) {
		Tools.startMethod("SubstanceTreeNode.paint(g,obs," + level + ")");
		super.paint(g, obs, level);

		Dimension ownDim = super.paint(g, obs, false);

		if (level > 0) {
			Graphics2D g2 = (Graphics2D) g;
			Stroke oldStroke = g2.getStroke();
			Font oldFont = g.getFont();
			float oldSize = oldFont.getSize();
			g.setFont(oldFont.deriveFont(oldSize * 5 / 7));

			int height = 0;

			for (ReactionTreeNode reaction : producingReactions) {
				if (LeveledTreeNode.hasBeenPainted(reaction)) continue;
				reaction.setParent(this);
				Dimension dim = reaction.nodeDimension(g, obs);
				height += dim.height + vdist;
			}
			if (height > 0) {
				int x = getOrigin().x - ownDim.width / 2 - hdist * level * level;
				int y = getOrigin().y + ((ownDim.height - height) / 2);
				g2.setStroke(arrowStroke);
				for (ReactionTreeNode reaction : producingReactions) {
					if (LeveledTreeNode.hasBeenPainted(reaction)) continue;
					Dimension dim = reaction.nodeDimension(g, obs);
					reaction.moveTowards(x - dim.width / 2, y);
					if (level > 1) drawArrow(g, reaction.getOrigin().x + dim.width / 2, reaction.getOrigin().y, getOrigin().x - ownDim.width / 2, getOrigin().y);
					y += dim.height + vdist;
				}
				g2.setStroke(oldStroke);
				y = getOrigin().y + ((ownDim.height - height) / 2);
				for (ReactionTreeNode reaction : producingReactions) {
					if (LeveledTreeNode.hasBeenPainted(reaction)) continue;
					Dimension dim = reaction.paint(g, obs, level - 1);
					y += dim.height + vdist;
				}
			}

			height = 0;

			for (ReactionTreeNode reaction : consuminggReactions) {
				if (LeveledTreeNode.hasBeenPainted(reaction)) continue;
				reaction.setParent(this);
				Dimension dim = reaction.nodeDimension(g, obs);
				height += dim.height + vdist;
			}
			if (height > 0) {
				int x = getOrigin().x + ownDim.width / 2 + hdist * level * level;
				int y = getOrigin().y + ((ownDim.height - height) / 2);
				g2.setStroke(arrowStroke);
				for (ReactionTreeNode reaction : consuminggReactions) {
					if (LeveledTreeNode.hasBeenPainted(reaction)) continue;
					Dimension dim = reaction.nodeDimension(g, obs);
					reaction.moveTowards(x + dim.width / 2, y);
					if (level > 1) drawArrow(g, getOrigin().x + ownDim.width / 2, getOrigin().y, reaction.getOrigin().x - dim.width / 2, reaction.getOrigin().y);
					y += dim.height + vdist;
				}
				g2.setStroke(oldStroke);
				y = getOrigin().y + ((ownDim.height - height) / 2);
				for (ReactionTreeNode reaction : consuminggReactions) {
					if (LeveledTreeNode.hasBeenPainted(reaction)) continue;
					Dimension dim = reaction.paint(g, obs, level - 1);
					y += dim.height + vdist;
				}
			}
			g.setFont(oldFont);
			super.paint(g, obs, true);
		}
		
		drawNameNodes(g, obs,ownDim,level);

		Tools.endMethod();
		return ownDim;
	}

	public static SubstanceTreeNode get(int id) throws SQLException {
		SubstanceTreeNode result = stns.get(id);
		if (result == null) result = new SubstanceTreeNode(id);
		return result;
	}

	public static TreeSet<SubstanceTreeNode> set() {
		return new TreeSet<SubstanceTreeNode>(ObjectComparator.get());
	}

	public int id() {
		return id;
	}

	public void addProducingReaction(ReactionTreeNode r) {
		if (!producingReactions.contains(r)) r.setOrigin(getOrigin());
		producingReactions.add(r);
	}

	public void addConsumingReaction(ReactionTreeNode r) {
		if (!consuminggReactions.contains(r)) r.setOrigin(getOrigin());
		consuminggReactions.add(r);
	}
}
