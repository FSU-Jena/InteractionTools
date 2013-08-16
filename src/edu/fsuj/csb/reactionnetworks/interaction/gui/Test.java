package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Dimension;

import javax.swing.JFrame;

import edu.fsuj.csb.tools.xml.Tools;

public class Test extends JFrame {
	
	public Test() {
		setPreferredSize(new Dimension(1024,768));
		setSize(getPreferredSize());
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		add(new MetabolicNetworkPanel());
		setVisible(true);
  }
	public static void main(String[] args) {
		Tools.enableLogging();
	  new Test();
  }
}
