package edu.fsuj.csb.reactionnetworks.databasefiller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.AlreadyBoundException;
import java.rmi.UnexpectedException;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.zip.DataFormatException;

import javax.naming.NameNotFoundException;
import javax.naming.directory.NoSuchAttributeException;

import edu.fsuj.csb.reactionnetworks.database.InteractionDB;
import edu.fsuj.csb.tools.newtork.pagefetcher.PageFetcher;
import edu.fsuj.csb.tools.organisms.Formula;
import edu.fsuj.csb.tools.urn.URN;
import edu.fsuj.csb.tools.urn.miriam.EnzymeUrn;
import edu.fsuj.csb.tools.urn.miriam.KeggGenomeUrn;
import edu.fsuj.csb.tools.urn.miriam.KeggPathwayUrn;
import edu.fsuj.csb.tools.urn.miriam.KeggReactionUrn;
import edu.fsuj.csb.tools.xml.NoTokenException;
import edu.fsuj.csb.tools.xml.ObjectComparator;
import edu.fsuj.csb.tools.xml.Tools;

/**
 * this little program creates a new interaction database, if it is not existant and fills it with data from kegg
 * 
 * @author Stephan Richter
 * 
 */
public class dbtool {

	private long startTime = 0;
	private static int keggProkaryotes;
	private static int keggEukaryotes;
	private static boolean skipClear,skipKegg,skipBiomodels,skipFiles,skipKeggPathways,skipKeggEnzymes,skipKeggCodes,skipKeggOrganisms,skipKeggCompounds,skipKeggSubstances,skipKeggReactions;
	private static String directory;
	private static boolean skipAsk;
	private static boolean clearDecisions;

