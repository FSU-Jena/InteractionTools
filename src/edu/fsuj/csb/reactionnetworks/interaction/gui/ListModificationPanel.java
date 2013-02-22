package edu.fsuj.csb.reactionnetworks.interaction.gui;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.TreeSet;

import javax.swing.JButton;

import edu.fsuj.csb.gui.VerticalPanel;
import edu.fsuj.csb.tools.xml.ObjectComparator;

public class ListModificationPanel extends VerticalPanel implements ActionListener {

  private static final long serialVersionUID = 2066495889272042812L;
/*	private ObjectList rightList;
	private ObjectList leftList;*/
	private JButton toRight;
	private JButton toLeft;
	private TreeSet<ActionListener> listeners;
	public final static int RIGHT=0;
	public final static  int LEFT=1;

	public ListModificationPanel() {
		toRight=new JButton("<html>hinzufügen &rArr;");
		toRight.addActionListener(this);
		toLeft=new JButton("<html>entfernen &lArr;");
		toLeft.addActionListener(this);
		add(toRight);
		add(toLeft);
		scale();
	}
	
	public void addActionListener(ActionListener l){
		if (listeners==null) listeners=new TreeSet<ActionListener>(ObjectComparator.get());
		listeners.add(l);
	}

/*	public void addRightPanel(ObjectList rightList) {
	  this.rightList=rightList;
  }

	public void addLeftPanel(ObjectList leftList) {
		this.leftList=leftList;	  
  }*/
	
	public void actionPerformed(ActionEvent arg0) {
		if (listeners==null) return;
		Object source=arg0.getSource();
		if (source.equals(toRight)){
			sendActionEvent(new ActionEvent(this, RIGHT, "right"));
		}
	  if (source.equals(toLeft)){
			sendActionEvent(new ActionEvent(this, LEFT, "left"));
	  }
  }

	private void sendActionEvent(ActionEvent actionEvent) {
		for (Iterator<ActionListener> it = listeners.iterator();it.hasNext();) {
			it.next().actionPerformed(actionEvent);
		}
  }
}
