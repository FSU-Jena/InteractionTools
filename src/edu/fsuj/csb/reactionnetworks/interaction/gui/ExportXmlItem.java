package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.zip.DataFormatException;

import javax.swing.JMenuItem;

import edu.fsuj.csb.reactionnetworks.interaction.SeedOptimizationMappingNode;
import edu.fsuj.csb.tools.organisms.gui.CompartmentNode;
import edu.fsuj.csb.tools.xml.XmlObject;

public class ExportXmlItem extends JMenuItem {
	
  private static final long serialVersionUID = 2599180170787100265L;
	private XmlObject xmlobject;
	
	public ExportXmlItem(XmlObject targetObject) throws SQLException, DataFormatException, IOException {
		super("export XML");
		xmlobject=targetObject;
		if (targetObject instanceof SeedOptimizationMappingNode || targetObject instanceof CompartmentNode) setText("export SBML");
  }

	public void export(URL url) throws URISyntaxException, IOException {
		if (url==null) return;
		File outputFile=new File(url.toString().replace("file:", ""));
		BufferedWriter br=new BufferedWriter(new FileWriter(outputFile));
		StringBuffer sb=new StringBuffer();
		xmlobject.getCode(sb);
		br.write(sb.toString());
		br.close();
  }
}
