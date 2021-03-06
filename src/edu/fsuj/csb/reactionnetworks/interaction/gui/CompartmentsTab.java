package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JTabbedPane;

import edu.fsuj.csb.gui.HorizontalPanel;
import edu.fsuj.csb.gui.VerticalPanel;
import edu.fsuj.csb.reactionnetworks.database.InteractionDB;
import edu.fsuj.csb.reactionnetworks.interaction.CompartmentListNode;
import edu.fsuj.csb.reactionnetworks.interaction.CompartmentListener;
import edu.fsuj.csb.reactionnetworks.organismtools.DbCompartment;
import edu.fsuj.csb.tools.organisms.gui.CompartmentNode;
import edu.fsuj.csb.tools.organisms.gui.SortedTreeNode;
import edu.fsuj.csb.tools.xml.XmlObject;
import edu.fsuj.csb.tools.xml.XmlToken;

/**
 * a GUI component, which shows lists of compartments the user may select for building tasks
 * @author Stephan Richter
 *
 */
public class CompartmentsTab extends HorizontalPanel implements ActionListener,XmlObject {
	private static final long serialVersionUID = 8904107088279674639L;
	private CompartmentList listOfAllCompartments;
	private JTabbedPane speciesTabs;
	private CompartmentList userSelection;
	private JButton opensbmlButton;
	private JButton reaButton;
	private CompartmentListener cListener;
	private ListModificationPanel lmp;
	//private MetabolicNetworkPanel netViewer;

	/**
	 * creates a new compartments tab with the desired height and width
	 * @param width the desired width of the panel
	 * @param height the desired height of the panel
	 * @throws SQLException 
	 * @throws IOException
	 */
	public CompartmentsTab() throws SQLException, IOException {
		lmp = new ListModificationPanel();
		System.out.println("    |     `- reading compartment list...");
		//System.out.print("    |           `- ");
		add(createListOfAllCompartments());
		add(lmp);
		lmp.addActionListener(this);
		add(speciesTabs= createCompartmentsTabs());
		scale();
	}

	public void setNetworkViewer(MetabolicNetworkPanel networkViewer) {
//		netViewer=networkViewer;
		userSelection.setNetworkViewer(networkViewer);
  }

	
	/**
	 * creates a JTree based list of all compartments available through the interaction database
	 * @param width the desired width of the panel
	 * @param height the desired height of the panel 
	 * @return the panel with the list of all available compartments
	 * @throws SQLException 
	 * @throws IOException
	 */
	private VerticalPanel createListOfAllCompartments() throws SQLException, IOException {
		VerticalPanel panel = new VerticalPanel();
		panel.add(listOfAllCompartments = new CompartmentList("All species"));
		listOfAllCompartments.addActionListener(this);
		HorizontalPanel buttonPanel = new HorizontalPanel();
		buttonPanel.add(opensbmlButton = new JButton("open sbml file..."));
		opensbmlButton.addActionListener(this);
		buttonPanel.add(reaButton = new JButton("open rea file..."));
		reaButton.addActionListener(this);
		buttonPanel.scale();
		panel.add(buttonPanel);

		addCompartmentGroups(listOfAllCompartments);
		panel.scale();
		return panel;
	}

	/**
	 * adds the different compartment grouping nodes to the list of available compartments
	 * @param groupList
	 * @throws IOException 
	 */
	private void addCompartmentGroups(CompartmentList groupList) throws SQLException, IOException {
    String query="connection attempt:"; 
		
    try {
    	TreeSet<Integer> compartmentGroups = InteractionDB.getCompartmentGroupIds();
	    Statement compRequest = InteractionDB.createStatement();
    	
    	for (Integer groupId: compartmentGroups){
    		String groupName=InteractionDB.getName(groupId);
	    	SortedTreeNode group=new SortedTreeNode(groupName);
	    	
	    	query="SELECT id FROM compartments WHERE groups="+groupId+" AND id NOT IN (SELECT DISTINCT contained FROM hierarchy)";
	    	ResultSet comResult=compRequest.executeQuery(query);
	    	while (comResult.next()) group.addWithoutPublishing(compartmentTree(comResult.getInt(1)));
	    	group.publish();
	    	comResult.close();
	    	groupList.addElement(group);
	    }
    	compRequest.close();
    } catch (SQLException e) {    	
	    System.err.println("error on "+query);
	    throw e;
    }
  }

	/*
	 * public static SortedTreeNode getTreeOfAllKeggSpecies() throws IOException { SortedTreeNode result = new SortedTreeNode("Kegg Organisms");
	 * 
	 * SortedTreeNode realm = null;
	 * 
	 * String[] code = PageFetcher.fetchLines(KeggDispatcher.orgList); for (int i = 0; i < code.length; i++) { String line = code[i]; if (line.contains("<h4")) { if (realm != null) result.add(realm); realm = new SortedTreeNode(readContent(line)); } if (line.contains("<td align=center><a href='/kegg-bin/show_organism?org=")) realm.add(createKeggSpeciesNode(code, i));
	 * 
	 * } if (realm != null) result.add(realm); return result; }
	 */

	private CompartmentNode compartmentTree(int cid) throws SQLException {
		//System.err.println("CompartmentTabs.compartmentTree("+cid+")");
		DbCompartment c=DbCompartment.load(cid);
		CompartmentNode compartmentNode=null;
		try {
			compartmentNode=new CompartmentNode(c);
		} catch (NoSuchElementException e){
			System.err.println(cid);			
			throw e;
		}

		TreeSet<Integer> contained = c.containedCompartments(false);
		if (!contained.isEmpty()){
			CompartmentListNode cln=new CompartmentListNode("includes");
			for (Iterator<Integer> it = contained.iterator();it.hasNext();) cln.add(compartmentTree(it.next()));
			compartmentNode.add(cln);
		}
	  return compartmentNode;
  }

