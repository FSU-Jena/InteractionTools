package edu.fsuj.csb.reactionnetworks.interaction.tasks;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.CalculationClient;
import edu.fsuj.csb.reactionnetworks.interaction.results.FindPathResult;
import edu.fsuj.csb.reactionnetworks.organismtools.DbCompartment;
import edu.fsuj.csb.reactionnetworks.organismtools.DbReaction;
import edu.fsuj.csb.reactionnetworks.organismtools.DbSubstance;
import edu.fsuj.csb.tools.organisms.Compartment;
import edu.fsuj.csb.tools.organisms.ReactionSet;
import edu.fsuj.csb.tools.organisms.gui.SubstanceNode;
import edu.fsuj.csb.tools.xml.NoTokenException;
import edu.fsuj.csb.tools.xml.ObjectComparator;
import edu.fsuj.csb.tools.xml.Tools;

public class FindPathTask extends CalculationTask {
	private static final long serialVersionUID = 6385450099016633510L;
	private TreeSet<Integer> compartmentIds;
	private TreeSet<Integer> degradeSids;
	private TreeSet<Integer> produceSids;
	private ReactionSet reactions;
	private TreeSet<Integer> ignoreSids;
	
	private class Trace {
		
		private class Level{
			private TreeMap<Integer,Integer> mappingFromSubstanceIdsToCreatingReaction,mappingFromReactionIdToAllowingSubstance;
			
			public Level() {
				mappingFromReactionIdToAllowingSubstance=new TreeMap<Integer, Integer>();
				mappingFromSubstanceIdsToCreatingReaction=new TreeMap<Integer, Integer>();
      }

			public void addSubstance(int sid,Integer idOfCreatingReaction) {
				Tools.startMethod("Level.addSubstance("+sid+", "+idOfCreatingReaction+")");
				mappingFromSubstanceIdsToCreatingReaction.put(sid, idOfCreatingReaction);
				Tools.endMethod();
      }
			
			public Set<Integer> sids(){
				return mappingFromSubstanceIdsToCreatingReaction.keySet();
			}

			public void addReaction(int rid,int idOfAllowingSubstance) {
				Tools.startMethod("Level.addReaction("+rid+", "+idOfAllowingSubstance+")");
				mappingFromReactionIdToAllowingSubstance.put(rid,idOfAllowingSubstance);
				Tools.endMethod();
      }

			public void addSids(Collection<Integer> sids,int idOfCreatingReaction) {
				Tools.startMethod("Level.addSids("+sids+", "+idOfCreatingReaction+")");
				for (int sid:sids) mappingFromSubstanceIdsToCreatingReaction.put(sid,idOfCreatingReaction);
				Tools.endMethod();
      }
		}
		
		private Vector<Level> levels;
		private TreeSet<Integer> reachedSubstances;
		private ReactionSet reachedReactions;
		private int startSid;
		private TreeSet<Vector<Integer>> traces;
		
		public Trace(int sid) throws SQLException {
			Tools.startMethod("new Trace("+sid+")");
			startSid=sid;
			levels=new Vector<Level>();
			Level level0=new Level();
			reachedReactions=new ReactionSet();
			reachedSubstances=new TreeSet<Integer>();
			reachedSubstances.addAll(ignoreSids);
			reachedSubstances.add(sid);
			level0.addSubstance(sid,null);
			levels.add(level0);
			Tools.endMethod();
			traces=new TreeSet<Vector<Integer>>(ObjectComparator.get());
		}

