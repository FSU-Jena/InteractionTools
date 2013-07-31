package edu.fsuj.csb.reactionnetworks.interaction.results.lp;

import java.io.Serializable;

import edu.fsuj.csb.reactionnetworks.interaction.OptimizationSolution;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.CalculationTask;

public class FBAResult extends LinearProgrammingResult implements Serializable {

  private static final long serialVersionUID = -7959749688763855428L;

	public FBAResult(CalculationTask calculationTask, OptimizationSolution result) {
	  super(calculationTask, result);
  }

}
