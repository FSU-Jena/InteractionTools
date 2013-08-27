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
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import edu.fsuj.csb.gui.VerticalPanel;
import edu.fsuj.csb.reactionnetworks.organismtools.DbCompartment;
import edu.fsuj.csb.reactionnetworks.organismtools.gui.DbComponentNode;
import edu.fsuj.csb.tools.organisms.gui.CompartmentNode;
import edu.fsuj.csb.tools.organisms.gui.SortedTreeNode;
import edu.fsuj.csb.tools.xml.ObjectComparator;

/**
 * a gui element to display compartments
 * @author Stephan Richter
 *
 */
public class CompartmentList extends VerticalPanel implements ChangeListener, MouseListener, TreeSelectionListener {

  private static final long serialVersionUID = 10;
  private SortedTreeNode root;
  private JTree compartmentTree;
  private TreeSet<ChangeListener> changeListeners;
  private String rootText;
	private TreeSet<ActionListener> actionListeners;
	private JScrollPane scp;
	private MetabolicNetworkPanel netViewer;
	private static final Dimension initialSize=new Dimension(150,300);
  
	/**
	 * create new compartment list
	 * @param width the desired width for the list
	 * @param height the desired height for the list
	 * @param rootText the text which shall be applied to the root node
	 */
	public CompartmentList(String rootText) {
		this.rootText=rootText;
		root=new SortedTreeNode(rootText);
		root.addChangeListener(this);
		compartmentTree=new JTree(new DefaultTreeModel(root));
		compartmentTree.addMouseListener(this);
		compartmentTree.addTreeSelectionListener(this);
		scp = new JScrollPane(compartmentTree);
		scp.setPreferredSize(initialSize);
		scp.setSize(scp.getPreferredSize());
		add(scp);
		scale();
  }
	
	/**
	 * @param e informs all registered change listeners about a change event from the compartment list
	 */
	private void alert(ChangeEvent e){
		if (changeListeners!=null){
			for (Iterator<ChangeListener> it = changeListeners.iterator(); it.hasNext();)
				it.next().stateChanged(e);
		}
		
	}

	/**
	 * @param subtree adds a tree node to the root node and informs all registered listeners
	 */
	public void addElement(DefaultMutableTreeNode subtree) {
		root.add(subtree);
		alert(new ChangeEvent(this));
  }

	/**
	 * @return the set of selected compartment nodes
	 */
	public TreeSet<CompartmentNode> getSelected() {
		TreeSet<CompartmentNode> ids=new TreeSet<CompartmentNode>(ObjectComparator.get());
		TreePath[] paths = compartmentTree.getSelectionPaths();
		if (paths==null) return ids;
		for (int i=0; i<paths.length; i++){
			Object component = paths[i].getLastPathComponent();
			if (component instanceof CompartmentNode){
				Integer cid = ((CompartmentNode)component).compartment().id();
				if (cid!=null) ids.add((CompartmentNode) paths[i].getLastPathComponent());
			}
		}
	  return ids;
  }

	/**
	 * adds a bunch of compartment nodes to the root node
	 * @param treeSet 
	 */
	public void addCompartments(TreeSet<CompartmentNode> treeSet) {
		JFrame frame = (JFrame) SwingUtilities.getRoot(this);
		frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		root.addAll(treeSet); 
		alert(new ChangeEvent(this));
		frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		for (CompartmentNode cn:treeSet){
			if (netViewer !=null && cn!=null && cn.compartment()!=null)	netViewer.addReactions(cn.compartment().reactions());
		}
  }
	
	/**
	 * refreshes the list of compartments
	 */
	public void refreshList() {
		DefaultTreeModel model = (DefaultTreeModel) compartmentTree.getModel();
		model.reload();
  }

	/**
	 * removes the selected nodes from the list of compartments
	 */
	public void removeSelected() {
		JFrame frame = (JFrame) SwingUtilities.getRoot(this);
		frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		DefaultTreeModel model = (DefaultTreeModel) compartmentTree.getModel();
		for (Iterator<CompartmentNode> it = getSelected().iterator();it.hasNext();){
			try {
				model.removeNodeFromParent(it.next());
			} catch (NullPointerException npe){
				// don't know, why this exception is thrown here...
			}
		}
		alert(new ChangeEvent(this));
		frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
  }

