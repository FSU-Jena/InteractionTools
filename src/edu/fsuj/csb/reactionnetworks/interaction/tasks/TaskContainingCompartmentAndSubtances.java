package edu.fsuj.csb.reactionnetworks.interaction.tasks;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.sql.SQLException;
import java.util.Collection;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.SubstanceListNode;
import edu.fsuj.csb.tools.organisms.gui.ComponentNode;
import edu.fsuj.csb.tools.xml.NoTokenException;


public abstract class TaskContainingCompartmentAndSubtances extends CalculationTask {
  private static final long serialVersionUID = 8394003713911216366L;
	protected Collection<Integer> substanceIds;
	protected int compartmentId;
	

	public TaskContainingCompartmentAndSubtances(int compartmentId, Collection<Integer> substances) {
		this.compartmentId=compartmentId;
		this.substanceIds=substances;
  }
	
	public Collection<Integer> getSubstances(){
		return substanceIds;
	}
	
	public int getCompartmentId(){
		return compartmentId;
	}
	
	@Override
	public String toString() {	 
	  return "TaskContainingOrganismAndSubtances("+substanceIds+" | "+compartmentId+")";
	}

	public DefaultMutableTreeNode inputTree() throws SQLException {
		return new SubstanceListNode("given input substances",substanceIds);
  }
	
	public DefaultMutableTreeNode treeRepresentation(String text) throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		DefaultMutableTreeNode result = new DefaultMutableTreeNode(text);
		DefaultMutableTreeNode inputs = inputTree();
		result.add(ComponentNode.create(compartmentId));
		result.add(inputs);
		return result;
	} 
	
	public MutableTreeNode treeRepresentation() throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		return treeRepresentation(this.getClass().getSimpleName());
	}
}
