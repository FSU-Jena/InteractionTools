package edu.fsuj.csb.reactionnetworks.interaction.results;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collection;
import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.SubstanceListNode;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.graph.ProductCalculationTask;

public class ProductCalculationResult extends CalculationResult implements Serializable {

  private static final long serialVersionUID = -4296568528575679231L;

	public ProductCalculationResult(ProductCalculationTask productCalculationTask, Collection<Integer> productIds) {
		super(productCalculationTask, productIds);
  }


	@SuppressWarnings("unchecked")
  public DefaultMutableTreeNode treeRepresentation() throws SQLException {		
		return new SubstanceListNode("Result: produced substances", (TreeSet<Integer>) this.result);
	}
}
