package edu.fsuj.csb.reactionnetworks.interaction.results;

import java.io.Serializable;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.fsuj.csb.reactionnetworks.interaction.tasks.ProcessorSearchTask;

public class ProcessorCalculationResult extends CalculationResult implements Serializable {

	private static final long serialVersionUID = -1355599796879378411L;
	private TreeSet<Integer> idsOfspontaneouslyReachedSubstances;
	

	public ProcessorCalculationResult(ProcessorSearchTask processorSearchTask, TreeSet<Integer> spontaneouslyReached, TreeMap<Integer, TreeMap<Integer, TreeSet<Integer>>> mappingFromNumberOfProcessedSubstancesToSpeciesToProcessedSubstances) {		
		super(processorSearchTask,mappingFromNumberOfProcessedSubstancesToSpeciesToProcessedSubstances);
		idsOfspontaneouslyReachedSubstances=spontaneouslyReached;
	}

}
