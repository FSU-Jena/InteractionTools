package edu.fsuj.csb.reactionnetworks.databasefiller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.zip.DataFormatException;

import javax.naming.directory.NoSuchAttributeException;

import edu.fsuj.csb.reactionnetworks.database.InteractionDB;
import edu.fsuj.csb.reactionnetworks.organismtools.DbCompartment;
import edu.fsuj.csb.stringComparison.MD5Hash;
import edu.fsuj.csb.tools.urn.FileUrn;
import edu.fsuj.csb.tools.urn.URLURN;
import edu.fsuj.csb.tools.urn.URN;
import edu.fsuj.csb.tools.urn.miriam.BiomodelUrn;
import edu.fsuj.csb.tools.urn.miriam.CasUrn;
import edu.fsuj.csb.tools.urn.miriam.ChEBIUrn;
import edu.fsuj.csb.tools.urn.miriam.KeggCompoundUrn;
import edu.fsuj.csb.tools.urn.miriam.MiriamUrn;
import edu.fsuj.csb.tools.urn.miriam.PubChemSubstanceUrn;
import edu.fsuj.csb.tools.xml.NoTokenException;
import edu.fsuj.csb.tools.xml.ObjectComparator;
import edu.fsuj.csb.tools.xml.Tools;
import edu.fsuj.csb.tools.xml.XMLReader;
import edu.fsuj.csb.tools.xml.XmlToken;

public class SBMLLoader {

	/**
	 * reads an sbml file and saves it's contents into the database. the compartment of the sbml will be added to the indicated compartment group
	 * @param fileUrl the url of the file to be parsed
	 * @param groupId the number of the compartment group, to which the models compartment will be added
	 * @param modelName the name of the model. will be used for grouping
	 * @throws IOException
	 * @throws NoTokenException
	 * @throws NoSuchAlgorithmException
	 * @throws SQLException
	 * @throws NoSuchMethodException
	 * @throws DataFormatException
	 * @throws NoSuchAttributeException
	 */
	public static void loadSBMLFile(URL fileUrl, int groupId, String modelName) throws IOException, NoTokenException, NoSuchAlgorithmException, SQLException, NoSuchMethodException, DataFormatException, NoSuchAttributeException {
		if (fileUrl == null) return;
		XMLReader xmlr = new XMLReader(fileUrl);
		XmlToken token = xmlr.readToken();
		xmlr.close();
		MD5Hash hash = new MD5Hash(token);
		
		URN urn = new FileUrn(hash.toString());
		if (InteractionDB.readIdFor(urn) != null) {
			System.out.println("This model is already included in the database!");
			return;
		}

  	Integer superCompartmentId=InteractionDB.createCompartment(modelName, urn, groupId, fileUrl);
		
  	//Integer superCompartmentId=InteractionDB.getOrCreateCompartment(urn, groupId,urn);
  	//if (modelName!=null)InteractionDB.insertName(superCompartmentId, modelName);
  	
  	DbCompartment superCompartment = DbCompartment.load(superCompartmentId);
		xmlr = new XMLReader(fileUrl);
  	loadSBMLFile(xmlr,groupId,superCompartment,fileUrl);
		
		xmlr.close();
	}

	public static void loadSBMLFile(URN urn, int groupId, String modelName, DbCompartment superCompartment) throws NoSuchAlgorithmException, MalformedURLException, IOException, NoTokenException, SQLException, NoSuchMethodException, DataFormatException, NoSuchAttributeException {
		URL url=null;
		if (urn instanceof BiomodelUrn) url = ((BiomodelUrn) urn).downloadUrl();
		if (urn instanceof URLURN) url=((URLURN) urn).url();
		if (url==null)throw new DataFormatException("URN ("+urn+") of unknown type");
		loadSBMLFile(new XMLReader(url), groupId, superCompartment,url);		
	}

	/**
	 * reads an sbml file and saves it's contents into the database. the compartment of the sbml will be added to the indicated compartment group
	 * 
	 * @param xmlr the xml file reader as handle to the source code
	 * @param groupId the number of the compartment group, to which the models compartment will be added
	 * @param superCompartment the compartment, to which all compartments in the model will be added
	 * @param source 
	 * @throws IOException
	 * @throws NoTokenException
	 * @throws NoSuchAlgorithmException
	 * @throws SQLException
	 * @throws DataFormatException
	 * @throws NoSuchAttributeException 
	 * @throws NoSuchMethodException
	 */
	public static void loadSBMLFile(XMLReader xmlr, int groupId, DbCompartment superCompartment, URL source) throws NoSuchAlgorithmException, IOException, NoTokenException, SQLException, DataFormatException {
		String query = "SELECT max(id) FROM ids";
		ResultSet rs = InteractionDB.createStatement().executeQuery(query);
		rs.next();
		int maxDBid = rs.getInt(1);
		rs.close();
		XmlToken token = xmlr.readToken();
		MD5Hash hash = new MD5Hash(token);

		// in the following two models, the species ids match kegg substance ids. thus, with the following code, the referenced substance kegg ids will be added to the annotoation
		if (superCompartment.names().contains("journal.pcbi.1000799.s003.xml")) addUrisToToken(token);
		if (superCompartment.names().contains("1752-0509-5-116-s4.xml")) addUrisToToken(token);
		
		
		if (!token.tokenClass().equals("sbml")) {
			System.out.println("This does not seem to be an sbml file :(");
			return;
		}

		try {
			writeSbmlIntoDatabase(token, hash, groupId, superCompartment,source);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println();
			try {
	      dbtool.cleanDb(maxDBid-1); // -1 since the id for the sbml model is added before this code 
      } catch (SQLException e1) {   }
			System.exit(-1);
		}
	}

	private static void addUrisToToken(XmlToken token) throws NoTokenException, DataFormatException {
		for (Iterator<XmlToken> it = token.subtokenIterator(); it.hasNext();){
			token=it.next();
			if (token.instanceOf("model") || token.instanceOf("listOfSpecies")) {
				addUrisToToken(token);
			} else if (token.instanceOf("species")){
				addUrisToSpecies(token);
			} else if (token.instanceOf("listOfCompartments") || token.instanceOf("listOfReactions") || token.instanceOf("listOfUnitDefinitions")) {
				// no changes to compartments or reactions here
			} else if (token.instanceOf("notes")){
				// no interes in notes of model or notes of listOfSpecies
			} else {
				throw new NoTokenException("found unexpected tokenclass: "+token.tokenClass());
			}
		}
  }

	private static void addUrisToSpecies(XmlToken token) throws DataFormatException {
		//System.out.println("addUrisToSpecies("+token.getValue("id")+")");
		String id=token.getValue("id");		
		if (id.length()==6 && id.startsWith("C")){
			String number=id.substring(1);
			try {
				Integer.parseInt(number);		
				addUrisToSpecies(token,"C"+number);
			} catch (NumberFormatException e) {
				Tools.note(id+" is not a valid kegg id");
			}
		} 
		if (id.length()==8 && id.startsWith("CPD")){
			String number=id.substring(3);
			try {
				Integer.parseInt(number);		
				addUrisToSpecies(token,"C"+number);
			} catch (NumberFormatException e) {
				Tools.note(id+" is not a valid kegg id");
			}
		}
  }

