package edu.fsuj.csb.reactionnetworks.interaction.tasks;

import edu.fsuj.csb.tools.organisms.CalculationIntermediate;


public abstract class StructuredTask extends CalculationTask {

	/**
   * 
   */
  private static final long serialVersionUID = -7182005260920692888L;
	protected int numberOfPendingSubtasks=0;
	
	/**
	 * increases the counter of the related subtasks
	 */
	public void increaseSubtaskCounter() {
		numberOfPendingSubtasks++;	  
  }

	/**
	 * adds an intermediate result (i.e. a result of one of the subtasks)
	 * @param result a result of a related subtask
	 */
	public void addIntermediateResult(Object result) {
	  if (!(result instanceof CalculationIntermediate)) throw new ClassCastException("Result is not an Seed Calculation Intermediate!");
	  numberOfPendingSubtasks--;
	  handleIntermediateResultData((CalculationIntermediate)result);
  }
	
	abstract void handleIntermediateResultData(CalculationIntermediate result);

	/**
	 * tests whether te current task is ready to be started, i.e. whether all pending substasks have returned their results
	 * @return true, if and only if there are no pending substasks left
	 */
	public boolean readyToRun(){
		return numberOfPendingSubtasks==0;
	}
}
