package edu.fsuj.csb.reactionnetworks.interaction.gui;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class ResultTreeRoot extends DefaultMutableTreeNode {
  private static final long serialVersionUID = -7735797332731779635L;
  private JComponent ownerPanel;
  
	public ResultTreeRoot(JComponent owner, String title) {
		super(title);
		ownerPanel=owner;
	}
	
	private class upThread implements Runnable{
		public void run() {
			SwingUtilities.updateComponentTreeUI(ownerPanel);			
		}
	}
	
	public synchronized void update(){
		SwingUtilities.invokeLater(new upThread());
	}
	
	public void removeAllChildren() {
	  super.removeAllChildren();
	  update();
	}

	public void add(MutableTreeNode newChild) {
	  super.add(newChild);
	  update();
	}
	
	public void addWithoutUpdate(MutableTreeNode newChild){
		super.add(newChild);
	}
}