	/**
	 * start program by creating a new dbtool instance
	 * 
	 * @param args
	 * @throws SQLException
	 * @throws IOException
	 * @throws NameNotFoundException
	 * @throws NoSuchMethodException
	 * @throws NoTokenException
	 * @throws NoSuchAlgorithmException
	 * @throws DataFormatException
	 * @throws NoSuchAttributeException
	 * @throws AlreadyBoundException 
	 * @throws ClassNotFoundException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, NameNotFoundException, SQLException, NoSuchMethodException, NoTokenException, NoSuchAlgorithmException, DataFormatException, NoSuchAttributeException, AlreadyBoundException, ClassNotFoundException, InterruptedException {
		parseArgs(args);
		new dbtool();
	}

	public static void run(String args) throws NameNotFoundException, NoSuchAlgorithmException, NoSuchAttributeException, IOException, SQLException, NoSuchMethodException, NoTokenException, DataFormatException, AlreadyBoundException, ClassNotFoundException, InterruptedException{
		main(args.split(" "));
	}
	
	/**
	 * set several variables by reading command line parameters
	 * 
	 * @param args the arguments passed to the main program
	 * @throws InterruptedException 
	 */
	private static void parseArgs(String[] args) throws InterruptedException {
		Tools.disableLogging();
		skipClear=false;
		skipKegg=false;
		skipBiomodels=false;
		skipFiles=false;
		skipKeggOrganisms=false;
		skipKeggPathways=false;
		skipKeggSubstances=false;
		skipKeggCodes=false;
		skipKeggCompounds=false;
		skipKeggReactions=false;
		skipKeggEnzymes=false;
		skipAsk=false;
		clearDecisions=false;
		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith("--cachedir=")) PageFetcher.setCache(args[i].substring(11));
			if (args[i].equals("--skip-question")) {
				Tools.warn("Will not ask before deletion of database content! Cancel within 20 seconds to avoid!");
				skipAsk=true;
				Thread.sleep(20000);
			}			
			if (args[i].equals("--clear-all")) {
				Tools.warn("Will also clear decision table.");
				clearDecisions=true;
			}
			if (args[i].equals("--skip-clear")) {
				Tools.note("Disabling deletion of database content.");
				skipClear=true;
			}			
			if (args[i].equals("--skip-kegg")) {
				Tools.note("Will skip all Kegg entries.");
				skipKegg=true;
			}
			if (args[i].startsWith("--folder=")) {
				directory=args[i].substring(9);
				Tools.note("Reading sbml files from "+directory+".");
			}
			if (args[i].equals("--skip-biomodels")) {
				Tools.note("Will skip biomodels.");
				skipBiomodels=true;
			}
			if (args[i].equals("--skip-files")) {
				Tools.note("Will skip SBML files.");
				skipFiles=true;
			}
			if (args[i].equals("--skip-kegg-orgs")) {
				Tools.note("Will skip Organism part of Kegg database.");
				skipKeggOrganisms=true;
			}
			if (args[i].equals("--skip-kegg-paths")) {
				Tools.note("Will skip pathway part of Kegg database.");
				skipKeggPathways=true;
			}
			if (args[i].equals("--skip-kegg-substances")) {
				Tools.note("Will skip subtance list of Kegg database.");
				skipKeggSubstances=true;
			}
			if (args[i].equals("--skip-kegg-codes")) {
				Tools.note("Will skip monosaccaride codes list of Kegg database.");
				skipKeggCodes=true;
			}
			if (args[i].equals("--skip-kegg-compounds")) {
				Tools.note("Will skip compound list of Kegg database.");
				skipKeggCompounds=true;
			}
			if (args[i].equals("--skip-kegg-reactions")) {
				Tools.note("Will skip reactions from Kegg database");
				skipKeggReactions=true;
			}
			if (args[i].equals("--skip-kegg-enzymes")) {
				Tools.note("Will skip enzyme entries of Kegg database.");
				skipKeggEnzymes=true;
			}
			if (args[i].equals("--help")) displayCLIoptions();
			if (args[i].equals("--verbose")) Tools.enableLogging();
		}
	}
	
	private static void displayCLIoptions() {
		System.out.println("This is the database filling tool for the InteractionDB.");
		System.out.println("This software is being developed by Stephan Richter.\n");
		System.out.println("Here are the commandline options:\n");
		System.out.println("--help\t\t\tdisplay this thext and exit");
		System.out.println("--cachedir=<directory>\tuse the given <directory>to store cached files");
		System.out.println("--verbose\t\t\tshow verbose output");
		System.out.println("--skip-clear\t\tdo not clear the database before reading in new data");
		System.out.println("--clear-all\t\twill also clear the decisions table");
		System.out.println("--skip-kegg\t\tdo not gather ANY data from kegg");
		System.out.println("--skip-biomodels\tdo not read the biomodels database");
		System.out.println("--skip-files\t\tdo not read sbml models from local files\n");
		System.out.println("the following options will only be used, if no --skip-kegg option is given:\n");
		System.out.println("--skip-kegg-orgs\tthis will skip the organism list of kegg, which in turn switches off the pathway and enzymes part. In other words: only Substances and Reactions will be read.");
		System.out.println("--skip-kegg-paths\twill omit the patways section, so no links between rections and organisms will be available.");
		System.out.println("--skip-kegg-enzymes\tBy activating this, you can prevent the tool from reading kegg enzyme data.");
		System.out.println("--skip-kegg-substances\tWith this option enabled, no substances and no reactions (which depend upon substances) will be read from kegg. This will render all Data from Kegg useless and is only for debuggin purposes.");
		System.out.println("--skip-kegg-codes\tWith this option enabled, no codes monosaccarde codes will be downloaded, and thus, glycans will not be resolved to formulas.");
		System.out.println("--skip-kegg-compounds\tWith this option enabled, no compounds will be read from kegg. This will render all Data from Kegg useless and is only for debuggin purposes.");
		System.out.println("--skip-kegg-reactions\tWith this option, you can skip the reaction-parsing part. Makes the data quite useless.");
		System.out.println("--skip-question\tWith this option, you can skip the confirmation question before erasing all database content.");
		System.exit(0);
  }
	/**
	 * creates a new program instance
	 * 
	 * @throws IOException
	 * @throws SQLException
	 * @throws NameNotFoundException
	 * @throws NoSuchMethodException
	 * @throws NoTokenException
	 * @throws NoSuchAlgorithmException
	 * @throws DataFormatException
	 * @throws NoSuchAttributeException
	 * @throws AlreadyBoundException 
	 * @throws ClassNotFoundException 
	 */
	public dbtool() throws IOException, SQLException, NameNotFoundException, NoSuchMethodException, NoTokenException, NoSuchAlgorithmException, DataFormatException, NoSuchAttributeException, AlreadyBoundException, ClassNotFoundException {
		Tools.startMethod("dbtool()");
		displayTimeStamp();
		String query="drop table abbrevations, compartment_pathways, compartments, dates, enzymes, enzymes_compartments, hierarchy, id_names, ids, names, products, reaction_directions, reaction_enzymes, reactions, substances, substrates,urls,urn_urls, urns";
		try {			
			if (!skipClear){
				if (!skipAsk){
					System.out.println("This will clear the database! Do you really want this?");
					BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
					String answer=in.readLine();
					if (!answer.toUpperCase().equals("YES")) System.exit(-1);
				}
			  if (clearDecisions) query=query+", decisions";
				InteractionDB.createStatement().execute(query);
			}
		} catch (SQLException e){
			Tools.warn("was not able to erase all tables: "+e.getMessage()+"\n\nQuery was: "+query);
		}
		InteractionDB.checkTables(); // assure, that required tables exist
		displayTimeStamp();		
		
		if (!skipKegg) readKeggContent(); // read kegg data // disabled, so i can not accidentally overwrite
		Integer lastKeggId = InteractionDB.getLastID();
		System.out.println("last kegg id: " + lastKeggId);
		
		if (!skipBiomodels) Biomodels.parse();
		Integer lastBiomodelsId = InteractionDB.getLastID();
		System.out.println("last kegg id: " + lastKeggId);
		System.out.println("last biomodels id: " + lastBiomodelsId);// */

		if (!skipFiles) parseSbmlFiles(getSbmlFileList(directory));
		Integer lastSbmlId = InteractionDB.getLastID();
		System.out.println("last kegg id: " + lastKeggId);
		System.out.println("last biomodels id: " + lastBiomodelsId);// */
		System.out.println("last sbml id: " + lastSbmlId);
		InteractionDB.printMissingAbbrevations();
		System.out.println();
		Tools.endMethod();
	}

	/**
	 * read all the sbml files in the list and parse their content into the database
	 * 
	 * @param sbmlFileList the list of sbml files
	 * @throws SQLException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchAttributeException
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws NoTokenException
	 * @throws NoSuchMethodException
	 * @throws DataFormatException
	 */
	private static void parseSbmlFiles(TreeMap<String, TreeSet<File>> sbmlFileList) throws SQLException, NoSuchAlgorithmException, NoSuchAttributeException, MalformedURLException, IOException, NoSuchMethodException, DataFormatException, NoTokenException {
		int count = 0;
		int number = 0;
		boolean skip = false;
		for (Iterator<Entry<String, TreeSet<File>>> it = sbmlFileList.entrySet().iterator(); it.hasNext();) {
			Entry<String, TreeSet<File>> entry = it.next();
			String category = entry.getKey();
			TreeSet<File> files = entry.getValue();
			System.out.println("reading files in " + category);
			int categoryId = InteractionDB.getOrCreateGroup(category);
			number += files.size();
			for (Iterator<File> fit = files.iterator(); fit.hasNext();) {
				File file = fit.next();
				System.out.println(((++count) * 100 / number) + "% - " + file.getPath());
				if (file.getPath().contains("Test")) skip = false;
				if (skip) continue;
				try {
					SBMLLoader.loadSBMLFile(file, categoryId);
				} catch (NoTokenException e) {
					if (!e.getMessage().contains("end of file reached")) throw e;
				} catch (FileNotFoundException e) {
					Tools.warnOnce(e.getMessage());
				}
			}

		}
		InteractionDB.setDateMark("Read SBML files");

	}

	/**
	 * get a list of all sbml files in a given directory
	 * 
	 * @param directory the directory, in which the files will be searched
	 * @return the list of sbml files found within the directory
	 */
	private static TreeMap<String, TreeSet<File>> getSbmlFileList(String directory) {
		File f = new File(directory);
		File[] folders = f.listFiles();
		TreeMap<String, TreeSet<File>> result = new TreeMap<String, TreeSet<File>>(ObjectComparator.get());

		for (int index = 0; index < folders.length; index++) {
			System.out.println(folders[index]);
			String category = folders[index].getName();
			result.put(category, getSbmlFileList(folders[index]));
		}
		return result;
	}

	private static TreeSet<File> getSbmlFileList(File f) {
		TreeSet<File> result = new TreeSet<File>(ObjectComparator.get());
		File[] files = f.listFiles();
		if (files==null) return result;
		for (int index = 0; index < files.length; index++) {
			File file = files[index];
			String name = file.getName().toLowerCase();
			if (name.endsWith(".sbml") || name.endsWith(".xml")) result.add(file);
			if (files[index].isDirectory()) result.addAll(getSbmlFileList(files[index]));
		}
		return result;
	}

	/**
	 * display time elapsed since first call of this method
	 */
	private void displayTimeStamp() {
		if (startTime == 0) startTime = System.currentTimeMillis(); // at first call: save system time
		long diff = System.currentTimeMillis() - startTime; // calculate time difference to first call
		System.out.println("Elapsed time: " + (diff / 1000) + "." + (diff % 1000) + " secs");
	}

	
	/*private static void getFormulaAndEvaluate(String code) throws SQLException, DataFormatException{
		int id=InteractionDB.getOrCreateIdFor(new KeggCompoundUrn(code), InteractionDB.SUBSTANCE);
		Substance s=DbSubstance.load(id);
		Formula formula=s.formula();
		System.out.println(replaceN(formula.toString()));		
	}*/



	/**
	 * reads various data from the kegg database
	 * 
	 * @throws IOException
	 * @throws SQLException
	 * @throws NameNotFoundException
	 * @throws NoSuchMethodException
	 * @throws DataFormatException
	 * @throws AlreadyBoundException 
	 * @throws NoTokenException 
	 * @throws ClassNotFoundException 
	 */
	@SuppressWarnings("unused")
	private void readKeggContent() throws IOException, SQLException, NameNotFoundException, NoSuchMethodException, DataFormatException, AlreadyBoundException, NoTokenException, ClassNotFoundException {
		Tools.startMethod("readKeggContent()");
		addKeggGroups();
		
		TreeMap<String, Integer> mappingFromKeggSubstanceIdsToDbIds = new TreeMap<String, Integer>(ObjectComparator.get());
		
		if (!skipKeggCodes) {
			InteractionDB.preprareAbbrevations(mappingFromKeggSubstanceIdsToDbIds);
			displayTimeStamp(); 
		}
		
		TreeMap<String, Integer> mappingFromKeggOrganismIdsToDbIds = null;
	
		if (!skipKeggOrganisms){
			mappingFromKeggOrganismIdsToDbIds = readKeggOrganisms();
			displayTimeStamp();
			if (!skipKeggPathways) readKeggPathways(mappingFromKeggOrganismIdsToDbIds.keySet());
			displayTimeStamp();
		}
		
		if (!skipKeggSubstances) {
			readKeggSubstances(mappingFromKeggSubstanceIdsToDbIds);
			displayTimeStamp();
		}
		if (!skipKeggOrganisms && !skipKeggEnzymes) readKeggEnzymes(mappingFromKeggOrganismIdsToDbIds);
		
		displayTimeStamp();
		
		if (!skipKeggSubstances && !skipKeggReactions) {
			TreeMap<String, Integer> mappingFromKeggReactionIdsToDbIds = readKeggReactions(mappingFromKeggSubstanceIdsToDbIds);
			displayTimeStamp();
		}
		// readReversibilityInformation(mappingFromKeggReactionIdsToDbIds, mappingFromKeggSubstanceIdsToDbIds, mappingFromKeggOrganismIdsToDbIds);
		displayTimeStamp();
		Tools.endMethod();
	}





	/**
	 * adds the basic Kegg Organism Groups to the database
	 * 
	 * @throws SQLException
	 */
	private void addKeggGroups() throws SQLException {
		keggEukaryotes = InteractionDB.getOrCreateGroup("Kegg Eukaryotes");
		keggProkaryotes = InteractionDB.getOrCreateGroup("Kegg Prokaryotes");
	}

	/**
	 * constructs a query, which is used to retrieve the list of compartments which - hold the current reaction - are assigned to the current pathway
	 * 
	 * @param pathwayId the kegg pathway code
	 * @param rid the MYSQL database intern id of the kegg reaction
	 * @return a set of compartments, which fulfill the above conditions
	 * @throws SQLException if any MYSQL acces violation occurs
	 */
	private TreeSet<Integer> getCompartmentsHoldingPathwayAndReaction(String pathwayId, int rid) throws SQLException {
		// in the following, a query is constructed, which is used to retrieve the list of compartments which
		// - hold the current reaction
		// - are assigned to the current pathway
		String selectAllCompartmentsOfThisReaction = "SELECT cid FROM enzymes_compartments WHERE eid IN (SELECT eid FROM reaction_enzymes WHERE rid=" + rid + ")";
		String selectAllCompartmentsHoldingThisPathway = "SELECT cid FROM compartment_pathways WHERE pid='" + pathwayId + "'";
		String selectAllCompartmentsHoldingThisPathwayAndThisReaction = selectAllCompartmentsOfThisReaction + " AND cid IN (" + selectAllCompartmentsHoldingThisPathway + ")";
		TreeSet<Integer> compartmentsHoldingThisPathwayAndThisReaction = new TreeSet<Integer>();
		ResultSet rs = InteractionDB.createStatement().executeQuery(selectAllCompartmentsHoldingThisPathwayAndThisReaction);
		while (rs.next())
			compartmentsHoldingThisPathwayAndThisReaction.add(rs.getInt(1));
		rs.close();
		return compartmentsHoldingThisPathwayAndThisReaction;
	}

	/**
	 * parses the kegg reaction_mapformula.lst file for additional information on reaction directions
	 * 
	 * @param mappingFromKeggReactionIdsToDbIds this map contains information on which kegg Reaction is associated with which key in our MYSQL database
	 * @param mappingFromKeggSubstanceIdsToDbIds this map holds the mapping from Kegg Compartments to our MSQL database substance ids
	 * @param mappingFromKeggOrganismIdsToDbIds this map maps kegg organisms to compartment ids in our MYSQL database
	 * @throws IOException if the reaction_mapformula.lst can not be read
	 * @throws SQLException if any database acces violation occurs
	 */
	@SuppressWarnings("unused")
	private void readReversibilityInformation(TreeMap<String, Integer> mappingFromKeggReactionIdsToDbIds, TreeMap<String, Integer> mappingFromKeggSubstanceIdsToDbIds, TreeMap<String, Integer> mappingFromKeggOrganismIdsToDbIds) throws IOException, SQLException {
		String[] reactionMapList = PageFetcher.fetchLines("ftp://ftp.genome.jp/pub/kegg/ligand/reaction/reaction_mapformula.lst"); // fetch the map file
		String[] parts = null; // will hold the different parts of the distinct file lines
		Statement statement = InteractionDB.createStatement();
		int lineCount = reactionMapList.length;
		for (int lineNumber = 0; lineNumber < lineCount; lineNumber++) { // parse the file line by line
			parts = reactionMapList[lineNumber].split(": "); // divide each line into reaction, pathway and formula part
			int rid = mappingFromKeggReactionIdsToDbIds.get(parts[0]); // get the reaction id by the kegg reaction code for the current line
			String pathwayId = parts[1]; // buffer the kegg pathway id
			String equation = parts[2]; // buffer the equation part
			TreeSet<Integer> compartmentsHoldingThisPathwayAndThisReaction = getCompartmentsHoldingPathwayAndReaction(pathwayId, rid);

			boolean reversed = testReactionInversion(rid, equation, mappingFromKeggSubstanceIdsToDbIds); // check, whether reaction is reversse to the saved direction
			String direction = null;
			for (Iterator<Integer> compartmentIterator = compartmentsHoldingThisPathwayAndThisReaction.iterator(); compartmentIterator.hasNext();) {
				int cid = compartmentIterator.next();
				if (!equation.contains("<=>")) { // if bi-directional: set both directions to true
					if ((equation.contains("=>") && !reversed) || (equation.contains("<=") && reversed)) direction = "forward";
					else direction = "backward";
					statement.execute("INSERT INTO reaction_directions (rid,cid," + direction + ") VALUES (" + rid + ", " + cid + ", true) ON DUPLICATE KEY UPDATE " + direction + "=true");
				} else statement.execute("INSERT INTO reaction_directions VALUES (" + rid + ", " + cid + ", true,true) ON DUPLICATE KEY UPDATE forward=true,backward=true"); // this has to be written, since another pathway with a directed duplicate reaction might otherwise falsefully set this reaction directed.
			}
			if (lineNumber % 100 == 0) {
				System.out.print((lineNumber * 100 / lineCount) + "%, ");
				displayTimeStamp();
			}
		}
		statement.execute("DELETE FROM reaction_directions WHERE forward=true AND backward=true"); // delete entries stating, that the reaction is reversible
		statement.close();
	}

	/**
	 * tests, whether the reaction as annoted in the reaction_mapformula.lst file is inverted relative to the reaction definition from the reaction file
	 * 
	 * @param rid the MYSQL database id of the reaction
	 * @param equation the equation from the reaction_mapformula.lst equation to be checked
	 * @param mappingFromKeggSubstanceIdsToDbIds maps for each substance code from kegg to the database internal id
	 * @return true, iff all substrates from the mapformula equation occur on the substrate site in the databases reaction definition and all products from the mapformula equation occur on the product site in the database
	 * @throws SQLException if any database acces violation happens
	 */
	private boolean testReactionInversion(int rid, String equation, TreeMap<String, Integer> mappingFromKeggSubstanceIdsToDbIds) throws SQLException {
		String parts[] = null;
		if (equation.contains(" <=> ")) return false; // a bidirectional reactionmay not be inverted
		if (equation.contains(" => ")) parts = equation.split(" => ");
		if (equation.contains(" <= ")) parts = equation.split(" <= ");

		String[] substrates = parts[0].split(" \\+ "); // split substrates part
		String[] products = parts[1].split(" \\+ "); // split products part
		TreeSet<Integer> substratIds = new TreeSet<Integer>();
		for (int i = 0; i < substrates.length; i++)
			substratIds.add(mappingFromKeggSubstanceIdsToDbIds.get(substrates[i]));
		TreeSet<Integer> productIds = new TreeSet<Integer>();
		for (int i = 0; i < products.length; i++)
			productIds.add(mappingFromKeggSubstanceIdsToDbIds.get(products[i]));

		// the actual test follows:
		String query = "SELECT COUNT(sid) FROM substrates WHERE rid=" + rid + " AND sid IN (" + substratIds.toString().substring(1).replace(']', ')'); // check, whether all substrates mentioned in the mapfile are encountered in the database reaction description
		Statement statement = InteractionDB.createStatement();
		ResultSet resultSet = statement.executeQuery(query);
		if (resultSet.next() && (resultSet.getInt(1) >= substrates.length)) { // if all substrates occur: // check, whether all products mentioned in the mapfile are encountered in the database reaction description
			query = "SELECT COUNT(sid) FROM products WHERE rid=" + rid + " AND sid IN (" + productIds.toString().substring(1).replace(']', ')');
			resultSet = statement.executeQuery(query);
			if (resultSet.next() && (resultSet.getInt(1) >= products.length)) return false; // all substrates present and all products present means the reaction is not inverted
		} // either not all substrates of the map file equation are present, or not all products => reaction should be inverted
		statement.close();
		return true; // not all substrates found or not all products found => must be inverted (?)
	}

	/**
	 * iterates through the organisms of the kegg database. for every organism, the pathway information is analyzed
	 * 
	 * @param keggOrganismCodes a set of kegg organism identifier strings
	 * @throws IOException if an error occurs while fetching the kegg organism pathway map
	 * @throws SQLException if any access violation occurs on accessing the mysql database
	 * @throws DataFormatException
	 */
	private void readKeggPathways(Set<String> keggOrganismCodes) throws IOException, SQLException, DataFormatException {
		int count = 0;
		int number = keggOrganismCodes.size();
		for (Iterator<String> it = keggOrganismCodes.iterator(); it.hasNext();) {
			count++;
			analyzeKeggPathwayMap(it.next());
			System.out.print((100 * count / number) + "% - ");
		}
		InteractionDB.setDateMark("Read KEGG pathways");

	}

	/**
	 * downloads the kegg pathway map file for a given organism an analyzes it: all the pathways listed are stored in the MYSQL database
	 * 
	 * @param keggOrganismCode a string containing an organism identifier as used by the kegg database
	 * @throws IOException if the pathway map file cannot be downloaded
	 * @throws SQLException if errors occur during access of the MYSQL database
	 * @throws DataFormatException
	 */
	private void analyzeKeggPathwayMap(String keggOrganismCode) throws IOException, SQLException, DataFormatException {
		KeggGenomeUrn orgUrn = new KeggGenomeUrn(keggOrganismCode); // miriam:kegg.genome:xxx
		Integer compartmentId = InteractionDB.getOrCreateIdFor(orgUrn,InteractionDB.COMPARTMENT);
		if (compartmentId != null) {
			String[] pathwayMap = orgUrn.fetchLines();
			System.out.print("collecting pathway information for organism " + keggOrganismCode + "...");
			Statement st = InteractionDB.createStatement();
			for (int i = 0; i < pathwayMap.length; i++) {
				if (pathwayMap[i].length() > 5) {
					String keggPathwayId = pathwayMap[i].substring(0, 5);
					try {
						Integer.parseInt(keggPathwayId);
						int k = pathwayMap[i].indexOf(">");
						int l = pathwayMap[i].indexOf("<", k);
						String name = pathwayMap[i].substring(k + 1, l);
						KeggPathwayUrn urn = new KeggPathwayUrn(keggOrganismCode + keggPathwayId);

						Integer pid = InteractionDB.createPathway(urn,name,orgUrn.url());
						InteractionDB.linkPathway(pid,compartmentId);
						Tools.indent("");

//						try {
//							st.execute("INSERT INTO compartment_pathways VALUES(" + compartmentId + ", " + pid + ")");
//						} catch (SQLException e) {
//							if (!e.getMessage().contains("Duplicate key")) throw e;
//						}
//						InteractionDB.insertName(pid, name);
					} catch (NumberFormatException nfe) {}
				}
			}
			st.close();
			System.out.println("done.");
		} else {
			System.err.println("No compartmentid for organism " + keggOrganismCode);
			System.exit(0);
		}
	}

	/**
	 * read the kegg species list
	 * 
	 * @throws IOException if species list can not be fetched or have a formatting error
	 * @throws SQLException
	 * @throws DataFormatException
	 */
	private TreeMap<String, Integer> readKeggOrganisms() throws IOException, SQLException, DataFormatException {
		URL listSource=new URL("http://www.genome.jp/kegg/catalog/org_list.html");
		String[] code = PageFetcher.fetchLines(listSource);
		System.out.print("Reading KEGG organism list...");
		TreeMap<String, Integer> mappingFromKeggIdsToDbIds = new TreeMap<String, Integer>(ObjectComparator.get());
		Statement st = InteractionDB.createStatement();

		int currentKeggGroup = keggEukaryotes;
		for (int index = 0; index < code.length; index++) {
			String line = code[index];
			String speciesName = null;
			if (line.contains("<td align=center><a href='/kegg-bin/show_organism?org=")) {

				speciesName = code[index + 1];
				int position = speciesName.indexOf("'>");
				if (position < 0) {
					position = speciesName.indexOf(">");
					if (position < 0) throw new IOException("file format error in dbtool.readKeggSpecies() line " + (index + 1) + " (" + speciesName + ")");
				}
				speciesName = speciesName.substring(position + 2);
				speciesName = speciesName.substring(0, speciesName.indexOf("<"));
				position = line.indexOf(">", 58) + 1;
				int position2 = line.indexOf("<", 58);
				String keggId = line.substring(position, position2).toLowerCase();
				String sql = null;
				try {
					KeggGenomeUrn urn = new KeggGenomeUrn(keggId);
					
					Integer cid=InteractionDB.createCompartment(speciesName,urn,currentKeggGroup,listSource);
				  Tools.indent("");

//					Integer cid = InteractionDB.getOrCreateCompartment(urn, currentKeggGroup,urn);
//					InteractionDB.insertName(cid, speciesName);
					mappingFromKeggIdsToDbIds.put(keggId, cid);
				} catch (SQLException e) {
					System.err.println(sql);
					e.printStackTrace();
					System.exit(0);
				}
			} else if (line.contains("Prokaryotes")) {
				currentKeggGroup = keggProkaryotes;
			}
		}
		st.close();
		InteractionDB.setDateMark("Read KEGG organisms");
		System.out.println("done, found " + mappingFromKeggIdsToDbIds.size() + " species entries.");
		return mappingFromKeggIdsToDbIds;
	}

	/**
	 * reads all kegg enzymes from the kegg website, writes the enzyme info to the local database and returns a set of ids of the enzymes
	 * 
	 * @return the set of the ids of the enzymes in the local database
	 * @throws IOException
	 */
	private Stack<String> getKeggEnzymeIds() throws IOException {
		StringBuffer data = PageFetcher.fetch("http://www.genome.jp/dbget-bin/www_bfind_sub?max_hit=nolimit&dbkey=enzyme&keywords=ec");
		Stack<String> result = new Stack<String>();
		int pos = 0;
		String keggEnzymeId = null;
		while ((pos = data.indexOf("?ec:", pos)) > 0) {
			pos += 4;
			int endpos = data.indexOf("\"", pos);
			keggEnzymeId = data.substring(pos, endpos);
			result.push(keggEnzymeId);
		}
		System.out.println("found " + result.size() + " enzymes.");
		return result;
	}

	/*
	 * reads all kegg reactions from the kegg website, writes the reaction info to the local database and returns a set of ids of the reactions
	 * 
	 * @return the set of the ids of the reactions in the local database
	 * 
	 * @throws IOException
	 */
	private Stack<String> getKeggReactionIds() throws IOException {
		StringBuffer data = PageFetcher.fetch("http://www.genome.jp/dbget-bin/www_bfind_sub?max_hit=nolimit&dbkey=reaction&keywords=R");
		Stack<String> result = new Stack<String>();
		int pos = 0;
		String keggReactionId = null;
		while ((pos = data.indexOf("rn:", pos)) > 0) {
			pos += 4;
			try {
				keggReactionId = data.substring(pos, pos + 5);
				Integer.parseInt(keggReactionId); // just to check, whether this is a number
				result.push("R" + keggReactionId);
			} catch (NumberFormatException e) {}
		}
		System.out.println("found " + result.size() + " reactions.");
		return result;
	}

	/**
	 * reads all kegg substances from the kegg website, writes the substance info to the local database and returns a set of ids of the substances
	 * 
	 * @return the set of the ids of the substances in the local database
	 * @throws IOException
	 */
	private Stack<String> getKeggSubstanceIds() throws IOException {
		Stack<String> result = new Stack<String>();
		
		/* Glykane werden vor den Compounds eingelesen, so dass die Compounds OBEN auf dem Stack liegen.
		 * Das bewirkt wiederum, dass die Substanzen zuerst eingelesen werden, so dass auch deren Formeln genutzt werden */
		int glycans = 0;
		StringBuffer data = PageFetcher.fetch("http://www.genome.jp/dbget-bin/www_bfind_sub?max_hit=nolimit&dbkey=glycan&keywords=G");
		int pos = 0;
		String keggId = null;
		while ((pos = data.indexOf("gl:", pos)) > 0) {
			pos += 4;
			try {
				keggId = data.substring(pos, pos + 5);
				Integer.parseInt(keggId); // just to check, whether this is a number
				result.push("G" + keggId);
				glycans++;
			} catch (NumberFormatException e) {}
		}
		Tools.indent("found " + glycans + " glycans.");
		
		if (!skipKeggCompounds){
			int compounds=0;
			data = PageFetcher.fetch("http://www.genome.jp/dbget-bin/www_bfind_sub?max_hit=nolimit&dbkey=compound&keywords=C");
			pos = 0;
			keggId = null;
			while ((pos = data.indexOf("cpd:", pos)) > 0) {
				pos += 5;
				try {
					keggId = data.substring(pos, pos + 5);
					Integer.parseInt(keggId); // just to check, whether this is a number
					result.push("C" + keggId);
					compounds++;
				} catch (NumberFormatException e) {}
			}
			Tools.indent("found " + compounds + " substances.");
		}

		return result;
	}

	/**
	 * reads information about substances in the kegg database
	 * @param source 
	 * 
	 * @return a mapping from kegg ids to their respecitve databse ids
	 * 
	 * @throws IOException
	 * @throws SQLException
	 * @throws NameNotFoundException
	 * @throws NoSuchMethodException
	 * @throws DataFormatException
	 * @throws AlreadyBoundException 
	 * @throws NoTokenException 
	 */
	private void readKeggSubstances(TreeMap<String, Integer> mappingFromKeggSubstanceIdsToDbIds) throws IOException, NameNotFoundException, SQLException, NoSuchMethodException, DataFormatException, AlreadyBoundException, NoTokenException {
		Stack<String> keggSubstanceIds = getKeggSubstanceIds();
		System.out.print("Reading substance list...");
		System.out.println("done, found " + keggSubstanceIds.size() + " substances.");
		int count = 0;
		System.out.print((100 * count / (keggSubstanceIds.size() + count)) + "% - ");
		while (!keggSubstanceIds.isEmpty()) {
			count++;			
			if (InteractionDB.parseSubstanceInfo(keggSubstanceIds, mappingFromKeggSubstanceIdsToDbIds)) System.out.print((100 * count / (keggSubstanceIds.size() + count)) + "% - ");
		}
		InteractionDB.setDateMark("Read KEGG substances");

	}

	/**
	 * reads the reaction description file from the kegg database and parses the reaction information (data is stored in the MYSQL database)
	 * @param source 
	 * 
	 * @return a a mapping from the kegg reaction identifiers to the corrosponding MYSQL database ids
	 * @throws IOException if the reaction data file cannot be downloaded
	 * @throws DataFormatException
	 * @throws SQLException 
	 * @throws NoTokenException 
	 * @throws AlreadyBoundException 
	 */
	private TreeMap<String, Integer> readKeggReactions(TreeMap<String, Integer> mappingFromKeggIdsToDbIds) throws IOException, DataFormatException, SQLException, AlreadyBoundException, NoTokenException {
		// String[] reactionInfos = PageFetcher.fetch("ftp://ftp.genome.jp/pub/kegg/ligand/reaction/reaction").toString().split("///");
		Stack<String> keggReactionIds = getKeggReactionIds();
		int count = 0;
		while (!keggReactionIds.isEmpty()) {
			System.out.print((100 * (++count) / (keggReactionIds.size() + count)) + "% - ");
			String kid = keggReactionIds.peek();
			if (mappingFromKeggIdsToDbIds.containsKey(kid)) {
				System.out.println(kid + " already analyzed");
				keggReactionIds.pop();
			} else {
				try {
					if (kid.contains("10121")) {
						Tools.resetIntendation();
						Tools.enableLogging();
						
					}
					parseReactionInfo(keggReactionIds, mappingFromKeggIdsToDbIds);
					if (kid.contains("10121")) Tools.disableLogging();
					Tools.indent("");
				} catch (SQLException e) {
					e.printStackTrace();
				} catch (NameNotFoundException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				}
			}
		}
		InteractionDB.setDateMark("Read KEGG reactions");
		return mappingFromKeggIdsToDbIds;
	}

	/**
	 * reads various data from the kegg database
	 * 
	 * @param mappingFromKeggOrganismIdsToDbIds
	 * @param source 
	 * 
	 * @throws IOException
	 * @throws DataFormatException
	 * @throws SQLException 
	 */
	private void readKeggEnzymes(TreeMap<String, Integer> mappingFromKeggOrganismIdsToDbIds) throws IOException, DataFormatException, SQLException {
		System.out.println("Reading enzyme list...");
		// String[] enzymeInfos = PageFetcher.fetch("ftp://ftp.genome.jp/pub/kegg/ligand/enzyme/enzyme").toString().split("///"); // get the kegg enzyme list
		Stack<String> enzymeIds = getKeggEnzymeIds();
		int count = 0;
		while (!enzymeIds.isEmpty()) {
			count++;
			System.out.print((100 * count / (count + enzymeIds.size())) + "% - ");
			try {
				parseEnzymeInfo(enzymeIds, mappingFromKeggOrganismIdsToDbIds);
				Tools.indent("");

			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		InteractionDB.setDateMark("Read KEGG enzymes");

	}

	private TreeSet<String> findGenes(String source) throws UnexpectedException {
		int start = source.indexOf("<nobr>");
		TreeSet<String> result = Tools.StringSet();
		while (start > 0) {
			int end = source.indexOf(":", start);
			if (end < 0) throw new UnexpectedException("Unexpected end of line ('" + source + "') while looking for ':'");
			result.add(source.substring(start + 6, end).toLowerCase());
			start = source.indexOf("<nobr>", end);
		}
		return result;
	}

	/**
	 * parses the information text belonging to an enzyme
	 * 
	 * @param mappingFromKeggOrganismIdsToDbIds
	 * @param source 
	 * 
	 * @param enzymeIds the text token from the enzyme description file
	 * @throws SQLException if any database related error occurs
	 * @throws IOException if data file can not be read from remote host
	 * @throws DataFormatException
	 */
	private void parseEnzymeInfo(Stack<String> unexploredEnzymeIds, TreeMap<String, Integer> mappingFromKeggOrganismIdsToDbIds) throws SQLException, IOException, DataFormatException {
		String enzymeCode = unexploredEnzymeIds.pop();
		EnzymeUrn enzymeUrn = new EnzymeUrn(enzymeCode);
		String description = PageFetcher.fetch(enzymeUrn.keggUrl()).toString();
		System.out.print("parsing " + enzymeCode + "...");
		if (description.length() < 5) return;
		String[] lines = description.split("\n");

		TreeSet<String> names = Tools.StringSet();
		TreeSet<Integer> orgIds = new TreeSet<Integer>();
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].contains("<nobr>Name</nobr>")) {
				while (!lines[++i].contains("</div>")) {
					String name = Tools.removeHtml(lines[i]);
					names.add(name.endsWith(";") ? (name.substring(0, name.length() - 1)) : name); // only remove end-of-line semicolons, preserve in-string semicolons
				}
			}

			if (lines[i].contains("<nobr>Genes</nobr>")) {
				TreeSet<String> orgList = findGenes(lines[++i]);
				for (Iterator<String> it = orgList.iterator(); it.hasNext();) {
					String org = it.next();
					Integer orgid = mappingFromKeggOrganismIdsToDbIds.get(org);
					if (orgid == null) throw new UnexpectedException("Unexpectedly, we found no database id for the organism '" + org + "'");
					orgIds.add(orgid);
				}
				if (lines[++i].length() > 3) throw new UnexpectedException("Unexpected input '" + lines[i] + "' in line " + i + " of " + enzymeUrn.keggUrl());
			}
// TODO: read "Other DBs" links
		}

		int enzymeDBId = InteractionDB.createEnzyme(names, enzymeCode, null, enzymeUrn, enzymeUrn.keggUrl());
		InteractionDB.linkOrganismsToEnzyme(orgIds,enzymeDBId);
		