		public boolean complete() throws SQLException {
			Tools.startMethod("Trace["+startSid+"].complete()");

			Level previousLevel=levels.lastElement();
			Level newLevel=new Level();
			levels.add(newLevel);
			ReactionSet possibleReactions=null;
      possibleReactions = reactions.clone();
			possibleReactions.removeAll(reachedReactions);
			boolean newReactionExplored=false;
			TreeSet<Integer> newSubstancesAtThisLevel=new TreeSet<Integer>();
			for (Integer rid:possibleReactions){				
				if (reachedReactions.contains(rid)) continue; // do not use reaction twice
				DbReaction reaction=DbReaction.load(rid);
				for (Integer sid:previousLevel.sids()){
					if (reaction.hasReactant(sid)){
						newLevel.addReaction(rid,sid);
						reachedReactions.add(rid);

						Collection<Integer> newSubstances = reaction.productIds();
						newSubstances.removeAll(reachedSubstances);
						
						newLevel.addSids(newSubstances,rid);
						newSubstancesAtThisLevel.addAll(newSubstances);
						newReactionExplored=true;

						for (Integer newSid:newSubstances){
							if (produceSids.contains(newSid)) saveTrace(newSid);
						}

					}
					if (reaction.hasProduct(sid)){
						newLevel.addReaction(rid,sid);
						reachedReactions.add(rid);
						
						Collection<Integer> newSubstances = reaction.substrateIds();
						newSubstances.removeAll(reachedSubstances);


						newLevel.addSids(newSubstances,rid);
						newSubstancesAtThisLevel.addAll(newSubstances);
						newReactionExplored=true;
						
						for (Integer newSid:newSubstances){
							if (produceSids.contains(newSid)) saveTrace(newSid);
						}

					}					
				}
			}
			reachedSubstances.addAll(newSubstancesAtThisLevel);
			if (!newReactionExplored) {
				Tools.endMethod(true);
				return true;
			}
			Tools.endMethod(false);
			return false;			
		}

		private void saveTrace(Integer newSid) throws SQLException {
			Tools.startMethod("Trace.saveTrace("+newSid+")");
			Vector<Integer> trace=new Vector<Integer>();
			int lastSid=newSid;
			trace.insertElementAt(lastSid, 0);
			for (int index=levels.size(); index>0; index--){
				Level level=levels.elementAt(index-1);
				int rid=level.mappingFromSubstanceIdsToCreatingReaction.get(lastSid);
				trace.insertElementAt(rid,0);
				lastSid=level.mappingFromReactionIdToAllowingSubstance.get(rid);
				trace.insertElementAt(lastSid,0);
				if (lastSid==startSid) break;
			}			
			traces.add(trace);
	    Tools.endMethod();
    }
	}

	public FindPathTask(TreeSet<Integer> compartmentList, TreeSet<Integer> degradeList, TreeSet<Integer> produceList, TreeSet<Integer> ignoreList) {
		compartmentIds = compartmentList;
		degradeSids = degradeList;
		produceSids = produceList;
		ignoreSids = ignoreList;
	}

	@Override
	public void run(CalculationClient calculationClient) throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		//Tools.enableLogging();
		//Tools.resetIntendation();
		Tools.startMethod("run");
		reactions=new ReactionSet();
		Tools.startMethod("Fetch All Reactions");
		for (int cid : compartmentIds) {
			Compartment comp = DbCompartment.load(cid);
			reactions.addAll(comp.reactions());
		}
		Tools.endMethod(reactions);
		TreeSet<Trace> incompleteTraces = new TreeSet<Trace>(ObjectComparator.get());
		TreeSet<Trace> completeTraces = new TreeSet<Trace>(ObjectComparator.get());
		for (int sid:degradeSids) incompleteTraces.add(new Trace(sid));
		while (!incompleteTraces.isEmpty()){
			for (Trace trace:incompleteTraces){
				if (trace.complete()) {
					completeTraces.add(trace);
					incompleteTraces.remove(trace);					
				}
			}
		}
		TreeSet<Vector<Integer>> traces=new TreeSet<Vector<Integer>>(ObjectComparator.get());
		for (Trace trace:completeTraces){
			traces.addAll(trace.traces);
		}
		FindPathResult result=new FindPathResult(this,traces);

		calculationClient.sendObject(result);
		Tools.endMethod();
	}
	
	@Override
	public MutableTreeNode treeRepresentation() throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		DefaultMutableTreeNode result = new DefaultMutableTreeNode("Task: find paths");
	  DefaultMutableTreeNode inputs=new DefaultMutableTreeNode("Substances to degrade");
	  for (int sid:degradeSids) inputs.add(new SubstanceNode(DbSubstance.load(sid)));
	  DefaultMutableTreeNode targets=new DefaultMutableTreeNode("Substances to produce");
	  for (int sid:produceSids) targets.add(new SubstanceNode(DbSubstance.load(sid)));
	  DefaultMutableTreeNode ignore=new DefaultMutableTreeNode("ignored substances");
	  for (int sid:ignoreSids) ignore.add(new SubstanceNode(DbSubstance.load(sid)));
	  result.add(inputs);
	  result.add(targets);
	  result.add(ignore);
	  return result;
	}
}