	private static void addUrisToSpecies(XmlToken token, String id) throws DataFormatException {
		KeggCompoundUrn urn = new KeggCompoundUrn(id);
		Tools.note("addUrisToSpecies("+token.getValue("id")+", "+urn+")");
		XmlToken annotation=new XmlToken("annotation");
		XmlToken rdf=new XmlToken("rdf:RDF");
		XmlToken description=new XmlToken("rdf:Description");
		XmlToken bqBiolIs=new XmlToken("bqbiol:is");
		XmlToken bag=new XmlToken("rdf:Bag");
		XmlToken rdfLi=new XmlToken("rdf:li");
		rdfLi.setValue("rdf:resource",urn.toString());
		bag.add(rdfLi);
		bqBiolIs.add(bag);
		description.add(bqBiolIs);
		rdf.add(description);
		annotation.add(rdf);
		token.add(annotation);

  }

	/**
	 * feeds the contents of ax ml token into the interaction database
	 * 
	 * @param sbmlToken the sbml token to extract FROM
	 * @param fileHash the hash-code of the source file
	 * @param groupnumber the number of the compartment group, to which the model's compartment shall be added
	 * @param superCompartment the compartment, to which this model shall be added
	 * @param source 
	 * @throws SQLException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchAttributeException if a referenced compartment is missing
	 * @throws DataFormatException
	 * @throws NoSuchMethodException
	 * @throws NoTokenException 
	 * @throws IOException 
	 */
	private static void writeSbmlIntoDatabase(XmlToken sbmlToken, MD5Hash fileHash, int groupnumber, DbCompartment superCompartment, URL source) throws SQLException, NoSuchAlgorithmException, NoSuchAttributeException, NoSuchMethodException, DataFormatException, NoTokenException, IOException {
		TreeMap<String, Integer> mapFromCompartmentIdsToDbIds = new TreeMap<String, Integer>(ObjectComparator.get());
		TreeMap<String, Integer> mapFromSubstanceIdsToDbIds = new TreeMap<String, Integer>(ObjectComparator.get());
		TreeMap<Integer, Integer> mapFromSubstanceToCompartment = new TreeMap<Integer, Integer>();
		TreeMap<String, String> mapFromModelSubstanceToECNumber = new TreeMap<String, String>(ObjectComparator.get());
		for (Iterator<XmlToken> it = sbmlToken.subtokenIterator(); it.hasNext();) {
			XmlToken subtoken = it.next();
			if (subtoken.instanceOf("model")) {
				writeModelIntoDatabase(subtoken, fileHash, groupnumber, mapFromCompartmentIdsToDbIds, mapFromSubstanceIdsToDbIds, mapFromSubstanceToCompartment, superCompartment,source,mapFromModelSubstanceToECNumber);
			} else Tools.noteOnce("Note: found " + subtoken.tokenClass() + " token in sbml");
		}

	}

	/**
	 * writes the given xml model into the database
	 * 
	 * @param model the sbml token of the model
	 * @param fileHash the hash-code of the source file
	 * @param groupnumber the number of the compartment group, to which the model's compartment shall be added
	 * @param mapFromCompartmentIdsToCids a mapping from this models compartment ids to their respective database compartment ids
	 * @param mapFromSubstanceIdsToSids a mapping from this models substance ids to their respective database substance ids
	 * @param mapFromSubstanceToCompartment a mapping from this models substnance ids to the ids of the compartments, where they are related by annotation
	 * @param superCompartment the compartment, in which all compartments of the model will be embedded
	 * @param source 
	 * @param mapFromModelSubstanceToECNumber 
	 * @throws SQLException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchAttributeException if a referenced compartment is missing
	 * @throws DataFormatException
	 * @throws NoSuchMethodException
	 * @throws NoTokenException 
	 * @throws IOException 
	 */
	private static void writeModelIntoDatabase(XmlToken model, MD5Hash fileHash, int groupnumber, TreeMap<String, Integer> mapFromCompartmentIdsToCids, TreeMap<String, Integer> mapFromSubstanceIdsToSids, TreeMap<Integer, Integer> mapFromSubstanceToCompartment, DbCompartment superCompartment, URL source, TreeMap<String, String> mapFromModelSubstanceToECNumber) throws SQLException, NoSuchAlgorithmException, NoSuchAttributeException, NoSuchMethodException, DataFormatException, NoTokenException, IOException {
		String modelName = model.getValue("name");
		if (modelName == null) modelName = model.getValue("id");
		if (modelName == null) modelName = fileHash.toString();
		for (Iterator<XmlToken> it = model.subtokenIterator(); it.hasNext();) {
			XmlToken subtoken = it.next();
			if (subtoken.instanceOf("listOfCompartments")) {
				writeCompartmentListIntoDatabase(subtoken, fileHash, groupnumber, mapFromCompartmentIdsToCids, modelName, superCompartment,source);
			} else if (subtoken.instanceOf("listOfSpecies")) {
				writeSubstancesListIntoDatabase(subtoken, fileHash, groupnumber, mapFromSubstanceIdsToSids, mapFromCompartmentIdsToCids, mapFromSubstanceToCompartment,superCompartment,source,mapFromModelSubstanceToECNumber);
			} else if (subtoken.instanceOf("listOfReactions")) {
				writeReactionListIntoDatabase(subtoken, fileHash, groupnumber, mapFromSubstanceIdsToSids, mapFromCompartmentIdsToCids, mapFromSubstanceToCompartment,superCompartment,source,mapFromModelSubstanceToECNumber);
			} else Tools.noteOnce("found " + subtoken.tokenClass() + " token in model...");
		}
	}

	/**
	 * writes the list of compartments contained within this sbml file into the database
	 * 
	 * @param compartmentList the list of compartments token of the sbml file
	 * @param fileHash the hash-value of the sbml file
	 * @param groupnumber the number of the compartment group, to which the models compartment shall be added
	 * @param mapFromCompartmentIdsToCids a mapping from this models compartment ids to their respective database compartment ids
	 * @param modelName the name of the model as given in its code
	 * @param superCompartment the compartment, in which all compartments of the model will be embedded 
	 * @param source 
	 * @throws SQLException
	 * @throws NoSuchAlgorithmException
	 * @throws DataFormatException
	 * @throws NoSuchMethodException
	 * @throws IOException 
	 */
	private static void writeCompartmentListIntoDatabase(XmlToken compartmentList, MD5Hash fileHash, int groupnumber, TreeMap<String, Integer> mapFromCompartmentIdsToCids, String modelName, DbCompartment superCompartment, URL source) throws SQLException, NoSuchAlgorithmException, NoSuchMethodException, DataFormatException, IOException {
		Vector<XmlToken> unparsedSubTokens = new Vector<XmlToken>();
		for (Iterator<XmlToken> it = compartmentList.subtokenIterator(); it.hasNext();)	unparsedSubTokens.add(it.next());
		while (!unparsedSubTokens.isEmpty()) {
			XmlToken subtoken = unparsedSubTokens.remove(0);
			if (subtoken.instanceOf("compartment")) {
				try {
					writeCompartmentIntoDatabase(subtoken, fileHash, groupnumber, mapFromCompartmentIdsToCids,superCompartment,source);
				} catch (NoSuchElementException nse) {
					Tools.note(subtoken.tokenClass() + " token references " + nse.getMessage() + ", will be parsed later.");
					unparsedSubTokens.add(subtoken); // subtoken can not yet be parsed, since referenced token has not been parsed before. so put it in queue
				}
			} else Tools.noteOnce("found " + subtoken.tokenClass() + " token in listOfCompartments");
		}
	}

