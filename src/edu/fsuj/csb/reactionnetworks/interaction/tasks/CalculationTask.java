package edu.fsuj.csb.reactionnetworks.interaction.tasks;
import java.io.IOException;
import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.sql.SQLException;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.CalculationClient;
import edu.fsuj.csb.reactionnetworks.interaction.results.CalculationResult;
import edu.fsuj.csb.tools.xml.NoTokenException;


/**
 * thsi is the base class for all calculation tasks
 * @author Stephan Richter
 *
 */
public abstract class CalculationTask implements Serializable {

  private static final long serialVersionUID = -252544614439255597L;
  private int number=0;
  private static int currentNumber=0;
  
  /**
   * default constructor
   */
  public CalculationTask() {
  	number=++currentNumber;
  }
  
  /**
   * calls the run method of the derived classes and sends the "done" signal after calculation has completed
   * @param calculationClient the calculations client, which is used to return the results to the interaction toolbox
   * @throws IOException
   * @throws NoTokenException
   * @throws AlreadyBoundException
   * @throws SQLException 
   */
  public void start(CalculationClient calculationClient) throws IOException, NoTokenException, AlreadyBoundException, SQLException{
  	run(calculationClient);
  	try {
	    Thread.sleep(1000);
    } catch (InterruptedException e) {
	    e.printStackTrace();
    }
  	calculationClient.sendObject(new CalculationResult(this, "done"));
  	System.out.println("execution finished.");
  }
  
  /**
   * in the derived classes, this method does the actual computation
   * @param calculationClient
   * @throws IOException
   * @throws NoTokenException
   * @throws AlreadyBoundException
   * @throws SQLException 
   */
  public abstract void run(CalculationClient calculationClient) throws IOException, NoTokenException, AlreadyBoundException, SQLException;
	/**
	 * @return a hierarchic representation of this task. should be overridden by extending classes
	 * @throws IOException
	 * @throws NoTokenException
	 * @throws AlreadyBoundException
	 * @throws SQLException 
	 */
	public MutableTreeNode treeRepresentation() throws IOException, NoTokenException, AlreadyBoundException, SQLException {
	  return new DefaultMutableTreeNode(this.getClass().getSimpleName());
  }
	
	/**
	 * @return the class name of this calculation task
	 */
	public String className(){
		return this.getClass().getSimpleName();
	}

	/**
	 * @return the task number of this task
	 */
	public int getNumber() {
	  return number;
  }
	
	/**
	 * changes the number of this taks
	 * @param number the new number for this task
	 */
	public void setNumber(int number) {
		this.number=number;
  }
}
