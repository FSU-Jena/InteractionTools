package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.ImageObserver;
import java.util.TreeSet;

import de.srsoftware.gui.treepanel.TreeNode;
import edu.fsuj.csb.tools.xml.ObjectComparator;
import edu.fsuj.csb.tools.xml.Tools;

public abstract class LeveledTreeNode extends TreeNode {
	protected int hdist = 30;
	protected int vdist = 5;

	private static TreeSet<LeveledTreeNode> visible=set();
	private static TreeSet<LeveledTreeNode> painted=set();

	public LeveledTreeNode(String f) {
		super(f);
  }

	public Dimension paint(Graphics g, ImageObserver obs, int level){
		Tools.startMethod("LeveledTreeNode.paint(g,obs,"+level+")");
		if (level>0){
			painted.add(this);
			visible.add(this);
		} else visible.remove(this);
		Tools.endMethod(null);
		return null;
	}

	public static TreeSet<LeveledTreeNode> visibleNodes() {
	  return visible;
  }

	public static void clearPainted() {
		painted.clear();
	  
  }

	private static TreeSet<LeveledTreeNode> set() {
	  return new TreeSet<LeveledTreeNode>(ObjectComparator.get());
  }

	public static boolean hasBeenPainted(LeveledTreeNode node) {
	  return painted.contains(node);
  }
	
	protected static void drawArrow(Graphics g, int x, int y, int x2, int y2) {
		int l=30;
		double d=30*Math.PI/360;

		g.drawLine(x,y,x2,y2);
		double h=y2-y;
		double b=x2-x;
		double alpha=-Math.atan(h/b);
		
		x=x2-(int) Math.round(l*Math.cos(alpha-d));
		y=y2+(int) Math.round(l*Math.sin(alpha-d));
		g.drawLine(x,y,x2,y2);
		
		x=x2-(int) Math.round(l*Math.cos(alpha+d));
		y=y2+(int) Math.round(l*Math.sin(alpha+d));
		g.drawLine(x,y,x2,y2);
  }
}
