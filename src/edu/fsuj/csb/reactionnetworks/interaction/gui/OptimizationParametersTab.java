package edu.fsuj.csb.reactionnetworks.interaction.gui;

import javax.swing.JCheckBox;

import edu.fsuj.csb.gui.IntegerInputField;
import edu.fsuj.csb.gui.VerticalPanel;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.ParameterSet;

public class OptimizationParametersTab extends VerticalPanel {
	
  private static final long serialVersionUID = -5681082612373593097L;
  
  private IntegerInputField numberOfAllReactions,numberOfInflows,rateOfInflows,numberOfOutflows,rateOfOutflows;
	private JCheckBox skipUnbalancedReactions;

	public OptimizationParametersTab() {
		super();
		numberOfAllReactions=new IntegerInputField("Importance of minimzation of number of ALL REACTIONS",0);
		add(numberOfAllReactions);
		
		VerticalPanel inflow=new VerticalPanel("Inflows");
		inflow.add(numberOfInflows=new IntegerInputField("Importance of minimzation of number of INFLOW REACTIONS",0));
		numberOfInflows.setzewert(1);
		inflow.add(rateOfInflows=new IntegerInputField("Importance of maximization of rate of DESIRED INFLOW REACTIONS",0));
		inflow.scale();
		add(inflow);
		
		VerticalPanel outflow=new VerticalPanel("Outflows");
		outflow.add(numberOfOutflows=new IntegerInputField("Importance of minimzation of number of OUTFLOW REACTIONS",0));
		numberOfOutflows.setzewert(1);
		outflow.add(rateOfOutflows=new IntegerInputField("Importance of maximization of rate of TARGET OUTFLOW REACTIONS",0));
		outflow.scale();
		add(outflow);		
		
		skipUnbalancedReactions=new JCheckBox("<html>Skip unbalanced reactions");
		skipUnbalancedReactions.setToolTipText("<html>Unbalanced reactions wil not be taken into account, when using methods which use stoichiometry.");
		add(skipUnbalancedReactions);
		
		scale();
  }
	
	public ParameterSet optimizationParameterSet(){
		return new ParameterSet(numberOfAllReactions.wert(),numberOfInflows.wert(),rateOfInflows.wert(),numberOfOutflows.wert(),rateOfOutflows.wert(),skipUnbalancedReactions.isSelected(),false);
	}
	
}
