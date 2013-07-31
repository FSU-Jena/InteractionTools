package edu.fsuj.csb.reactionnetworks.interaction.results.lp;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.sql.SQLException;
import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.OptimizationSolution;
import edu.fsuj.csb.reactionnetworks.interaction.SubstanceListNode;
import edu.fsuj.csb.reactionnetworks.interaction.results.CalculationResult;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.CalculationTask;
import edu.fsuj.csb.tools.xml.NoTokenException;

public class LinearProgrammingResult extends CalculationResult {

	private static final long serialVersionUID = -5548444563265563130L;

  public LinearProgrammingResult(CalculationTask calculationTask, OptimizationSolution result) {
	  super(calculationTask, result);
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
