package edu.fsuj.csb.reactionnetworks.interaction.tasks;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.sql.SQLException;
import java.util.Collection;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.CalculationClient;
import edu.fsuj.csb.reactionnetworks.interaction.results.ProductCalculationResult;
import edu.fsuj.csb.reactionnetworks.organismtools.DbCompartment;
import edu.fsuj.csb.reactionnetworks.organismtools.gui.DbComponentNode;
import edu.fsuj.csb.tools.organisms.Compartment;
import edu.fsuj.csb.tools.xml.NoTokenException;

public class ProductCalculationTask extends TaskContainingCompartmentAndSubtances {

	/**
   * 
   */
	private static final long serialVersionUID = 6894766314073591869L;

	public ProductCalculationTask(int cid, Collection<Integer> substances) {
		super(cid, substances);
	}
	
	public void run(CalculationClient client) throws IOException, SQLException  {
		Compartment c=DbCompartment.load(compartmentId);
		Collection<Integer> productIds;
    productIds = c.calculateProductsOf(substanceIds);
    client.sendObject(new ProductCalculationResult(this, productIds));
	}
		
	public MutableTreeNode treeRepresentation() throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		DefaultMutableTreeNode result = new DefaultMutableTreeNode("Task: Calculate products ["+this.getClass().getSimpleName()+"]");
		result.add(DbComponentNode.create(compartmentId));
		DefaultMutableTreeNode inputs=super.inputTree();
		result.add(inputs);
		return result;
	}

	public String toString() {
		return "ProductCalculationTask(" + substanceIds + " ==> " + compartmentId + ")";
	}

}
