package edu.fsuj.csb.reactionnetworks.interaction.results;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.SubstanceListNode;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.EvolveSeedsTask;

public class EvolveSeedsResult extends CalculationResult implements Serializable {

  private static final long serialVersionUID = -8961053913277252926L;
  
	public EvolveSeedsResult(EvolveSeedsTask evolveSeedsTask, TreeSet<Integer> seeds) {
		super(evolveSeedsTask, seeds);
  }
	
  @SuppressWarnings("unchecked")
  public DefaultMutableTreeNode resultTreeRepresentation() throws SQLException{		
		if (result instanceof TreeSet<?>){
			return new SubstanceListNode("Seed substances", (TreeSet<Integer>) result);
		}
		return null;
	}
	
}
