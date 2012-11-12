package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.SubstanceList;
import edu.fsuj.csb.reactionnetworks.interaction.SubstanceListNode;
import edu.fsuj.csb.tools.organisms.Substance;
import edu.fsuj.csb.tools.organisms.gui.SubstanceNode;
import edu.fsuj.csb.tools.xml.ObjectComparator;

public class SubstanceContextMenu extends JPopupMenu implements ActionListener {

	private static final long serialVersionUID = 6377997627401841813L;
	JMenuItem addToNutrients;
	TreeMap<String, SubstanceList> substanceLists;
	TreeSet<Integer> lastClickedSubstances;

	public SubstanceContextMenu() {
		super();
		substanceLists = new TreeMap<String, SubstanceList>(ObjectComparator.get());
		JMenuItem google = new JMenuItem("Search on Google");
		google.addActionListener(this);
		add(google);
		setBorder(BorderFactory.createLineBorder(Color.black));
	}

	public void actionPerformed(ActionEvent arg0) {
		try {
		if (arg0.getSource() instanceof JMenuItem) {
			JMenuItem item = (JMenuItem) arg0.getSource();
			String text = item.getText();
			if (text.contains("Add")) {
				SubstanceList sl = substanceLists.get(text.replace("Add to ", ""));
				for (Iterator<Integer> sIt = lastClickedSubstances.iterator(); sIt.hasNext();)
					sl.addSubstance(Substance.get(sIt.next()));
			}
			if (text.contains("Google")) {
				for (Iterator<Integer> sIt = lastClickedSubstances.iterator(); sIt.hasNext();){
						String cmd = "gnome-open http://www.google.com/search?q=" + Substance.get(sIt.next()).mainName().replace(" ", "%20") + "&btnI";
						Runtime.getRuntime().exec(cmd);
				}
			}
		}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
    }
	}

	public void show(JComponent scrollpane, Point p, SubstanceNode sn) {
		super.show(scrollpane, p.x, p.y);
		lastClickedSubstances = new TreeSet<Integer>();
		lastClickedSubstances.add(sn.substance().id());
	}

	public void addSubstanceList(SubstanceList sl) {
		substanceLists.put(sl.toString(), sl);
		JMenuItem dummy = new JMenuItem("Add to " + sl);
		dummy.addActionListener(this);
		add(dummy);
	}

	public void show(Component invoker, Point p, SubstanceListNode sln) {
		super.show(invoker, p.x, p.y);

		lastClickedSubstances = new TreeSet<Integer>();
		for (@SuppressWarnings("unchecked")
    Enumeration<DefaultMutableTreeNode> childNodes = sln.children(); childNodes.hasMoreElements();) {
			DefaultMutableTreeNode child = childNodes.nextElement();
			if (child instanceof SubstanceNode) {
				SubstanceNode sNode = (SubstanceNode) child;
				lastClickedSubstances.add(sNode.substance().id());
			}
		}
	}
}
