package edu.fsuj.csb.reactionnetworks.interaction;

import java.util.Enumeration;
import java.util.TreeSet;

import javax.swing.tree.MutableTreeNode;

import edu.fsuj.csb.reactionnetworks.organismtools.gui.DbComponentNode;
import edu.fsuj.csb.tools.organisms.gui.SortedTreeNode;
import edu.fsuj.csb.tools.organisms.gui.SubstanceNode;

public class SubsstanceGroupNode extends SortedTreeNode {

  private static final long serialVersionUID = 5847903660440916938L;
	private TreeSet<Integer> sids=new TreeSet<Integer>();
	public SubsstanceGroupNode(String name) {
		super(name);
  }
	
	@SuppressWarnings("rawtypes")
  public void removeSubstances(TreeSet<Integer> selectedSubstances) {
		for (Enumeration childEnum = children();childEnum.hasMoreElements();){
			SubstanceNode child=(SubstanceNode)childEnum.nextElement();
			if (selectedSubstances.contains(child.substance().id())) child.removeFromParent();
		}
  }

	public void add(int sid) {
		if (!sids.contains(sid)){
			MutableTreeNode sn = DbComponentNode.create(sid);
			//System.err.println("adding sid="+sid);
			sids.add(sid);
			super.add(sn);
		}
  }
}