	/**
	 * @return the set of selected nodes
	 */
  public TreeSet<CompartmentNode> getListed() {
		return getCompartments(root); 
  }

	private TreeSet<CompartmentNode> getCompartments(TreeNode root) {
	  TreeSet<CompartmentNode> result=CompartmentNode.set();
		@SuppressWarnings("rawtypes")
    Enumeration children = root.children();
		while (children.hasMoreElements()){
			TreeNode child = (TreeNode)children.nextElement();
			if (child instanceof CompartmentNode) {
				result.add((CompartmentNode)child);
			}
			if (!child.toString().equals("contained in"))	result.addAll(getCompartments(child));
		}
		return result;
  }

	/**
	 * @param cl registers a new change listener, which will be informed, if the content of this list is changed
	 */
	public void addChangeListener(ChangeListener cl) {
		if (changeListeners==null) changeListeners=new TreeSet<ChangeListener>(ObjectComparator.get());
	  changeListeners.add(cl);
  }

	/* (non-Javadoc)
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	public void stateChanged(ChangeEvent e) {
		refreshList();
  }

	/**
	 * @return the text of the root node
	 */
	public String caption(){
		return rootText;
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount()>=2) fireEvent(new ActionEvent(this, 2, "Click"));
		if (e.getButton()==MouseEvent.BUTTON3) showPopupForComponentAt(e.getPoint());
  }
	
	private void showPopupForComponentAt(Point pos1) {
    TreePath path = compartmentTree.getPathForLocation(pos1.x,pos1.y);
		if (path == null) return;
		try {
	    PopupMenu.showPopupFor(path.getLastPathComponent(),pos1,compartmentTree);
    } catch (SQLException e) {
	    e.printStackTrace();
    } catch (DataFormatException e) {
	    e.printStackTrace();
    } catch (IOException e) {
	    e.printStackTrace();
    }
	}  

	/**
	 * adds an action listener to this list of compartments
	 * @param al the listener, which will be registered
	 */
	public void addActionListener(ActionListener al) {
		if (actionListeners == null) actionListeners = new TreeSet<ActionListener>(ObjectComparator.get());
		actionListeners.add(al);
	}

	/**
	 * informs all registered action listeners about an action event occured within this list
	 * @param actionEvent the action event, which shall be propagated
	 */
	private void fireEvent(ActionEvent actionEvent) {
		if (actionListeners==null) return;
	  for (Iterator<ActionListener> it=actionListeners.iterator(); it.hasNext();)	it.next().actionPerformed(actionEvent);
  }

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	public void mouseEntered(MouseEvent arg0) {  }

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	public void mouseExited(MouseEvent arg0) {  }

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent arg0) {  }

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public void mouseReleased(MouseEvent arg0) {  }

	public void valueChanged(TreeSelectionEvent event) {
		Object lastSelected = compartmentTree.getLastSelectedPathComponent();
		if (lastSelected instanceof CompartmentNode){
			try {
				CompartmentNode cn=((CompartmentNode) lastSelected);
	      cn.loadDetails();
	      if (cn.getChildCount()>0) compartmentTree.expandPath(new TreePath(((DefaultMutableTreeNode)cn.getFirstChild()).getPath()));
      } catch (MalformedURLException e1) {
	      e1.printStackTrace();
      } catch (SQLException e2) {
	      e2.printStackTrace();
      } catch (DataFormatException e1) {
	      e1.printStackTrace();
      } catch (IOException e) {
	      e.printStackTrace();
      }
		}		
  }

	public void addCompartment(DbCompartment compartment) throws SQLException {
		JFrame frame = (JFrame) SwingUtilities.getRoot(this);
		frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		root.add(DbComponentNode.create(compartment));
		alert(new ChangeEvent(this));
		frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));		
  }
	
	public void setScrollPaneSize(Dimension d){
		scp.setPreferredSize(d);
	}

	public void setNetworkViewer(MetabolicNetworkPanel networkViewer) {
		netViewer=networkViewer;
  }

	public MetabolicNetworkPanel networkViewer() {
		return netViewer;
  }
}
