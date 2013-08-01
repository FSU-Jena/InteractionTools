package edu.fsuj.csb.reactionnetworks.interaction.results.lp;

import java.io.Serializable;
import java.sql.SQLException;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.OptimizationSolution;
import edu.fsuj.csb.reactionnetworks.interaction.results.CalculationResult;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.CalculationTask;

public class LinearProgrammingResult extends CalculationResult implements Serializable{

	private static final long serialVersionUID = -5548444563265563130L;

  public LinearProgrammingResult(CalculationTask calculationTask, OptimizationSolution result) {
	  super(calculationTask, result);
  }

  public DefaultMutableTreeNode treeRepresentation() throws SQLException {		
		return new DefaultMutableTreeNode("Result:");
	}
}
