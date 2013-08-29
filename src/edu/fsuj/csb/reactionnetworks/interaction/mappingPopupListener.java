package edu.fsuj.csb.reactionnetworks.interaction;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;

import edu.fsuj.csb.gui.PanelTools;
import edu.fsuj.csb.tools.xml.XMLWriter;

public class mappingPopupListener implements ActionListener {

	
	private SeedOptimizationMappingNode mapping;

	public mappingPopupListener(SeedOptimizationMappingNode mapping) {
	  this.mapping=mapping;
  }

	public void actionPerformed(ActionEvent arg0) {
		try {
	    exportMapping(askForFileName());
    } catch (IOException e) {
	    e.printStackTrace();
    } catch (URISyntaxException e) {
	    e.printStackTrace();
    } catch (SQLException e) {
	    e.printStackTrace();
    }
	}

	public static URL askForFileName(String title) {
	  return PanelTools.showSelectFileDialog(title, null, null, null);
	}
	public static URL askForFileName() {
		return askForFileName("File prefix");
  }

	private void exportMapping(URL url) throws IOException, URISyntaxException, SQLException {
		if (url==null) return;
		String filename=url.toString().replace("file:", "");
		XMLWriter writer=new XMLWriter(filename+".sbml");
		writer.write(mapping);
		writer.close();
		mapping.writeDotFile(filename+".dot");
  }
}
