package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Dimension;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import edu.fsuj.csb.gui.HorizontalPanel;
import edu.fsuj.csb.gui.IntegerInputField;
import edu.fsuj.csb.gui.VerticalPanel;

public class ReactionTab extends VerticalPanel {
	private VerticalPanel settingsPanel;
	private VerticalPanel buttonPanel;
	private JTextField nameField;
	private JButton addButton;
	private JButton alterButton;
	private JButton removeButton;
	private static final Dimension initialSize=new Dimension(600, 200);


	public ReactionTab() {
		createComponents();
  }

	private void createComponents() {
	  ReactionListPanel reactionList=new ReactionListPanel("reactions fitting your substances");
	  
	  HorizontalPanel rEditPanel=new HorizontalPanel("Reaction Editing");
	  
	  rEditPanel.add(settingsPanel=createSettingsPanel());
	  rEditPanel.add(buttonPanel=createButtonPanel());
	  
	  add(reactionList);
	  add(rEditPanel);
	  
	  scale();
	  
  }

	private VerticalPanel createButtonPanel() {
	  VerticalPanel result=new VerticalPanel();
	  result.add(addButton=new JButton("Save & Add"));
	  result.add(alterButton=new JButton("Alter in DB"));
	  result.add(removeButton=new JButton("Remove"));
	  removeButton.setEnabled(false);
	  return result;
  }

	private VerticalPanel createSettingsPanel() {
		
	  VerticalPanel result=new VerticalPanel();
	  result.add(nameField=new JTextField("name of new reaction"));
	  result.add(createStoichList());
		return result;
  }

	private JScrollPane createStoichList() {
		VerticalPanel list=new VerticalPanel();
		
		for (int i=0; i<5; i++){
			list.add(new IntegerInputField("Substance"+i));			
		}
		list.scale();
	  JScrollPane result=new JScrollPane(list);
	  result.setPreferredSize(initialSize);
		return result;
  }

	public void addActionListener(ActionListener l) {
	  // TODO Auto-generated method stub
	  
  }
}