	/**
	 * writes the list of substances declared in this sbml file into the database
	 * 
	 * @param token the substance list token
	 * @param fileHash the hash-value of the containing file
	 * @param groupnumber the number of the compartment group, to which the file's compartment shall be added
	 * @param mapFromCompartmentIdsToCids a mapping from this models compartment ids to their respective database compartment ids
	 * @param mapFromSubstanceIdsToSids a mapping from this models substance ids to their respective database substance ids
	 * @param mapFromSubstanceToCompartment a mapping from this models substnance ids to the ids of the compartments, where they are related by annotation
	 * @param superCompartment the compartment, in which all compartments of the model will be embedded
	 * @param source 
	 * @param mapFromModelSubstanceToECNumber 
	 * @throws SQLException
	 * @throws NoSuchAttributeException if a referenced compartment is not present.
	 * @throws DataFormatException
	 * @throws NoSuchMethodException 
	 * @throws IOException 
	 */
	private static void writeSubstancesListIntoDatabase(XmlToken token, MD5Hash fileHash, int groupnumber, TreeMap<String, Integer> mapFromSubstanceIdsToCids, TreeMap<String, Integer> mapFromCompartmentIdsToCids, TreeMap<Integer, Integer> mapFromSubstanceToCompartment, DbCompartment superCompartment, URL source, TreeMap<String, String> mapFromModelSubstanceToECNumber) throws SQLException, NoSuchAttributeException, DataFormatException, NoSuchMethodException, IOException {
		for (Iterator<XmlToken> it = token.subtokenIterator(); it.hasNext();) {
			XmlToken subtoken = it.next();
			if (subtoken.instanceOf("species")) {
				writeSpeciesIntoDatabase(subtoken, fileHash, groupnumber, mapFromSubstanceIdsToCids, mapFromCompartmentIdsToCids, mapFromSubstanceToCompartment,superCompartment,source,mapFromModelSubstanceToECNumber);
			} else Tools.noteOnce("found " + subtoken.tokenClass() + " token in listOfSpecies");
		}
	}

	/**
	 * writes the reaction list into the database
	 * 
	 * @param token the reaction list token of the sbml file
	 * @param fileHash the hash-code of the sbml file
	 * @param groupnumber the number of the compartment group, to which the model's compartment shall be added
	 * @param mapFromCompartmentIdsToCids a mapping from this models compartment ids to their respective database compartment ids
	 * @param mapFromSubstanceIdsToSids a mapping from this models substance ids to their respective database substance ids
	 * @param mapFromSubstanceToCompartment a mapping from this models substance ids to the ids of the compartments, where they are related by annotation
	 * @param superCompartment the compartment, in which all compartments of the model will be embedded
	 * @param mapFromModelSubstanceToECNumber 
	 * @throws SQLException
	 * @throws NoSuchAlgorithmException
	 * @throws NoTokenException 
	 * @throws NoSuchMethodException 
	 * @throws IOException 
	 */
	private static void writeReactionListIntoDatabase(XmlToken token, MD5Hash fileHash, int groupnumber, TreeMap<String, Integer> mapFromSubstanceIdsToSids, TreeMap<String, Integer> mapFromCompartmentIdsToCids, TreeMap<Integer, Integer> mapFromSubstanceToCompartment, DbCompartment superCompartment,URL source, TreeMap<String, String> mapFromModelSubstanceToECNumber) throws SQLException, NoSuchAlgorithmException, NoTokenException, NoSuchMethodException, IOException {
		for (Iterator<XmlToken> it = token.subtokenIterator(); it.hasNext();) {
			XmlToken subtoken = it.next();

			if (subtoken.instanceOf("reaction")) {
				writeReactionIntoDatabase(subtoken, mapFromSubstanceIdsToSids, mapFromCompartmentIdsToCids, mapFromSubstanceToCompartment, fileHash,superCompartment,source,mapFromModelSubstanceToECNumber);

			} else Tools.noteOnce("found " + subtoken.tokenClass() + " token in ListOfReactions");
		}
	}

	/**
	 * writes the data of a singel compartment into the database
	 * 
	 * @param token the compartment token of the sbml file
	 * @param fileHash the hash-value of the sbml file
	 * @param groupNumber the number of the compartment group, to which the models compartment shall be added
	 * @param mapFromCompartmentIdsToCids a mapping from this models compartment ids to their respective database compartment ids
	 * @param superCompartment the compartment, in which all compartments of the model will be embedded
	 * @param source 
	 * @throws SQLException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchMethodException
	 * @throws DataFormatException
	 * @throws IOException 
	 */
	private static void writeCompartmentIntoDatabase(XmlToken token, MD5Hash fileHash, int groupNumber, TreeMap<String, Integer> mapFromCompartmentIdsToCids, DbCompartment superCompartment, URL source) throws SQLException, NoSuchAlgorithmException, NoSuchMethodException, DataFormatException, IOException {
		String idUsedInFile = token.getValue("id");
		String nameUsedInFile = token.getValue("name");
		
		if (nameUsedInFile!=null && nameUsedInFile.equals(".")) {
			Tools.warnOnce(nameUsedInFile+" is not an appropriate name. will abolish it.");
			nameUsedInFile=null;
		}
		if (idUsedInFile==null){
			Tools.warn("no compartment id given for "+nameUsedInFile);			
			if (nameUsedInFile==null){
				Tools.warn("and no compartment name given!");
			} else {
				idUsedInFile=nameUsedInFile;
			}
		}
		
		String outside = token.getValue("outside");
		TreeSet<URN> urns = readUrns(token);
		if (outside != null && !mapFromCompartmentIdsToCids.containsKey(outside)) throw new NoSuchElementException(outside); // comartment is referencing an outer compartment, but the outer compartment has not been parsed, yet
		if (nameUsedInFile == null) { // no name? use id...
			nameUsedInFile = "id: " + idUsedInFile;
		} else nameUsedInFile = nameUsedInFile + " (id: " + idUsedInFile + ")"; // append id
		//System.out.print("writing compartment into dataase (name = "+nameUsedInFile+", original id = "+idUsedInFile+", outside = "+outside+", urns = "+urns+", superCompartment = "+superCompartment+")");		
		String query = null;
		try {
			/* now we write the new compartment into the database */
			//Integer databaseCompartmentId = InteractionDB.getOrCreateCompartment(urns, groupNumber,source);
			Integer databaseCompartmentId = InteractionDB.createCompartment(nameUsedInFile, urns, groupNumber, source);
//			System.out.println(" => "+databaseCompartmentId);

// TODO: werden die folgenden drei zeilen noch gebraucht?			
//			if (urns == null || urns.isEmpty()) {
//				InteractionDB.insertUrn(databaseCompartmentId, new FileUrn(fileHash + ":compartment:" + idUsedInFile));
//			}
			
			
//			InteractionDB.insertName(databaseCompartmentId, nameUsedInFile);
			Statement statement = InteractionDB.createStatement();
			if (outside != null) {
				Integer outsideId = mapFromCompartmentIdsToCids.get(outside);
				query = "INSERT INTO hierarchy (contained,container) VALUES (" + databaseCompartmentId + ", " + outsideId + ")";
				try {
					statement.execute(query);
				} catch (SQLException e){
					if (!e.getMessage().contains("Duplicate key")) throw e;
				}
			}
			if (superCompartment != null){
				query = "INSERT INTO hierarchy (contained,container) VALUES (" + databaseCompartmentId + ", " + superCompartment.id() + ")";
				try {
					statement.execute(query);
				} catch (SQLException e){
					if (!e.getMessage().contains("Duplicate key")) throw e;
				}
			}
			statement.close();

			mapFromCompartmentIdsToCids.put(idUsedInFile, databaseCompartmentId); // save the compartment id for further usage

			/* here we save an url for the compartment into the database */

		} catch (SQLException e) {
			System.err.println("Error on " + query);
			throw e;
		}
	}

