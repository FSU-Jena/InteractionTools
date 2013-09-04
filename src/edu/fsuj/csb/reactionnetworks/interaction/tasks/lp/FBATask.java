package edu.fsuj.csb.reactionnetworks.interaction.tasks.lp;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.sql.SQLException;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.CalculationClient;
import edu.fsuj.csb.reactionnetworks.interaction.OptimizationSolution;
import edu.fsuj.csb.reactionnetworks.interaction.results.lp.FBAResult;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.ParameterSet;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.SubstanceSet;
import edu.fsuj.csb.reactionnetworks.organismtools.DbCompartment;
import edu.fsuj.csb.tools.organisms.ReactionSet;
import edu.fsuj.csb.tools.organisms.gui.CompartmentNode;
import edu.fsuj.csb.tools.xml.NoTokenException;
import edu.fsuj.csb.tools.xml.Tools;

public class FBATask extends LinearProgrammingTask {

  private static final long serialVersionUID = 2619391610630359556L;

	public FBATask(Integer compartmentId, SubstanceSet substanceSet, ReactionSet additionalReactions, ParameterSet parameterSet) {
		super(compartmentId,substanceSet,additionalReactions,parameterSet);
  }

	public boolean addNewSolution(OptimizationSolution solution) {
		Tools.startMethod("FBATask.addNewSolution("+solution.toString().substring(0,50)+")");
		Tools.endMethod(false);
		return false;
	}
	
	@Override
	public void run(CalculationClient calculationClient) throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		Tools.startMethod("FBATask.run("+calculationClient+")");
		try {
			OptimizationSolution solution = runInternal(); // start the actual calculation
			calculationClient.sendObject(new FBAResult(this, solution));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DataFormatException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Tools.endMethod();
	}
	
	/**
	 * @return a hierarchic representation of this task. should be overridden by extending classes
	 * @throws IOException
	 * @throws NoTokenException
	 * @throws AlreadyBoundException
	 * @throws SQLException 
	 */
	public MutableTreeNode treeRepresentation() throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		Tools.startMethod("FBATask.treeRepresentation()");
		DefaultMutableTreeNode result = new DefaultMutableTreeNode(this.getClass().getSimpleName());
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("Compartment/Organism/Species");
		node.add(new CompartmentNode(DbCompartment.load(getCompartmentId())));
		result.add(node);
		result.add(inputTree());
		result.add(outputTree());
		Tools.endMethod(result);
	  return result;
  }
}
