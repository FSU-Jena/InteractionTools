package edu.fsuj.csb.reactionnetworks.interaction.tasks.lp;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.sql.SQLException;
import java.util.zip.DataFormatException;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.CalculationClient;
import edu.fsuj.csb.reactionnetworks.interaction.OptimizationSolution;
import edu.fsuj.csb.reactionnetworks.interaction.results.lp.LinearProgrammingResult;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.ParameterSet;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.SubstanceSet;
import edu.fsuj.csb.tools.xml.NoTokenException;

public class FBATask extends LinearProgrammingTask {

  private static final long serialVersionUID = 2619391610630359556L;

	public FBATask(Integer compartmentId, SubstanceSet substanceSet, ParameterSet parameterSet) {
		super(compartmentId,substanceSet,parameterSet);
  }

	public boolean addNewSolution(OptimizationSolution solution) {
		return false;
	}
	
	public void run(CalculationClient calculationClient) throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		try {
			while (true) {
				solutions = OptimizationSolution.set();
				OptimizationSolution solution = runInternal(); // start the actual calculation
				if (!addNewSolution(solution)) break;
				calculationClient.sendObject(new FBAResult(this, solution));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DataFormatException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @return a hierarchic representation of this task. should be overridden by extending classes
	 * @throws IOException
	 * @throws NoTokenException
	 * @throws AlreadyBoundException
	 * @throws SQLException 
	 */
	public MutableTreeNode treeRepresentation() throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		DefaultMutableTreeNode result = new DefaultMutableTreeNode(this.getClass().getSimpleName());
		result.add(inputTree());
		result.add(outputTree());
	  return result;
  }
}
