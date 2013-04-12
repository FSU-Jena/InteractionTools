package edu.fsuj.csb.reactionnetworks.interaction;
import java.awt.Dimension;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

import javax.swing.JList;
import javax.swing.JScrollPane;

import edu.fsuj.csb.gui.VerticalPanel;
import edu.fsuj.csb.tools.xml.ObjectComparator;


public class ObjectList extends VerticalPanel implements Serializable{

	/**
   * 
   */
  private static final long serialVersionUID = -4964623372264127718L;
	private ObjectListModel model;
	@SuppressWarnings("rawtypes")
	private JList list;
	private JScrollPane scrollpane;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ObjectList(int width, int height) {
		super();
		model=new ObjectListModel();
		list=new JList(model);
		scrollpane=new JScrollPane(list);
		scrollpane.setPreferredSize(new Dimension(width,height));
		add(scrollpane);
		scale();		
  }

	public TreeSet<Object> getSelectedElements() {
		TreeSet<Object> result = new TreeSet<Object>(ObjectComparator.get());
		@SuppressWarnings("deprecation")
		Object[] dummy=list.getSelectedValues();
		for (int i=0; i<dummy.length; i++) result.add(dummy[i]);
	  return result; 
  }

	public void addAll(Collection<? extends Object> elements) {
		model.addAll(elements);
  }

	public void removeAll(TreeSet<? extends Object> elements) {
	  for (Iterator<? extends Object> it = elements.iterator();it.hasNext();) model.removeElement(it.next());	  
  }

	public void add(Object name) {
		model.addElement(name);		
  }

	public void remove(Object o) {
		model.removeElement(o);
  }
	
	public void deselect() {
		list.clearSelection();
  }
}
