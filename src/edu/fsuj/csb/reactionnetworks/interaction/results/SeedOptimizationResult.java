package edu.fsuj.csb.reactionnetworks.interaction.results;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.sql.SQLException;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.SeedOptimizationMappingNode;
import edu.fsuj.csb.reactionnetworks.interaction.SeedOptimizationSolution;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.OptimizeBuildTask;
import edu.fsuj.csb.tools.xml.NoTokenException;

public class SeedOptimizationResult extends CalculationResult implements Serializable {

	private static final long serialVersionUID = 8605356780899770529L;

	public SeedOptimizationResult(OptimizeBuildTask optimizeBuildTask, SeedOptimizationSolution result) {
		super(optimizeBuildTask, result);
	}

	@SuppressWarnings({})
	public DefaultMutableTreeNode treeRepresentation() throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		DefaultMutableTreeNode result = superTreeRepresentation();
		result.add(resultTreeRepresentation());
		return result;
	}

	public DefaultMutableTreeNode resultTreeRepresentation() throws SQLException {
		
		return new SeedOptimizationMappingNode(this);
	}
	
	public SeedOptimizationSolution result(){
		return (SeedOptimizationSolution) super.result();		
	}
}
