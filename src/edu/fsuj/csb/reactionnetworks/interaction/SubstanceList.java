package edu.fsuj.csb.reactionnetworks.interaction;

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
import edu.fsuj.csb.reactionnetworks.organismtools.DbSubstance;
import edu.fsuj.csb.tools.organisms.Substance;
import edu.fsuj.csb.tools.organisms.gui.SortedTreeNode;
import edu.fsuj.csb.tools.organisms.gui.SubstanceNode;
import edu.fsuj.csb.tools.xml.ObjectComparator;
import edu.fsuj.csb.tools.xml.XmlObject;
import edu.fsuj.csb.tools.xml.XmlToken;

public class SubstanceList extends VerticalPanel implements ChangeListener,TreeSelectionListener, MouseListener, ActionListener,XmlObject {

	private static final long serialVersionUID = 10;
	private SortedTreeNode root;
	private JTree substancesTree;
	private TreeSet<ChangeListener> changeListeners;
	private TreeSet<ActionListener> actionListeners;
	private String name;
	private JCheckBox degradeButton;
	private JCheckBox produceButton;
	private JCheckBox ignoreButton;
	private JCheckBox noOutflowButton;
	private JScrollPane scp;
	private HorizontalPanel buttonPane;
	private static SubstanceList degradeList;
	private static SubstanceList produceList;
	private static SubstanceList ignoreList;
	private static SubstanceList noOutflowList;
	private static final Dimension initialSize=new Dimension(150, 300);
	private static final int DEGRADE = -1;
	private static final int PRODUCE = 1;
	private static final int IGNORE = 0;
	private static final int NOOUTFLOW = -2;

	public SubstanceList(String name,boolean showBoxes) {
		this.name=name;
		root = new SortedTreeNode("Substances");
		root.addChangeListener(this);
		substancesTree = new JTree(new DefaultTreeModel(root));
		substancesTree.addTreeSelectionListener(this);
		substancesTree.addMouseListener(this);
		scp = new JScrollPane(substancesTree);
		scp.setPreferredSize(initialSize);
		scp.setSize(scp.getPreferredSize());
		if (showBoxes){
			buttonPane=new HorizontalPanel();
			buttonPane.add(degradeButton=new JCheckBox("<html>substances&nbsp;&nbsp;&nbsp;&nbsp;<br>to degrade"));
			degradeButton.addActionListener(this);
			buttonPane.add(produceButton=new JCheckBox("<html>substances&nbsp;&nbsp;&nbsp;&nbsp;<br>to produce"));
			produceButton.addActionListener(this);
			buttonPane.add(ignoreButton=new JCheckBox("<html>substances&nbsp;&nbsp;&nbsp;&nbsp;<br>to ignore"));
			ignoreButton.addActionListener(this);
			buttonPane.add(noOutflowButton=new JCheckBox("<html>forbid<br/>outflow"));
			noOutflowButton.addActionListener(this);
			buttonPane.scale();
			add(buttonPane);
			if (degradeList==null) {
				setDegradeList(true); 
			} else if (produceList==null){
				setProduceList(true);
			} else if (ignoreList==null){
				setIgnoreList(true);
			} else if (noOutflowList==null){
				setNoOutflowList(true);
			}
		}
		add(scp);
		scale();
		
	}
	
	public void addSubstance(Substance s) throws SQLException {
		addSubstanceSilently(s);
		fireEvent(new ActionEvent(this, 0, "activate"));
	}
	
	public void addSubstanceSilently(Substance s) throws SQLException {
		//System.err.println("SubstanceList.addSubstance("+s+")");
		int sid=s.id();
		for (String name:s.names()) {
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
			addSubstanceSilently(it.next());
		}
		fireEvent(new ActionEvent(this, 0, "activate"));
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
	  if (e.getClickCount()>=2)	fireEvent(new ActionEvent(this, 2, "Click"));
		if (e.getButton()==MouseEvent.BUTTON3) showPopupForComponentAt(e.getPoint());
  }
	
	private void showPopupForComponentAt(Point pos1) {
    TreePath path = substancesTree.getPathForLocation(pos1.x,pos1.y);
		if (path == null) return;
		try {
	    PopupMenu.showPopupFor(path.getLastPathComponent(),pos1,substancesTree);
    } catch (SQLException e) {
	    e.printStackTrace();
    } catch (DataFormatException e) {
	    e.printStackTrace();
    } catch (IOException e) {
	    e.printStackTrace();
    }
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
		if (source==noOutflowButton) setNoOutflowList(noOutflowButton.isSelected());
  }
	
	private void setNoOutflowList(boolean on) {
		if (on){
			noOutflowButton.setSelected(true);
			setDegradeList(false);
			setProduceList(false);
			setIgnoreList(false);
			if (noOutflowList!=null && noOutflowList!=this) noOutflowList.setNoOutflowList(false);
			noOutflowList=this;
		} else {
			noOutflowButton.setSelected(false);
			if (noOutflowList==this) noOutflowList=null;
		}
  }

	
	private void setDegradeList(boolean on) {
		if (on){
			degradeButton.setSelected(true);
			setProduceList(false);
			setIgnoreList(false);
			setNoOutflowList(false);
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
			setNoOutflowList(false);

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
			setNoOutflowList(false);
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
	
	public void scaleScrollPane(Dimension d){
		scp.setPreferredSize(d);
	}
	
	public StringBuffer getCode() {
		TreeSet<Integer> listed = getListed();
		if (listed==null||listed.isEmpty()) return new StringBuffer();
		XmlToken result=new XmlToken(name);

		if (this==degradeList) result.setValue("role",DEGRADE);
		if (this==produceList) result.setValue("role",PRODUCE);
		if (this==ignoreList) result.setValue("role",IGNORE);
		if (this==noOutflowList) result.setValue("role",NOOUTFLOW);
		// TODO: activate if (this==noInflowList) result.setValue("role",NOINFLOW);
		result.setContent(listed);
		return result.getCode();
  }

	public static SubstanceList getNoOutflowList() {
	  return noOutflowList;
  }

	public void loadState(XmlToken token) throws SQLException {
		if (!token.tokenClass().equals(name.replace(" ", ""))) return;
		String content = token.content();
		
		int role = token.getIntValue("role");
		switch (role){
		case DEGRADE: setDegradeList(true); break;
		case PRODUCE: setProduceList(true); break;
		case IGNORE: setIgnoreList(true); break;
		case NOOUTFLOW: setNoOutflowList(true); break;
		}
		if (content==null || content.length()==0) return;
		String[] idStrings = content.replace(" ", "").split(",");
		for (String idString:idStrings){
			int id=Integer.parseInt(idString);
			Substance s=DbSubstance.load(id);
			addSubstanceSilently(s);
		}
  }
}