//		Statement statement = InteractionDB.createStatement();
//		for (Iterator<Integer> it = orgIds.iterator(); it.hasNext();) {
//			int organismDBId = it.next();
//			String query = "INSERT INTO enzymes_compartments VALUES (" + organismDBId + "," + enzymeDBId + ")";
//			try {
//				statement.execute(query);
//			} catch (SQLException e) {
//				if (e.getMessage().contains("Duplicate key")) continue;
//				System.err.println(query);
//				throw e;
//			}
//		}
//		for (Iterator<String> name = names.iterator(); name.hasNext();)
//			InteractionDB.insertName(enzymeDBId, name.next());
//		statement.close();
		System.out.println("done");
	}

	/**
	 * parses a single reaction data entry (token) from the reaction description file
	 * 
	 * @param token the description of a single reaction
	 * @param mappingFromKeggIdsToDbIds a mapping, which is filled with references from the Kegg reaction identifiers to the MYSQL reaction ids
	 * @param source 
	 * @throws SQLException if errors occur while writing reaction information to the MYSQL database
	 * @throws IOException
	 * @throws NoSuchMethodException
	 * @throws NameNotFoundException
	 * @throws DataFormatException
	 * @throws NoTokenException 
	 * @throws AlreadyBoundException 
	 */
	private void parseReactionInfo(Stack<String> unexploredKeggIds, TreeMap<String, Integer> mappingFromKeggIdsToDbIds) throws SQLException, IOException, NoSuchMethodException, NameNotFoundException, DataFormatException, AlreadyBoundException, NoTokenException {
		String keggReactionId = unexploredKeggIds.pop();
		Tools.startMethod("parseReactionInfo("+keggReactionId+", ...)");
		TreeSet<URN> urns = new TreeSet<URN>(ObjectComparator.get());

		
		KeggReactionUrn reactionUrn = new KeggReactionUrn(keggReactionId);
		urns.add(reactionUrn);
		String description = reactionUrn.fetch();

		if (description.length() < 5) {
			Tools.endMethod();
			return;
		}
		if (!Tools.logging()) System.out.print("parsing " + keggReactionId + "...");

		/************ the following lines of code are fixes for some special entries *******************************/

		if (description.contains("No such database")) {
			throw new IllegalArgumentException("was not able to create valid url from content of " + keggReactionId);
		}

		if (description.toLowerCase().contains("obsolete")) {
			throw new IllegalArgumentException("found \"obsolete\" keyword in file! going to sleep");
		}

		/************** end of fixes *****************************/

		String[] lines = description.split("\n");

		TreeSet<String> names = Tools.StringSet();
		TreeSet<String> synonyms = Tools.StringSet();
		TreeSet<String> enzymes = Tools.StringSet();
		TreeMap<Integer, Integer> substrates = null; // mapping from the substrate database ids to their stoichiometry
		TreeMap<Integer, Integer> products = null;
		boolean spontan = false;

		for (int i = 0; i < lines.length; i++) {

			if (lines[i].contains("<nobr>Name</nobr>")) {
				while (!lines[++i].contains("</div>")) {
					String name = Tools.removeHtml(lines[i]);
					name=name.endsWith(";") ? (name.substring(0, name.length() - 1)) : name; // only remove end-of-line semicolons, preserve in-string semicolons
					Tools.indent("add name "+name);
					names.add(name);
				}
			}

			if (lines[i].contains("<nobr>Definition</nobr>") && names.isEmpty()) {
				while (!lines[++i].contains("</div>")) {
					String definition = Tools.removeHtml(lines[i]);
					String name=definition.endsWith(";") ? (definition.substring(0, definition.length() - 1)) : definition; // only remove endo-of-line semicolons, preserve in-string semicolons
					Tools.indent("add name "+name+ "(from definition)");
					names.add(name);
				}
			}

			if (lines[i].contains("<nobr>Comment</nobr>")) {
				String comment = "";
				while (!lines[++i].contains("</tr>"))
					comment += Tools.removeHtml(lines[i]) + "\n";
				if (comment.toLowerCase().contains("spontan")) spontan = true;
			}

			if (lines[i].contains("<nobr>Equation</nobr>")) {
				String equation = Tools.removeHtml(lines[++i]);
				Tools.indent("equation: "+equation);
				substrates = new TreeMap<Integer, Integer>();
				products = new TreeMap<Integer, Integer>();
				String[] equationParts = equation.split("<=>");
				String[] subs = equationParts[0].trim().split(" \\+ ");
				String[] prods = equationParts[1].trim().split(" \\+ ");
				Integer substanceDbId = null;

				for (int k = 0; k < subs.length; k++) {
					String dummy = replaceN(subs[k]).replace("(", "").replace(")", "");
					int spacePos = dummy.indexOf(" ");
					if (spacePos < 0) { // no stoichiometric coefficients
						String substanceKeggId = dummy.substring(0, 6);
						Tools.indent("substrate: "+substanceKeggId);
//						substanceDbId = mappingFromKeggIdsToDbIds.get(substanceKeggId);
						substanceDbId = InteractionDB.readIdFor(InteractionDB.urnForComponent(substanceKeggId));
						if (substanceDbId == null) {
							unexploredKeggIds.push(substanceKeggId);
							while (!unexploredKeggIds.isEmpty()) InteractionDB.parseSubstanceInfo(unexploredKeggIds, mappingFromKeggIdsToDbIds);
							substanceDbId = InteractionDB.readIdFor(InteractionDB.urnForComponent(substanceKeggId));
							if (substanceDbId == null) {
								Tools.warn("No information found for " + substanceKeggId + ", one of the substrates of reaction " + keggReactionId);
								continue;
							}
						}
						if (substrates.containsKey(substanceDbId)){
							substrates.put(substanceDbId, substrates.get(substanceDbId)+1);
						} else substrates.put(substanceDbId, 1);
					} else { // stoichiometric coefficients present
						String substanceKeggId = dummy.substring(spacePos + 1, spacePos + 7);
						Tools.indent("substrate: "+substanceKeggId);
//						substanceDbId = mappingFromKeggIdsToDbIds.get(substanceKeggId);
						substanceDbId = InteractionDB.readIdFor(InteractionDB.urnForComponent(substanceKeggId));
						if (substanceDbId == null) {
							unexploredKeggIds.push(substanceKeggId);
							while (!unexploredKeggIds.isEmpty()) InteractionDB.parseSubstanceInfo(unexploredKeggIds, mappingFromKeggIdsToDbIds);
							substanceDbId = InteractionDB.readIdFor(InteractionDB.urnForComponent(substanceKeggId));
							if (substanceDbId == null) {
								Tools.warn("No information found for " + substanceKeggId + ", one of the substrates of reaction " + keggReactionId);
								continue;
							}
						}
						if (substrates.containsKey(substanceDbId)){
							substrates.put(substanceDbId, substrates.get(substanceDbId)+Integer.parseInt(dummy.substring(0, spacePos)));
						} else substrates.put(substanceDbId, Integer.parseInt(dummy.substring(0, spacePos)));
					}
				}

				for (int k = 0; k < prods.length; k++) {
					String dummy = replaceN(prods[k]).replace("(", "").replace(")", "");
					int spacePos = dummy.indexOf(" ");
					if (spacePos < 0) {
						String substanceKeggId = dummy.substring(0, 6);
						Tools.indent("product: "+substanceKeggId);
//						substanceDbId = mappingFromKeggIdsToDbIds.get(substanceKeggId);
						substanceDbId = InteractionDB.readIdFor(InteractionDB.urnForComponent(substanceKeggId));
						if (substanceDbId == null) {
							unexploredKeggIds.push(substanceKeggId);
							while (!unexploredKeggIds.isEmpty()) InteractionDB.parseSubstanceInfo(unexploredKeggIds, mappingFromKeggIdsToDbIds);
							substanceDbId = InteractionDB.readIdFor(InteractionDB.urnForComponent(substanceKeggId));
							if (substanceDbId == null) {
								Tools.warn("No information found for " + substanceKeggId + ", one of the products of reaction " + keggReactionId);
								continue;
							}
						}
						if (products.containsKey(substanceDbId)){
							products.put(substanceDbId, products.get(substanceDbId)+1);
						} else products.put(substanceDbId, 1);
					} else {
						String substanceKeggId = dummy.substring(spacePos + 1, spacePos + 7);
						Tools.indent("product: "+substanceKeggId);
//						substanceDbId = mappingFromKeggIdsToDbIds.get(substanceKeggId);
						substanceDbId = InteractionDB.readIdFor(InteractionDB.urnForComponent(substanceKeggId));
						if (substanceDbId == null) {
							unexploredKeggIds.push(substanceKeggId);
							while (!unexploredKeggIds.isEmpty()) InteractionDB.parseSubstanceInfo(unexploredKeggIds, mappingFromKeggIdsToDbIds);
							substanceDbId = InteractionDB.readIdFor(InteractionDB.urnForComponent(substanceKeggId));
							if (substanceDbId == null) {
								Tools.warn("No information found for " + substanceKeggId + ", one of the products of reaction " + keggReactionId);
								continue;
							}
						}
						if (products.containsKey(substanceDbId)){
							products.put(substanceDbId, products.get(substanceDbId)+Integer.parseInt(dummy.substring(0, spacePos)));
						} else products.put(substanceDbId, Integer.parseInt(dummy.substring(0, spacePos)));
					}
				}
			}
			if (lines[i].contains("<nobr>Remark</nobr>")) {
				String remark = Tools.removeHtml(lines[++i]);
				if (remark.toLowerCase().contains("spontan")) {
					System.out.println("Remark: " + remark);
					spontan = true;
					System.out.println("SPONTAN!");
					System.exit(0);
				}

				if (remark.contains("Same as")) {
					String[] dummy = remark.replace("Same as:&nbsp;", "").split(" ");
					for (int k = 0; k < dummy.length; k++) {
						URN urn = InteractionDB.urnForComponent(dummy[k]);
						if (urn != null) {
							Tools.indent("same as "+dummy[k]);
							synonyms.add(dummy[k]);
							urns.add(urn);
						}
					}
				}
			}
			if (lines[i].contains("<nobr>Enzyme</nobr>")) {
				lines[++i] = Tools.removeHtml(lines[i]);
				while (lines[i].contains("  "))
					lines[i] = lines[i].replace("  ", " ");
				String[] codes = lines[i].split(" ");
				for (int k = 0; k < codes.length; k++){
					Tools.indent("Enzyme: "+codes[k]);
					enzymes.add(codes[k]);
				}
			}
		}
		if (names.isEmpty()) throw new NameNotFoundException();

		if (synonyms.size() > 5) {
			Tools.warn("reaction with numerous synonyms. check: " + synonyms + " (" + synonyms.size() + ")");
			System.exit(0);
		}
		for (Iterator<String> it = synonyms.iterator(); it.hasNext();)
			unexploredKeggIds.push(it.next());

		int rid = InteractionDB.createReaction(names,urns,spontan,reactionUrn.url());
		
		//int rid = InteractionDB.getOrCreateReactionId(urns,reactionUrn);
		mappingFromKeggIdsToDbIds.put(keggReactionId, rid);

		/*Statement st = InteractionDB.createStatement();

		ResultSet rs = st.executeQuery("SELECT name FROM names WHERE id=" + rid); // get name of the substance
		while (rs.next())
			names.remove(rs.getString(1)); // we must'n add names which are already in the database, so remove those names from the set of names to add
		rs.close();

		for (Iterator<String> name = names.iterator(); name.hasNext();)
			InteractionDB.insertName(rid, name.next()); // add new names only
*/


		for (Iterator<Entry<Integer, Integer>> it = substrates.entrySet().iterator(); it.hasNext();) {
			Entry<Integer, Integer> reactant = it.next();			
			InteractionDB.addSubstrateToReaction(rid,reactant.getKey(),reactant.getValue());
		}
		for (Iterator<Entry<Integer, Integer>> it = products.entrySet().iterator(); it.hasNext();) {
			Entry<Integer, Integer> reactant = it.next();
			InteractionDB.addProductToReaction(rid,reactant.getKey(),reactant.getValue());
		}
		if (!enzymes.isEmpty())	InteractionDB.linkEnzymesToReaction(rid,enzymes);
		if (!Tools.logging())System.out.println("done.");
	}

	private static String replaceN(String input) {
		try {
			do {
				input=input.replace("n1","n").replace("n2", "m");
				int npos=Math.max(input.indexOf("n"),input.indexOf("m"));
				if (npos==-1) break;
				int pos=npos;
				while (pos>0 && Character.isDigit(input.charAt(pos-1))) pos--;
				int factor=1;
				if (pos>-1 && pos<npos) factor=Integer.parseInt(input.substring(pos,npos));
				input=input.substring(0,pos)+(factor*Formula.nReplacement)+input.substring(npos+1);
			} while (true);
			
			int operatorPos=-1;
			int plusPos=input.indexOf('+');
			int minusPos=input.indexOf('-');
			boolean addition=true;
			if (plusPos>-1){
				if (minusPos>-1) { // both operators present
					operatorPos=Math.min(plusPos, minusPos);
					addition=input.charAt(operatorPos)=='+';
				} else operatorPos=plusPos; // only plus present		
			} else if (minusPos>-1){
				operatorPos=minusPos; // only minus present
				addition=false;
			}
		
			while (operatorPos>0){
				int index1=operatorPos;			
				while (input.charAt(index1-1)==' ') index1--;
				while (index1>0 && Character.isDigit(input.charAt(index1-1))) index1--;			
				String dummy=input.substring(index1, operatorPos).trim();
				int operand1=Integer.parseInt(dummy);
			
				int index2=operatorPos+1;
				while (input.charAt(index2)==' ') index2++;
				while (index2<input.length() && Character.isDigit(input.charAt(index2))) index2++;
				dummy=input.substring(operatorPos+1, index2).trim();
				int operand2=Integer.parseInt(dummy);
			
				String key=input.substring(index1,index2);
				int sum=addition?operand1+operand2:operand1-operand2;
				input=input.replace(key, ""+sum);
			
				operatorPos=-1;
				plusPos=input.indexOf('+');
				minusPos=input.indexOf('-');
				addition=true;
				if (plusPos>-1){
					if (minusPos>-1) { // both operators present
						operatorPos=Math.min(plusPos, minusPos);
						addition=input.charAt(operatorPos)=='+';
					} else operatorPos=plusPos; // only plus present		
				} else if (minusPos>-1){
					operatorPos=minusPos; // only minus present
					addition=false;
				}
			}
			return input;
		} catch (NumberFormatException nfe){
			Tools.warn("dbtool.replaceN("+input+") failed!");
			throw nfe;
		}
  }





	/**
	 * removes all content, which contains ids larger than the given one, from the database
	 * 
	 * @param lastRemainingId the last id, which will be not deleted
	 * 
	 * @throws SQLException
	 */
	public static void cleanDb(int lastRemainingId) throws SQLException {
		Statement st = InteractionDB.createStatement();
		Vector<String> queries = new Vector<String>();
		queries.add("DELETE FROM compartment_pathways WHERE cid>" + lastRemainingId + " OR pid>" + lastRemainingId);
		queries.add("DELETE FROM compartments WHERE id>" + lastRemainingId);
		queries.add("DELETE FROM enzymes WHERE id>" + lastRemainingId);
		queries.add("DELETE FROM enzymes_compartments WHERE cid>" + lastRemainingId + " OR eid>" + lastRemainingId);
		queries.add("DELETE FROM hierarchy WHERE contained>" + lastRemainingId + " OR container>" + lastRemainingId);
		queries.add("DELETE FROM ids WHERE id>" + lastRemainingId);
		queries.add("DELETE FROM names WHERE id>" + lastRemainingId);
		queries.add("DELETE FROM products WHERE sid>" + lastRemainingId + " OR rid>" + lastRemainingId);
		queries.add("DELETE FROM reaction_directions WHERE rid>" + lastRemainingId + " OR cid>" + lastRemainingId);
		queries.add("DELETE FROM reaction_enzymes WHERE rid>" + lastRemainingId + " OR eid>" + lastRemainingId);
		queries.add("DELETE FROM reactions WHERE id>" + lastRemainingId);
		queries.add("DELETE FROM substances WHERE id>" + lastRemainingId);
		queries.add("DELETE FROM substrates WHERE sid>" + lastRemainingId + " OR rid>" + lastRemainingId);
		queries.add("DELETE FROM unifications WHERE id>" + lastRemainingId + " OR id2>" + lastRemainingId);
		queries.add("DELETE FROM urns WHERE id>" + lastRemainingId);
		queries.add("ALTER TABLE ids AUTO_INCREMENT=" + (lastRemainingId + 1));
		try {
			while (!queries.isEmpty()) {
				System.out.println("executing " + queries.firstElement());
				st.execute(queries.firstElement());
				queries.remove(0);
			}
		} catch (SQLException e) {
			System.err.println(queries.firstElement());
			throw e;
		}
	}
}
