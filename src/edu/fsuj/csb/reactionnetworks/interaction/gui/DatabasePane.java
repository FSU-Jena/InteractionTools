package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.zip.DataFormatException;

import javax.naming.directory.NoSuchAttributeException;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.apache.batik.swing.JSVGCanvas;

import edu.fsuj.csb.gui.GenericFileFilter;
import edu.fsuj.csb.gui.HorizontalPanel;
import edu.fsuj.csb.gui.PanelTools;
import edu.fsuj.csb.gui.VerticalPanel;
import edu.fsuj.csb.reactionnetworks.database.InteractionDB;
import edu.fsuj.csb.reactionnetworks.databasefiller.SBMLLoader;
import edu.fsuj.csb.reactionnetworks.databasefiller.dbtool;
import edu.fsuj.csb.reactionnetworks.interaction.Commons;
import edu.fsuj.csb.reactionnetworks.interaction.UnificationNode;
import edu.fsuj.csb.reactionnetworks.organismtools.DbSubstance;
import edu.fsuj.csb.reactionnetworks.organismtools.gui.DbComponentNode;
import edu.fsuj.csb.tools.organisms.Substance;
import edu.fsuj.csb.tools.organisms.gui.SubstanceNode;
import edu.fsuj.csb.tools.organisms.gui.URLNode;
import edu.fsuj.csb.tools.organisms.gui.UrnNode;
import edu.fsuj.csb.tools.xml.NoTokenException;
import edu.fsuj.csb.tools.xml.ObjectComparator;
import edu.fsuj.csb.tools.xml.Tools;

/**
 * this panel has been implemented to allow to feed new sbml models into the database right FROM the GUI of interaction tools
 * 
 * @author Stephan Richter
 * 
 */
public class DatabasePane extends HorizontalPanel implements ActionListener, MouseListener, TreeSelectionListener {
	private static final long serialVersionUID = -7663240543407963010L;
	private JButton loadFileButton;
	private JComboBox sources;
	private String newGroup = "<add new group>";
	private JButton cleanButton;
	private TreeSet<ChangeListener> changeListeners;
	private TreeMap<Integer, Integer> mappingFromGroupIndicesToIds;
	private JTree unificationTree;
	private JScrollPane scrollPane;
	private JButton examinationButton;
	private JSVGCanvas svg;
	private JComponent addModPanel;
	private static String substancePrefix="Substance #";

	/**
	 * create a new database pane
	 * 
	 * @throws SQLException
	 * @throws DataFormatException 
	 * @throws IOException 
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public DatabasePane() throws SQLException, DataFormatException, IOException {
		add(createButtonPanel());
		add(createUnificationPanel());
		scale();
	}	
	

	private VerticalPanel createButtonPanel() throws SQLException, IOException, DataFormatException {
		VerticalPanel result=new VerticalPanel();
		result.add(addModPanel=createAddModelPanel());
		result.add(createUnificationTree());
		examinationButton=new JButton("Analyze all unifications (may take a LONG time!)");
		examinationButton.addActionListener(this);
		result.add(examinationButton);
	  return result;
  }


	/**
	 * @return creates a unification panel including the tree of unified substances and a svg panel showing graphical information
	 * @throws SQLException
	 * @throws DataFormatException
	 * @throws IOException 
	 */
	private JSVGCanvas createUnificationPanel() throws SQLException, DataFormatException, IOException {
		System.out.println("   `- creating panel for unification examination...");
		
		svg=new JSVGCanvas();
		svg.setPreferredSize(new Dimension(630,630));
		svg.setSize(svg.getPreferredSize());
		
	  return svg;
  }
	
	/**
	 * creates a swing component with the unification tree inside a scroll area
	 * @return the swing component
	 * @throws SQLException
	 * @throws DataFormatException
	 * @throws IOException 
	 */
	private JComponent createUnificationTree() throws SQLException, DataFormatException, IOException {
		unificationTree=new JTree(getUnificationTree());
		unificationTree.addMouseListener(this);
		unificationTree.addTreeSelectionListener(this);		
		scrollPane=new JScrollPane(unificationTree);
		scrollPane.setPreferredSize(new Dimension(600	, 650));		
		return scrollPane;
  }


