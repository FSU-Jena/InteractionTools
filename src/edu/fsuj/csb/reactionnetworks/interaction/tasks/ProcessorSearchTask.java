package edu.fsuj.csb.reactionnetworks.interaction.tasks;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import edu.fsuj.csb.reactionnetworks.database.InteractionDB;
import edu.fsuj.csb.reactionnetworks.interaction.CalculationClient;
import edu.fsuj.csb.reactionnetworks.interaction.results.ProcessorCalculationResult;
import edu.fsuj.csb.reactionnetworks.organismtools.DbReaction;
import edu.fsuj.csb.reactionnetworks.organismtools.gui.DbComponentNode;
import edu.fsuj.csb.tools.organisms.Reaction;
import edu.fsuj.csb.tools.xml.NoTokenException;
import edu.fsuj.csb.tools.xml.ObjectComparator;

public class ProcessorSearchTask extends CalculationTask {

	/**
   * 
   */
  private static final long serialVersionUID = -8585064130135195312L;
	private TreeSet<Integer> sids;
	public ProcessorSearchTask(TreeSet<Integer> substanceIds) {
		super();
		sids=substanceIds;
  }

	public String toString() {
		return "ProcessorSearchTask(" + sids + ")";
	}
	
	@Override
	public void run(CalculationClient calculationClient) throws IOException, NoTokenException, AlreadyBoundException {
		try {
			//System.out.println("Original set of substances: "+sids);
			TreeSet<Integer> oldSIDs = sids;
			sids=getSpontaneousClosure(sids);
			TreeSet<Integer> spontaneouslyReached = new TreeSet<Integer>(sids);
			spontaneouslyReached.removeAll(oldSIDs);
			
			TreeMap<Integer, TreeSet<Integer>> mappingFromCompartmentsToProcessedSubstances = findProcessors(sids);
			
			TreeMap<Integer,TreeMap<Integer,TreeSet<Integer>>> mappingFromNumberOfProcessedSubstancesToSpeciesToProcessedSubstances=new TreeMap<Integer, TreeMap<Integer,TreeSet<Integer>>>();
			
			for (Entry<Integer, TreeSet<Integer>> entry:mappingFromCompartmentsToProcessedSubstances.entrySet()){
				int cid=entry.getKey();
				TreeSet<Integer> idsOfProcessedSubstances = entry.getValue();
				int size=idsOfProcessedSubstances.size();
				
				TreeMap<Integer, TreeSet<Integer>> mapFromCidToProcessedSubstances = mappingFromNumberOfProcessedSubstancesToSpeciesToProcessedSubstances.get(size);
				if (mapFromCidToProcessedSubstances==null) {
					mapFromCidToProcessedSubstances=new TreeMap<Integer, TreeSet<Integer>>();
					mappingFromNumberOfProcessedSubstancesToSpeciesToProcessedSubstances.put(size, mapFromCidToProcessedSubstances);
				}
				mapFromCidToProcessedSubstances.put(cid, idsOfProcessedSubstances);
			}
			calculationClient.sendObject(new ProcessorCalculationResult(this, spontaneouslyReached,mappingFromNumberOfProcessedSubstancesToSpeciesToProcessedSubstances));
    } catch (SQLException e) {
	    e.printStackTrace();
    }
	}
	
	/**
	 * @param sids a set of substrates ("seed")
	 * @return the set of substances, which may reached from the seed by processing through spontaneous reactions
	 * @throws SQLException
	 * @throws IOException 
	 */
	public static TreeSet<Integer> getSpontaneousClosure(TreeSet<Integer> sids) throws SQLException, IOException {
		TreeSet<Integer> reactions = getSpontaneousReactions();
		TreeSet<Integer> result = new TreeSet<Integer>(sids);
		int size = 0;
		do {
			size = result.size();
			for (Iterator<Integer> reactionIterator = reactions.iterator(); reactionIterator.hasNext();) {
				Reaction r = DbReaction.load(reactionIterator.next());
				if (result.containsAll(r.substrateIds())) result.addAll(r.productIds());
				if (result.containsAll(r.productIds())) result.addAll(r.substrateIds());
			}
		} while (size != result.size());
		return result;
	}
	
