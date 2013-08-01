package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.fsuj.csb.gui.HorizontalPanel;
import edu.fsuj.csb.gui.VerticalPanel;
import edu.fsuj.csb.reactionnetworks.interaction.CompartmentListener;
import edu.fsuj.csb.reactionnetworks.interaction.SubstanceList;
import edu.fsuj.csb.reactionnetworks.organismtools.DbCompartment;
import edu.fsuj.csb.reactionnetworks.organismtools.DbSubstance;
import edu.fsuj.csb.tools.organisms.Substance;
import edu.fsuj.csb.tools.organisms.gui.CompartmentNode;
import edu.fsuj.csb.tools.xml.NoTokenException;

public class SubstancesTab extends HorizontalPanel implements ActionListener, CompartmentListener, ChangeListener {


  private static final long serialVersionUID = -7263495456892770721L;
	private SubstanceList choosableSubstances;
	//private NetworkLoader networkLoader;
	private JTabbedPane selectedSubstancesTabs;
	private Vector<SubstanceList> substanceLists;
	private JButton clearButton;
	private ListModificationPanel lmp;
	private HorizontalPanel searchPanel;
	private SubstanceList list1,list2,list3,list4,list5;
	private SubstanceSearchBox searchBox;
	
	public SubstancesTab() throws IOException, NoTokenException, AlreadyBoundException, SQLException {

		add(createChoosableSubstancesList());
		add(createSelectedSubstancesTabs());
		scale();
  }

	private JTabbedPane createSelectedSubstancesTabs() {
		substanceLists=new Vector<SubstanceList>();
	  selectedSubstancesTabs=new JTabbedPane();
	  
	  
	  substanceLists.add(list1=new SubstanceList("Substances List 1",true));
	  selectedSubstancesTabs.add(list1,"List 1");
	  PopupMenu.addSubstanceList(list1);
	  list1.addActionListener(this);
	  
	  substanceLists.add(list2=new SubstanceList("Substances List 2",true));
	  selectedSubstancesTabs.add(list2,"List 2");	  
	  PopupMenu.addSubstanceList(list2);
	  list2.addActionListener(this);
	  
	  substanceLists.add(list3=new SubstanceList("Substances List 3",true));
	  selectedSubstancesTabs.add(list3,"List 3");	  
	  PopupMenu.addSubstanceList(list3);
	  list3.addActionListener(this);
	  
	  substanceLists.add(list4=new SubstanceList("Substances List 4",true));
	  selectedSubstancesTabs.add(list4,"List 4");	  
	  PopupMenu.addSubstanceList(list4);
	  list4.addActionListener(this);
	  
	  substanceLists.add(list5=new SubstanceList("Substances List 5",true));
	  selectedSubstancesTabs.add(list5,"List 5");	  
	  PopupMenu.addSubstanceList(list5);
	  list4.addActionListener(this);
	  return selectedSubstancesTabs;
  }

	private VerticalPanel createChoosableSubstancesList() throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		VerticalPanel result=new VerticalPanel();

		HorizontalPanel inner=new HorizontalPanel();
		
		inner.add(choosableSubstances = new SubstanceList("Nutrient substances",false));
		inner.add(lmp = new ListModificationPanel());		
		choosableSubstances.addActionListener(this);
		lmp.addActionListener(this);

		/*
		 ________________________________
		 |result                         |
		 | _____________________________ |
		 | |inner                       ||        
		 | | ____________________  ____ ||
		 | | |choosableSubstances| |lmp|||
		 | |____________________________||
		 | _____________________________ |
		 | |searchPanel                 ||
		 | | ____________  ____________ ||
		 | | |clearButton| |searchBox  |||
		 | |____________________________||
		 |_______________________________|
		 */
		
