package edu.fsuj.csb.reactionnetworks.interaction.tasks.lp;

import edu.fsuj.csb.reactionnetworks.interaction.gui.OptimizationParametersTab.OptimizationParameterSet;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.ParameterSet;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.SubstanceSet;

public class FBATask extends LinearProgrammingTask {

  private static final long serialVersionUID = 2619391610630359556L;

	public FBATask(Integer compartmentId, SubstanceSet substanceSet, OptimizationParameterSet parameterSet) {
		super(compartmentId,substanceSet,parameterSet);
  }

}
