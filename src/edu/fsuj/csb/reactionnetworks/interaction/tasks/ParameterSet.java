package edu.fsuj.csb.reactionnetworks.interaction.tasks;

import java.io.Serializable;

import javax.swing.tree.DefaultMutableTreeNode;


	public class ParameterSet implements Serializable{
    private static final long serialVersionUID = -868195336437264452L;
		private int numberOfAllReactions,numberOfInflows,rateOfInflows,numberOfOutflows,rateOfOutflows;
		private boolean ignoreUnbalancedReactions,useMilp;
		
		public ParameterSet(int numberOfAllReactions, int numberOfInflows, int rateOfInflows, int numberOfOutflows, int rateOfOutflows, boolean ignoreUnbalancedReactions,boolean useMilp) {
			this.numberOfInflows=numberOfInflows;
			this.numberOfOutflows=numberOfOutflows;
			this.numberOfAllReactions=numberOfAllReactions;
			this.rateOfInflows=rateOfInflows;
			this.rateOfOutflows=rateOfOutflows;
			this.ignoreUnbalancedReactions=ignoreUnbalancedReactions;
			this.useMilp=useMilp;
    }
		
		public DefaultMutableTreeNode tree(){
			DefaultMutableTreeNode parameters=new DefaultMutableTreeNode("Parameters:");
			parameters.add(new DefaultMutableTreeNode("Importance of minimization of total reaction number: "+getNumberOfAllReactions()));
			parameters.add(new DefaultMutableTreeNode("Importance of minimization of number of inflows: "+numberOfInflows));
			parameters.add(new DefaultMutableTreeNode("Importance of minimization of number of outflows: "+numberOfOutflows));
			parameters.add(new DefaultMutableTreeNode("Importance of maximization of rate of desired inflows: "+rateOfInflows));
			parameters.add(new DefaultMutableTreeNode("Importance of maximization of rate of target outflows: "+rateOfOutflows));
			return parameters;
		}

		public int getNumberOfAllReactions() {
	    return numberOfAllReactions;
    }

		public int getNumberOfInflows() {
	    return numberOfInflows;
    }

		public int getNumberOfOutflows() {
	    return numberOfOutflows;
    }

		public int getRateOfInflows() {
	    return rateOfInflows;
    }

		public int getRateOfOutflows() {
	    return rateOfOutflows;
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
