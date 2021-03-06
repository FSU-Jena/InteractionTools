package edu.fsuj.csb.reactionnetworks.interaction;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.distributedcomputing.tools.ClientHandle;
import edu.fsuj.csb.distributedcomputing.tools.Master;
import edu.fsuj.csb.reactionnetworks.interaction.gui.OptimizationParametersTab.OptimizationParameterSet;
import edu.fsuj.csb.reactionnetworks.interaction.gui.ResultTreeRoot;
import edu.fsuj.csb.reactionnetworks.interaction.results.CalculationResult;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.CalculationTask;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.EvolveSeedsTask;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.FindPathTask;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.OptimizeBuildTask;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.SeedCalculationTask;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.StructuredTask;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.graph.AdditionsCalculationTask;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.graph.ProcessorSearchTask;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.graph.ProductCalculationTask;
import edu.fsuj.csb.tools.organisms.gui.CompartmentNode;
import edu.fsuj.csb.tools.xml.NoTokenException;
import edu.fsuj.csb.tools.xml.ObjectComparator;

/**
 * central dispatcher for tasks and results. this class handles all task calls from the GUI and also manages incoming results
 * @author Stephan Richter
 *
 */
/**
 * @author Stephan Richter
 *
 */
public class ActionHandler extends Master {

	private JTree resultTree;
	//private NetworkLoader networkLoader;
	private TreeMap<Integer, DefaultMutableTreeNode> mappingFromTaskNumbersToResultCollectors = new TreeMap<Integer, DefaultMutableTreeNode>(ObjectComparator.get());
	private TreeMap<Integer,StructuredTask> mappingFromSubtasktToOwner = new TreeMap<Integer, StructuredTask>(ObjectComparator.get());
	private SimpleDateFormat formatter = new SimpleDateFormat("yy-MM-dd HH:mm:ss");

	/**
	 * this thread is used to send a calculation task without blocking the main application while waiting for a client
	 * @author Stephan Richter
	 *
	 */
	private class sendThread extends Thread {
		private CalculationTask ct;
		/**
		 * creates a new thread instance
		 * @param ct the calculation task, which shall be sent
		 */
		public sendThread(CalculationTask ct){
			this.ct=ct;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		public void run(){
			try {
	      sleep(100);
      } catch (InterruptedException e) {}
			try {
	      sendTask(ct);
      } catch (IOException e) {
      	e.printStackTrace();
      }
		}
	}

	/**
	 * creates a new action handler instane
	 * @param resultTree the JTree, to which result trees shall be added in order to display them
	 * @throws IOException 
	 */
	public ActionHandler(JTree resultTree,int port) throws IOException {
		super(port);
		this.resultTree = resultTree;
	}

	/**
	 * sends a task object to the next available client
	 * @param ct the calculation task which shall be sended
	 * @throws IOException
	 */
	public void sendTask(CalculationTask ct) throws IOException {
		ClientHandle handle = getIdleHandle();
		while (handle == null) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {}
			handle = getIdleHandle();
		}
		handle.send(ct);
	}

	/**
	 * tries to calculate the precursor substances for all given targetSubstances in all given compartments
	 * @param compartmentNodes the ids of the compartments to be examined
	 * @param targetSubstanceIds the ids of the targeted substances
	 * @param ignoredSubstances 
	 * @param ignoreUnbalanced 
	 * @param useMilp 
	 * @throws IOException
	 */
	public void calcSeeds(TreeSet<CompartmentNode> compartmentNodes, TreeSet<Integer> targetSubstanceIds, TreeSet<Integer> ignoredSubstances, boolean ignoreUnbalanced, boolean useMilp) throws IOException {
		if (warnforEmptyList(targetSubstanceIds)) return;
		for (Iterator<CompartmentNode> it = compartmentNodes.iterator(); it.hasNext();) {	// loop through the set of compartments		
			int cid = it.next().compartment().id();
			calcSeeds(cid,targetSubstanceIds,ignoredSubstances,ignoreUnbalanced,useMilp); // calculate seed for specific compartment
		}
	}

	/**
	 * Tries to calculate the seed substances for all given target substances in a specific compartment. Therefor, potential precursors for all target substances are calculated first. As soon as the precursors for all targets are calculated, the actual seedset calculation is started
	 * @param compartmentId the id of the compartment to be examined
	 * @param targetSubstanceIds the id of the targeted substances
	 * @param ignoredSubstances 
	 * @throws IOException
	 */
	private void calcSeeds(int compartmentId, TreeSet<Integer> targetSubstanceIds, TreeSet<Integer> ignoredSubstances,boolean ignoreUnbalanced,boolean useMilp) throws IOException {
		SeedCalculationTask sct=new SeedCalculationTask(compartmentId,targetSubstanceIds,ignoredSubstances,useMilp,ignoreUnbalanced);
		sendTask(sct);
  }

