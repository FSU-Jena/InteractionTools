package edu.fsuj.csb.reactionnetworks.interaction.results;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.SubstanceListNode;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.graph.AdditionsCalculationTask;

/**
 * the result of a AdditionsCalculationTask, i.e. a set of substances, which maximizes the yield of a given compartments reaction set
 * @author Stephan Richter
 *
 */
public class AdditionsCalculationResult extends CalculationResult implements Serializable {

  private static final long serialVersionUID = -5310753200115114801L;

	/**
	 * creates a new result instance
	 * @param additionsCalculationTask the task to which the result belongs
	 * @param mappingFromNumberOfReachedProductsToAdditionalSubtanceId a mapping from the number of reached products to the sets of substances producing them
	 */
	public AdditionsCalculationResult(AdditionsCalculationTask additionsCalculationTask, TreeMap<String, TreeSet<Integer>> mappingFromNumberOfReachedProductsToAdditionalSubtanceId) {
		super(additionsCalculationTask,mappingFromNumberOfReachedProductsToAdditionalSubtanceId);
  }
	
  /* (non-Javadoc)
   * @see edu.fsuj.csb.reactionnetworks.interaction.CalculationResult#resultTreeRepresentation()
   */
  public DefaultMutableTreeNode treeRepresentation() throws SQLException{
		if (getTask() instanceof AdditionsCalculationTask){ // then the result set is a set of integers representing substance ids
			@SuppressWarnings("unchecked")
      TreeMap<String, TreeSet<Integer>> bestAdditionals = (TreeMap<String,TreeSet<Integer>>) result;
			DefaultMutableTreeNode result=new DefaultMutableTreeNode("Result: additionals");
			for (Iterator<Entry<String, TreeSet<Integer>>> it = bestAdditionals.entrySet().iterator();it.hasNext();){
				Entry<String, TreeSet<Integer>> entry = it.next();
				String s=entry.getKey();				
				result.add(new SubstanceListNode("generating "+s+" new products",entry.getValue()));
			}
			return result;
		}
		return null; 
	}
	
}
