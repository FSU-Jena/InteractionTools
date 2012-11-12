package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import edu.fsuj.csb.reactionnetworks.interaction.CompartmentList;
import edu.fsuj.csb.reactionnetworks.organismtools.DbCompartment;
import edu.fsuj.csb.tools.organisms.gui.CompartmentNode;
import edu.fsuj.csb.tools.xml.ObjectComparator;

/**
 * this context menu may be popped up, if a user clicks on a compartment node within the result panel of the interaction tool
 * @author Stephan Richter
 *
 */
public class CompartmentContextMenu extends JPopupMenu implements ActionListener {


  private static final long serialVersionUID = 5254738240367514827L;
	private TreeMap<String, CompartmentList> comparmentLists;
	private TreeSet<Integer> lastClickedCompartments;
	public CompartmentContextMenu() {
		super();
		comparmentLists=new TreeMap<String, CompartmentList>(ObjectComparator.get());
		JMenuItem google = new JMenuItem("Search on Google");
		google.addActionListener(this);
		add(google);
		setBorder(BorderFactory.createLineBorder(Color.black));

	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent arg0) {
    try {
		if (arg0.getSource() instanceof JMenuItem){
			String text=((JMenuItem)arg0.getSource()).getText();
			if (text.contains("Add")) {
				CompartmentList sl=comparmentLists.get(text.replace("Add to ", ""));
				for (Iterator<Integer> sIt = lastClickedCompartments.iterator(); sIt.hasNext();)
	          sl.addCompartment(DbCompartment.load(sIt.next()));
			}
			if (text.contains("Google")) {
				for (Iterator<Integer> sIt = lastClickedCompartments.iterator(); sIt.hasNext();){
						String cmd = "gnome-open http://www.google.com/search?q=" + DbCompartment.load(sIt.next()).mainName().replace(" ", "%20") + "&btnI";
						Runtime.getRuntime().exec(cmd);
				}
			}
		}
    } catch (SQLException e) {
      e.printStackTrace();
    } catch (IOException e) {
	    e.printStackTrace();
    }
  }
	/**
	 * displays this context menu at the given position
	 * @param panel the parent component, to which the menu shall be assigned
	 * @param p the point, at which the menu shall appear
	 * @param cns the set of compartment nodes, to which the selected action is applied
	 */
	public void show(JComponent panel, Point p, TreeSet<CompartmentNode> cns) {
	  super.show(panel,p.x,p.y);
	  lastClickedCompartments=new TreeSet<Integer>();
	  for (Iterator<CompartmentNode> it = cns.iterator(); it.hasNext();){
	  	CompartmentNode child = it.next();
	  	lastClickedCompartments.add(child.compartment().id());
	  }
  }
	
	/**
	 * @param cl adds a compartment list to the list of selectable compartment lists displayed in the menu popup
	 */
	public void addCompartmentList(CompartmentList cl) {
	  comparmentLists.put(cl.caption(), cl);
		JMenuItem dummy=new JMenuItem("Add to "+cl.caption());
		dummy.addActionListener(this);
		add(dummy);
		pack();
  }	
}