		searchPanel = new HorizontalPanel();		
		searchPanel.add(clearButton=new JButton("↑ clear list"));
		searchPanel.add(searchBox=new SubstanceSearchBox(choosableSubstances));		
		clearButton.addActionListener(this);
		


		
		result.add(inner);
		result.add(searchPanel);
		result.scale();
	  return result;
  }

	public void actionPerformed(ActionEvent arg0) {
		Object source = arg0.getSource();
		try {
		if (source==choosableSubstances){
			addSubstancesToUserList();			
		} else if (source.toString().startsWith("Substances List")){
			removeSubstancesFromUserList();
		} else if (source==clearButton) {
		  choosableSubstances.clear();
		} else {
			switch (arg0.getID()) {
			case ListModificationPanel.RIGHT:
				addSubstancesToUserList();
				break;
			case ListModificationPanel.LEFT:
				removeSubstancesFromUserList();
				break;
			}
		}
		} catch (SQLException e){
			e.printStackTrace();
		}
  }

	private void removeSubstancesFromUserList() {
		getSelectedList().removeSelected();
  }

	private void addSubstancesToUserList() throws SQLException {
		getSelectedList().addSubstances(choosableSubstances.getSelected());
  }
	
	public SubstanceList getSelectedList() {
		Component component = selectedSubstancesTabs.getSelectedComponent();
		if (component instanceof SubstanceList) return (SubstanceList) component;
		return null;
	}	
	
	public TreeSet<Integer> selectUserList(String question){
		Object[] dummy = nonemptySubstanceLists().toArray();
		if (dummy.length<1) {
			JOptionPane.showMessageDialog(this, "None of the substance lists is containing substances!");
			return null;
		}
		if (dummy.length==1) return ((SubstanceList)(dummy[0])).getListed();
		Object result=JOptionPane.showInputDialog(this, question, "Select substance list", JOptionPane.PLAIN_MESSAGE, null, dummy, null);
		if (result==null) return null;
		return ((SubstanceList) result).getListed();
	}

	private Vector<SubstanceList> nonemptySubstanceLists() {
		Vector<SubstanceList> result = new Vector<SubstanceList>();
		for (Iterator<SubstanceList> it = substanceLists.iterator();it.hasNext();){
			SubstanceList list=it.next();
			if (!list.getListed().isEmpty()) result.add(list);
		}		
	  return result;
  }

	public void compartmentListChanged(CompartmentList compartmentList) throws SQLException {
		//System.out.println(compartmentList);
		TreeSet<CompartmentNode> list = compartmentList.getListed();
		//System.out.println(list);
		//System.err.println("compartmentListChanged("+list+")");
		TreeSet<Integer> substanceIds=new TreeSet<Integer>();		
		for (Iterator<CompartmentNode> iter = list.iterator(); iter.hasNext();){
			CompartmentNode cNode = iter.next();
			System.err.println("...reading compartment node "+cNode);
			Integer cid=cNode.compartment().id();
			System.err.print("...trying to load compartment with id "+cid);
			DbCompartment c=DbCompartment.load(cid);
			System.err.print("..."+c+" loaded.\n...loading list of substances");
			substanceIds.addAll(c.utilizedSubstances());
			System.err.println("...done");
		}
		System.err.println("...ids collected");
		choosableSubstances.clear();
		for (Iterator<Integer> it = substanceIds.iterator();it.hasNext();){
			int sid=it.next();
			Substance subs=DbSubstance.load(sid);
			choosableSubstances.addSubstance(subs);
		}
  }

	public void stateChanged(ChangeEvent e) {
		Object source = e.getSource();
		if (source instanceof CompartmentList){
			try {
	      compartmentListChanged((CompartmentList) source);
      } catch (SQLException e1) {
	      e1.printStackTrace();
      }
		}
  }

	public Vector<SubstanceList> getLists() {
		return substanceLists;
  }

	public TreeSet<Integer> produceList() {
		SubstanceList dummy =  SubstanceList.getProduceList();
		if (dummy==null) return null;
	  return dummy.getListed();
  }

	public TreeSet<Integer> degradeList() {
		SubstanceList dummy = SubstanceList.getDegradeList();
		if (dummy==null) return null;
	  return dummy.getListed();
  }

	public TreeSet<Integer> ignoreList() {
		SubstanceList dummy =  SubstanceList.getIgnoreList();
		if (dummy==null) return null;
	  return dummy.getListed();
  }
	
	public TreeSet<Integer> noInflowList() {
		SubstanceList dummy =  SubstanceList.getNoInflowList();
		if (dummy==null) return null;
	  return dummy.getListed();
  }

	public TreeSet<Integer> noOutflowList() {
		SubstanceList dummy =  SubstanceList.getNoOutflowList();
		if (dummy==null) return null;
	  return dummy.getListed();
  }
	
	public void scaleScrollPanes(Dimension d) {
		int width=(d.width-lmp.getWidth()-60)/2;
		choosableSubstances.scaleScrollPane(new Dimension(width-30,d.height-searchPanel.getHeight()-70));
		Dimension dim = searchBox.getPreferredSize();
		dim.width=choosableSubstances.getWidth()+lmp.getWidth()-clearButton.getWidth();
		searchBox.setPreferredSize(dim);

		selectedSubstancesTabs.setPreferredSize(new Dimension(width+30,d.height-40));
		d=new Dimension(width+10,d.height-200);
		list1.scaleScrollPane(d);
		list2.scaleScrollPane(d);
		list3.scaleScrollPane(d);
		list4.scaleScrollPane(d);
		list5.scaleScrollPane(d);
		list1.scale();
		list2.scale();
		list3.scale();
		list4.scale();
		list5.scale();
  }



}
