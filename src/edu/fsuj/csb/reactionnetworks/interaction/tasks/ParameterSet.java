package edu.fsuj.csb.reactionnetworks.interaction.tasks;

import java.io.Serializable;

import javax.swing.tree.DefaultMutableTreeNode;


	public class ParameterSet implements Serializable{
    private static final long serialVersionUID = -868195336437264452L;
		private int reactionWeight,auxiliaryInflowWeight,desiredInflowWeight,auxiliaryOutflowWeight,desiredOutflowWeight;
		private boolean ignoreUnbalancedReactions,useMilp;
		
		public ParameterSet(int reactionWeight, int auxiliaryInflowWeight, int desiredInflowWeight, int auxiliaryOutflowWeight, int desiredOutflowWeight, boolean ignoreUnbalancedReactions,boolean useMilp) {
			this.auxiliaryInflowWeight=auxiliaryInflowWeight;
			this.auxiliaryOutflowWeight=auxiliaryOutflowWeight;
			this.reactionWeight=reactionWeight;
			this.desiredInflowWeight=desiredInflowWeight;
			this.desiredOutflowWeight=desiredOutflowWeight;
			this.ignoreUnbalancedReactions=ignoreUnbalancedReactions;
			this.useMilp=useMilp;
    }
		
		public DefaultMutableTreeNode tree(){
			DefaultMutableTreeNode parameters=new DefaultMutableTreeNode("Parameters:");
			parameters.add(new DefaultMutableTreeNode("Importance of minimization of total reaction number: "+reactionWeight));
			parameters.add(new DefaultMutableTreeNode("Importance of minimization of auxiliary inflows: "+auxiliaryInflowWeight));
			parameters.add(new DefaultMutableTreeNode("Importance of minimization of auxiliary outflows: "+auxiliaryOutflowWeight));
			parameters.add(new DefaultMutableTreeNode("Importance of maximization of desired inflows: "+desiredInflowWeight));
			parameters.add(new DefaultMutableTreeNode("Importance of maximization of desired outflows: "+desiredOutflowWeight));
			return parameters;
		}

		public double reactionWeight() {
	    return reactionWeight;
    }

		public double auxiliaryInflowWeight() {
	    return auxiliaryInflowWeight;
    }

		public double auxiliaryOutflowWeight() {
	    return auxiliaryOutflowWeight;
    }

		public double desiredInflowWeight() {
	    return desiredInflowWeight;
    }

		public double desiredOutflowWeight() {
	    return desiredOutflowWeight;
    }
		
		public boolean skipUnbalancedReactions(){
			return ignoreUnbalancedReactions;
		}

		public boolean useMILP() {
	    return useMilp;
    }

		public boolean ignoreUnbalanced() {
	    return ignoreUnbalancedReactions;
    }
	}