	/**
	 * tries to read all urns given within an annotation token
	 * @param urns the set of urns, to which newly found urns shall be added
	 * @param token the token to be inspected
	 * @param path the path to this token (for debugging output)
	 * @throws DataFormatException
	 */
	private static void getAnnotationURNs(TreeSet<URN> urns, XmlToken token, String path) throws DataFormatException {
		path=path+"/"+token.tokenClass();
		for (Iterator<XmlToken> subtokens = token.subtokenIterator(); subtokens.hasNext();) {
			token=subtokens.next();
			if (token.instanceOf("rdf:RDF")){
				getURNsFromRdf(urns,token,path);
			} else if (token.instanceOf("COPASI")){
				getAnnotationURNs(urns,token,path);
			} else if (token.instanceOf("jd:display") || token.instanceOf("jd:listOfShadows")){
				// we are not interested in display flags
			} else if (token.instanceOf("VCellInfo")){
				// we are not interested in VCellInfo
			} else if (token.instanceOf("body")){
				// we are not interested in body token of annotation
			} else if (token.instanceOf("celldesigner:name")){
				// celldesigner names seem to be identical to names given in species/compartment tags, so unnecessary
			} else if (token.instanceOf("celldesigner:positionToCompartment")){
				// we are not interested in node positions
			} else if (token.instanceOf("celldesigner:speciesIdentity")){
				// we are not interested in this
			} else if (token.instanceOf("celldesigner:extension")){
				// we are not interested in celldesigner extensions
			} else if (token.instanceOf("celldesigner:listOfCatalyzedReactions")){
				// shoould be annotated in modifiers of reaction, too
			} else if (token.instanceOf("srsoftware:urn")){
				urns.add(new MiriamUrn(token.getValue("urn")));
			} else {
				throw new DataFormatException("found "+token.tokenClass()+" token in "+path+":\n"+token);
			}
		}
	}
	
	/**
	 * tries to read all urns from a rdf token
	 * @param urns the set of urns, to which newly found urns shall be added
	 * @param token the token, which shall be inspected
	 * @param path the path which leads to the token (for debugging output)
	 * @throws DataFormatException
	 */
	private static void getURNsFromRdf(TreeSet<URN> urns, XmlToken token, String path) throws DataFormatException {
		path=path+"/"+token.tokenClass();
		for (Iterator<XmlToken> subtokens = token.subtokenIterator(); subtokens.hasNext();) {
			token=subtokens.next();
			if (token.instanceOf("rdf:Description")){
				getURNsFromRdfDescription(urns,token,path);
			} else {
				throw new DataFormatException("found "+token.tokenClass()+" token in "+path+"...");
			}
		}
	}

	/**
	 * scans the rdf:description tokens for urns related to a higher-level token 
	 * @param urns the urns of the higher-level token, to which given urns shall be added
	 * @param token the token to be inspected
	 * @param path the path to this token, for debugging output
	 * @throws DataFormatException
	 */
	private static void getURNsFromRdfDescription(TreeSet<URN> urns, XmlToken token, String path) throws DataFormatException {
		path=path+"/"+token.tokenClass();
		for (Iterator<XmlToken> subtokens = token.subtokenIterator(); subtokens.hasNext();) {
			token=subtokens.next();
			if (token.instanceOf("bqbiol:is") || token.instanceOf("bqmodel:is") || token.instanceOf("CopasiMT:is")) {
				getURNsFromBqBiolIs(urns,token,path);
			} else if (token.instanceOf("bqbiol:isVersionOf") || token.instanceOf("bqbiol:hasVersion") || token.instanceOf("CopasiMT:isVersionOf")){
				// we are currently not interested in versions
			} else if (token.instanceOf("bqbiol:isHomologTo")||token.instanceOf("CopasiMT:isHomologTo")){
				// we are currently not interested in homologies
			} else if (token.instanceOf("bqbiol:hasProperty")){
				getEnzymeUrnsFromBqBiolHasProperty(urns,token,path);
			} else if (token.instanceOf("bqbiol:occursIn")){
				// we are currently not interested in this kind of occurences
			} else if (token.instanceOf("bqbiol:hasPart")||token.instanceOf("bqbiol:isPartOf")){
				// we are currently not interested in parts
			} else if (token.instanceOf("bqbiol:encodes") || token.instanceOf("CopasiMT:encodes") || token.instanceOf("bqbiol:isEncodedBy")){
				// we are currently not interested in genes
			} else if (token.instanceOf("bqmodel:isDescribedBy")||token.instanceOf("bqbiol:isDescribedBy")){
				// we are currently not interested in descriptions
			} else if (token.instanceOf("dcterms:created")){
				// we don't care about creation times
			} else if (token.instanceOf("dcterms:bibliographicCitation")){
				// we don't care about bibs
			} else if (token.instanceOf("dc:relation")){
				getURNsFromDCrelation(urns,token,path);
			} else {
				throw new DataFormatException("found "+token.tokenClass()+" token in "+path+":\n"+token);
			}
		}
	}
	
	private static void getEnzymeUrnsFromBqBiolHasProperty(TreeSet<URN> urns, XmlToken token, String path) throws DataFormatException {
		path=path+"/"+token.tokenClass();
		for (Iterator<XmlToken> subtokens = token.subtokenIterator(); subtokens.hasNext();) {
			token=subtokens.next();
			if (token.instanceOf("rdf:Bag")){
				getEnzymeUrnsFromBqBiolHasPropertyBag(urns,token,path);
			} else {
				throw new DataFormatException("found "+token.tokenClass()+" token in "+path+"...");
			}
		}
	}

	private static void getEnzymeUrnsFromBqBiolHasPropertyBag(TreeSet<URN> urns, XmlToken token, String path) throws DataFormatException {
		path=path+"/"+token.tokenClass();
		for (Iterator<XmlToken> subtokens = token.subtokenIterator(); subtokens.hasNext();) {
			token=subtokens.next();
			if (token.instanceOf("rdf:li")){
				String u=token.getValue("rdf:resource");
				if (u!=null && u.startsWith("urn") && u.contains("ec-code")){
					urns.add(new MiriamUrn(u));					
				} 
			} else throw new DataFormatException("found "+token.tokenClass()+" token in "+path+"...");
		}
	}

