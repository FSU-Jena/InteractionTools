package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.io.Serializable;

import javax.swing.JCheckBox;
import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.gui.IntegerInputField;
import edu.fsuj.csb.gui.VerticalPanel;
import edu.fsuj.csb.tools.xml.XmlObject;
import edu.fsuj.csb.tools.xml.XmlToken;

public class OptimizationParametersTab extends VerticalPanel implements XmlObject {
	
	public class OptimizationParameterSet implements Serializable{
    private static final long serialVersionUID = -868195336437264452L;
		private int numberOfAllReactions,numberOfInflows,rateOfInflows,numberOfOutflows,rateOfOutflows;
		private boolean skipUnbalancedReactions;
		
		public OptimizationParameterSet(int numberOfAllReactions, int numberOfInflows, int rateOfInflows, int numberOfOutflows, int rateOfOutflows, boolean skipUnbalancedReactions) {
			this.numberOfInflows=numberOfInflows;
			this.numberOfOutflows=numberOfOutflows;
			this.numberOfAllReactions=numberOfAllReactions;
			this.rateOfInflows=rateOfInflows;
			this.rateOfOutflows=rateOfOutflows;
			this.skipUnbalancedReactions=skipUnbalancedReactions;
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
			return skipUnbalancedReactions;
		}

		public StringBuffer getCode() {
			XmlToken result = new XmlToken("OptimizationParameters");
	
			result.setValue("min_in", numberOfInflows);
			result.setValue("min_out", numberOfOutflows);
			result.setValue("min_all", numberOfAllReactions);
			result.setValue("rate_in", rateOfInflows);
			result.setValue("rate_out", rateOfOutflows);
			result.setValue("skip_unbalanced", ""+skipUnbalancedReactions);
	    return result.getCode();
    }
	}

  private static final long serialVersionUID = -5681082612373593097L;
  
  private IntegerInputField numberOfAllReactions,numberOfInflows,rateOfInflows,numberOfOutflows,rateOfOutflows;
	private JCheckBox skipUnbalancedReactions;

	public OptimizationParametersTab() {
		super();
		
		add(numberOfAllReactions=new IntegerInputField("Importance of minimzation of number of ALL REACTIONS",0));
		numberOfAllReactions.setzewert(1);
		
		VerticalPanel inflow=new VerticalPanel("Inflows");
		inflow.add(numberOfInflows=new IntegerInputField("Importance of minimzation of number of INFLOW REACTIONS",0));
		numberOfInflows.setzewert(1);
		inflow.add(rateOfInflows=new IntegerInputField("Importance of maximization of rate of DESIRED INFLOW REACTIONS",0));
		rateOfInflows.setzewert(1);
		inflow.scale();
		add(inflow);
		
		VerticalPanel outflow=new VerticalPanel("Outflows");
		outflow.add(numberOfOutflows=new IntegerInputField("Importance of minimzation of number of OUTFLOW REACTIONS",0));
		numberOfOutflows.setzewert(1);
		outflow.add(rateOfOutflows=new IntegerInputField("Importance of maximization of rate of TARGET OUTFLOW REACTIONS",0));
		rateOfOutflows.setzewert(1);
		outflow.scale();
		add(outflow);		
		
		skipUnbalancedReactions=new JCheckBox("<html>Skip unbalanced reactions");
		skipUnbalancedReactions.setToolTipText("<html>Unbalanced reactions wil not be taken into account, when using methods which use stoichiometry.");
		add(skipUnbalancedReactions);
		
		scale();
  }
	
	public OptimizationParameterSet optimizationParameterSet(){
		return new OptimizationParameterSet(numberOfAllReactions.wert(),numberOfInflows.wert(),rateOfInflows.wert(),numberOfOutflows.wert(),rateOfOutflows.wert(),skipUnbalancedReactions.isSelected());
	}

	public boolean skipUnbalancedReactions() {
	  return skipUnbalancedReactions.isSelected();
  }


//	private int numberOfAllReactions,numberOfInflows,rateOfInflows,numberOfOutflows,rateOfOutflows;
//	private boolean skipUnbalancedReactions;

	public StringBuffer getCode() {
		return optimizationParameterSet().getCode();
	}

	public void loadState(XmlToken token) {
		
	  numberOfAllReactions.setzewert(token.getIntValue("min_all"));
	  numberOfInflows.setzewert(token.getIntValue("min_in"));
	  rateOfInflows.setzewert(token.getIntValue("rate_in"));
	  numberOfOutflows.setzewert(token.getIntValue("min_out"));
	  rateOfOutflows.setzewert(token.getIntValue("rate_out"));
		skipUnbalancedReactions.setSelected(token.getValue("skip_unbalanced").equals("true"));
  }
	
}
