package edu.fsuj.csb.reactionnetworks.interaction.tasks.graph;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.tree.MutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.CalculationClient;
import edu.fsuj.csb.reactionnetworks.interaction.results.AdditionsCalculationResult;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.TaskContainingCompartmentAndSubtances;
import edu.fsuj.csb.reactionnetworks.organismtools.DbCompartment;
import edu.fsuj.csb.tools.xml.NoTokenException;
import edu.fsuj.csb.tools.xml.ObjectComparator;

/**
 * this task calculates for a given set of substances in a given compartment, which set of substances maximies the yoield of the compartments reaction. here, yield means the number of reachable substances
 * @author Stephan Richter
 *
 */
public class AdditionsCalculationTask extends TaskContainingCompartmentAndSubtances {

	private static final long serialVersionUID = 6894766314073591869L;

	/**
	 * @param cid the compartment id of the compartment to use
	 * @param substances the set of substances, for which the best set of additional substances shall be calculated
	 */
	public AdditionsCalculationTask(int cid, Collection<Integer> substances) {
		super(cid, substances);
	}

	/* (non-Javadoc)
	 * @see edu.fsuj.csb.reactionnetworks.interaction.CalculationTask#run(edu.fsuj.csb.reactionnetworks.interaction.CalculationClient)
	 */
	public void run(CalculationClient client) throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		TreeMap<String, TreeSet<Integer>> mappingFromNumberOfReachedProductsToAdditionalSubtanceId = new TreeMap<String, TreeSet<Integer>>(ObjectComparator.get());
		DbCompartment c = DbCompartment.load(compartmentId);

    Collection<Integer> currentProducts = c.calculateProductsOf(substanceIds);
    int currentSize = currentProducts.size();

    TreeSet<Integer> allSubstances = c.utilizedSubstances();
    allSubstances.removeAll(currentProducts);
    for (Iterator<Integer> it = allSubstances.iterator(); it.hasNext();) {
    	int currentAdditional = it.next();
    	TreeSet<Integer> testSet = new TreeSet<Integer>(substanceIds);
    	testSet.add(currentAdditional);
    	
    	Collection<Integer> newProducts = c.calculateProductsOf(testSet);
    	Integer newCount=newProducts.size() -1 - currentSize; // -1 because, the tested component is also in the new products but not actually produced
    	String key = ((newCount<100)?"0":"")+((newCount<10)?"0":"")+newCount.toString();				
    	
    	if (!mappingFromNumberOfReachedProductsToAdditionalSubtanceId.containsKey(key)) mappingFromNumberOfReachedProductsToAdditionalSubtanceId.put(key, new TreeSet<Integer>());
    	mappingFromNumberOfReachedProductsToAdditionalSubtanceId.get(key).add(currentAdditional);
    }
    client.sendObject(new AdditionsCalculationResult(this, mappingFromNumberOfReachedProductsToAdditionalSubtanceId));
	}

	/* (non-Javadoc)
	 * @see edu.fsuj.csb.reactionnetworks.interaction.TaskContainingCompartmentAndSubtances#toString()
	 */
	public String toString() {
		return "AdditionsCalculationTask(" + substanceIds + ")";
	}
	
	/* (non-Javadoc)
	 * @see edu.fsuj.csb.reactionnetworks.interaction.TaskContainingCompartmentAndSubtances#treeRepresentation()
	 */
	public MutableTreeNode treeRepresentation() throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		MutableTreeNode result=super.treeRepresentation("Task: Calculate best additionals ["+this.getClass().getSimpleName()+"]");
		return result;
	}

}
