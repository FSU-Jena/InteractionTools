package edu.fsuj.csb.reactionnetworks.interaction;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import edu.fsuj.csb.gui.HorizontalPanel;
import edu.fsuj.csb.gui.VerticalPanel;
import edu.fsuj.csb.reactionnetworks.interaction.gui.PopupMenu;
import edu.fsuj.csb.tools.organisms.Substance;
import edu.fsuj.csb.tools.organisms.gui.SortedTreeNode;
import edu.fsuj.csb.tools.organisms.gui.SubstanceNode;
import edu.fsuj.csb.tools.xml.ObjectComparator;

public class SubstanceList extends VerticalPanel implements ChangeListener,TreeSelectionListener, MouseListener, ActionListener {

	private static final long serialVersionUID = 10;
	private SortedTreeNode root;
	private JTree substancesTree;
	private TreeSet<ChangeListener> changeListeners;
	private TreeSet<ActionListener> actionListeners;
	private String name;
	private JCheckBox degradeButton;
	private JCheckBox produceButton;
	private JCheckBox ignoreButton;
	private static SubstanceList degradeList;
	private static SubstanceList produceList;
	private static SubstanceList ignoreList;

	public SubstanceList(int width, int height, String name,boolean showBoxes) {
		this.name=name;
		root = new SortedTreeNode("Substances");
		root.addChangeListener(this);
		substancesTree = new JTree(new DefaultTreeModel(root));
		substancesTree.addTreeSelectionListener(this);
		substancesTree.addMouseListener(this);
		JScrollPane scp = new JScrollPane(substancesTree);
		scp.setPreferredSize(new Dimension(width, height));
		scp.setSize(scp.getPreferredSize());
		if (showBoxes){
			HorizontalPanel buttonPane=new HorizontalPanel();
			buttonPane.add(degradeButton=new JCheckBox("<html>substances&nbsp;&nbsp;&nbsp;&nbsp;<br>to degrade"));
			degradeButton.addActionListener(this);
			buttonPane.add(produceButton=new JCheckBox("<html>substances&nbsp;&nbsp;&nbsp;&nbsp;<br>to produce"));
			produceButton.addActionListener(this);
			buttonPane.add(ignoreButton=new JCheckBox("<html>substances&nbsp;&nbsp;&nbsp;&nbsp;<br>to ignore"));
			ignoreButton.addActionListener(this);
			buttonPane.skalieren();
			add(buttonPane);
			if (degradeList==null) {
				setDegradeList(true); 
			} else if (produceList==null){
				setProduceList(true);
			} else if (ignoreList==null){
				setIgnoreList(true);
			}
		}
		add(scp);
		skalieren();
		
	}

	public void addSubstance(Substance s) throws SQLException {
		//System.err.println("SubstanceList.addSubstance("+s+")");
		int sid=s.id();
		for (Iterator<String> names = s.names().iterator(); names.hasNext();) {
			String name = names.next();
			Object child = root.getChildByName(name);
			//System.err.println("child="+child);
			SubsstanceGroupNode sgn=null;
			if (child != null) {
				if (child instanceof SubsstanceGroupNode) {
					sgn=((SubsstanceGroupNode) child);
					//System.err.println("using existing substanceGroupNode "+sgn);
				}
			} else {
				//System.err.println("creating new substanceGroupNode("+name+")");
				sgn=new SubsstanceGroupNode(name);
				root.addWithoutPublishing(sgn);
			}
			sgn.add(sid);
		}
		root.publish();		
	}
	
	public void addSubstances(TreeSet<Substance> substances) throws SQLException {
		for (Iterator<Substance> it = substances.iterator(); it.hasNext();) {
			addSubstance(it.next());
		}
	}

	public TreeSet<Substance> getSelected() {
		TreeSet<Substance> substances = new TreeSet<Substance>(ObjectComparator.get());
		for (Iterator<SubstanceNode> nodes = getSelectedNodes().iterator(); nodes.hasNext();){
			substances.add(Substance.get(nodes.next().substance().id()));
		}
		return substances;
	}

	private TreeSet<Integer> getSelectedIds() {
		TreeSet<Integer> ids = new TreeSet<Integer>(ObjectComparator.get());
		for (Iterator<SubstanceNode> nodes = getSelectedNodes().iterator(); nodes.hasNext();){
			ids.add(nodes.next().substance().id());
		}
		return ids;
	}
	
	public void refreshList() {
		DefaultTreeModel model = (DefaultTreeModel) substancesTree.getModel();
		model.reload();
	}

	@SuppressWarnings("rawtypes")
  public void removeSelected() {
		TreeSet<Integer> selectedSubstances = getSelectedIds();
		DefaultTreeModel model = (DefaultTreeModel)substancesTree.getModel();
		TreeSet<SortedTreeNode> removableGroups=new TreeSet<SortedTreeNode>(ObjectComparator.get());
		for (Enumeration en = root.children();en.hasMoreElements();){			
			SortedTreeNode groupNode = (SortedTreeNode)en.nextElement();
			if (groupNode instanceof SubsstanceGroupNode)	((SubsstanceGroupNode) groupNode).removeSubstances(selectedSubstances);			
			if (groupNode.getChildCount()==0) removableGroups.add(groupNode);
		}
		for (Iterator<SortedTreeNode> it = removableGroups.iterator();it.hasNext();) model.removeNodeFromParent(it.next());
		refreshList();
		if (changeListeners != null) {
			for (Iterator<ChangeListener> listeners = changeListeners.iterator(); listeners.hasNext();)
				listeners.next().stateChanged(new ChangeEvent(this));
		}
	}