	/**
	 * calculates the set of substnaces which may be produced by the given compartments when supplied with the given substances
	 * @param compartments the set of compartments, for which the calculation shall be performed
	 * @param substanceIds the ids of the substances, which shall be given as substrates to the calculation
	 * @throws IOException
	 */
	public void calculateProducts(TreeSet<CompartmentNode> compartments, TreeSet<Integer> substanceIds) throws IOException {
		if (warnforEmptyList(substanceIds)) return;
		for (Iterator<CompartmentNode> speciesIterator = compartments.iterator(); speciesIterator.hasNext();) {
			int cid = speciesIterator.next().compartment().id();
			ProductCalculationTask pct = new ProductCalculationTask(cid, substanceIds);
			sendTask(pct);
		}
	}
	
	/**
	 * checks, whether a given CalculationResult is assigned to a structured task, i.e. whether the result is a calculation intermediate
	 * @param cr the calculation result, which will be tested
	 * @return true, if there is a mapping from the calculation result to a structured task
	 */
	public boolean belongsToStructuredTask(CalculationResult cr){
		return mappingFromSubtasktToOwner.containsKey(cr.getTask().getNumber());
	}
	
	/**
	 * requests the task an intermediate calculation result belongs to
	 * @param cr the calculation result
	 * @return the structured task it belongs to, or null, if it does not belong to a structured task
	 */
	public StructuredTask getOwningTask(CalculationResult cr){
		return mappingFromSubtasktToOwner.get(cr.getTask().getNumber());
	}

	/**
	 * ovverides the Master.handleObject method:
	 * implements actions to respond to recieved objects (basically, handling of calculation results)
	 */
	public void handleObject(Object o) {
		Date date=new Date();
		
		if (o instanceof CalculationResult) {
			CalculationResult calculationResult = (CalculationResult) o;
			if (!calculationResult.result().equals("done"))	{
				System.out.println(formatter.format(date)+": recieved "+o.getClass().getSimpleName());
				if (belongsToStructuredTask(calculationResult)){ // if we catch an intermediate result: add it to it's owning task
					StructuredTask owningTask = getOwningTask(calculationResult);
					owningTask.addIntermediateResult(calculationResult.result());
					if (owningTask.readyToRun()) (new sendThread(owningTask)).start(); // if all preprocessing is done: start the actual task
					return;
				}
			}			
			try {
				handleResult(calculationResult);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
//				e.printStackTrace();
			} catch (NoTokenException e) {
				e.printStackTrace();
			} catch (AlreadyBoundException e) {
				e.printStackTrace();
			} catch (SQLException e) {
	      e.printStackTrace();
      }
		} else System.out.println(formatter.format(date)+": recieved "+o.getClass().getSimpleName());
	}
	
	/**
	 * @return the root node of the tree in the result panel
	 */
	private ResultTreeRoot resultTreeRoot(){
		return (ResultTreeRoot)resultTree.getModel().getRoot();
	}

