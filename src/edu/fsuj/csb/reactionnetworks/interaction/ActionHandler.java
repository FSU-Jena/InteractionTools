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

import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.distributedcomputing.tools.ClientHandle;
import edu.fsuj.csb.distributedcomputing.tools.Master;
import edu.fsuj.csb.reactionnetworks.interaction.gui.ResultTree;
import edu.fsuj.csb.reactionnetworks.interaction.gui.ResultTreeRoot;
import edu.fsuj.csb.reactionnetworks.interaction.results.CalculationResult;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.CalculationTask;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.ParameterSet;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.SubstanceSet;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.graph.AdditionsCalculationTask;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.graph.ProcessorSearchTask;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.graph.ProductCalculationTask;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.lp.FBATask;
import edu.fsuj.csb.tools.organisms.ReactionSet;
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

	private ResultTree resultTree;
	//private NetworkLoader networkLoader;
	private TreeMap<Integer, DefaultMutableTreeNode> mappingFromTaskNumbersToResultCollectors = new TreeMap<Integer, DefaultMutableTreeNode>(ObjectComparator.get());
	private SimpleDateFormat formatter = new SimpleDateFormat("yy-MM-dd HH:mm:ss");

	/**
	 * creates a new action handler instane
	 * @param resultTree the JTree, to which result trees shall be added in order to display them
	 * @throws IOException 
	 */
	public ActionHandler(ResultTree resultTree,int port) throws IOException {
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
		System.out.print(formatter.format(new Date())+": ");
		handle.send(ct);
	}

	/**
	 * calculates the set of substnaces which may be produced by the given compartments when supplied with the given substances
	 * @param compartments the set of compartments, for which the calculation shall be performed
	 * @param substanceIds the ids of the substances, which shall be given as substrates to the calculation
	 * @throws IOException
	 */
	public void calculateProducts(TreeSet<CompartmentNode> compartments, TreeSet<Integer> substanceIds) throws IOException {
		if (warnforEmptyList(substanceIds,"Nutrients")) return;
		for (Iterator<CompartmentNode> speciesIterator = compartments.iterator(); speciesIterator.hasNext();) {
			int cid = speciesIterator.next().compartment().id();
			ProductCalculationTask pct = new ProductCalculationTask(cid, substanceIds);
			sendTask(pct);
		}
	}
	
	/**
	 * ovverides the Master.handleObject method:
	 * implements actions to respond to recieved objects (basically, handling of calculation results)
	 */
	public void handleObject(Object o) {
		Date date=new Date();
		if (o instanceof CalculationResult) {
			CalculationResult calculationResult = (CalculationResult) o;
			if (calculationResult.result()==null) return;
			if (!calculationResult.result().equals("done"))	System.out.println(formatter.format(date)+": recieved "+o.getClass().getSimpleName());
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
			resultTree.activate();
	}

	/**
	 * sends a call to calculate the best potential additionals for a given set of substances in certain compartments to a calculation client
	 * @param compartments the set of compartments, for which the best additionals shall be computed
	 * @param substanceIds the set of substances, which are given as substrates
	 * @throws IOException
	 */
	public void calcPotentialAdditionals(TreeSet<CompartmentNode> compartments, TreeSet<Integer> substanceIds, TreeSet<Integer> internalSubstances) throws IOException {
		if (warnforEmptyList(substanceIds,"Nutrients")) return;
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
		if (warnforEmptyList(substanceIds,"Substances")) return;
		ProcessorSearchTask pst=new ProcessorSearchTask(substanceIds); 	  
		sendTask(pst);
  }

	/**
	 * checks, whether the id list is null/empty and prints a warning if it is
	 * @param ids the list of substances to be checked
	 * @return true, only if the substance id list is neither null nor empty
	 */
	private boolean warnforEmptyList(TreeSet<Integer> ids, String type) {
		if (ids==null)	return true;
		if (ids.isEmpty()){
			System.out.println("NOTE: List of "+type+" is empty!");
			return true;
		}
		return false;
  }

	public void startFBA(TreeSet<Integer> compartmentIds, SubstanceSet substanceSet, ReactionSet additionalReactions, ParameterSet parameterSet) throws IOException {
		if (warnforEmptyList(compartmentIds,"Organisms/Compartments")) return;
		if (warnforEmptyList(substanceSet.desiredInflows(),"Substances to consume") || warnforEmptyList(substanceSet.desiredOutFlows(),"Substances to produce")) return;
		for (Integer compartmentId:compartmentIds) startFBA(compartmentId, substanceSet, additionalReactions, parameterSet);
  }

	private void startFBA(Integer compartmentId, SubstanceSet substanceSet, ReactionSet additionalReactions, ParameterSet parameterSet) throws IOException{
		if (compartmentId==null) System.out.println("NOTE: No compartment selected!");
		if (warnforEmptyList(substanceSet.desiredInflows(),"Substances to consume") || warnforEmptyList(substanceSet.desiredOutFlows(),"Substances to produce")) return;
		FBATask fba=new FBATask(compartmentId, substanceSet,additionalReactions, parameterSet);		
		sendTask(fba);
  }
}