	@SuppressWarnings("rawtypes")
  private TreeSet<SubstanceNode> getSelectedNodes() {
		TreeSet<SubstanceNode> substanceNodes=new TreeSet<SubstanceNode>(ObjectComparator.get());
		TreePath[] paths = substancesTree.getSelectionPaths();
		if (paths == null) return substanceNodes;
		for (int i = 0; i < paths.length; i++) {
			Object lastPathComponent = paths[i].getLastPathComponent();
			if (lastPathComponent instanceof SubstanceNode) {
				substanceNodes.add((SubstanceNode) lastPathComponent);
			}
			if (lastPathComponent instanceof SortedTreeNode) {
				for (Enumeration it = ((SortedTreeNode) lastPathComponent).children(); it.hasMoreElements();) {
					Object child = it.nextElement();
					if (child instanceof SubstanceNode) {
						substanceNodes.add((SubstanceNode) child);
					}
				}
			}
		}
		return substanceNodes;
  }

	@SuppressWarnings("rawtypes")
  public TreeSet<Integer> getListed() {
		TreeSet<Integer> result=new TreeSet<Integer>();
		for (Enumeration en = root.children();en.hasMoreElements();){
			SortedTreeNode nameNode = (SortedTreeNode)en.nextElement();
			for (Enumeration children = nameNode.children();children.hasMoreElements();){
				SubstanceNode child=(SubstanceNode)children.nextElement();
				result.add(child.substance().id());
			}
		}
//		System.out.println(result);
		return result;
	}

	public void addChangeListener(ChangeListener cl) {
		if (changeListeners == null) changeListeners = new TreeSet<ChangeListener>(ObjectComparator.get());
		changeListeners.add(cl);
	}

	public void addActionListener(ActionListener al) {
		if (actionListeners == null) actionListeners = new TreeSet<ActionListener>(ObjectComparator.get());
		actionListeners.add(al);
	}
	
	public void stateChanged(ChangeEvent e) {
		refreshList();
	}

	public void treeCollapsed(TreeExpansionEvent arg0) {
	  // TODO Auto-generated method stub
	  
  }

	public void valueChanged(TreeSelectionEvent e) {
		Object lastSelected = substancesTree.getLastSelectedPathComponent();
		if (lastSelected instanceof SubstanceNode){
			try {
				SubstanceNode sn=((SubstanceNode) lastSelected);
	      sn.loadDetails();	      
	      substancesTree.expandPath(new TreePath(((DefaultMutableTreeNode)sn.getFirstChild()).getPath()));
      } catch (MalformedURLException e1) {
	      e1.printStackTrace();
      } catch (DataFormatException e1) {
	      e1.printStackTrace();
      }
		}		
  }
	
	public String toString(){
		return name;
	}

	public void clear() {
		root.removeAllChildren();
		refreshList();
	  
  }

	public void mouseClicked(MouseEvent e) {
	  if (e.getClickCount()>=2) 	fireEvent(new ActionEvent(this, 2, "Click"));
		if (e.getButton()==MouseEvent.BUTTON3) showPopupForComponentAt(e.getPoint());
  }
	
	private void showPopupForComponentAt(Point pos1) {
    TreePath path = substancesTree.getPathForLocation(pos1.x,pos1.y);
		if (path == null) return;
		PopupMenu.showPopupFor(path.getLastPathComponent(),pos1,substancesTree);
	}  

	private void fireEvent(ActionEvent actionEvent) {
		if (actionListeners==null) return;
	  for (Iterator<ActionListener> it=actionListeners.iterator(); it.hasNext();){
	  	it.next().actionPerformed(actionEvent);
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

	public void actionPerformed(ActionEvent arg0) {
		Object source=arg0.getSource();
		if (source==degradeButton) setDegradeList(degradeButton.isSelected());
		if (source==produceButton) setProduceList(produceButton.isSelected());
		if (source==ignoreButton) setIgnoreList(ignoreButton.isSelected());
  }
	
	private void setDegradeList(boolean on) {
		if (on){
			degradeButton.setSelected(true);
			setProduceList(false);
			setIgnoreList(false);
			if (degradeList!=null && degradeList!=this) degradeList.setDegradeList(false);
			degradeList=this;
		} else {
			degradeButton.setSelected(false);
			if (degradeList==this) degradeList=null;
		}
  }

	private void setIgnoreList(boolean on) {
		if (on){
			ignoreButton.setSelected(true);
			setDegradeList(false);
			setProduceList(false);
			if (ignoreList!=null && ignoreList!=this) ignoreList.setIgnoreList(false);
			ignoreList=this;
		} else {
			ignoreButton.setSelected(false);
			if (ignoreList==this) ignoreList=null;
		}
  }

	private void setProduceList(boolean on) {
		if (on){
			produceButton.setSelected(true);
			setDegradeList(false);
			setIgnoreList(false);
			if (produceList!=null && produceList!=this) produceList.setProduceList(false);
			produceList=this;
		} else {
			produceButton.setSelected(false);
			if (produceList==this) produceList=null;
		}
  }

	public static SubstanceList getProduceList() {
	  return produceList;
  }

	public static SubstanceList getDegradeList() {
	  return degradeList;
  }

	public static SubstanceList getIgnoreList() {
	  return ignoreList;
  }
}
