package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.TreeSet;

import javax.swing.JTree;

import edu.fsuj.csb.tools.xml.ObjectComparator;

public class ResultTree extends JTree {

	private static final long serialVersionUID = 5052993412392493322L;
	private TreeSet<ActionListener> listeners;

	public ResultTree(ResultTreeRoot resultTreeRoot) {
		super(resultTreeRoot);
  }

	public void activate() {
		fireEvent(new ActionEvent(this, 0, "activate"));
  }

	private void fireEvent(ActionEvent actionEvent) {
		if (listeners==null) return;
		for (ActionListener listener:listeners){
			listener.actionPerformed(actionEvent);
		}
  }
	
	public void addActionListener(ActionListener l){
		if (listeners==null) listeners=new TreeSet<ActionListener>(ObjectComparator.get());
		listeners.add(l);
	}

}
