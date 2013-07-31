package edu.fsuj.csb.reactionnetworks.interaction.results;

import java.io.Serializable;
import java.sql.SQLException;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.SeedOptimizationMappingNode;
import edu.fsuj.csb.reactionnetworks.interaction.SeedOptimizationSolution;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.OptimizeBuildTask;

public class SeedOptimizationResult extends CalculationResult implements Serializable {

	private static final long serialVersionUID = 8605356780899770529L;

	public SeedOptimizationResult(OptimizeBuildTask optimizeBuildTask, SeedOptimizationSolution result) {
		super(optimizeBuildTask, result);
	}

	public DefaultMutableTreeNode treeRepresentation() throws SQLException {
		return new SeedOptimizationMappingNode(this);
	}
	
	public SeedOptimizationSolution result(){
		return (SeedOptimizationSolution) super.result();		
	}
}
