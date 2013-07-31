package edu.fsuj.csb.reactionnetworks.interaction.results;

import java.sql.SQLException;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.tasks.CalculationTask;
import edu.fsuj.csb.reactionnetworks.organismtools.DbReaction;
import edu.fsuj.csb.reactionnetworks.organismtools.DbSubstance;
import edu.fsuj.csb.tools.organisms.gui.ReactionNode;
import edu.fsuj.csb.tools.organisms.gui.SubstanceNode;

public class FindPathResult extends CalculationResult {

  private static final long serialVersionUID = 8974320650183162037L;

	public FindPathResult(CalculationTask calculationTask, Object result) {
	  super(calculationTask, result);
  }

	public DefaultMutableTreeNode treeRepresentation() throws SQLException {
		DefaultMutableTreeNode rep = new DefaultMutableTreeNode("Result");
		@SuppressWarnings("unchecked")
    TreeSet<Vector<Integer>> traces=(TreeSet<Vector<Integer>>) result;
		for (Vector<Integer> trace:traces){
			DefaultMutableTreeNode traceNode=new DefaultMutableTreeNode("Trace");
			boolean substance=true;
			for (int id:trace){
				if (substance){
					traceNode.add(new SubstanceNode(DbSubstance.load(id)));
				} else traceNode.add(new ReactionNode(DbReaction.load(id)));
				substance=!substance;
			}
			rep.add(traceNode);
		}
		return rep;
	}
}