	/**
	 * creates a treee of nodes which include unifications
	 * @return the reference to the root of the tree
	 * @throws SQLException
	 * @throws DataFormatException
	 * @throws IOException 
	 */
	public static DefaultMutableTreeNode getUnificationTree() throws SQLException, DataFormatException, IOException {
		Vector<Integer> ids = InteractionDB.getIdsOfSubstancesWithMultipleReferencingURLs();
		DefaultMutableTreeNode root=new DefaultMutableTreeNode("Unifications ("+ids.size()+")");
		TreeSet<MutableTreeNode> nodes=new TreeSet<MutableTreeNode>(ObjectComparator.get());
		for (Iterator<Integer> idIterator = ids.iterator(); idIterator.hasNext();) {
	    Integer id = idIterator.next();
	    MutableTreeNode node = new UnificationNode(id);
	    nodes.add(node);
    }
		for (Iterator<MutableTreeNode> it = nodes.iterator(); it.hasNext();) root.add(it.next());
	  return root;
  }

	private HorizontalPanel createAddModelPanel() throws SQLException, IOException {
		HorizontalPanel addModelPanel = new HorizontalPanel();
		loadFileButton = new JButton("load SBML file");
		loadFileButton.addActionListener(this);

		sources = createDropDown();

		cleanButton = new JButton("clean Database");
		cleanButton.addActionListener(this);
		addModelPanel.add(loadFileButton);
		addModelPanel.add(sources);
		addModelPanel.add(cleanButton);
		addModelPanel.scale();
		return addModelPanel;
  }


	/**
	 * creates a drop down list of the compartment groups
	 * 
	 * @return the newly created drop-down list
	 * @throws SQLException
	 * @throws IOException 
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	private JComboBox createDropDown() throws SQLException, IOException {
		Statement st = InteractionDB.createStatement();
		ResultSet rs = st.executeQuery("SELECT id,name FROM names NATURAL JOIN ids WHERE type=" + InteractionDB.COMPARTMENT_GROUP);
		Vector<String> strings = new Vector<String>();
		mappingFromGroupIndicesToIds = new TreeMap<Integer, Integer>();
		int index = 0;
		while (rs.next()) {
			mappingFromGroupIndicesToIds.put(index++, rs.getInt(1));
			strings.add(rs.getString(2));
		}
		strings.add(newGroup);
		JComboBox result = new JComboBox(strings);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent arg0) {
		Object source = arg0.getSource();
		try {
			if (source == loadFileButton) {
				loadSBMLFile();
			} else if (source == cleanButton) {
				int[] keggRange=InteractionDB.getRange(Commons.KEGG_IDS);
				int[] biomodelsRange=InteractionDB.getRange(Commons.BIOMODELS_IDS);
				dbtool.cleanDb(keggRange);
				dbtool.cleanDb(biomodelsRange);
			} else if (source == examinationButton){
				examineAll();
			}	else System.out.println(source);			
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		} catch (NoTokenException e) {
			e.printStackTrace();
			System.exit(0);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			System.exit(0);
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		} catch (NoSuchMethodException e) {
	    e.printStackTrace();
			System.exit(0);
    } catch (DataFormatException e) {
	    e.printStackTrace();
			System.exit(0);
    } catch (NoSuchAttributeException e) {
	    e.printStackTrace();
			System.exit(0);
    }
	}

	private void databaseChanged() {
		ChangeEvent ce = new ChangeEvent(this);
		for (Iterator<ChangeListener> it = changeListeners.iterator(); it.hasNext();) {
			it.next().stateChanged(ce);
		}
	}

	/**
	 * tries to determine the number of the group, which the user has selected in the drop-down list
	 * 
	 * @param sources the drop-down list, on which this method is applied
	 * @return the group index of the selected group
	 * @throws SQLException
	 * @throws IOException 
	 */
	private int getGroup(JComboBox sources) throws SQLException, IOException {
		int i = sources.getSelectedIndex();
		if (i + 1 == sources.getItemCount()) return addGroup();
		return mappingFromGroupIndicesToIds.get(i);
	}

	/**
	 * adds a group to the compartment group list
	 * 
	 * @return the number of the added group
	 * @throws SQLException
	 * @throws IOException 
	 */
	private int addGroup() throws SQLException, IOException {
		String groupname = JOptionPane.showInputDialog("Enter name of new Group");
		int id = InteractionDB.getOrCreateGroup(groupname);
		int index = sources.getItemCount() - 1;
		sources.removeItemAt(index);
		sources.addItem(groupname);
		sources.addItem(newGroup);
		sources.setSelectedIndex(index);
		mappingFromGroupIndicesToIds.put(index, id);
		return id;
	}

