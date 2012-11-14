package edu.fsuj.csb.reactionnetworks.databasefiller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import javax.naming.directory.NoSuchAttributeException;

import edu.fsuj.csb.reactionnetworks.database.InteractionDB;
import edu.fsuj.csb.reactionnetworks.organismtools.DbCompartment;
import edu.fsuj.csb.tools.urn.miriam.BiomodelUrn;
import edu.fsuj.csb.tools.xml.CachedXMLReader;
import edu.fsuj.csb.tools.xml.NoTokenException;
import edu.fsuj.csb.tools.xml.ObjectComparator;
import edu.fsuj.csb.tools.xml.Tools;
import edu.fsuj.csb.tools.xml.XMLReader;
import edu.fsuj.csb.tools.xml.XmlToken;

public class Biomodels {
	
	public static void parse() throws IOException, NoTokenException, SQLException, NoSuchAlgorithmException, DataFormatException, NoSuchMethodException, NoSuchAttributeException {
		int bioModelsGroup=InteractionDB.getOrCreateGroup("Biomodels");

		TreeSet<BiomodelUrn> modelUrns = urns();
		int count=0;
		int number=modelUrns.size();
		
		for (Iterator<BiomodelUrn> it = modelUrns.iterator();it.hasNext();) {
			BiomodelUrn urn=it.next();
			String model=urn.code();
			count++;
			System.out.print(((count)*100/number)+"% - ");
		  	
		  Integer superCompartmentId=InteractionDB.createCompartment(model,urn, bioModelsGroup,urn.downloadUrl());
		  DbCompartment superCompartment = DbCompartment.load(superCompartmentId);
		  	
		  System.out.println("parsing "+urn.downloadUrl());
		  try {
		  	SBMLLoader.loadSBMLFile(urn,bioModelsGroup,model,superCompartment);
		  } catch (DataFormatException e){
		  	System.err.println("error thrown on "+count+"th model.");
		  	throw e;
		  }
//		  	if (supercompartments.size()>1)	throw new IndexOutOfBoundsException(model+" seems to contain too many super compartments!");
//		  	if (supercompartments.size()<1) throw new IndexOutOfBoundsException(model+" contains no compartments!");
//		  	InteractionDB.insertUrn(supercompartments.first(), urn);
		 }
//		}
		System.out.println("Finished importing Biomodels.");
		InteractionDB.setDateMark("Read Biomodels");

	}

	private static TreeSet<String> parseContentTable(XmlToken table) throws MalformedURLException {
		for (Iterator<XmlToken> it = table.subtokenIterator();it.hasNext();){
			XmlToken token = it.next();
			if (token.instanceOf("tbody")) return parseContentTableBody(token);
		}
		return null;	  
  }
	
	private static TreeSet<String> parseContentTableBody(XmlToken table) throws MalformedURLException {
		TreeSet<String> models = Tools.StringSet();
		for (Iterator<XmlToken> it = table.subtokenIterator();it.hasNext();){
			XmlToken token = it.next();
			if (token.instanceOf("tr")) {
				models.add(parseContentTableRow(token));
			}
		}	 
		return models;
  }
	private static String parseContentTableRow(XmlToken row) throws MalformedURLException {
		return parseLink(row.subtokenIterator().next());
  }

	private static String parseLink(XmlToken link) throws MalformedURLException {
		for (Iterator<XmlToken> it = link.subtokenIterator();it.hasNext();){
			XmlToken token = it.next();
			if (token.instanceOf("a")) return token.getValue("href");
		}
		return null;
  }

	public static TreeSet<BiomodelUrn> urns() throws IOException, NoTokenException, DataFormatException {
		URL url = new URL("http://www.ebi.ac.uk/biomodels-main/publ-models-plain.do?cmd=SET:COUNT&cou=3700");
		//System.out.println("aquiring model list from "+url+".");
		CachedXMLReader xmlr=new CachedXMLReader(url);
		XmlToken token=xmlr.readToken();
		while (token.getValue("class")==null) {
			token=xmlr.readToken();
		}
		TreeSet<String> models=null;
		if (token.getValue("class").equals("content_bordered")) models=parseContentTable(token);
		TreeSet<BiomodelUrn> result=new TreeSet<BiomodelUrn>(ObjectComparator.get());
		for (Iterator<String> it = models.iterator();it.hasNext();) {
			String model=it.next();			
			result.add(new BiomodelUrn(model));
		}
		System.out.println("found "+result.size()+" biomodels.");
		return result;
	}
}