	/**
	 * @return the set of all reactions that may fire spontaneously, when supplied with the appropriate substrates
	 * @throws SQLException
	 * @throws IOException 
	 */
	public static TreeSet<Integer> getSpontaneousReactions() throws SQLException, IOException {
		Statement st = InteractionDB.createStatement();
		String query = "SELECT id FROM reactions WHERE spontan";

		ResultSet rs = st.executeQuery(query);
		TreeSet<Integer> reactions = new TreeSet<Integer>();
		while (rs.next())
			reactions.add(rs.getInt(1));
		rs.close();
		st.close();
		return reactions;
	}
	
	private static TreeMap<Integer, TreeSet<Integer>> findProcessors(TreeSet<Integer> substances) throws SQLException, IOException {
		
		TreeMap<Integer,TreeSet<Integer>> mappingFromCompartmentsToProcessedSubstances=new TreeMap<Integer, TreeSet<Integer>>(ObjectComparator.get());
		for (Iterator<Integer> substanceIterator = substances.iterator();substanceIterator.hasNext();){
			int substance=substanceIterator.next();
			
			TreeSet<Integer> processingCompartments = findProcessors(substance);
			
			for (Iterator<Integer> compIt = processingCompartments.iterator(); compIt.hasNext();){
				int cid=compIt.next();
				if (!mappingFromCompartmentsToProcessedSubstances.containsKey(cid)){
					TreeSet<Integer> processedSubstances=new TreeSet<Integer>();
					processedSubstances.add(substance);
					mappingFromCompartmentsToProcessedSubstances.put(cid, processedSubstances);
				} else mappingFromCompartmentsToProcessedSubstances.get(cid).add(substance);
			}			
		}		
		return mappingFromCompartmentsToProcessedSubstances; // at this point, the mapping contains substances and ids of compartments which can process those substances
  }


	private static TreeSet<Integer> findProcessors(int substanceId) throws SQLException, IOException {
		//System.out.println("findProcessors("+substanceId+")");
	  TreeSet<Integer> forwardReactions = findForwardReactions(substanceId); // find reactions, which have the targets on their substrate site
	  //System.out.println("forwardReactions: "+forwardReactions);
	  TreeSet<Integer> compartments = findForwardCompartments(forwardReactions); // find compartments, which have at least the forward flag set on this reactions
	  //System.out.println("compartments: "+compartments);
	  //System.out.println("****************************************");
	  
	  TreeSet<Integer> backwardReactions = findBackwardReactions(substanceId);	  
	  //System.out.println("backwardReactions: "+backwardReactions);
	  compartments.addAll(findBackwardCompartments(backwardReactions));
	  
	  //System.out.println("Compartments: "+compartments);
	  //TODO: Es gibt Reaktionen im Reference-Pathway, die keinem Compartment zuzuordnen sind. Wie sind diese zu behandeln?
	  
	  return compartments;
  }
	
	
	/**
	 * @param sid database id of a substance
	 * @return all those reactions, which have this substance in their substrate set
	 * @throws SQLException
	 * @throws IOException 
	 */
	public static TreeSet<Integer> findForwardReactions(int sid) throws SQLException, IOException {
		String query = "SELECT DISTINCT rid FROM substrates WHERE sid = " + sid;
		// System.out.print(query);
		TreeSet<Integer> forwardReactions = new TreeSet<Integer>();
		Statement st = InteractionDB.createStatement();
		ResultSet rs = st.executeQuery(query);
		while (rs.next())
			forwardReactions.add(rs.getInt(1));
		// System.out.println(" => rids="+forwardReactions);
		rs.close();
		st.close();
		return forwardReactions;
	}

	/**
	 * @param sid the database id of a certain substance
	 * @return a set of all the reactions, which have this substance in their product set
	 * @throws SQLException
	 * @throws IOException 
	 */
	public static TreeSet<Integer> findBackwardReactions(int sid) throws SQLException, IOException {
		String query = "SELECT DISTINCT rid FROM products WHERE sid=" + sid;
		// System.out.print(query);
		TreeSet<Integer> backwardReactions = new TreeSet<Integer>();
		Statement st = InteractionDB.createStatement();
		ResultSet rs = st.executeQuery(query);
		while (rs.next())
			backwardReactions.add(rs.getInt(1));
		// System.out.println(" => rids="+backwardReactions);
		rs.close();
		st.close();
		return backwardReactions;
	}
	private static TreeSet<Integer> findBackwardCompartments(TreeSet<Integer> backwardReactions) throws SQLException, IOException {
		TreeSet<Integer> compartments=new TreeSet<Integer>();
	  for (Iterator<Integer> reactionIterator = backwardReactions.iterator();reactionIterator.hasNext();){
	  	compartments.addAll(findBackwardCompartments(reactionIterator.next()));
	  }
	  return compartments;
  }
	
