package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.image.ImageObserver;
import java.sql.SQLException;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import de.srsoftware.gui.treepanel.TreeNode;
import edu.fsuj.csb.reactionnetworks.organismtools.DbReaction;
import edu.fsuj.csb.tools.xml.ObjectComparator;
import edu.fsuj.csb.tools.xml.Tools;

public class ReactionTreeNode extends LeveledTreeNode {
	static TreeMap<Integer,ReactionTreeNode> rtns=new TreeMap<Integer, ReactionTreeNode>();
	private TreeSet<SubstanceTreeNode> substrates=SubstanceTreeNode.set();
	private TreeSet<SubstanceTreeNode> products=SubstanceTreeNode.set();
	private int id;
	private BasicStroke arrowStroke=new BasicStroke(2); 
	private DbReaction dbr;

	public ReactionTreeNode(int id) throws SQLException {
		super("\\small{"+id+"}");
		dbr = DbReaction.load(id);
		setText("\\small{Reaction "+id+"}\\n "+names().first().replace(" <=>", " \\<=> "));
		this.id=id;
		rtns.put(id, this);
  }
	
	int counter1=0;
	int counter2=0;

	@Override
	public TreeNode getRoot() {
	  return this;
	}
	
	private TreeSet<String> names() {
		return dbr.names();
  }

	public static TreeSet<ReactionTreeNode> set() {
	  return new TreeSet<ReactionTreeNode>(ObjectComparator.get());
  }

	public static ReactionTreeNode get(int id) throws SQLException {
		ReactionTreeNode result = rtns.get(id);
		if (result==null) result=new ReactionTreeNode(id);
	  return result;
  }
	
	public Dimension paint(Graphics g, ImageObserver obs,int level) {
		Tools.startMethod("SubstanceTreeNode.paint(g,obs,"+level+")");
		super.paint(g, obs,level);
		Dimension ownDim = super.paint(g, obs, false);
		if (level>0) {
			Graphics2D g2=(Graphics2D) g;
			Stroke stroke = g2.getStroke();
			
			Font oldFont = g.getFont();
			float oldSize = oldFont.getSize();
  		g.setFont(oldFont.deriveFont(oldSize * 5 / 6));

			int height = 0;	
			for (SubstanceTreeNode substrate:substrates){
				if (LeveledTreeNode.hasBeenPainted(substrate)) continue;

				Dimension dim = substrate.nodeDimension(g, obs);
				height += dim.height + vdist;
			}			
			if (height>0){
				int y = getOrigin().y+vdist + ((ownDim.height - height) / 2);
				int x = getOrigin().x - hdist*level*level - ownDim.width/2;
				g2.setStroke(arrowStroke);
				for (SubstanceTreeNode substrate:substrates) {
					if (LeveledTreeNode.hasBeenPainted(substrate)) continue;
					Dimension dim=substrate.nodeDimension(g, obs);
					substrate.moveTowards(x-dim.width/2, y);
					if (level>1) drawArrow(g, substrate.getOrigin().x+dim.width/2, substrate.getOrigin().y,getOrigin().x-ownDim.width/2, getOrigin().y);
					substrate.paint(g, obs, 1);
					y += dim.height + vdist;
				}
				g2.setStroke(stroke);
			}
			
			
			height = 0;
			for (SubstanceTreeNode product:products){
				if (LeveledTreeNode.hasBeenPainted(product)) continue;

				Dimension dim = product.nodeDimension(g, obs);
				height += dim.height + vdist;
			}			
			if (height>0){
				int y = getOrigin().y + ((ownDim.height - height) / 2);
				int x = getOrigin().x + ownDim.width/2 + hdist*level*level;
				g2.setStroke(arrowStroke);
				for (SubstanceTreeNode product:products) {
					if (LeveledTreeNode.hasBeenPainted(product)) continue;
					Dimension dim=product.nodeDimension(g, obs);
					product.moveTowards(x+dim.width/2, y);
					if (level>1) drawArrow(g,getOrigin().x+ownDim.width/2, getOrigin().y, product.getOrigin().x-dim.width/2, product.getOrigin().y);
					product.paint(g, obs, level-1);
					y += dim.height + vdist;
				}
				g2.setStroke(stroke);
			}

			g.setFont(oldFont);
			super.paint(g, obs, true);
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

	public void setParent(SubstanceTreeNode node) {
		//factor=(products.contains(node))?-1:1;
  }

	public int id() {
	  return id;
  }

	public void loadSubstances() throws SQLException {
		for (Entry<Integer, Integer> substrateMap:dbr.substrates().entrySet()){
			addSubstrate(SubstanceTreeNode.get(substrateMap.getKey()));
		}
		for (Entry<Integer, Integer> productMap:dbr.products().entrySet()){
			addProduct(SubstanceTreeNode.get(productMap.getKey()));
		}
  }

	public boolean hasSubstrate(int id) {
	  return dbr.substrateIds().contains(id);
  }

}