	/**
	 * try to read urns from given dc:relation tags
	 * @param urns the set of urns, to which the results shall be added
	 * @param token the token containing the relations
	 * @param path the path to this token
	 * @throws DataFormatException
	 */
	private static void getURNsFromDCrelation(TreeSet<URN> urns, XmlToken token, String path) throws DataFormatException {
		path=path+"/"+token.tokenClass();
		for (Iterator<XmlToken> subtokens = token.subtokenIterator(); subtokens.hasNext();) {
			token=subtokens.next();
			if (token.instanceOf("rdf:Bag")){
				getURNsFromDcRelationBag(urns,token,path);
			} else {
				throw new DataFormatException("found "+token.tokenClass()+" token in "+path+"...");
			}
		}
	}

	/**
	 * try to read urns from a dc:relation/rdf:bag tag
	 * @param urns the urn set, to which new urns shall be added
	 * @param token the current token
	 * @param path the path to the current token
	 * @throws DataFormatException
	 */
	private static void getURNsFromDcRelationBag(TreeSet<URN> urns, XmlToken token, String path) throws DataFormatException {
		path=path+"/"+token.tokenClass();
		for (Iterator<XmlToken> subtokens = token.subtokenIterator(); subtokens.hasNext();) {
			token=subtokens.next();
			if (token.instanceOf("rdf:li")){
				String u=token.getValue("rdf:resource");
				if (u==null) throw new DataFormatException("found rdf:li, but it has no rdf:ressource! ("+token+")");
				if (u.startsWith("http://www.genome.jp/kegg/compound/#C")) {
					String code=u.substring(u.indexOf("#")+1);
					urns.add(new KeggCompoundUrn(code));
				} else if (u.startsWith("http://bigg.ucsd.edu/#")){
					Tools.noteOnce("found bigg resource in "+token.tokenClass()+" in "+path+"...");
				} else  if (u.startsWith("http://www.genoscope.cns.fr/acinetocyc/#")){
					Tools.noteOnce("found genoscope resource in "+token.tokenClass()+" in "+path+"...");
				} else throw new DataFormatException("unkown rdf:ressource type "+u+" token in "+path+"...");
			} else throw new DataFormatException("found "+token.tokenClass()+" token in "+path+"...");
		}
	}

	/**
	 * reads urns from bq:biol tags
	 * @param urns the urns, to which given urns shall be added
	 * @param token the current token
	 * @param path the path to the current token
	 * @throws DataFormatException
	 */
	private static void getURNsFromBqBiolIs(TreeSet<URN> urns, XmlToken token, String path) throws DataFormatException {
		path=path+"/"+token.tokenClass();
		for (Iterator<XmlToken> subtokens = token.subtokenIterator(); subtokens.hasNext();) {
			token=subtokens.next();
			if (token.instanceOf("rdf:Bag")){
				getURNsFromBqBiolIsBag(urns,token,path);
			} else {
				throw new DataFormatException("found "+token.tokenClass()+" token in "+path+"...");
			}
		}
	}

	/**
	 * reads urns from an bq:biol rdf-bag
	 * @param urns the urn list, to which new urns shall be added
	 * @param token the bq:biol/rdf:bag token
	 * @param path the path to this token
	 * @throws DataFormatException
	 */
	private static void getURNsFromBqBiolIsBag(TreeSet<URN> urns, XmlToken token, String path) throws DataFormatException {
		path=path+"/"+token.tokenClass();
		for (Iterator<XmlToken> subtokens = token.subtokenIterator(); subtokens.hasNext();) {
			token=subtokens.next();
			if (token.instanceOf("rdf:li")){
				String u=token.getValue("rdf:resource");
				if (u!=null){
					if (u.startsWith("urn") || u.startsWith("http://identifiers.org")){
						urns.add(new MiriamUrn(u));
					} else System.err.println("Notice: found rdf:li, but its rdf:ressource has unknown format! ("+token+")");
				} else System.err.println("Notice: found rdf:li, but it has no rdf:ressource! ("+token+")"); 
			} else throw new DataFormatException("found "+token.tokenClass()+" token in "+path+"...");
		}
	}

	/**
	 * should reduce the cids list to a single compartment containing all other compartments, which were in the list before
	 * 
	 * @param cids
	 * @return
	 * @throws SQLException
	 */
	private static Collection<Integer> getTopCompartments(Collection<Integer> givenCompartmentIDs) throws SQLException {
		TreeSet<Integer> suborderedCompartments = new TreeSet<Integer>();
		TreeSet<Integer> result = new TreeSet<Integer>();
		for (Iterator<Integer> it = givenCompartmentIDs.iterator(); it.hasNext();) suborderedCompartments.addAll((DbCompartment.load(it.next())).containedCompartments(true));
		result.addAll(givenCompartmentIDs);
		result.removeAll(suborderedCompartments);
		return result;
	}

//	private static Integer createCompartmentContaining(Collection<Integer> includedcompartments, MD5Hash fileHash, String compartmentName, int groupnumber) throws SQLException, MalformedURLException, DataFormatException {
//		/* cid,names,molecules,includedin,includes */
//		URN fileUrn = new MiriamUrn("file:" + fileHash + ":compartment:SuperCompartment");
//		Integer cid = InteractionDB.getIdFor(fileUrn);
//		if (cid == null) {
//			cid = InteractionDB.insertId(InteractionDB.COMPARTMENT);
//			InteractionDB.insertUrn(cid, fileUrn);
//		}
//		InteractionDB.insertName(cid, compartmentName);
//		Statement statement = InteractionDB.createStatement();
//		String query = "INSERT INTO compartments VALUES(" + cid + ", " + groupnumber + ")";
//		statement.execute(query);
//		statement.close();
//
//		Compartment superCompartment = Compartment.get(cid);
//
//		for (Iterator<Integer> it = includedcompartments.iterator(); it.hasNext();)
//			superCompartment.addContainedCompartment(it.next());
//		return cid;
//	}

