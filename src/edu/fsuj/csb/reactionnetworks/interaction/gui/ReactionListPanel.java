package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Dimension;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.gui.HorizontalPanel;
import edu.fsuj.csb.reactionnetworks.interaction.ReactionSelectionListener;
import edu.fsuj.csb.reactionnetworks.organismtools.DbReaction;
import edu.fsuj.csb.tools.organisms.gui.CompartmentNode;
import edu.fsuj.csb.tools.organisms.gui.ReactionNode;
import edu.fsuj.csb.tools.organisms.gui.SubstanceNode;
import edu.fsuj.csb.tools.xml.ObjectComparator;

public class ReactionListPanel extends HorizontalPanel implements TreeSelectionListener {

	private static final Dimension initialSize=new Dimension(600, 300);
	private ResultTreeRoot rTreeRoot;
	private JTree tree;
	private JScrollPane scrollpane;
	private TreeSet<ReactionSelectionListener> listeners;


	public ReactionListPanel(String string) {
	  super(string);
	  
	  rTreeRoot = new ResultTreeRoot(this,"Reactions");
	  tree=new JTree(rTreeRoot);
	  tree.addTreeSelectionListener(this);
	  scrollpane=new JScrollPane(tree);
	  scrollpane.setPreferredSize(initialSize);
		add(scrollpane);

  }


	public void setReactions(TreeSet<Integer> rids) throws SQLException {
		rTreeRoot.removeAllChildren();
		for (Integer rid:rids) rTreeRoot.addWithoutUpdate(new ReactionNode(DbReaction.load(rid)));
		rTreeRoot.update();
  }


	public void valueChanged(TreeSelectionEvent arg0) {
		Object lastSelected = tree.getLastSelectedPathComponent();
		try{
			if (lastSelected instanceof ReactionNode) {
				ReactionNode rn = ((ReactionNode) lastSelected);
				rn.loadDetails();
				if (listeners!=null){
					for (ReactionSelectionListener l:listeners) l.changed(rn.reactionId());
				}
			}
			rTreeRoot.update();
		} catch (DataFormatException e) {
	    e.printStackTrace();
    } catch (MalformedURLException e) {
	    e.printStackTrace();
    } catch (SQLException e) {
	    e.printStackTrace();
    }
	}


	public void addReactionSelectionListener(ReactionSelectionListener rsl) {
		if (listeners==null) listeners=new TreeSet<ReactionSelectionListener>(ObjectComparator.get());
		listeners.add(rsl);
  }

}
