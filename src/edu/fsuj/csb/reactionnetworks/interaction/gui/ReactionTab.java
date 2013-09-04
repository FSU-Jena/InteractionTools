package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import edu.fsuj.csb.gui.HorizontalPanel;
import edu.fsuj.csb.gui.IntegerInputField;
import edu.fsuj.csb.gui.VerticalPanel;
import edu.fsuj.csb.reactionnetworks.database.InteractionDB;
import edu.fsuj.csb.reactionnetworks.interaction.ReactionSelectionListener;
import edu.fsuj.csb.reactionnetworks.interaction.SubstanceList;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.ParameterSet;
import edu.fsuj.csb.reactionnetworks.organismtools.DbReaction;
import edu.fsuj.csb.reactionnetworks.organismtools.DbSubstance;
import edu.fsuj.csb.tools.organisms.ReactionSet;
import edu.fsuj.csb.tools.xml.ObjectComparator;
import edu.fsuj.csb.tools.xml.Tools;

public class ReactionTab extends VerticalPanel implements ReactionSelectionListener, ActionListener {
	private VerticalPanel settingsPanel;
	private VerticalPanel buttonPanel;
	private JTextField nameField;
	private JButton saveButton;
	private JButton alterButton;
	private JButton removeButton;
	private ReactionListPanel fittingReactionList;
	private VerticalPanel substanceStoichList;
	private JCheckBox spontanBox;
	private static final Dimension initialSize = new Dimension(400, 300);
	private TreeMap<IntegerInputField, Integer> mapToSubstanceId;
	private TreeSet<Integer> rids = new TreeSet<Integer>();
	private ReactionListPanel selectedReactionList;
	private JButton addButton;
	private int lastSelected;

	public ReactionTab() {
		createComponents();
	}

	private void createComponents() {
		fittingReactionList = new ReactionListPanel("reactions fitting your substances");
		fittingReactionList.addReactionSelectionListener(this);

		selectedReactionList = new ReactionListPanel("reactions added to model");

		HorizontalPanel rListPanel = new HorizontalPanel("Reaction Lists");
		rListPanel.add(fittingReactionList);
		rListPanel.add(selectedReactionList);

		HorizontalPanel rEditPanel = new HorizontalPanel("Reaction Editing");
		rEditPanel.add(settingsPanel = createSettingsPanel());
		rEditPanel.add(buttonPanel = createButtonPanel());

		add(rListPanel);
		add(rEditPanel);

		scale();

	}

	private VerticalPanel createButtonPanel() {
		VerticalPanel result = new VerticalPanel();
		result.add(saveButton = new JButton("Save New"));
		saveButton.addActionListener(this);
		result.add(addButton = new JButton("Add to Model"));
		addButton.addActionListener(this);
		result.add(alterButton = new JButton("Alter in DB"));
		result.add(removeButton = new JButton("Remove"));
		result.add(spontanBox = new JCheckBox("spontaneous"));
		removeButton.setEnabled(false);
		return result;
	}

	private VerticalPanel createSettingsPanel() {

		VerticalPanel result = new VerticalPanel();
		result.add(nameField = new JTextField("name of new reaction"));
		result.add(createStoichList());
		return result;
	}

	private JScrollPane createStoichList() {
		substanceStoichList = new VerticalPanel();

		for (int i = 0; i < 5; i++) {
			substanceStoichList.add(new IntegerInputField("Substance" + i));
		}
		substanceStoichList.scale();
		JScrollPane result = new JScrollPane(substanceStoichList);
		result.setPreferredSize(initialSize);
		return result;
	}

	public void addActionListener(ActionListener l) {}

	public void edit(SubstanceList source) throws SQLException, IOException {
		rids = InteractionDB.getReactionsFor(source.getListed());
		fittingReactionList.setReactions(rids);
	}

	public void changed(int rid) {
		lastSelected=rid;
		substanceStoichList.removeAll();
		mapToSubstanceId = new TreeMap<IntegerInputField, Integer>(ObjectComparator.get());
		try {
			DbReaction r = DbReaction.load(rid);
			spontanBox.setSelected(r.isSpontan());
			nameField.setText(r.names().first());
			for (Entry<Integer, Integer> sid : r.substrates().entrySet()) {
				DbSubstance s = DbSubstance.load(sid.getKey());
				IntegerInputField iif = new IntegerInputField(s.names().first(), Integer.MIN_VALUE);
				iif.setzewert(-sid.getValue());
				mapToSubstanceId.put(iif, s.id());
				substanceStoichList.add(iif);
			}
			for (Entry<Integer, Integer> sid : r.products().entrySet()) {
				DbSubstance s = DbSubstance.load(sid.getKey());
				IntegerInputField iif = new IntegerInputField(s.names().first(), Integer.MIN_VALUE);
				iif.setzewert(sid.getValue());
				mapToSubstanceId.put(iif, s.id());
				substanceStoichList.add(iif);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		substanceStoichList.scale();
	}

	private int addReactionToDatabase() throws SQLException, NoSuchMethodException, IOException {
		int newRid = InteractionDB.createReaction(nameField.getText(), null, spontanBox.isSelected(), new URL("http://interactiontools"));
		for (Entry<IntegerInputField, Integer> entry : mapToSubstanceId.entrySet()) {
			IntegerInputField iif = entry.getKey();
			if (iif.wert() < 0) InteractionDB.addSubstrateToReaction(newRid, entry.getValue(), iif.wert());
			if (iif.wert() > 0) InteractionDB.addProductToReaction(newRid, entry.getValue(), iif.wert());
		}
		rids.add(newRid);
		fittingReactionList.setReactions(rids);
		System.out.println("New reaction with id " + newRid + " created in database");
		return newRid;
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		try {
			if (source == saveButton) {
				int newRid = addReactionToDatabase();
				fittingReactionList.addReaction(newRid);
			}
			if (source == addButton) {
				selectedReactionList.addReaction(lastSelected);
			}

		} catch (SQLException e1) {
			e1.printStackTrace();
		} catch (NoSuchMethodException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

	}

	public ReactionSet selectedReactions() {		
	  return selectedReactionList.getListed();
  }

}
