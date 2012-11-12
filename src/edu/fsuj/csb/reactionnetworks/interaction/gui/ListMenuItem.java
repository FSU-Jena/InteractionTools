package edu.fsuj.csb.reactionnetworks.interaction.gui;

import javax.swing.JMenuItem;

import edu.fsuj.csb.reactionnetworks.interaction.SubstanceList;

public class ListMenuItem extends JMenuItem {

  private static final long serialVersionUID = -7232901790368682227L;
	private SubstanceList list;
	public ListMenuItem(SubstanceList l) {
		super("add to "+l);
		list=l;
  }
	
	SubstanceList getList(){
		return list;
	}
}
