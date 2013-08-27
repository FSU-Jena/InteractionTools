package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.omg.CORBA.NVList;

import edu.fsuj.csb.gui.GenericFileFilter;
import edu.fsuj.csb.gui.PanelTools;
import edu.fsuj.csb.reactionnetworks.interaction.SubstanceList;
import edu.fsuj.csb.reactionnetworks.organismtools.gui.DbCompartmentNode;
import edu.fsuj.csb.tools.organisms.gui.CompartmentNode;
import edu.fsuj.csb.tools.organisms.gui.SubstanceNode;
import edu.fsuj.csb.tools.organisms.gui.URLNode;
import edu.fsuj.csb.tools.organisms.gui.UrnNode;
import edu.fsuj.csb.tools.xml.ObjectComparator;
import edu.fsuj.csb.tools.xml.XmlObject;

public class PopupMenu extends JPopupMenu implements ActionListener {

  private static final long serialVersionUID = -7722530530571660850L;
	private Object targetObject;
	private String objectText;
	private JMenuItem search;
	private JMenuItem clip;
	private JMenuItem cListItem,netViewItem;	
	private static CompartmentList compartmentList=null;
	private static TreeSet<SubstanceList> substanceLists=new TreeSet<SubstanceList>(ObjectComparator.get());

	public PopupMenu(Object targetObject, Point pos,Object parent) throws SQLException, DataFormatException, IOException {
		super();
		setLocation(pos);
		
		objectText=targetObject.toString();
		System.out.println(targetObject.getClass());

		search = new JMenuItem("Search on Google");
		search.addActionListener(this);
		add(search);
		
		clip= new JMenuItem("copy to clipboard");
		clip.addActionListener(this);
		add(clip);
		
		if (targetObject instanceof URLNode){			
			JMenuItem urlItem = ((URLNode)targetObject).menuItem();
			urlItem.addActionListener(this);
			add(urlItem);
		}
		if (targetObject instanceof UrnNode){			
			JMenuItem urnItem = ((UrnNode)targetObject).menuItem();
			urnItem.addActionListener(this);
			add(urnItem);
		}

		if (targetObject instanceof CompartmentNode){
			objectText=((CompartmentNode)targetObject).compartment().mainName();
			cListItem=new JMenuItem("add to \""+compartmentList.caption()+"\"");
			cListItem.addActionListener(this);
			add(cListItem);
			
		}
		if (targetObject instanceof SubstanceNode){
			objectText=((SubstanceNode)targetObject).substance().mainName();
			for (Iterator<SubstanceList> it = substanceLists.iterator();it.hasNext();){
				SubstanceList sl = it.next();
				ListMenuItem item = new ListMenuItem(sl);
				item.addActionListener(this);
				add(item);
			}
			
			netViewItem=new JMenuItem("show in network view");
			netViewItem.addActionListener(this);
			add(netViewItem);
		}
		
		if (targetObject instanceof XmlObject){
			JMenuItem item=new ExportXmlItem((XmlObject) targetObject);
			item.addActionListener(this);
			add(item);
		}
		
		JMenuItem cancel = new JMenuItem("cancel");
		cancel.addActionListener(this);
		add(cancel);
		
		this.targetObject=targetObject;
		setBorder(BorderFactory.createLineBorder(Color.black));
	}

	private void searchFor(Object targetObject) {
		try {
	    Runtime.getRuntime().exec("gnome-open http://www.google.com/search?q="+targetObject.toString().replace(" ", "+"));
    } catch (IOException e) {
	    e.printStackTrace();
    }
  }

	private void copyToClipboard(Object targetObject) {
		java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(targetObject.toString()), null);
  }

	public static void showPopupFor(Object source, Point pos,Component parent) throws SQLException, DataFormatException, IOException{
		//System.err.println("showPopupFor("+source.getClass().getSimpleName()+")");
		PopupMenu popup = new PopupMenu(source,pos,parent);
		popup.show(parent,pos.x,pos.y);
	}
	
	public static void setCompartmentList(CompartmentList cl){
		compartmentList=cl;
	}

	public static void addSubstanceList(SubstanceList sl){
		substanceLists.add(sl);
	}
	
	public void actionPerformed(ActionEvent arg0) {
		setVisible(false);
		Object option = arg0.getSource();
		if (option==search) searchFor(objectText);
		if (option==clip) copyToClipboard(objectText);
		if (option==netViewItem) {
			try {
	      compartmentList.networkViewer().jumpToSubatance(((SubstanceNode)targetObject).substance().id());	      
      } catch (SQLException e) {
	      e.printStackTrace();
      }
		}
		if (option==cListItem) try {
			DbCompartmentNode dbComp = (DbCompartmentNode)targetObject;
	    compartmentList.addCompartment(dbComp.compartment());
    } catch (SQLException e) {
	    e.printStackTrace();
    }
		if (option instanceof ExportXmlItem){
			try {
				URL filename = PanelTools.showSelectFileDialog("State output file", "network.sbml.xml", new GenericFileFilter("SBML file", "*.xml"), this);
				if (filename!=null){
					System.out.println("Starting SBML export. This may take a while.");
					((ExportXmlItem) option).export(filename);
					System.out.println("Exported to "+filename);
				}
      } catch (URISyntaxException e) {
	      e.printStackTrace();
      } catch (IOException e) {
	      e.printStackTrace();
      }
		}
    if (option instanceof ListMenuItem){
    	try {
	      ((ListMenuItem) option).getList().addSubstance(((SubstanceNode)targetObject).substance());
      } catch (SQLException e) {
	      e.printStackTrace();
      }
    }
  }
}