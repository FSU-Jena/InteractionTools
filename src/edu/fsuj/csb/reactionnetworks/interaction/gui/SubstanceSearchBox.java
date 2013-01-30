package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.TreeSet;

import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;

import edu.fsuj.csb.reactionnetworks.database.InteractionDB;
import edu.fsuj.csb.reactionnetworks.interaction.SubstanceList;
import edu.fsuj.csb.reactionnetworks.organismtools.DbSubstance;
import edu.fsuj.csb.tools.xml.ObjectComparator;

public class SubstanceSearchBox extends JComboBox implements KeyListener {

  private static final long serialVersionUID = 5837689672708421760L;
  private JTextComponent editor;  
  private SubstanceList list;

	public SubstanceSearchBox(SubstanceList substanceList) {
		super();
		addItem("Search for substances");
	  setEditable(true);
	  editor=(JTextComponent) this.getEditor().getEditorComponent();
	  editor.addKeyListener(this);
	  this.addActionListener(this);
	  list=substanceList;
  }

	public void keyPressed(KeyEvent arg0) {  }

	public void keyReleased(KeyEvent arg0) {  }

	public void keyTyped(KeyEvent arg0) {
    try {
  		String t=editor.getText();
  		if (t.length()<3) return;
  		
  		String query="SELECT DISTINCT name FROM names NATURAL JOIN id_names NATURAL JOIN ids WHERE type="+InteractionDB.SUBSTANCE+" AND name LIKE '%"+t+"%'";
  		Statement st;
	    st = InteractionDB.createStatement();
			ResultSet rs=st.executeQuery(query);
			TreeSet<String> substanceNames=new TreeSet<String>(ObjectComparator.get());
			while (rs.next())	substanceNames.add(rs.getString(1));
			
		  this.removeAllItems();
		  this.addItem("");
		  for (Iterator<String> it = substanceNames.iterator();it.hasNext();) this.addItem(it.next());
		  editor.setText(t);	  
		  rs.close();
		  st.close();
    } catch (SQLException e) {
	    e.printStackTrace();
    } catch (IOException e) {
	    e.printStackTrace();
    }
  }
	
	@Override
	public void actionPerformed(ActionEvent event) {
		super.actionPerformed(event);
		Object item = this.getSelectedItem();
		if (item instanceof String){
			String s=(String)item;
			if (s.length()>0) try {
	      addSubstancesToList(s);
      } catch (SQLException e1) {
	      e1.printStackTrace();
      } catch (IOException e) {
	      e.printStackTrace();
      }
		}
	}

	private void addSubstancesToList(String s) throws SQLException, IOException {
		//System.err.println("SubstanceSearchBox.addSubstancesToLost("+s+")");
		String query="SELECT DISTINCT id FROM names NATURAL JOIN id_names NATURAL JOIN ids WHERE type="+InteractionDB.SUBSTANCE+" AND name='"+s.replace("'", "\\'")+"'";
		Statement st;
		st = InteractionDB.createStatement();
		ResultSet rs=st.executeQuery(query);		
		while (rs.next())	list.addSubstance(DbSubstance.load(rs.getInt(1)));		
		rs.close();
		st.close();
  }
}
