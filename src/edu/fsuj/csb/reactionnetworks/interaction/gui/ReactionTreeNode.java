package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.ImageObserver;
import java.util.TreeMap;
import java.util.TreeSet;

import de.srsoftware.gui.treepanel.TreeNode;
import edu.fsuj.csb.tools.xml.ObjectComparator;
import edu.fsuj.csb.tools.xml.Tools;

public class ReactionTreeNode extends LeveledTreeNode {
	static TreeMap<Integer,ReactionTreeNode> rtns=new TreeMap<Integer, ReactionTreeNode>();
	private TreeSet<SubstanceTreeNode> substrates=SubstanceTreeNode.set();
	private TreeSet<SubstanceTreeNode> products=SubstanceTreeNode.set();

	public ReactionTreeNode(int id) {
		super("\\small{"+id+"}\\n "+names(id).first());
  }
	
	int counter1=0;
	int counter2=0;

	@Override
	public TreeNode getRoot() {
	  return this;
	}
	
	private static TreeSet<String> names(int id) {
		TreeSet<String> result = Tools.StringSet();
		result.add("reaction_"+id);
		return result;
  }

	public static TreeSet<ReactionTreeNode> set() {
	  return new TreeSet<ReactionTreeNode>(ObjectComparator.get());
  }

	public static ReactionTreeNode get(int id) {
		ReactionTreeNode result = rtns.get(id);
		if (result==null) result=new ReactionTreeNode(id);
	  return result;
  }
	
	public Dimension paint(Graphics g, ImageObserver obs,int levels) {
		Tools.startMethod("SubstanceTreeNode.paint(g,obs,"+levels+")");
		super.paint(g, obs,levels);
		Dimension ownDim = super.paint(g, obs, levels>0);
		if (levels>0) {
			int height = 0;
			for (SubstanceTreeNode product:products){
				if (LeveledTreeNode.hasBeenPainted(product)) continue;

				Dimension dim = product.nodeDimension(g, obs);
				height += dim.height + dist;
			}
			if (height>0){
				int y = getOrigin().y + ((ownDim.height - height) / 2);
				int x = getOrigin().x + ownDim.width + 100;
				for (SubstanceTreeNode product:products) {
					if (LeveledTreeNode.hasBeenPainted(product)) continue;
					product.moveTowards(x, y);
					Dimension dim = product.paint(g, obs, levels-1);
					y += dim.height + dist;
				}
			}
		}
		Tools.endMethod(ownDim);
		return ownDim;
	}
	
	public void addSubstrate(SubstanceTreeNode s) {
	  substrates.add(s);
  }

	public void addProduct(SubstanceTreeNode s) {
		products.add(s);
  }

}
