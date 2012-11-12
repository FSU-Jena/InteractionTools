package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.io.Serializable;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.gui.IntegerInputField;
import edu.fsuj.csb.gui.VerticalPanel;

public class OptimizationParametersTab extends VerticalPanel {
	
	public class OptimizationParameterSet implements Serializable{
    private static final long serialVersionUID = -868195336437264452L;
		private int numberOfAllReactions,numberOfInflows,rateOfInflows,numberOfOutflows,rateOfOutflows;
		
		public OptimizationParameterSet(int numberOfAllReactions, int numberOfInflows, int rateOfInflows, int numberOfOutflows, int rateOfOutflows) {
			this.numberOfInflows=numberOfInflows;
			this.numberOfOutflows=numberOfOutflows;
			this.numberOfAllReactions=numberOfAllReactions;
			this.rateOfInflows=rateOfInflows;
			this.rateOfOutflows=rateOfOutflows;
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
	}

  private static final long serialVersionUID = -5681082612373593097L;
  
  private IntegerInputField numberOfAllReactions,numberOfInflows,rateOfInflows,numberOfOutflows,rateOfOutflows;

	public OptimizationParametersTab(int i, int j) {
		super();
		numberOfAllReactions=new IntegerInputField("Importance of minimzation of number of ALL REACTIONS",0);
		add(numberOfAllReactions);
		
		VerticalPanel inflow=new VerticalPanel("Inflows");
		inflow.add(numberOfInflows=new IntegerInputField("Importance of minimzation of number of INFLOW REACTIONS",0));
		numberOfInflows.setzewert(1);
		inflow.add(rateOfInflows=new IntegerInputField("Importance of maximization of rate of DESIRED INFLOW REACTIONS",0));
		inflow.skalieren();
		add(inflow);
		
		VerticalPanel outflow=new VerticalPanel("Outflows");
		outflow.add(numberOfOutflows=new IntegerInputField("Importance of minimzation of number of OUTFLOW REACTIONS",0));
		numberOfOutflows.setzewert(1);
		outflow.add(rateOfOutflows=new IntegerInputField("Importance of maximization of rate of TARGET OUTFLOW REACTIONS",0));
		outflow.skalieren();
		add(outflow);		
		
		skalieren();
  }
	
	public OptimizationParameterSet optimizationParameterSet(){
		return new OptimizationParameterSet(numberOfAllReactions.wert(),numberOfInflows.wert(),rateOfInflows.wert(),numberOfOutflows.wert(),rateOfOutflows.wert());
	}
	
}
