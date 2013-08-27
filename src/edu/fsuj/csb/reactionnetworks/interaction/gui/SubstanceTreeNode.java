package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.ImageObserver;
import java.sql.SQLException;
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
	TreeSet<ReactionTreeNode> reactions=ReactionTreeNode.set();
	private int id;

	public SubstanceTreeNode(int id) throws SQLException {
		super(""+id);
		dbs=DbSubstance.load(id);
		setText("\\small{Substance " + id + "}\\n " + names().first() + "\\n " + formula());
		this.id=id;
		stns.put(id,this);
	}

	private String formula() {
		Formula formula = dbs.formula();
		return (formula!=null)?formula.toString():"no formula";
	}

	@Override
	public TreeNode getRoot() {
	  return this;
	}
	
	private TreeSet<String> names() throws SQLException {
		return dbs.names();
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
				height += dim.height + vdist;
			}			
			if (height>0){				
				int y = getOrigin().y + ((ownDim.height - height) / 2);
				int x = getOrigin().x + ownDim.width + 50*levels;
				for (ReactionTreeNode reaction : reactions) {
					if (LeveledTreeNode.hasBeenPainted(reaction)) continue;
					reaction.moveTowards(x, y);
					if (levels>1)	g.drawLine(getOrigin().x, getOrigin().y, reaction.getOrigin().x, reaction.getOrigin().y);
					Dimension dim = reaction.paint(g, obs, levels-1);
					y += dim.height + vdist;
				}
			}
			g.setFont(oldFont);
			super.paint(g, obs,true);
		}
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

	public void addReaction(ReactionTreeNode r) {
	  reactions.add(r);
	  
  }

	public int id() {
	  return id;
  }
}
