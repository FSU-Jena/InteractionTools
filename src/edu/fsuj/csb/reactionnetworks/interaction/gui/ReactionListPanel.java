package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Dimension;

import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.gui.HorizontalPanel;

public class ReactionListPanel extends HorizontalPanel {

	private static final Dimension initialSize=new Dimension(600, 300);


	public ReactionListPanel(String string) {
	  super(string);
	  
	  DefaultMutableTreeNode rTreeRoot = new DefaultMutableTreeNode("Reactions");
	  JTree tree=new JTree(rTreeRoot);
	  JScrollPane scrollpane=new JScrollPane(tree);
	  scrollpane.setPreferredSize(initialSize);
		add(scrollpane);

  }

}
