package edu.fsuj.csb.reactionnetworks.interaction.tasks;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.CalculationClient;
import edu.fsuj.csb.reactionnetworks.interaction.results.CalculationResult;
import edu.fsuj.csb.reactionnetworks.organismtools.DbCompartment;
import edu.fsuj.csb.reactionnetworks.organismtools.PrecursorCalculator;
import edu.fsuj.csb.reactionnetworks.organismtools.SeedCalculationIntermediate;
import edu.fsuj.csb.tools.organisms.Compartment;
import edu.fsuj.csb.tools.organisms.Reaction;
import edu.fsuj.csb.tools.organisms.Substance;
import edu.fsuj.csb.tools.xml.NoTokenException;
import edu.fsuj.csb.tools.xml.ObjectComparator;

public class PrecursorCalculationTask extends CalculationTask {

	private static final long serialVersionUID = 4261121514128308008L;
	int substanceId,compartmentId;
	private TreeSet<Integer> ignoredSubstances;

	public PrecursorCalculationTask(int compartmentId, TreeSet<Integer> ignoredSubstances, int targetSubstance) {
		this.compartmentId=compartmentId;
		this.substanceId=targetSubstance;
		this.ignoredSubstances=ignoredSubstances;
	}

	static double limit = 1000.0;
	
  public void start(CalculationClient calculationClient) throws IOException, NoTokenException, AlreadyBoundException{
  	run(calculationClient);
  }
	
	public void run(CalculationClient client) throws IOException, NoTokenException, AlreadyBoundException {
		try {
			DbCompartment compartment=DbCompartment.load(compartmentId);
	    SeedCalculationIntermediate seedCalculationIntermediate = PrecursorCalculator.calculatePrecursorsOf(compartment,substanceId,ignoredSubstances);
	    client.sendObject(new CalculationResult(this, seedCalculationIntermediate));
    } catch (SQLException e) {
	    e.printStackTrace();
    }
	}

	public MutableTreeNode treeRepresentation() throws IOException, NoTokenException, AlreadyBoundException {
		DefaultMutableTreeNode result = new DefaultMutableTreeNode("Task: Seed calculation ["+this.getClass().getSimpleName()+"]");
		return result;
	}
	

	/**
	 * creates the set of potential precursor substances of a certain target substance in the current compartment, as well as the substances which are produced in cycles
	 * 
	 * @param substanceId the substance id of the target substance
	 * @param ignoredSubstances
	 * @return a SeedCaclulationIntermediate object, which encapsulates the set of potential precursors of the target substance and the set of cycles found
	 * @throws SQLException if any database access error occurs
	 */
	public static SeedCalculationIntermediate calculatePrecursorsOf(Compartment c,int substanceId) throws SQLException {
		return calculatePrecursorsOf(c,substanceId, null);
	}

	public static SeedCalculationIntermediate calculatePrecursorsOf(Compartment c,int substanceId, TreeSet<Integer> ignoredSubstances) throws SQLException {
		System.out.println("starting precursor calculation for " + Substance.get(substanceId).mainName());
		Stack<Integer> trace = new Stack<Integer>();
		trace.push(substanceId);
		TreeSet<TreeSet<Integer>> cycles = new TreeSet<TreeSet<Integer>>(ObjectComparator.get());
		TreeSet<Integer> potPrecursors = new TreeSet<Integer>();
		calculatePrecursorsOfSubstance(c,substanceId, trace, cycles, potPrecursors, ignoredSubstances);
		cycles = simplify(cycles);
		for (Iterator<TreeSet<Integer>> cycleIt = cycles.iterator(); cycleIt.hasNext();) {
			potPrecursors.removeAll(cycleIt.next());
		}
		System.out.println(potPrecursors.size() + " potential precursors");
		System.out.println(cycles.size() + " cycles");
		return new SeedCalculationIntermediate(potPrecursors, cycles);
	}
	