	private static TreeSet<Integer> findBackwardCompartments(Integer rid) throws SQLException, IOException {
		//System.out.println(Reaction.get(rid));
		String query="SELECT eid FROM reaction_enzymes WHERE rid="+rid;
		//System.out.print(query);
	  TreeSet<Integer> enzymes=new TreeSet<Integer>();
	  Statement st=InteractionDB.createStatement();
	  ResultSet rs=st.executeQuery(query);
	  while (rs.next()) enzymes.add(rs.getInt(1));
	  rs.close();
	  //System.out.println(" => eids="+enzymes);
	  
	  TreeSet<Integer> compartments=new TreeSet<Integer>();
	  
	  /* look for enzyme-catalyzed reactions */
	  for (Iterator<Integer> enzymeIterator = enzymes.iterator();enzymeIterator.hasNext();){
	  	int eid=enzymeIterator.next();	  	
		  query="SELECT cid FROM enzymes_compartments WHERE eid="+eid+" AND cid NOT IN (SELECT cid FROM reaction_directions WHERE rid="+rid+" AND backward IS NOT TRUE)";
		  //System.out.print(query);
		  rs=st.executeQuery(query);
		  while (rs.next()) compartments.add(rs.getInt(1));
		  //System.out.println(" => cids="+compartments);
		  rs.close();
	  }
	  
	  /* look for non-enzymatic reactions */
	  query="SELECT cid FROM reaction_directions WHERE rid="+rid+" AND backward=true";
	  //System.out.println(query);
	  rs=st.executeQuery(query);
	  while (rs.next()) compartments.add(rs.getInt(1));
	  rs.close();

	  st.close();
	  return compartments;
  }
	
	private static TreeSet<Integer> findForwardCompartments(TreeSet<Integer> forwardReactions) throws SQLException, IOException {
		TreeSet<Integer> compartments=new TreeSet<Integer>();
	  for (Iterator<Integer> reactionIterator = forwardReactions.iterator();reactionIterator.hasNext();){
	  	compartments.addAll(findForwardCompartments(reactionIterator.next()));
	  }
	  return compartments;
  }

	private static TreeSet<Integer> findForwardCompartments(Integer rid) throws SQLException, IOException {
		//System.out.println(Reaction.get(rid));
	  String query="SELECT eid FROM reaction_enzymes WHERE rid="+rid;		
		//System.out.print(query);
	  TreeSet<Integer> enzymes=new TreeSet<Integer>();
	  Statement st=InteractionDB.createStatement();
	  ResultSet rs=st.executeQuery(query);
	  while (rs.next()) enzymes.add(rs.getInt(1));
	  //System.out.println(" => eids="+enzymes);	  
	  rs.close();
	  TreeSet<Integer> compartments=new TreeSet<Integer>();
		
	  /* look for enzyme-catalyzed reactions */
	  for (Iterator<Integer> enzymeIterator = enzymes.iterator();enzymeIterator.hasNext();){
	  	int eid=enzymeIterator.next();
		  query="SELECT cid FROM enzymes_compartments WHERE eid="+eid+" AND cid NOT IN (SELECT cid FROM reaction_directions WHERE rid="+rid+" AND forward IS NOT TRUE)";
		  //System.out.print(query);
		  rs=st.executeQuery(query);
		  while (rs.next()) compartments.add(rs.getInt(1));
		  //System.out.println(" => cids="+compartments);
		  rs.close();
	  }	  
	  
	  /* look for non-enzymatic reactions */
	  query="SELECT cid FROM reaction_directions WHERE rid="+rid+" AND forward=true";
	  rs=st.executeQuery(query);
	  while (rs.next()) compartments.add(rs.getInt(1));
	  rs.close();
	  st.close();
	  return compartments;
  }
	
	public MutableTreeNode treeRepresentation() throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("Task: Search processing compartments ["+this.getClass().getSimpleName()+"]");
		DefaultMutableTreeNode species = new DefaultMutableTreeNode("Input substances");
		for (Iterator<Integer> it = sids.iterator();it.hasNext();){			
			Integer sid=it.next();
			MutableTreeNode substanceNode = DbComponentNode.create(sid);
			species.add(substanceNode);			
		}
		node.add(species);
	  return node;
  }

}
