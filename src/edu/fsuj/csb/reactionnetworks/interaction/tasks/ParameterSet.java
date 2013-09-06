package edu.fsuj.csb.reactionnetworks.interaction.tasks;

import java.io.Serializable;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.tools.xml.XmlToken;


	public class ParameterSet extends XmlToken implements Serializable{
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
		
		public String toString() {		 
		  return "Parameters:\n - importance of desired inflows: "+desiredInflowWeight+"\n - importance of desired outflows: "+desiredOutflowWeight+"\n - importance of reduction of auxiliary inflows: "+auxiliaryInflowWeight+"\n - importance of reduction of auxiliary outflows: "+auxiliaryOutflowWeight+"\n - importance of reduction of internal reactions: "+reactionWeight;
		}

		public void getCode(StringBuffer sb) {
			tokenClass="OptimizationParameters";	
			setValue("min_in", auxiliaryInflowWeight);
			setValue("min_out", auxiliaryOutflowWeight);
			setValue("min_all", reactionWeight);
			setValue("rate_in", desiredInflowWeight);
			setValue("rate_out", desiredOutflowWeight);
			setValue("skip_unbalanced", ""+ignoreUnbalancedReactions);
	    super.getCode(sb);
    }
	}