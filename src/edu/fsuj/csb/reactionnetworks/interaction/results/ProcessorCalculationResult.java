package edu.fsuj.csb.reactionnetworks.interaction.results;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.SubstanceListNode;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.graph.ProcessorSearchTask;
import edu.fsuj.csb.reactionnetworks.organismtools.DbCompartment;
import edu.fsuj.csb.tools.organisms.gui.CompartmentNode;

public class ProcessorCalculationResult extends CalculationResult implements Serializable {

	private static final long serialVersionUID = -1355599796879378411L;
	private TreeSet<Integer> idsOfspontaneouslyReachedSubstances;
	

	public ProcessorCalculationResult(ProcessorSearchTask processorSearchTask, TreeSet<Integer> spontaneouslyReached, TreeMap<Integer, TreeMap<Integer, TreeSet<Integer>>> mappingFromNumberOfProcessedSubstancesToSpeciesToProcessedSubstances) {		
		super(processorSearchTask,mappingFromNumberOfProcessedSubstancesToSpeciesToProcessedSubstances);
		idsOfspontaneouslyReachedSubstances=spontaneouslyReached;
	}
	
	
	@SuppressWarnings("unchecked")
  @Override
	public DefaultMutableTreeNode treeRepresentation() throws SQLException {
		DefaultMutableTreeNode result = new DefaultMutableTreeNode("Result");
		result.add(new SubstanceListNode("Spontaneously formed substances", idsOfspontaneouslyReachedSubstances));
		TreeMap<Integer, TreeMap<Integer, TreeSet<Integer>>> mappingFromNumberOfProcessedSubstancesToSpeciesToProcessedSubstances=(TreeMap<Integer, TreeMap<Integer, TreeSet<Integer>>>) super.result;
		for (Entry<Integer, TreeMap<Integer, TreeSet<Integer>>> entry:mappingFromNumberOfProcessedSubstancesToSpeciesToProcessedSubstances.entrySet()){
			DefaultMutableTreeNode node=new DefaultMutableTreeNode("Species processing "+entry.getKey()+" substances");
			for (Entry<Integer, TreeSet<Integer>> mapFromCidToSid:entry.getValue().entrySet()){
				CompartmentNode cn=new CompartmentNode(DbCompartment.load(mapFromCidToSid.getKey()));
				cn.add(new SubstanceListNode("processed substances", mapFromCidToSid.getValue()));
				node.add(cn);
			}
			result.add(node);
		}
			
		
		return result;	
	}
}
