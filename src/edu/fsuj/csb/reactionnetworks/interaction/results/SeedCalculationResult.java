package edu.fsuj.csb.reactionnetworks.interaction.results;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.SubstanceListNode;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.CalculationTask;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.SeedCalculationTask;
import edu.fsuj.csb.tools.xml.NoTokenException;

public class SeedCalculationResult extends CalculationResult {

	private static final long serialVersionUID = 4369093089997239188L;
	private TreeSet<TreeSet<Integer>> cont;

	public SeedCalculationResult(CalculationTask calculationTask, TreeSet<TreeSet<Integer>> seedSets, TreeSet<TreeSet<Integer>> continouslyAvailableSubstances) {
		super(calculationTask, seedSets);
		cont = continouslyAvailableSubstances;
	}

	public DefaultMutableTreeNode resultTreeRepresentation() throws SQLException {
		@SuppressWarnings("unchecked")
    TreeSet<TreeSet<Integer>> seedSets = (TreeSet<TreeSet<Integer>>) result;
		DefaultMutableTreeNode result = new DefaultMutableTreeNode("Result");
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("continously available substances ("+cont.size()+")");
		for (Iterator<TreeSet<Integer>> cycleIterator = cont.iterator();cycleIterator.hasNext();){
			node.add(new SubstanceListNode("Cycle", cycleIterator.next()));
		}
		result.add(node);
		node = new DefaultMutableTreeNode("seed sets (" + seedSets.size() + ")");
		int number = 1;
		for (Iterator<TreeSet<Integer>> seedIterator = seedSets.iterator(); seedIterator.hasNext();) {			
			node.add(new SubstanceListNode("set " + (number++),seedIterator.next()));
		}
		result.add(node);
		return result;
	}
	
	/**
	 * @return a tree representation for the task this result belongs to, which can be used to display the result hierarchically
	 * @throws IOException
	 * @throws NoTokenException
	 * @throws AlreadyBoundException
	 * @throws SQLException 
	 */
	public DefaultMutableTreeNode superTreeRepresentation() throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		SeedCalculationTask task = (SeedCalculationTask) getTask();
		DefaultMutableTreeNode node=new DefaultMutableTreeNode("Task "+leadingZeros(task.getNumber())+"-"+leadingZeros(task.maxTaskNumber()));
		node.insert(task.treeRepresentation(), 0);
		return node;
	}
}
