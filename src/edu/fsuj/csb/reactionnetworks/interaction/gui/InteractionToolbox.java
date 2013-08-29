package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.AlreadyBoundException;
import java.sql.SQLException;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.batik.util.gui.xmleditor.XMLToken;

import edu.fsuj.csb.distributedcomputing.tools.Ports;
import edu.fsuj.csb.gui.HorizontalPanel;
import edu.fsuj.csb.gui.StatusPanel;
import edu.fsuj.csb.gui.VerticalPanel;
import edu.fsuj.csb.reactionnetworks.interaction.ActionHandler;
import edu.fsuj.csb.reactionnetworks.interaction.MappingPopupListener;
import edu.fsuj.csb.tools.configuration.Configuration;
import edu.fsuj.csb.tools.newtork.pagefetcher.PageFetcher;
import edu.fsuj.csb.tools.xml.NoTokenException;
import edu.fsuj.csb.tools.xml.Tools;
import edu.fsuj.csb.tools.xml.XMLReader;
import edu.fsuj.csb.tools.xml.XMLWriter;
import edu.fsuj.csb.tools.xml.XmlToken;

/**
 * InteractionToolbox is a java program that provides several Tools for metabolic network analyzation
 * @author Stephan Richter
 *
 */
public class InteractionToolbox extends JFrame implements ActionListener, ChangeListener {

	private static final long serialVersionUID = 7;
	//private static PrintStream out;
	private JButton calculateSeedsButton;
	private CompartmentsTab compartmentTab;
	private ActionHandler actionHandler;
	private JButton disconnectClients;
	private SubstancesTab substancesTab;
	private JButton calculateProductsButton,calcPotentialAdditionals,searchProcessors;
	private JTabbedPane taskTabs;
	private JButton optimizeSeeds,evolveSeeds;
	private JCheckBox onlyOdle,useMilp;
	private OptimizationParametersTab parametersTab;
	private StatusPanel statusPanel;
	private DatabasePane databasePane;
	private JTabbedPane taskResultPane;
	private VerticalPanel mainPanel;
	private ResultPanel resultPane;
	private HorizontalPanel taskPane;
	private VerticalPanel taskButtonPanel;
	private JButton findPath;
	private Configuration configuration;
	private MetabolicNetworkPanel networkPanel;
	private JButton storeButton;
	private JButton loadButton;
	/**
	 * create a new window instance
	 * @param splash
	 * @throws IOException
	 * @throws AlreadyBoundException 
	 * @throws NoTokenException 
	 * @throws SQLException 
	 * @throws DataFormatException 
	 * @throws UndispatchedException
	 */
	public InteractionToolbox() throws IOException, NoTokenException, AlreadyBoundException, SQLException, DataFormatException {
		super("Interaction Toolbox");
		configuration=new Configuration("InteractionTools");
		System.out.println("Creating GUI components:");
		createComponents();
		popUp();
	}

