package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.border.LineBorder;

import edu.fsuj.csb.gui.Splashscreen;
import edu.fsuj.csb.gui.StatusPanel;
import edu.fsuj.csb.gui.VerticalPanel;

public class InteractionSplash extends Splashscreen {
  private static final long serialVersionUID = 6579649477650541147L;
	private StatusPanel feed;
	private VerticalPanel panel;

	public InteractionSplash() {
		super();
		panel = new VerticalPanel();
		panel.add(new JLabel("<html><font size=\"6\">InteractionToolbox starting up..."));
		panel.add(feed=new StatusPanel(800,200));
		panel.skalieren();
		panel.setBorder(new LineBorder(Color.black,5));
		add(panel);
	}
	
	public void run() {
	  super.run();
	  setSize(panel.getPreferredSize());
		setLocationRelativeTo(null);

	}
	
	public void log(String message){
		feed.log(message);
	}

	public void stop(int i) {
		try {
	    Thread.sleep(i*1000);
    } catch (InterruptedException e) {
	    e.printStackTrace();
    }
    super.stop();
  }
}