	/**
	 * reads an sbml file and saves it's contents into the database. the compartment of the sbml will be added to the indicated compartment group
	 * 
	 * @param sourceId the number of the compartment group, to which the models compartment will be added
	 * @throws IOException
	 * @throws NoTokenException
	 * @throws NoSuchAlgorithmException
	 * @throws SQLException
	 * @throws DataFormatException 
	 * @throws NoSuchMethodException 
	 * @throws NoSuchAttributeException 
	 */
	private void loadSBMLFile() throws IOException, NoTokenException, NoSuchAlgorithmException, SQLException, NoSuchMethodException, DataFormatException, NoSuchAttributeException {
		int groupId = getGroup(sources);
		URL fileUrl = PanelTools.showSelectFileDialog("open SBML file", null, new GenericFileFilter("SBML file", "sbml;xml"), this);
  	Tools.resetWarnings();
		SBMLLoader.loadSBMLFile(fileUrl, groupId,null);
		databaseChanged();
	}

	public void addChangeListener(ChangeListener listener) {
		if (changeListeners == null) changeListeners = new TreeSet<ChangeListener>(ObjectComparator.get());
		changeListeners.add(listener);
	}

	TreeSet<UrnNode> urnNodesWithSource=new TreeSet<UrnNode>(ObjectComparator.get());
	private UnificationNode lastAnalyzed;

