package edu.fsuj.csb.reactionnetworks.interaction.gui;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;

	public class urlPopupListener implements ActionListener {
		URL u;

		public urlPopupListener(URL url) {
			u = url;
		}

		public void actionPerformed(ActionEvent e) {
			try {
				Runtime.getRuntime().exec("gnome-open " + u);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}