	/**
	 * dispatches incoming results
	 * @param result the result recieved from a calculation client
	 * @throws IOException
	 * @throws NoTokenException
	 * @throws AlreadyBoundException
	 * @throws SQLException 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private synchronized void handleResult(CalculationResult result) throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		ResultTreeRoot rootOfWholeResultTree = resultTreeRoot();			
			int taskNumber=result.getTask().getNumber();

			DefaultMutableTreeNode node = mappingFromTaskNumbersToResultCollectors.get(taskNumber);

			if (node==null){
				node = result.getTreeRepresentation();
				if (result.result().equals("done"))	node.setUserObject(node.toString()+" ✗");
				
				/* add result */
				rootOfWholeResultTree.add(node);
				mappingFromTaskNumbersToResultCollectors.put(taskNumber,node);
			} else {				
				if (result.result().equals("done"))	{
					node.setUserObject(node.toString()+" ✓");
				} else {
					DefaultMutableTreeNode resultTreeRepresentation = result.getTreeRepresentation();
					if (resultTreeRepresentation!=null)	node.add(resultTreeRepresentation);
				}
			}
				

			/* sort tree */
			ArrayList taskNodes = Collections.list(rootOfWholeResultTree.children());
			Collections.sort(taskNodes, ObjectComparator.get());
			rootOfWholeResultTree.removeAllChildren();
			for (Iterator childrenIterator = taskNodes.iterator(); childrenIterator.hasNext();)
				rootOfWholeResultTree.add((DefaultMutableTreeNode) childrenIterator.next());		
	}

	/**
	 * sends a call to calculate the best potential additionals for a given set of substances in certain compartments to a calculation client
	 * @param compartments the set of compartments, for which the best additionals shall be computed
	 * @param substanceIds the set of substances, which are given as substrates
	 * @throws IOException
	 */
	public void calcPotentialAdditionals(TreeSet<CompartmentNode> compartments, TreeSet<Integer> substanceIds, TreeSet<Integer> internalSubstances) throws IOException {
		if (warnforEmptyList(substanceIds)) return;
		TreeSet<Integer> nutrients=new TreeSet<Integer>(ObjectComparator.get());
		nutrients.addAll(substanceIds);
		nutrients.addAll(internalSubstances);
		for (Iterator<CompartmentNode> compartmentIterator = compartments.iterator(); compartmentIterator.hasNext();) {
			int cid = compartmentIterator.next().compartment().id();
			AdditionsCalculationTask act = new AdditionsCalculationTask(cid, substanceIds);
			sendTask(act);
		}
  }

	/**
	 * calls a calculation client and tells it to search for compartments processing a given set of substances
	 * @param substanceIds the set of substances, for which processors are searched
	 * @throws IOException
	 */
	public void searchProcessors(TreeSet<Integer>substanceIds) throws IOException {
		if (warnforEmptyList(substanceIds)) return;
		ProcessorSearchTask pst=new ProcessorSearchTask(substanceIds); 	  
		sendTask(pst);
  }

	/**
	 * checks, whether the substance id list is null/empty and prints a warning if it is
	 * @param substanceIds the list of substances to be checked
	 * @return true, only if the substance id list is neither null nor empty
	 */
	private boolean warnforEmptyList(TreeSet<Integer> substanceIds) {
		if (substanceIds==null)	return true;
		if (substanceIds.isEmpty()){
			System.out.println("Substance list is empty!");
			return true;
		}
		return false;
  }

	/**
	 * tries to find a set of seed substances in certain compartments by applying a mixed linear integer programming approach
	 * @param compartments the set of compartments, for which the calculation shall be performed
	 * @param decompose the set of substances, which shall be processed by those compartments
	 * @param build the set of substances, which shall be built be the respective compartments
	 * @param noOutflow set of substance ids, for which outflow shall be supressed
	 * @param optimizationParameterSet 
	 * @param useMilp 
	 * @param b 
	 * @param rateOfOutflowReactionsImportance 
	 * @param numberOfOutflowReactionsImportance 
	 * @param rateOfInflowReactionsImportance 
	 * @param numberOfInflowReactionsImportance 
	 * @param numberOfAllReactionsImportance 
	 * @param minimizeInternalReactions determines, whether the use of internal reactions shall be minimized
	 * @throws IOException
	 */
	public void optimizeSeeds(TreeSet<CompartmentNode> compartments, TreeSet<Integer> decompose, TreeSet<Integer> build, TreeSet<Integer> ignore, TreeSet<Integer> noOutflow, OptimizationParameterSet optimizationParameterSet, boolean ignoreUnbalanced, boolean useMilp) throws IOException {
		for (Iterator<CompartmentNode> it = compartments.iterator();it.hasNext();){
			int cid=it.next().compartment().id();
			sendTask(new OptimizeBuildTask(cid,decompose,build,ignore, optimizationParameterSet,ignoreUnbalanced,noOutflow,useMilp));
		}
	}

	/**
	 * tries to find a set of substances needed to form another set of target substances in certain compartments by means of an evolutionary algorith
	 * @param compartments the list of compartments, for which the calculation shall be done 
	 * @param decompose the ids of the substances, which shall be degraded
	 * @param build the ids of the substances, that shall be produced in the respective compartments
	 * @param treeSet 
	 * @throws IOException
	 */
	public void evovleSeeds(TreeSet<CompartmentNode> compartments, TreeSet<Integer> decompose, TreeSet<Integer> build, TreeSet<Integer> ignore) throws IOException {
		for (Iterator<CompartmentNode> it = compartments.iterator();it.hasNext();){
			int cid=it.next().compartment().id();
			sendTask(new EvolveSeedsTask(cid,decompose,build,ignore));
		}
  }

	public void findPath(TreeSet<Integer> compartmentList, TreeSet<Integer> degradeList, TreeSet<Integer> produceList, TreeSet<Integer> ignoreList) throws IOException {
		sendTask(new FindPathTask(compartmentList,degradeList,produceList,ignoreList));
  }

}
