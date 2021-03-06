package edu.fsuj.csb.reactionnetworks.interaction.results;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.tasks.CalculationTask;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.graph.ProductCalculationTask;
import edu.fsuj.csb.reactionnetworks.organismtools.gui.DbComponentNode;
import edu.fsuj.csb.tools.xml.NoTokenException;

/**
 * base class for the different resuts of calculation tasks
 * @author Stephan Richter
 *
 */
public class CalculationResult implements Serializable {

	private static final long serialVersionUID = 4979475206871894164L;
	private CalculationTask task;
	protected Object result;

	/**
	 * create new calculation result instance
	 * @param calculationTask
	 * @param result
	 */
	public CalculationResult(CalculationTask calculationTask, Object result) {
		task = calculationTask;
		this.result = result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return CalculationResult.class + "(" + task + ") = " + result;
	}
	
	/**
	 * converts a number to its string representation including up to 2 leading zeroes
	 * @param n the number to be converted
	 * @return the number's string representation with leading zeros
	 */
	public static String leadingZeros(int n){
		if (n<10) return "00"+n;
		if (n<100) return "0"+n;
		return ""+n;
	}

	/**
	 * @return a tree representation for the task this result belongs to, which can be used to display the result hierarchically
	 * @throws IOException
	 * @throws NoTokenException
	 * @throws AlreadyBoundException
	 * @throws SQLException 
	 */
	public DefaultMutableTreeNode getTreeRepresentation() throws IOException, NoTokenException, AlreadyBoundException, SQLException {		
		DefaultMutableTreeNode node=new DefaultMutableTreeNode("Task "+leadingZeros(task.getNumber()));
		System.out.println(task.getClass().getSimpleName());
		node.add(task.treeRepresentation());
		node.add(treeRepresentation());
		return node;
	}
	
	/**
	 * @return a tree representation of the result
	 * @throws SQLException 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
  public DefaultMutableTreeNode treeRepresentation() throws SQLException{		
		if (task instanceof ProductCalculationTask){ // then the result set is a set of integers representing substance ids
			Collection<Integer> resultset = ((Collection<Integer>) result);
			DefaultMutableTreeNode node=new DefaultMutableTreeNode("newly produced substances ("+(resultset.size()-((ProductCalculationTask)task).getSubstances().size())+" items)");
			for (Iterator<Integer> it = resultset.iterator();it.hasNext();){
				int sid=it.next();
				if (!((ProductCalculationTask)task).getSubstances().contains(sid)) node.add(DbComponentNode.create(sid));
			}
			return node;
		} 
		if (this.result instanceof TreeSet){
			TreeSet set=(TreeSet) this.result;
			DefaultMutableTreeNode node=new DefaultMutableTreeNode("Result ("+set.size()+" Items)");
			return node;
		}
		return null;
	}
	
	/**
	 * @return get the task, to which this result belongs
	 */
	public CalculationTask getTask(){
		return task;
	}
	
	/**
	 * @return get the internal result data
	 */
	public Object result(){
		return result;
	}
}