	/**
	 * searches all reactions, that may produce the given substance. Therefore, we have a close look on each reaction, test, whether it may produce the given substance. if it does so, then we go on and examine the reaction for substances needed to fire it and so on.
	 * if we occur to find a reaction we have already passed on our depth-first search, we have found a cycle, which will then be added to the set of cycles.
	 * if a substance has no producing reaction it is marked as potential precursor for the global target substance (from which the recursion started)
	 * @param substanceId the substance (its id) currently examined
	 * @param trace the list of reaction and substances we already have passed to get here
	 * @param cycles the ever growing list of sets containing substances produced in cycles
	 * @param potPrecursors the list of substances not produced by reactions
	 * @param ignoredSubstances 
	 * @throws SQLException if any database access error occurs
	 */
	private static void calculatePrecursorsOfSubstance(Compartment c,int substanceId, Stack<Integer> trace, TreeSet<TreeSet<Integer>> cycles, TreeSet<Integer> potPrecursors, TreeSet<Integer> ignoredSubstances) throws SQLException {
		// System.out.println("calculatePrecursorsOfSubstance("+Substance.get(substanceId).shortestName()+", trace="+print(trace)+", cont="+cycles+", pot="+potPrecursors+")");
		boolean notProduced = true;
		for (Iterator<Integer> reactionIterator = c.reactions().iterator(); reactionIterator.hasNext();) { // loop through all reactions of THIS compartment
			Reaction reaction = Reaction.get(reactionIterator.next());

			if (reaction.firesForwardIn(c) && reaction.productIds().contains(substanceId)) {
				if (trace.contains(-reaction.id())) {
					// System.out.println("Reaction "+Reaction.get(reaction.id())+" already in trace...skipping further recursion!");
					cycles.add(findCyclingSubstances(trace, -reaction.id()));
				} else {
					trace.push(-reaction.id()); // add reaction to trace; negative sign to discriminate from substances
					notProduced = false;
					calculatePrecursorsOfReaction(c,reaction, false, trace, cycles, potPrecursors,ignoredSubstances);
					trace.pop(); // remove reaction from trace
				}
			}
			if (reaction.firesBackwardIn(c) && reaction.substrateIds().contains(substanceId)) {
				if (trace.contains(-reaction.id())) {
					// System.out.println("Reaction "+Reaction.get(reaction.id())+" already in trace...skipping further recursion!");
					cycles.add(findCyclingSubstances(trace, -reaction.id()));
				} else {
					trace.push(-reaction.id()); // add reaction to trace; negative sign to discriminate from substances
					notProduced = false;
					calculatePrecursorsOfReaction(c,reaction, true, trace, cycles, potPrecursors,ignoredSubstances);
					trace.pop(); // remove reaction from trace
				}
			}
		}
		if (notProduced && !potPrecursors.contains(substanceId)) potPrecursors.add(substanceId);
	}
	
	/**
	 * When a reaction is found to produce a certain substance of interes, this method determines, which substances are required to fire the reaction in order to form the substance of interes.
	 * The resulting set of substances is then further traced for reactions, which provide them.
	 * @param reaction the reaction, which produces a substance of interest. 
	 * @param backward the direction in which the reaction has to fire, to produce the substance of interest. is crucial, as it determines, which substances act as substrates and have to be further examined.
	 * @param trace the trace, on which the reaction was found starting from the target substance
	 * @param cycles the set of cycles, which have already been found while crawling through the network
	 * @param potPrecursors this set collects substances, which have no building reactions and may therefore act as inputs to the network
	 * @param ignoredSubstances 
	 * @throws SQLException if any database access error occurs
	 */
	private static void calculatePrecursorsOfReaction(Compartment c,Reaction reaction, boolean backward, Stack<Integer> trace, TreeSet<TreeSet<Integer>> cycles, TreeSet<Integer> potPrecursors, TreeSet<Integer> ignoredSubstances) throws SQLException {
		// System.out.println("calculatePrecursorsOfReaction("+reaction.shortestName()+", backward="+backward+", trace="+print(trace)+", cont="+cycles+", pot="+potPrecursors+")");
		Collection<Integer> substrates = backward ? reaction.productIds() : reaction.substrateIds(); // if reaction is firing forward, then the substrates have to be examined. if not, then the products are of interest
		for (Iterator<Integer> subsIt = substrates.iterator(); subsIt.hasNext();) { // examine all actual substrates of the reaction
			int sid = subsIt.next();
			if (ignoredSubstances!=null && ignoredSubstances.contains(sid)) continue; // skip ignored substances
			if (trace.contains(sid)) { // if the examined substance already is in the trace, then we found a cycle
				// System.out.println("Substance "+Substance.get(sid)+" already in trace:");
				cycles.add(findCyclingSubstances(trace, sid)); // not sure, whether this is needed
				continue;
			}
			if (cyclesContainId(cycles, sid)) continue; // if the examined substance is known to be produced by a cycle, skip further tracking
			
			trace.push(sid);
			calculatePrecursorsOfSubstance(c,sid, trace, cycles, potPrecursors,ignoredSubstances); // look for reactions forming the substance of interest
			trace.pop();
		}

	}
	