	/**
	 * write the set of chemical species into the databse
	 * 
	 * @param token the species list token FROM the sbml file
	 * @param fileHash the hash-value of the sbml file
	 * @param groupNumber the number of the compartment group, to which the models compartment shall be added
	 * @param mapFromCompartmentIdsToCids a mapping from this models compartment ids to their respective database compartment ids
	 * @param mapFromSubstanceIdsToSids a mapping from this models substance ids to their respective database substance ids
	 * @param mapFromSubstanceToCompartment a mapping from this models substnance ids to the ids of the compartments, where they are related by annotation
	 * @param source 
	 * @param mapFromModelSubstanceToECNumber 
	 * @throws SQLException
	 * @throws NoSuchAttributeException if a referenced compartment is not present.
	 * @throws DataFormatException
	 * @throws NoSuchMethodException 
	 * @throws IOException 
	 */
	private static void writeSpeciesIntoDatabase(XmlToken token, MD5Hash fileHash, int groupNumber, TreeMap<String, Integer> mapFromSubstanceIdsToCids, TreeMap<String, Integer> mapFromCompartmentIdsToCids, TreeMap<Integer, Integer> mapFromSubstanceToCompartment,DbCompartment supercompartment, URL source, TreeMap<String, String> mapFromModelSubstanceToECNumber) throws SQLException, NoSuchAttributeException, DataFormatException, NoSuchMethodException, IOException {
		String id = token.getValue("id");
		String name = token.getValue("name");
		if (name!=null && name.equals(".")) {
			Tools.warnOnce(name+" is not an appropriate name. will abolish it.");
			name=null;
		}
		if (id==null) {
			Tools.warn("no id given for species with name \""+name+"\". Using name as id...");
			id=name;
		}
		String compartment = token.getValue("compartment");
		
		
		TreeSet<URN> urns = readUrns(token);		
		TreeSet<URN> enzymeUrns = new TreeSet<URN>(ObjectComparator.get());
		for (Iterator<URN> it = urns.iterator(); it.hasNext();) {
	    URN dummy = it.next();
	    if (dummy.toString().contains("ec-code")){
	    	enzymeUrns.add(dummy);
	    }
    }
  	urns.removeAll(enzymeUrns);
		
		if (!enzymeUrns.isEmpty()){
			if (enzymeUrns.size()==1){
			String ec=enzymeUrns.first().toString();
			ec=ec.substring(ec.lastIndexOf(":")+1);
			mapFromModelSubstanceToECNumber.put(id, ec);
			}
//TODO: reaktivieren, wenn Modelle bereinigt:			 if (enzymeUrns.size()>1) throw new DataFormatException(source+" contains multiple enzyme codes for "+name+"!");
		}
		
		if (urns.isEmpty()) urns.add(new FileUrn(fileHash+".substance:"+id));
		//System.out.println("writing substance into dataase (name = "+name+", original id = "+id+", urns = "+urns+")");		

		if (name==null) name=id;
		
		Integer sid = InteractionDB.createSubstance(name, null, urns, source);
//		Integer sid = InteractionDB.getOrCreateSubstanceId(urns,source);
		if (compartment != null) {
			Integer cid = mapFromCompartmentIdsToCids.get(compartment);
			if (cid == null) throw new NoSuchAttributeException("SBML states the substance \"" + name + "\" resides in \"" + compartment + "\", but no such compartment is given within the file!");
			mapFromSubstanceToCompartment.put(sid, cid);
		}

		mapFromSubstanceIdsToCids.put(id, sid);
	}

	/**
	 * tries to read the urns given in this token
	 * @param token the token which may or may not contain urns
	 * @return the set of relevant urns of this token (if any)
	 * @throws DataFormatException
	 */
	private static TreeSet<URN> readUrns(XmlToken token) throws DataFormatException {
		TreeSet<URN> urns = new TreeSet<URN>(ObjectComparator.get());
		String path=token.tokenClass();
		for (Iterator<XmlToken> it = token.subtokenIterator();it.hasNext();){
			token=it.next();
			if (token.instanceOf("annotation")){
				getAnnotationURNs(urns, token,path);
			} else if (token.instanceOf("notes")){
				getNotesURNs(urns,token,path);
			} else {
				throw new DataFormatException("found "+token.tokenClass()+" token in "+path);
			}
		}
		return urns;
  }

	private static void getNotesURNs(TreeSet<URN> urns, XmlToken token, String path) throws DataFormatException {
		path=path+"/"+token.tokenClass();
		for (Iterator<XmlToken> subtokens = token.subtokenIterator(); subtokens.hasNext();) {
			token=subtokens.next();
			if (token.instanceOf("html")){
				getURNsFromHtmlNotes(urns,token,path);
			} else if (token.instanceOf("body")){
				// we are note interedted in note bodies at the moment
			} else if (token.instanceOf("p")){
				// we are note interedted in note paragraphs at the moment
			} else {
				throw new DataFormatException("found "+token.tokenClass()+" token in "+path+"...");
			}
		}
  }

	private static void getURNsFromHtmlNotes(TreeSet<URN> urns, XmlToken token, String path) throws DataFormatException {
		path=path+"/"+token.tokenClass();
		for (Iterator<XmlToken> subtokens = token.subtokenIterator(); subtokens.hasNext();) {
			token=subtokens.next();
			if (token.instanceOf("p") || token.instanceOf("html:p")){
				if (!"wrong".equals(token.getValue("class")))	getURNsFromHtmlNotesParagraph(urns,token,path);
			} else if (token.instanceOf("body")){
				// we are note interedted in note bodies at the moment
			} else {
				throw new DataFormatException("found "+token.tokenClass()+" token in "+path+"...");
			}
		}
  }

	private static void getURNsFromHtmlNotesParagraph(TreeSet<URN> urns, XmlToken token, String path) throws DataFormatException {
		String text=token.content();
		if (text.startsWith("KEGGID:")) {
			text=text.substring(7).trim();
			if (text.toUpperCase().equals("NONE")) return;
			if (text.toUpperCase().equals("NA")) return;
			String[] ids = text.split(" ");
			for (int i=0; i<ids.length; i++) {
				if (ids[i].startsWith("C")) {
					urns.add(new KeggCompoundUrn(ids[i]));
				} else throw new DataFormatException("found KEGG id ("+ids[i]+") in "+token+", but it looks strange ;)");
			}
		} else if (text.startsWith("KEGG ID:")){
			text=text.substring(8).trim();
			if (text.toUpperCase().equals("NONE")) return;
			if (text.toUpperCase().equals("NA")) return;
			if (text.startsWith("phosphorylated version")) return; // this is a patch for GSNM118 - Salmonella typhimurium LT2(Thiele)
			String[] ids = text.split(" ");
			for (int i=0; i<ids.length; i++) {
				if (ids[i].startsWith("C")) {
					urns.add(new KeggCompoundUrn(ids[i]));
				} else throw new DataFormatException("found KEGG id ("+ids[i]+") in "+token+", but it looks strange ;)");
			}
		} else if (text.startsWith("PubChem ID:")){
			text=text.substring(11).trim();
			if (text.toUpperCase().equals("NONE")) return;
			if (text.toUpperCase().equals("NA")) return;
			String[] ids = text.split(" ");
			for (int i=0; i<ids.length; i++) {
				if (!ids[i].equals("")){
					if (!ids[i].contains("??")) {// this is a workaround for GSNM118 - Salmonella typhimurium LT2(Thiele)/STM_v1.0.xml
						urns.add(new PubChemSubstanceUrn(ids[i]));
					}
				}
			}
		} else if (text.startsWith("ChEBI ID:")){
			text=text.substring(9).trim();
			if (text.toUpperCase().equals("NONE")) return;
			if (text.toUpperCase().equals("NA")) return;
			String[] ids = text.split(" ");
			for (int i=0; i<ids.length; i++) {
				if (!ids[i].equals("")) urns.add(new ChEBIUrn(ids[i]));
			}
		} else if (text.startsWith("CASID:")){
			text=text.substring(6).trim();
			if (text.toUpperCase().equals("NONE")) return;
			if (text.toUpperCase().equals("NA")) return;
			String[] ids = text.split(" ");
			for (int i=0; i<ids.length; i++) {
				if (!ids[i].equals("")) urns.add(new CasUrn(ids[i]));
			}
		} else {
			if (text.toUpperCase().contains("ID")) throw new DataFormatException("found some kind of id in tag. Have a look here: "+text);
		}
  }