	/**
	 * make this Frame visible
	 */
	public void popUp() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}
	
	

	/**
	 * create and arrange all components
	 * @param splash 
	 * @throws IOException
	 * @throws AlreadyBoundException 
	 * @throws NoTokenException 
	 * @throws SQLException 
	 * @throws DataFormatException 
	 * @throws UndispatchedException
	 */
	private void createComponents() throws IOException, NoTokenException, AlreadyBoundException, SQLException, DataFormatException {

		
		//***** MAIN PANEL ********//
		mainPanel = new VerticalPanel();
		mainPanel.add(taskResultPane=taskResultPane());		
		mainPanel.add(statusPanel=new StatusPanel());		
		//statusPanel.setWidth(taskResultPane().getWidth());
		mainPanel.scale();
		add(mainPanel);
		pack();
	}

	private JTabbedPane taskResultPane() throws IOException, NoTokenException, AlreadyBoundException, SQLException, DataFormatException {
		JTabbedPane taskResultPane = new JTabbedPane();
		
		//***** TASKS *******//
		System.out.println("- creating panel for tasks...");
		taskResultPane.add(taskPane=createTaskPane(), "Tasks");

		//***** RESULTS *****//
		System.out.println("- creating panel for results...");
		taskResultPane.add(resultPane=createResultPane(), "Results");
		
		//****** NETWORK VIEW *****//
		System.out.println("- creating panel for network view...");
		taskResultPane.add(networkPanel=createNetworkPanel(),"Network View");
		networkPanel.addActionListener(this);
		compartmentTab.setNetworkViewer(networkPanel);
		
		//***** DATABASE **********//
		System.out.println("- creating panel for database actions...");
		databasePane = new DatabasePane();
		databasePane.addChangeListener(this);
		taskResultPane.add(databasePane,"Database");
		
		//***** INFO *********//
		taskResultPane.add(createInfoPanel(),"Information");
		return taskResultPane;
  }

	private MetabolicNetworkPanel createNetworkPanel() {
		MetabolicNetworkPanel result = new MetabolicNetworkPanel();
		result.setTextSize(16f);
	  return result;
  }

	/**
	 * creates a panel showing some short info related to the program
	 * @return the created panel
	 */
	private Component createInfoPanel() {
		VerticalPanel result = new VerticalPanel();
		JLabel text=new JLabel("<html>This is the <i>Interaction Toolbox</i><br/><br/><br/>Author: <b>Stephan Richter</b><br/>Bio System Analysis Group<br/><br/>Get more info at http://www.biosys.uni-jena.de<br/><br/>Bitte beachten sie bei den hiermit untersuchten Modellen, dass evtl. falsche Daten durch uneindeutige Annotation entstehen können.<br/><br/>(siehe Genome annotation errors in pathway databases due to semantic abiguity in partial EC numbers)");
		result.add(text);
		result.scale();
	  return result;
  }

	/**
	 * creates the ResultPanel, which is used to display calculation results
	 * @param width the width which the panel shall have
	 * @param height the height, which the panel shall have
	 * @return the newly creaed result panel
	 * @throws IOException 
	 */
	private ResultPanel createResultPane() throws IOException {
		ResultPanel resultPane=new ResultPanel();
		System.out.print("- trying to start server: ");
		int port=Integer.parseInt(configuration.get("port",""+Ports.registrationPort()));
		actionHandler = new ActionHandler(resultPane.getTree(),port);
		System.out.println();
		return resultPane;
	}

	/**
	 * creates the panel which is used to assemble tasks for the calculation
	 * @param width the width, which the task panel shall have
	 * @param height the height, which the task panel shall have
	 * @return the newly creates task panel
	 * @throws IOException
	 * @throws NoTokenException
	 * @throws AlreadyBoundException
	 * @throws SQLException
	 */
		
	private HorizontalPanel createTaskPane() throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		HorizontalPanel taskPane = new HorizontalPanel();
		taskPane.add(taskTabs=createTaskTabs());
		taskPane.add(taskButtonPanel=createTaskButtons());
		taskPane.scale();
		return taskPane;
	}

	/**
	 * creates the sidebar of the task panel, which holds the buttons to submit tasks
	 * @return the button panel after it has been created
	 */
	private VerticalPanel createTaskButtons() {
		VerticalPanel buttonPane = new VerticalPanel();
		VerticalPanel taskButtons=new VerticalPanel("Tasks");
		
		taskButtons.add(searchProcessors=new JButton("<html>Search for<br/>processing organisms"));
		searchProcessors.setToolTipText("<html>Search for organisms/compartments, which enable reactions that have at least one of the given substances as substrate</html>");
		searchProcessors.addActionListener(this);		
		
		VerticalPanel graphTasks=new VerticalPanel("Graph theoretic tasks");
		calculateProductsButton = new JButton("Calculate products");
		calculateProductsButton.setToolTipText("<html>This calculates the set of substances, which can be formed out of the given set of substances <i>directly or indirectly</i></html>");
		calculateProductsButton.addActionListener(this);
		graphTasks.add(calculateProductsButton);

		calcPotentialAdditionals=new JButton("<html>Calc additionals maximizing<br/>the set of products");
		calcPotentialAdditionals.setToolTipText("<html>Calculates the substances, which, thoghether with the given substances, maximize the scope (number of reachable substances) of the given substances</html>");
		calcPotentialAdditionals.addActionListener(this);
		graphTasks.add(calcPotentialAdditionals);
		taskButtons.add(graphTasks);
		
		VerticalPanel milpPanel=new VerticalPanel("Optimizations");
		calculateSeedsButton = new JButton("<html>Calculate Seeds<br/>with MILP (buggy)");
		calculateSeedsButton.setToolTipText("<html>This method should calculate the minimum sets of substances which can be supplied to form<ul><li>all Substances in the compartment (organism) <i>if no target substances are specified</i></li><li>for the specified target substances <i>otherwise</i></li></ul><font color=\"red\">not implemented, yet.</font></html>");
		calculateSeedsButton.addActionListener(this);
		milpPanel.add(calculateSeedsButton);
		
		optimizeSeeds=new JButton("<html>Calculate Flow<br/>Distributions for given<br/>Input/Output<br/>using linear programming");
		optimizeSeeds.setToolTipText("<html>Takes one substance list as targets<br/>and the other as \"desired nutrients\"<br/>and tries to optimize (maximize) flow<br/>towards targets and decomposition<br/>of the <i>desired nutrients</i> while keeping<br/>all other inflow reactions low.</html>");
		optimizeSeeds.addActionListener(this);
		milpPanel.add(optimizeSeeds);
		
		useMilp=new JCheckBox("<html>Use MILP<br/>(boolean switches;<br/>slower, more accurate)");
		milpPanel.add(useMilp);
		
		taskButtons.add(milpPanel);
		
		findPath=new JButton("<html>Find paths from<br/>substances-to-degrade<br/>to substances-to-produce");
		findPath.setToolTipText("<html>Tries to find connections between the substances to degrade<br/>and the substances that shall be built.");
		findPath.addActionListener(this);
		taskButtons.add(findPath);
		
		evolveSeeds=new JButton("<html>Calculate additionals<br/>with evolutionary Algorithm");
		evolveSeeds.setToolTipText("<html>Takes one substance list as targets<br/>and the other as \"desired nutrients\"<br/>and tries to optimize (maximize) flow<br/>towards targets and decomposition<br/>of the <i>desired nutrients</i> while keeping<br/>all other inflow reactions low.</html>");
		evolveSeeds.addActionListener(this);
		taskButtons.add(evolveSeeds);




		

		
		
		taskButtons.scale();
		buttonPane.add(taskButtons);
		
		VerticalPanel clientManager = new VerticalPanel("Client actions");
		
		disconnectClients = new JButton("Disconnect clients");
		disconnectClients.addActionListener(this);
		clientManager.add(disconnectClients);
		
		onlyOdle=new JCheckBox("only idle clients");
		onlyOdle.setSelected(true);
		clientManager.add(onlyOdle);
		
		
		clientManager.scale();
		buttonPane.add(clientManager);
		
		VerticalPanel storeGroup=new VerticalPanel("Settings storage");
		storeButton = new JButton("Store task settings");
		storeButton.addActionListener(this);
		storeGroup.add(storeButton);

		loadButton = new JButton("Load task settings");
		loadButton.addActionListener(this);
		storeGroup.add(loadButton);

		buttonPane.add(storeGroup);
		buttonPane.scale();
		return buttonPane;
	}

	/**
	 * creates the tabs of the task panel. those are: a tab for the compartment selection, a tab for the substance selection
	 * @param width the desired height for the tab panel
	 * @param height the desired height for the tab panel
	 * @return the newly created tab panel
	 * @throws IOException
	 * @throws NoTokenException
	 * @throws AlreadyBoundException
	 * @throws SQLException
	 */
	private JTabbedPane createTaskTabs() throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		taskTabs = new JTabbedPane();
		System.out.println("    |`- creating compartments panel");
		compartmentTab = new CompartmentsTab();
		System.out.println("    |`- creating substances panel");
		substancesTab = new SubstancesTab();
		substancesTab.addActionListener(this);
		System.out.println("    `-- creating parameters panel");		
		parametersTab = new OptimizationParametersTab();
		compartmentTab.getUserList().addChangeListener(substancesTab);
		compartmentTab.getUserList().addChangeListener(this);
		taskTabs.add(compartmentTab, "selectable Species");
		taskTabs.add(substancesTab,"selectable Substances");
		taskTabs.add(parametersTab,"Optimization parameters");
		return taskTabs;
	}

	/**
	 * creates and starts an instance of the interaction toolbox
	 * @param args
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws UnsupportedLookAndFeelException
	 * @throws IOException
	 * @throws NoTokenException
	 * @throws AlreadyBoundException
	 * @throws SQLException
	 * @throws DataFormatException 
	 */
	public static void main(String[] args) throws IOException, NoTokenException, AlreadyBoundException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, SQLException, DataFormatException  {
		parseArgs(args);
		Tools.disableLogging();
		InteractionSplash splash = new InteractionSplash();
		  (new Thread(splash)).start();
			UIManager.setLookAndFeel( "com.sun.java.swing.plaf.gtk.GTKLookAndFeel");			
	    try {
	      new InteractionToolbox();
	      splash.stop(1);
      } catch (SQLException e) {
      	splash.stop(1);
      	if (e.getMessage().contains("Unable to connect to database")){
      		System.err.println(e.getMessage()+" Is the mysql daemon running?");
      	} else throw e;
      }
	    //System.setOut(out);
	}
	
	/**
	 * set several variables by reading command line parameters
	 * 
	 * @param args the arguments passed to the main program
	 */
	private static void parseArgs(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith("--cachedir=")) PageFetcher.setCache(args[i].substring(11));
			if (args[i].equals("--help")) displayCLIoptions();
		}
	}

	private static void displayCLIoptions() {
		System.out.println("This is the InteractionToolbox for the InteractionDB.");
		System.out.println("This software is being developed by Stephan Richter.\n");
		System.out.println("Here are the commandline options:\n");
		System.out.println("--help\t\t\tdisplay this thext and exit");
		System.out.println("--cachedir=<directory>\tuse the given <directory>to store cached files");
		System.exit(0);
  }

	/**
	 * starts the calls to the different seed optimizers
	 * @param method if method is set to 1, the seed optimization will be performed by means of mixed linear integer programming, otherwise an evolutionary algorithm will be applied
	 * @throws IOException
	 */
	private void optimizeSeeds(int method) throws IOException {
		TreeSet<Integer> decompositionList = substancesTab.degradeList();
		if (decompositionList!=null && !decompositionList.isEmpty()){
			TreeSet<Integer> buildList = substancesTab.produceList();
			
			if (buildList!=null && !buildList.isEmpty()){
				if (method==1){
					actionHandler.optimizeSeeds(compartmentTab.getUserList().getListed(),decompositionList,buildList,substancesTab.ignoreList(),substancesTab.noOutflowList(),parametersTab.optimizationParameterSet(),parametersTab.optimizationParameterSet().skipUnbalancedReactions(),useMilp.isSelected());
				} else {
					actionHandler.evovleSeeds(compartmentTab.getUserList().getListed(), decompositionList, buildList,substancesTab.ignoreList());
				}
			}
		}
  }
	
	private void findPath() throws IOException {
		actionHandler.findPath(compartmentTab.getUserSpecies(),substancesTab.degradeList(),substancesTab.produceList(),substancesTab.ignoreList());
  }

	public void stateChanged(ChangeEvent arg0) {
		Object source=arg0.getSource();
		if (source instanceof CompartmentList) taskTabs.setSelectedIndex(0);
		if (source instanceof DatabasePane) reloadDatabase();
  }
	
	private void reloadDatabase() {
	  System.err.println("InteractionToolbox.reloadDatabase() not implemented,yet");
	  System.out.println("Database has been altered. Since automatic reload is not implemented, yet, you will have to restart the program.");
	  // TODO: implement
  }


	
	/**
	 * rescale application parts if necessary
	 * @see java.awt.Container#validate()	  
	 */
	public void validate() {
	  super.validate();
	  Dimension size = getSize();
	  size.width=size.width-20;
	  statusPanel.setWidth(size.width);
	  Dimension d=new Dimension(size.width,size.height-statusPanel.getHeight()-35);	  
	  taskResultPane.setPreferredSize(d);
	  databasePane.scaleTo(d);
	  resultPane.setSize(d);
	  databasePane.scale();
	  resultPane.scale();
	  
	  Dimension btnd = taskButtonPanel.getSize();
	  d=new Dimension(d.width-btnd.width-30,d.height-50);
	  taskTabs.setPreferredSize(d);
	  d=new Dimension(d.width-10,d.height-10);
	  compartmentTab.scaleScrollPanes(d);
	  compartmentTab.scale();
	  
	  substancesTab.scaleScrollPanes(d);
	  substancesTab.scale();
	  taskPane.scale();

	  
	  
	  mainPanel.scale();
	}
	
	
	private void loadTaskSettings() throws SecurityException, IOException, URISyntaxException, NoTokenException, SQLException {
		URL url=MappingPopupListener.askForFileName("File name");
		if (url==null) return;
		if (!url.toString().toUpperCase().endsWith(".XML")) url=new URL(url.toString()+".xml");
		System.out.println(url);
		XMLReader reader=new XMLReader(url);
		try {
			while (true){
				XmlToken token = reader.readToken();
				if (token.tokenClass().startsWith("SubstancesList")) substancesTab.loadState(token);
				if (token.tokenClass().equals("Compartments")) compartmentTab.loadState(token);
			}		
		} catch (NoTokenException nte){}
		reader.close();
  }
	
	private void storeTaskSettings() throws SecurityException, IOException, URISyntaxException {
		URL url=MappingPopupListener.askForFileName("File name");
		if (url==null) return;
		if (!url.toString().toUpperCase().endsWith(".XML")) url=new URL(url.toString()+".xml");
		XMLWriter writer=new XMLWriter(url);
		writer.write(compartmentTab);
		writer.write(substancesTab);
		writer.close();
  }

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent event) {
		Object source = event.getSource();
		Component frame=SwingUtilities.getRoot(this);
		frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		try {
			if (source == calculateSeedsButton) actionHandler.calcSeeds(compartmentTab.getUserList().getListed(),substancesTab.produceList(),substancesTab.ignoreList(),parametersTab.skipUnbalancedReactions(),useMilp.isSelected());
			if (source == calculateProductsButton){
						TreeSet<Integer> nutrients = substancesTab.degradeList();
						nutrients.addAll(substancesTab.ignoreList());
						actionHandler.calculateProducts(compartmentTab.getUserList().getListed(),nutrients);
			}
			if (source == disconnectClients) actionHandler.disconnect(onlyOdle.isSelected());
			if (source == calcPotentialAdditionals) actionHandler.calcPotentialAdditionals(compartmentTab.getUserList().getListed(),substancesTab.degradeList(),substancesTab.ignoreList());
			if (source == searchProcessors) actionHandler.searchProcessors(substancesTab.degradeList());
			if (source == optimizeSeeds) optimizeSeeds(1);
			if (source == evolveSeeds) optimizeSeeds(2);
			if (source == findPath) findPath();
			
			if (source==storeButton) storeTaskSettings();
			if (source==loadButton) loadTaskSettings();
			
			if (event.getActionCommand().equals("activate")){
				if (source == networkPanel) taskResultPane.setSelectedComponent(networkPanel);
				if (source == substancesTab) {
					taskTabs.setSelectedComponent(substancesTab);
					taskResultPane.setSelectedComponent(taskPane);
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
	    e.printStackTrace();
    } catch (URISyntaxException e) {
	    e.printStackTrace();
    } catch (NoTokenException e) {
	    e.printStackTrace();
    } catch (SQLException e) {
	    e.printStackTrace();
    }
		frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}


}