	/**
	 * creates the tabs for the different user selected compartment lists
	 * @param width
	 * @param height
	 * @return a tabbed pane containing the created tabs
	 */
	private JTabbedPane createCompartmentsTabs() {
		speciesTabs = new JTabbedPane();
		userSelection = new CompartmentList("selected compartments");
		userSelection.addActionListener(this);
		PopupMenu.setCompartmentList(userSelection);
		speciesTabs.add(userSelection, "User selection");
		return speciesTabs;
	}

	/*
	 * private static SpeciesNode createKeggSpeciesNode(String[] code, int i) throws MalformedURLException { String key = readContent(readContent(code[i])); KeggOrganismId koid = new KeggOrganismId(key); key = readContent(readContent(code[i + 1])); int id = CompartmentId.getOrCreate(new URL(KeggDispatcher.organismPrefix1 + koid)); SpeciesNode result = new SpeciesNode(id, key); return result; }
	 * 
	 * private static String readContent(String line) { line = line.trim(); String key = line.substring(1, line.indexOf(" ")); int i = line.indexOf(">") + 1; int k = line.indexOf("</" + key, i); if (k < 0) return line.substring(i); return line.substring(i, k); }
	 */


	
	
	/*
		 * 
		 * private void addReaFile() throws URISyntaxException { URL fileUrl = PanelTools.showSelectFileDialog("choose REA file", "*.rea", new GenericFileFilter("rea file", ".rea"), this); addUserFile(fileUrl); }
		 * 
		 * private void addSbmlFile() throws URISyntaxException { URL fileUrl = PanelTools.showSelectFileDialog("choose SBML file", "*.sbml", new GenericFileFilter("sbml file", ".xml;.sbml"), this); addUserFile(fileUrl); }
		 * 
		 * private void addUserFile(URL fileUrl) throws URISyntaxException { if (fileUrl==null) return; File f = new File(fileUrl.toURI()); if (f.exists()) { if (userNodes == null) { userNodes = new SortedTreeNode("User files"); listOfAllSpecies.addElement(userNodes); } CompartmentId cid = CompartmentId.getOrCreate(fileUrl); SpeciesNode userNode = new SpeciesNode(cid, f.getName()); userNodes.add(userNode); listOfAllSpecies.refreshList(); } } //
		 */

	/**
	 * removes the selected species from the current user list
	 * @throws SQLException 
	 */
	private void removeSpeciesFromUserList() throws SQLException {
		getUserList().removeSelected();
		if (cListener!=null) cListener.compartmentListChanged(getUserList());
	}

	/**
	 * adds the selected species from the current user list
	 * @throws SQLException 
	 */
	private void addCompartmentsToUserList() throws SQLException {
		TreeSet<CompartmentNode> compartmentNodeSet = getSelectedDatabaseCompartments();
		getUserList().addCompartments(compartmentNodeSet);
		if (cListener!=null) cListener.compartmentListChanged(getUserList());
	}

	/**
	 * @return the list of species present in the curent user list
	 */
	CompartmentList getUserList() {
		Component component = speciesTabs.getSelectedComponent();
		if (component instanceof CompartmentList) return (CompartmentList) component;
		return null;
	}

	/**
	 * @return the list of species selected in the database species list
	 */
	private TreeSet<CompartmentNode> getSelectedDatabaseCompartments() {
		return listOfAllCompartments.getSelected();
	}

	/**
	 * @return the set of species ids from the current active user list
	 */
	TreeSet<Integer> getUserSpecies() {
		TreeSet<CompartmentNode> set = userSelection.getListed();
		TreeSet<Integer> result = new TreeSet<Integer>();
		for (CompartmentNode cn:set) result.add(cn.compartment().id());
		return result;
	}

	/**
	 * Adds an object to the set of listeners, which will be informed, when one of the lists are changed
	 * @param cListener the CompartmentListener to be informed, when the list changes
	 */
	public void setCompartmentListener(CompartmentListener cListener) {
		this.cListener=cListener;	  
  }
	
	public void scaleScrollPanes(Dimension d) {
	  int width=(d.width-lmp.getWidth()-40)/2;
	  listOfAllCompartments.setScrollPaneSize(new Dimension(width,d.height-100));
	  speciesTabs.setPreferredSize(new Dimension(width,d.height-35));
	  userSelection.setScrollPaneSize(new Dimension(width-20,d.height-90));
	  userSelection.scale();
	}
	
	public void getCode(StringBuffer sb) {
		TreeSet<Integer> userSpec = getUserSpecies();
		if (userSpec==null || userSpec.isEmpty()) return;
		XmlToken result=new XmlToken("Compartments");
		result.setContent(userSpec);
	  result.getCode(sb);
  }

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent arg0) {
		Object source = arg0.getSource();
		try {
			if (source==listOfAllCompartments){
				addCompartmentsToUserList();
			} else if (source==userSelection){
				removeSpeciesFromUserList();
			} else {			
				switch (arg0.getID()) {
				case ListModificationPanel.RIGHT:
					addCompartmentsToUserList();
					break;
				case ListModificationPanel.LEFT:
					removeSpeciesFromUserList();
					break;
				}
			}
    } catch (SQLException e) {
      e.printStackTrace();
    }
	}

	public void loadState(XmlToken token) throws SQLException {
		userSelection.loadStae(token);
  }

}