	/**
	 * 
	 * writes a singular reaction FROM the sbml file into the database
	 * @param token the reaction token FROM the sbml file
	 * @param fileHash the hash-value of the sbml model
	 * @param mapFromCompartmentIdsToCids a mapping from this models compartment ids to their respective database compartment ids
	 * @param mapFromSubstanceIdsToSids a mapping from this models substance ids to their respective database substance ids
	 * @param mapFromSubstanceToCompartment a mapping from this models substnance ids to the ids of the compartments, where they are related by annotation
	 * @param superCompartment the compartment, in which all compartments of the model will be embedded
	 * @param mapFromModelSubstanceToECNumber 
	 * @throws SQLException
	 * @throws NoSuchAlgorithmException
	 * @throws NoTokenException 
	 * @throws NoSuchMethodException 
	 * @throws IOException 
	 */
	private static void writeReactionIntoDatabase(XmlToken token, TreeMap<String, Integer> mapFromSubstanceIdsToSids, TreeMap<String, Integer> mapFromCompartmentIdsToCids, TreeMap<Integer, Integer> mapFromSubstanceToCompartment, MD5Hash fileHash, DbCompartment superCompartment,URL source, TreeMap<String, String> mapFromModelSubstanceToECNumber) throws SQLException, NoSuchAlgorithmException, NoTokenException, NoSuchMethodException, IOException {
		String idString = token.getValue("id");
		String name = token.getValue("name");
		if (name!=null && name.equals(".")) {
			Tools.warnOnce(name+" is not an appropriate name. will abolish it.");
			name=null;
		}
		String compartment = token.getValue("compartment");
		String reversible = token.getValue("reversible");
		if (name == null) name = "id: " + idString;
		
		int rid=InteractionDB.createReaction(name, null, false, source);
//		int rid = InteractionDB.insertId(InteractionDB.REACTION);
//		InteractionDB.insertName(rid, name);

		TreeSet<Integer> participatingSubstances = new TreeSet<Integer>();

		try {
			for (Iterator<XmlToken> it = token.subtokenIterator(); it.hasNext();) {
				XmlToken subtoken = it.next();
				if (subtoken.instanceOf("listOfReactants")) {
					participatingSubstances.addAll(writeReactionReactantsIntoDatabase(subtoken, rid, mapFromSubstanceIdsToSids));
				} else if (subtoken.instanceOf("listOfProducts")) {
					participatingSubstances.addAll(writeReactionProductsIntoDatabase(subtoken, rid, mapFromSubstanceIdsToSids));
				} else if (subtoken.instanceOf("listOfModifiers")) {
					participatingSubstances.addAll(writeReactionModifiersIntoDatabase(subtoken, rid, mapFromSubstanceIdsToSids, fileHash,mapFromModelSubstanceToECNumber));
				} else Tools.noteOnce("found " + subtoken.tokenClass() + " token in Reaction");
			}
		} catch (SQLException e) {
			System.err.println("@ " + idString + " / " + name);
			throw e;
		} catch (NoTokenException e) {
			System.err.println("@token: "+token);
			throw e;
    }
		Collection<Integer> cids = new TreeSet<Integer>();
		if (compartment == null) {
			for (Iterator<Integer> it = participatingSubstances.iterator(); it.hasNext();) {
				int sid = it.next();
				Integer cid = mapFromSubstanceToCompartment.get(sid);
				if (cid != null) {
					cids.add(cid);
				}
			}
			if (cids.size() > 1) {
				cids = getTopCompartments(cids);
			}
			if (cids.size() > 1) {
				cids=new TreeSet<Integer>();
				cids.add(superCompartment.id());
			}
		} else cids.add(mapFromCompartmentIdsToCids.get(compartment));
		setReactionDirection(rid, cids, reversible);
		// TODO: wird das noch gebraucht? if (!modifiers) makeSpontan(rid);
	}

	/**
	 * appends the substrates of the reaction to the database content
	 * 
	 * @param token the substrate list token
	 * @param rid the referenced reaction
	 * @param mapFROMSubstanceIdsToSids
	 * @return
	 * @throws SQLException
	 * @throws IOException 
	 * @throws NoTokenException 
	 */
	private static TreeSet<Integer> writeReactionReactantsIntoDatabase(XmlToken token, int rid, TreeMap<String, Integer> mapFROMSubstanceIdsToSids) throws SQLException, IOException {
		TreeSet<Integer> databaseIds = new TreeSet<Integer>();
		if (!token.subtokenIterator().hasNext()) Tools.warn("no reactants found in reactant list!");
		for (Iterator<XmlToken> it = token.subtokenIterator(); it.hasNext();) {
			XmlToken subtoken = it.next();
			if (subtoken.instanceOf("speciesReference")) databaseIds.add(writeReactantSpeciesReferenceIntoDatabase(subtoken, rid, mapFROMSubstanceIdsToSids));
			else Tools.noteOnce("found " + subtoken.tokenClass() + " token in ListOfReactants");
		}
		return databaseIds;
	}

	/**
	 * writes the products of a certain reaction into the database
	 * 
	 * @param token the products list token FROM the sbml file
	 * @param rid the reaction the products are assigned with
	 * @param mapFROMSubstanceIdsToSids
	 * @return
	 * @throws SQLException
	 * @throws NoTokenException 
	 * @throws IOException 
	 */
	private static TreeSet<Integer> writeReactionProductsIntoDatabase(XmlToken token, int rid, TreeMap<String, Integer> mapFROMSubstanceIdsToSids) throws SQLException, NoTokenException, IOException {
		TreeSet<Integer> databaseIds = new TreeSet<Integer>();
		if (!token.subtokenIterator().hasNext()) Tools.warn("no products found in product list!");
		for (Iterator<XmlToken> it = token.subtokenIterator(); it.hasNext();) {
			XmlToken subtoken = it.next();
			if (subtoken.instanceOf("speciesReference")) databaseIds.add(writeProductSpeciesReferenceIntoDatabase(subtoken, rid, mapFROMSubstanceIdsToSids));
			else Tools.noteOnce("found " + subtoken.tokenClass() + " token in ListOfReactants");
		}
		return databaseIds;
	}

	/**
	 * writes the modifiers of the current reaction into the database
	 * 
	 * @param token the reaction modifiers token
	 * @param rid the database id of the current reaction
	 * @param mapFROMSubstanceIdsToSids
	 * @param fileHash the hash-code of the containing sbml file
	 * @param mapFromModelSubstanceToECNumber 
	 * @return the set of database ids of the given modifiers
	 * @throws SQLException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException 
	 */
	private static TreeSet<Integer> writeReactionModifiersIntoDatabase(XmlToken token, int rid, TreeMap<String, Integer> mapFROMSubstanceIdsToSids, MD5Hash fileHash, TreeMap<String, String> mapFromModelSubstanceToECNumber) throws SQLException, NoSuchAlgorithmException, IOException {
		TreeSet<Integer> databaseIds = new TreeSet<Integer>();
		for (Iterator<XmlToken> it = token.subtokenIterator(); it.hasNext();) {
			XmlToken subtoken = it.next();
			if (subtoken.instanceOf("modifierSpeciesReference")) writeModifierSpeciesReferenceIntoDatabase(fileHash, subtoken, rid, mapFROMSubstanceIdsToSids,mapFromModelSubstanceToECNumber);
			else Tools.noteOnce("found " + subtoken.tokenClass() + " token in ListOfReactants");
		}
		return databaseIds;
	}

