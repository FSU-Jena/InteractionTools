package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Dimension;

import edu.fsuj.csb.gui.HorizontalPanel;
import edu.fsuj.csb.reactionnetworks.interaction.CompartmentList;


public class SpeciesSelector extends HorizontalPanel {

  private static final long serialVersionUID = 112142573504768402L;

	public SpeciesSelector(int width, int height) {
		super();
		setPreferredSize(new Dimension(width,height));
		
		ListModificationPanel lmp=new ListModificationPanel();
		CompartmentList listOfAllSpecies=new CompartmentList("all species");
		CompartmentList userList=new CompartmentList("user list");
		add(listOfAllSpecies);
		add(lmp);
		add(userList);
	}
	
}
