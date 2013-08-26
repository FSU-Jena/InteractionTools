package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.ImageObserver;
import java.util.Iterator;
import java.util.TreeSet;

import de.srsoftware.gui.treepanel.TreeNode;
import edu.fsuj.csb.tools.xml.ObjectComparator;
import edu.fsuj.csb.tools.xml.Tools;

public abstract class LeveledTreeNode extends TreeNode {
	protected int hdist = 30;
	protected int vdist = 5;

	private static TreeSet<LeveledTreeNode> visible=set();
	private static TreeSet<LeveledTreeNode> painted=set();

	protected TreeSet<TreeNode> nameNodes=new TreeSet<TreeNode>(ObjectComparator.get());

	public LeveledTreeNode(String f) {
		super(f);
  }
	
	protected void drawNameNodes(Graphics g, ImageObserver obs,Dimension ownDim, int level) {
		if (level == 3 && !nameNodes.isEmpty()) {
			int x = getOrigin().x;
			int y = getOrigin().y + ownDim.height/2;
			
			int h=hdist;
			Iterator<TreeNode> it=nameNodes.iterator();
			it.next();
			TreeNode nameNode=null;
			while (it.hasNext()){
				nameNode = it.next();
				Dimension d=nameNode.nodeDimension(g, obs);
				h+=vdist+d.height/2;
				nameNode.moveTowards(x, y+h);
				h+=d.height/2;
			}
			if (nameNode!=null)	g.drawLine(x, y, nameNode.getOrigin().x,nameNode.getOrigin().y);
			y+=hdist;
			
			it=nameNodes.iterator();
			it.next();
			while (it.hasNext()){
				it.next().paint(g, obs);
			}			
		}
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
