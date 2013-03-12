package edu.fsuj.csb.reactionnetworks.interaction;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.reactionnetworks.organismtools.DbSubstance;
import edu.fsuj.csb.reactionnetworks.organismtools.gui.DbComponentNode;

public class SubstanceListNode extends DefaultMutableTreeNode {


  private static final long serialVersionUID = 3209175178946905517L;

	public SubstanceListNode(String caption, Collection<Integer> substanceIds) throws SQLException {
	  super(caption+" ("+substanceIds.size()+")");
	  for (Iterator<Integer> it = substanceIds.iterator(); it.hasNext();) {
	  	int sId=it.next();
	  	DbSubstance.load(sId);
	  	add(DbComponentNode.create(sId));
	  }
  }

}
