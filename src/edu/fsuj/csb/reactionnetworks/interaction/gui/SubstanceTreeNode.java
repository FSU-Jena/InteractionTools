package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.ImageObserver;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import de.srsoftware.gui.treepanel.TreeNode;
import edu.fsuj.csb.tools.xml.Tools;

public class SubstanceTreeNode extends TreeNode {
	
	static TreeMap<Integer,SubstanceTreeNode> stns=new TreeMap<Integer, SubstanceTreeNode>();
	
	private int dist=5;
	public SubstanceTreeNode(int id) {
		super("\\small{"+id+"}\\n "+names(id).first()+"\\n "+formula(id));
  }
	
	private static String formula(int id) {
	  return "H2O";
  }

	private static TreeSet<String> names(int id) {
		TreeSet<String> result = Tools.StringSet();
		result.add("Water");
		return result;
  }

	public Dimension paint(Graphics g, ImageObserver obs) {
		Dimension ownDim=super.paint(g, obs);
		Vector<ReactionTreeNode> reactions=new Vector<ReactionTreeNode>();
		TreeNode child = this.firstChild();
		int height=0;
		while (child!=null){
			if (child instanceof ReactionTreeNode) {
				reactions.add((ReactionTreeNode) child);
				Dimension dim = child.nodeDimension(g, obs);
				height+=dim.height+dist;
			}
			child=child.next();
		}	
		int y=getOrigin().y+((ownDim.height-height)/2);
		int x=getOrigin().x+ownDim.width+100;
		for (ReactionTreeNode reaction:reactions){
			reaction.setOrigin(new Point(x,y));
			Dimension dim = reaction.paint(g, obs);
			y+=dim.height+dist;
		}
	  return ownDim;
	}

	public static SubstanceTreeNode get(int id) {
		SubstanceTreeNode result = stns.get(id);
		if (result==null) result=new SubstanceTreeNode(id);
	  return result;
  }
}
