package edu.fsuj.csb.reactionnetworks.interaction;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

import edu.fsuj.csb.tools.xml.ObjectComparator;

public class ObjectListModel implements ListModel<Object> {
	TreeSet<ListDataListener> listeners;
	TreeSet<Object> entries;

	public ObjectListModel() {
		listeners = new TreeSet<ListDataListener>(new ObjectComparator());
		entries = new TreeSet<Object>(new ObjectComparator());
	}

	public void addListDataListener(ListDataListener arg0) {
		listeners.add(arg0);
	}

	public synchronized Object getElementAt(int arg0) {
		Object[] dummy = entries.toArray();
		if (arg0>=dummy.length) return null;
		return dummy[arg0];
	}

	public synchronized Iterator<Object> elements() {
		return entries.iterator();
	}

	public synchronized int getSize() {
		return entries.size();
	}

	public void removeListDataListener(ListDataListener arg0) {
		listeners.remove(arg0);
	}

	void change() {
		try {
	    Thread.sleep(1);
    } catch (InterruptedException e) {
	    e.printStackTrace();
    }
		for (Iterator<ListDataListener> it = listeners.iterator(); it.hasNext();) it.next().contentsChanged(null);
	}
	
	public void addAll(Collection<? extends Object> elements){
		synchronized (this) {
		entries.addAll(elements);
		}
		change();
	}

	public void addElement(Object element) {
		synchronized (this) {
			if (!entries.contains(element)) entries.add(element);
		}
		change();
	}

	public void removeElement(Object toRemove) {
		synchronized (this) {
			if (toRemove!=null && entries.contains(toRemove)) entries.remove(toRemove);
		}
		change();
	}

	public synchronized boolean isEmpty() {
		return entries.isEmpty();
	}
}
