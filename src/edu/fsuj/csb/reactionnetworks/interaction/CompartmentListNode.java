package edu.fsuj.csb.reactionnetworks.interaction;

import java.sql.SQLException;
import java.util.Enumeration;
import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.reactionnetworks.organismtools.gui.DbCompartmentNode;
import edu.fsuj.csb.reactionnetworks.organismtools.gui.DbComponentNode;
import edu.fsuj.csb.tools.organisms.gui.CompartmentNode;
import edu.fsuj.csb.tools.xml.ObjectComparator;

/**
 * This node collects a set of compartmentNodes. It has been created, because for nodes holding compartmentNodes as children a special popup shall be applied.
 * @author Stephan Richter
 *
 */
public class CompartmentListNode extends DefaultMutableTreeNode {

  /**
   * creates a new node with a given text
   * @param string the text to apply to this node
   */
  public CompartmentListNode(String string) {
  	super(string);
  }

	private static final long serialVersionUID = 7944818555348453096L;

	/**
	 * @return the list of compartmentNodes appended to this node
	 * @throws SQLException 
	 */
	public TreeSet<CompartmentNode> compartments() throws SQLException {
	
	  TreeSet<CompartmentNode> result=new TreeSet<CompartmentNode>(ObjectComparator.get());
	  for (@SuppressWarnings("unchecked") Enumeration<DefaultMutableTreeNode>children=children(); children.hasMoreElements();){
	  	DefaultMutableTreeNode child=children.nextElement();
	  	if (child instanceof DbCompartmentNode) {
	  		DbCompartmentNode cn=(DbCompartmentNode)child;
	  		result.add((DbCompartmentNode) DbComponentNode.create(cn.compartment().id()));
	  	}
	  }
		return result;
  }

}