	public void mouseClicked(MouseEvent evt) {		
		if (evt.getButton()==MouseEvent.BUTTON3) { /* Rechtsklick => Popupmenü */
			showPopupForComponentAt(evt.getPoint());
		} else { /* Linksklick: */
			Object o=getComponent(evt.getPoint());
			if ((o instanceof UrnNode)&&(!urnNodesWithSource.contains(o))){ /* clicked node was urn: load information and links */
				UrnNode urnNode=(UrnNode)o;
				try {
		      TreeSet<URL> sources=InteractionDB.getReferencingURLs(urnNode.getUrn());
		      if (!sources.isEmpty()) {
		      	DefaultMutableTreeNode ref = new DefaultMutableTreeNode("referenced from:");
		      	for (Iterator<URL> it = sources.iterator();it.hasNext();)	ref.add(new URLNode(it.next()));
		      	urnNode.add(ref);
		      }      
		    } catch (SQLException e1) {
		      e1.printStackTrace();
		    } catch (IOException e) {
	        e.printStackTrace();
        }				
				try {
		      Set<URL> sites=urnNode.getUrn().urls();
		      if (!sites.isEmpty()) {
		      	DefaultMutableTreeNode ref = new DefaultMutableTreeNode("links to:");
		      	for (Iterator<URL> it = sites.iterator();it.hasNext();)	ref.add(new URLNode(it.next()));
		      	urnNode.add(ref);
		      }
		    } catch (MalformedURLException e) {
		      e.printStackTrace();
		    }		
		  
				urnNodesWithSource.add(urnNode);
		
				
			}
			if (o instanceof UnificationNode) { /* clicked node was unification node: analyze */
	      UnificationNode node = (UnificationNode) o;
	      try {
//	      	Tools.enableLogging();
	      	this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	        svg.setURI(node.analyze(true));
	        lastAnalyzed=node;
        } catch (SQLException e1) {
	        e1.printStackTrace();
        } catch (DataFormatException e1) {
	        e1.printStackTrace();
        } catch (MalformedURLException e) {
	        e.printStackTrace();
        } catch (IOException e) {
	        e.printStackTrace();
        } catch (NoTokenException e) {
	        e.printStackTrace();
        }
      	this.setCursor(Cursor.getDefaultCursor());
      }
			if (o instanceof DefaultMutableTreeNode) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) o;
	      unificationTree.expandPath(new TreePath(node.getPath()));
      }
		}
  }
	
	private void examineAll() {
		TreeModel model = unificationTree.getModel();
		Object root = model.getRoot();
		int count=model.getChildCount(root);
  	this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
  	boolean skip=true;
		for (int i=0; i<count; i++){
			Object child = model.getChild(root, i);
			if (child instanceof UnificationNode) {
	      UnificationNode unode = (UnificationNode) child;
	      if (skip && (lastAnalyzed==null || unode.equals(lastAnalyzed))){
	      	skip=false;
	      	continue; // the next is the first to be analyzed
	      }
	      if (skip) continue;
	      try {
	      	System.out.println("\n("+i+"/"+count+") Analyzing "+unode);
	        unode.analyze(false);
        } catch (NullPointerException e) {
	        e.printStackTrace();
	        break;
        } catch (SQLException e) {
	        e.printStackTrace();
	        System.exit(0);
        } catch (DataFormatException e) {
	        e.printStackTrace();
	        System.exit(0);
        } catch (IOException e) {
	        e.printStackTrace();
	        System.exit(0);
        } catch (NoTokenException e) {
	        e.printStackTrace();
	        System.exit(0);
        }
      }
		}
  	this.setCursor(Cursor.getDefaultCursor());

  }
	
	private Object getComponent(Point pos){
		TreePath path = unificationTree.getPathForLocation(pos.x,pos.y);
		if (path == null) return null;
		return path.getLastPathComponent();
	}
	
	private void showPopupForComponentAt(Point pos1)  {
		Object o=getComponent(pos1);
		 try {
			 if (o!=null) PopupMenu.showPopupFor(o,pos1,unificationTree);
    } catch (SQLException e) {
	    e.printStackTrace();
    } catch (DataFormatException e) {
	    e.printStackTrace();
    } catch (IOException e) {
	    e.printStackTrace();
    }
	}  


	public void mouseEntered(MouseEvent e) {
	  // TODO Auto-generated method stub
  }


	public void mouseExited(MouseEvent e) {
	  // TODO Auto-generated method stub
  }


	public void mousePressed(MouseEvent e) {
	  // TODO Auto-generated method stub
  }


	public void mouseReleased(MouseEvent e) {
	  // TODO Auto-generated method stub
  }


	public void valueChanged(TreeSelectionEvent event) {
		Object lastSelected = unificationTree.getLastSelectedPathComponent();
		if (lastSelected instanceof SubstanceNode){
			try {
				SubstanceNode sn=((SubstanceNode) lastSelected);
	      sn.loadDetails();
        sn.insert(referencingUrls(sn.substance().id()),0);
      } catch (MalformedURLException e) {
	      e.printStackTrace();
      } catch (DataFormatException e) {
	      e.printStackTrace();
      } catch (SQLException e) {
	      e.printStackTrace();
      } catch (IOException e) {
	      e.printStackTrace();
      }
		} else if (lastSelected instanceof DefaultMutableTreeNode) {
	    DefaultMutableTreeNode node = (DefaultMutableTreeNode) lastSelected;	    
	    
	    
	    if (node.toString().startsWith(substancePrefix)){
	    	int id=Integer.parseInt(node.toString().substring(substancePrefix.length()));
	    	System.out.println("<"+id+">");
        try {
  	    	Substance substance = DbSubstance.load(id);  	    	
		    	node.insert(DbComponentNode.create(substance), 0);
        } catch (SQLException e1) {
	        e1.printStackTrace();
        }
	    }
		}
  }


	private MutableTreeNode referencingUrls(int id) throws SQLException, IOException {
		DefaultMutableTreeNode result=new DefaultMutableTreeNode("referencing URLs");
		Vector<URL> urls = InteractionDB.getReferencingURLs(id);
		for (Iterator<URL> it = urls.iterator(); it.hasNext();) {
	    URL url = it.next();
	    result.add(new URLNode(url));
    }
	  return result;
  }
	
	@Override
	public void setSize(Dimension d) {
	  super.setSize(d);
	  d.width=d.width-10;
	  d.height=d.height-addModPanel.getHeight()-examinationButton.getHeight();
	  Dimension scpd = scrollPane.getSize();
	  scpd.height=d.height-135;
	  scpd.width=d.width-svg.getWidth()-45;	  
	  if (scpd.width<addModPanel.getWidth()) scpd.width=addModPanel.getWidth();
	  scrollPane.setPreferredSize(scpd);
	}

	public void scaleTo(Dimension d) {
		int width=(d.width/2)-150;
		
		if (width<addModPanel.getWidth())width=addModPanel.getWidth();
		
		Dimension scpd = new Dimension(width,d.height-addModPanel.getHeight()-examinationButton.getHeight()-70);
		
	  Dimension svgd=new Dimension(d.width-width-40,d.height-60);
	  svg.setPreferredSize(svgd);
	  svg.setSize(svgd);

	  scrollPane.setPreferredSize(scpd);

	}

}
