package edu.fsuj.csb.reactionnetworks.interaction;

import java.sql.SQLException;

import edu.fsuj.csb.reactionnetworks.interaction.gui.CompartmentList;

/**
 * defines the methods of classes implementing a compartment listener
 * @author Stephan Richter
 *
 */
public interface CompartmentListener {
	/**
	 * is called, when the set of selected compartments is altered
	 * @param compartmentList the list, in which the selection is changed
	 * @throws SQLException 
	 */
	public void compartmentListChanged(CompartmentList compartmentList) throws SQLException;
}
