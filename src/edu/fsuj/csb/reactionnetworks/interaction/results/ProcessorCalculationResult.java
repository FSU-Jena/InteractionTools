package edu.fsuj.csb.reactionnetworks.interaction.results;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.CompartmentListNode;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.ProcessorSearchTask;
import edu.fsuj.csb.reactionnetworks.organismtools.gui.DbCompartmentNode;
import edu.fsuj.csb.reactionnetworks.organismtools.gui.DbComponentNode;
import edu.fsuj.csb.tools.organisms.gui.ComponentNode;
import edu.fsuj.csb.tools.xml.NoTokenException;

public class ProcessorCalculationResult extends CalculationResult implements Serializable {

	private static final long serialVersionUID = -1355599796879378411L;
	private TreeSet<Integer> idsOfspontaneouslyReachedSubstances;
	

	public ProcessorCalculationResult(ProcessorSearchTask processorSearchTask, TreeSet<Integer> spontaneouslyReachedSubstances, TreeMap<Integer, TreeSet<Integer>> mappingFromCompartmentsToProcessedSubstances) {		
		super(processorSearchTask, mappingFromCompartmentsToProcessedSubstances);
		this.idsOfspontaneouslyReachedSubstances=spontaneouslyReachedSubstances;
	}

	public DefaultMutableTreeNode resultTreeRepresentation() throws SQLException {
		DefaultMutableTreeNode result = new DefaultMutableTreeNode("Result");
		@SuppressWarnings("unchecked")
		TreeMap<Integer, TreeSet<Integer>> processingCompartments = (TreeMap<Integer, TreeSet<Integer>>) this.result;
		TreeMap<Integer,Integer> mappingFromSizeToNumber=new TreeMap<Integer, Integer>();
		int largest = 0;
		for (Iterator<TreeSet<Integer>> it = processingCompartments.values().iterator(); it.hasNext();) {
			TreeSet<Integer> substanceSet = it.next();
			int size=substanceSet.size();
			if (size > largest) largest = substanceSet.size();
			if (mappingFromSizeToNumber.containsKey(size)) {
				mappingFromSizeToNumber.put(size, mappingFromSizeToNumber.get(size)+1);
			} else mappingFromSizeToNumber.put(size, 1);
		}
		while (largest > 0) {
			DefaultMutableTreeNode set = new CompartmentListNode(mappingFromSizeToNumber.get(largest)+" Compartments processing " + largest + " substances:");
			for (Iterator<Entry<Integer, TreeSet<Integer>>> entryIt = processingCompartments.entrySet().iterator(); entryIt.hasNext();) {
				Entry<Integer, TreeSet<Integer>> entry = entryIt.next();
				TreeSet<Integer> substances = entry.getValue();
				if (substances.size() == largest) {
					DbCompartmentNode compNode = (DbCompartmentNode) DbComponentNode.create(entry.getKey());
/*					try {
	          compNode.loadDetails();
          } catch (MalformedURLException e) {
	          e.printStackTrace();
          } catch (DataFormatException e) {
	          e.printStackTrace();
          }//*/
          DefaultMutableTreeNode processedSubstances=new DefaultMutableTreeNode("can process");
          
					for (Iterator<Integer> subsIt = entry.getValue().iterator(); subsIt.hasNext();)	processedSubstances.add(ComponentNode.create(subsIt.next()));
					compNode.add(processedSubstances);
					set.add(compNode);
				}
			}
			result.add(set);
			largest--;
		}
		return result;
	}

	public DefaultMutableTreeNode treeRepresentation() throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		DefaultMutableTreeNode result = superTreeRepresentation();
		result.add(resultTreeRepresentation());
		return result;
	}

}