	/**
	 * sets the reaction direction data for a certain reaction within the database
	 * 
	 * @param rid the database id of the reaction
	 * @param cids
	 * @param reversible the hash-value of the sbml file containing the reaction
	 * @throws SQLException
	 * @throws IOException 
	 */
	private static void setReactionDirection(int rid, Collection<Integer> cids, String reversible) throws SQLException, IOException {
		Statement st = InteractionDB.createStatement();
		String query;
		for (Iterator<Integer> cit = cids.iterator(); cit.hasNext();) {
			if (reversible == null || reversible.equals("false")) {
				query = "INSERT INTO reaction_directions VALUES (" + rid + "," + cit.next() + ",true,false)";
			} else query = "INSERT INTO reaction_directions VALUES (" + rid + "," + cit.next() + ",true	,true)";
			st.execute(query);
		}
		st.close();
	}

	/**
	 * writes a single substrate into the database
	 * 
	 * @param token the substrate (reactant) token FROM the sbml file
	 * @param rid the reaction assigned ti the substrate
	 * @param mapFromSubstanceIdsToSids
	 * @return
	 * @throws SQLException
	 * @throws IOException 
	 */
	private static int writeReactantSpeciesReferenceIntoDatabase(XmlToken token, int rid, TreeMap<String, Integer> mapFromSubstanceIdsToSids) throws SQLException, IOException {
		String ref = token.getValue("species");
		String stoichValue=token.getValue("stoichiometry");
		double stoich=1.0;
		if (stoichValue!=null) stoich=Double.parseDouble(stoichValue);
		Integer sid = mapFromSubstanceIdsToSids.get(ref);
		if (sid == null) throw new NullPointerException("no map entry for " + token);
		String query = "INSERT INTO substrates (sid,rid,stoich) VALUES(" + sid + ", " + rid + ","+stoich+")";
		Statement st = InteractionDB.createStatement();
		try {
			st.execute(query);
		} catch (SQLException e) {
			if (e.getMessage().contains("Duplicate key")) {
				query = "UPDATE substrates SET stoich=stoich+"+stoich+" WHERE sid=" + sid + " AND rid=" + rid;
				st.execute(query);
			} else {
				System.err.println(query);
				throw e;
			}
		}
		st.close();
		return sid;
	}

	/**
	 * writes a singe reaction product into the database
	 * 
	 * @param token the reaction product token FROM the sbml file
	 * @param rid the reaction to which the product shall be assigned
	 * @param mapFROMSubstanceIdsToSids a map from the substances ids used in the model file to their respective database ids
	 * @return the database id of the referenced substance
	 * @throws SQLException
	 * @throws IOException 
	 */
	private static int writeProductSpeciesReferenceIntoDatabase(XmlToken token, int rid, TreeMap<String, Integer> mapFROMSubstanceIdsToSids) throws SQLException, IOException {
		String ref = token.getValue("species");
		String stoichValue=token.getValue("stoichiometry");
		double stoich=1.0;
		if (stoichValue!=null) stoich=Double.parseDouble(stoichValue);
		Integer sid = mapFROMSubstanceIdsToSids.get(ref);
		if (sid==null) throw new NullPointerException("could not find substance id for "+ref+" in model!");
		String query = "INSERT INTO products (sid,rid,stoich) VALUES(" + sid + ", " + rid + ","+stoich+")";
		Statement st = InteractionDB.createStatement();
		try {
			st.execute(query);
		} catch (SQLException e) {
			if (e.getMessage().contains("Duplicate key")) {
				query = "UPDATE products SET stoich=stoich+"+stoich+" WHERE sid=" + sid + " AND rid=" + rid;
				st.execute(query);
			} else {
				System.err.println(query);
				throw e;
			}
		}
		st.close();
		return sid;
	}

	/**
	 * inserts the link FROM modifiers to their references species into the database
	 * 
	 * @param fileHash the hash-code of the containing file
	 * @param token the sbml token of the modifier
	 * @param rid the database id of the reaction whose modifiers are processed
	 * @param mapFromSubstanceIdsToSids
	 * @param mapFromModelSubstanceToECNumber 
	 * @return
	 * @throws SQLException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException 
	 */
	private static void writeModifierSpeciesReferenceIntoDatabase(MD5Hash fileHash, XmlToken token, int rid, TreeMap<String, Integer> mapFromSubstanceIdsToSids, TreeMap<String, String> mapFromModelSubstanceToECNumber) throws SQLException, NoSuchAlgorithmException, IOException {
		String ref = token.getValue("species");
		int sid = mapFromSubstanceIdsToSids.get(ref);
		
		String ec=mapFromModelSubstanceToECNumber.get(ref);
		ec=InteractionDB.dbString(ec);

		String query = "INSERT INTO enzymes VALUES(" + sid + ","+ec+",TRUE)";
		
		Statement st = InteractionDB.createStatement();
		try {
			st.execute(query);
		} catch (SQLException e) {
			if (!e.getMessage().contains("Duplicate key")) {
				System.err.println(query);
				throw e;
			}
		}
		try {
			query = "INSERT INTO reaction_enzymes VALUES(" + rid + "," + sid + ")";
			st.execute(query);
			st.close();
		} catch (SQLException e) {
			if (e.getMessage().contains("Duplicate key")) {
				Tools.warn("Modifier " + ref + " appears in reaction multiple times!");
			} else {
				System.err.println(query);
				throw e;
			}
		}
	}

	public static void loadSBMLFile(URL url, int group) throws NoSuchAlgorithmException, NoSuchAttributeException, IOException, NoTokenException, SQLException, NoSuchMethodException, DataFormatException {
		loadSBMLFile(url,group,null);
	  
  }

	/**
	 * writes the content of a local sbml file into the database
	 * @param file the file handle
	 * @param group the compartment group, in which the data will be stored
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchAttributeException
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws NoTokenException
	 * @throws SQLException
	 * @throws NoSuchMethodException
	 * @throws DataFormatException
	 */
	@SuppressWarnings("deprecation")
  public static void loadSBMLFile(File file, int group) throws NoSuchAlgorithmException, NoSuchAttributeException, MalformedURLException, IOException, NoTokenException, SQLException, NoSuchMethodException, DataFormatException {
		String name = file.getName();
		String directory=file.getParentFile().getName();
		String path=file.getPath();
		if (path.contains("GSMN") || path.contains("SBRGDB")) name=directory;
		if (path.contains("msb410") || path.contains("MG1655") || path.contains("GSMN051")) name+="/"+file.getName();
		name=name.replace(".xml", "").replace(".sbml", "");
		loadSBMLFile(file.toURL(),group,name);
  }

}
