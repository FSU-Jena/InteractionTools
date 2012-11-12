package edu.fsuj.csb.reactionnetworks.interaction.results;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.SubstanceListNode;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.ProductCalculationTask;
import edu.fsuj.csb.tools.xml.NoTokenException;

public class ProductCalculationResult extends CalculationResult implements Serializable {

  private static final long serialVersionUID = -4296568528575679231L;

	public ProductCalculationResult(ProductCalculationTask productCalculationTask, Collection<Integer> productIds) {
		super(productCalculationTask, productIds);
  }
	@SuppressWarnings({})
	public DefaultMutableTreeNode treeRepresentation() throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		DefaultMutableTreeNode result = superTreeRepresentation();
		result.add(resultTreeRepresentation());
		return result;
	}

	@SuppressWarnings("unchecked")
  public DefaultMutableTreeNode resultTreeRepresentation() throws SQLException {		
		return new SubstanceListNode("produced substances", (TreeSet<Integer>) this.result);
	}
}
