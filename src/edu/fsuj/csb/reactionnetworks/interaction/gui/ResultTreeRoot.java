package edu.fsuj.csb.reactionnetworks.interaction.gui;

import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class ResultTreeRoot extends DefaultMutableTreeNode {
  private static final long serialVersionUID = -7735797332731779635L;
  private ResultPanel ownerPanel;
	public ResultTreeRoot(ResultPanel resultPanel) {
		super("Results");
		ownerPanel=resultPanel;
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
		System.out.println("ResultTreeRoot.removeAllChildren()");
	  super.removeAllChildren();
	  update();
	}

	public void add(MutableTreeNode newChild) {
		System.out.println("ResultTreeRoot.add(...)");
	  super.add(newChild);
	  update();
	}
}