	/**
	 * tests, whether one of the current cycles already contains the substance represented by its subnstance id
	 * @param cycles the set of cycles found up to now
	 * @param sid the substance id, for which we are looking
	 * @return true, if and only if the sid is contained in at least one of the cycles in the set
	 */
	private static boolean cyclesContainId(TreeSet<TreeSet<Integer>> cycles, int sid) {
		for (Iterator<TreeSet<Integer>> it = cycles.iterator(); it.hasNext();) {
			if (it.next().contains(sid)) return true;
		}
		return false;
	}
	
	/**
	 * joins cycles which have elements in common. 
	 * 
	 * @param cycles the cycles as found by the algorithm. may be non-disjoint
	 * @return the resulting set of disjoint cycles
	 */
	private static TreeSet<TreeSet<Integer>> simplify(TreeSet<TreeSet<Integer>> cycles) {
		
		/**
		 *  first we build a map pointing from each substance in the cycles to the first substance in the respective cycle.
		 * if there is already a map entry for a certain substance, than the root of the entry and the first substance of our current cycle are connected by a mapping
		 */
		TreeMap<Integer,Integer> map=new TreeMap<Integer, Integer>();		
		for (Iterator<TreeSet<Integer>> cycleIterator = cycles.iterator();cycleIterator.hasNext();){
			TreeSet<Integer> cycle=cycleIterator.next();
			Integer root=null;
			for (Iterator<Integer> substanceIterator=cycle.iterator(); substanceIterator.hasNext();){
				Integer sid=substanceIterator.next();
				if (root==null) {
					root=getRoot(map,sid);
				} else {
					sid=getRoot(map,sid);					
					if (sid>root) map.put(sid, root);
					if (sid<root) {
						map.put(root, sid);
						root=sid;
					}
				}				
			}
		}
		

		
		/**
		 * now the inverse map is built. we loop through all substances in the first map, look for their roots and for each root construct a substance set (=cycle)
		 */
		TreeMap<Integer,TreeSet<Integer>> mappingFromRootsToCycles=new TreeMap<Integer, TreeSet<Integer>>();
		
		for (Iterator<Integer> keyIterator = map.keySet().iterator();keyIterator.hasNext();){
			int key=keyIterator.next();
			int root=key;
			while (map.containsKey(root)) root=map.get(root);
			if (!mappingFromRootsToCycles.containsKey(root)) {
				TreeSet<Integer> newCycle=new TreeSet<Integer>();
				newCycle.add(root);
				mappingFromRootsToCycles.put(root, newCycle);
			}
			mappingFromRootsToCycles.get(root).add(key);
		}	
		
		TreeSet<TreeSet<Integer>> result = new TreeSet<TreeSet<Integer>>(ObjectComparator.get()); // as mappingFromRootsToCycles will return a Collection object, which is not serializable, we have to wrap teh set in a (serializable) treeset
		result.addAll(mappingFromRootsToCycles.values());
		return result;
	}
	
	/**
	 * in an id-to-id mapping, this method uses the input substance as key, determines the substance id it maps to and takes this as key and so on. finally, what we get back is a substance id not pointing at any other id
	 * @param map the id-to-id map
	 * @param sid ths starting point for the search
	 * @return the id which is found by depth search
	 */
	private static Integer getRoot(TreeMap<Integer, Integer> map, Integer sid) {		
		while (map.containsKey(sid)) sid=map.get(sid);
	  return sid;
  }

	/**
	 * this method is called, when a substance is found to occur twice in the trace. Then the substance and all substances in the trace between the occurences are part of a cycle and will be assembled to a set by this method
	 * @param trace the trace containing alternating substances and reactions starting from the target substance
	 * @param mark the substance (its id) which from which the cycle starts and ends
	 * @return the set of all substances (their ids) in the cycle
	 */
	private static TreeSet<Integer> findCyclingSubstances(Stack<Integer> trace, int mark) {
		// System.out.println("findCyclingSubstance("+trace+", "+mark+")");
		int size = trace.size();
		int i = size - 1;
		while (i>=0 && trace.get(i)!=mark) i--; // loop backward through the trace and look for mark
		TreeSet<Integer> result = new TreeSet<Integer>();
		int id = 0;
		for (; i < size; i++) { // loop forward through the trace (starting from mark) and assemble set of substances
			id = trace.get(i);
			if (id > 0) result.add(id);
		}
		// System.out.println("returning "+result+"\n");
		return result;
	}
	

}
