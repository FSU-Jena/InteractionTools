package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.ImageObserver;
import java.util.TreeSet;

import de.srsoftware.gui.treepanel.TreeNode;
import edu.fsuj.csb.tools.xml.ObjectComparator;
import edu.fsuj.csb.tools.xml.Tools;

public abstract class LeveledTreeNode extends TreeNode {
	protected int hdist = 20;
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

}
