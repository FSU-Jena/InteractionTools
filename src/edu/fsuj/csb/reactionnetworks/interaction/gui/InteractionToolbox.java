package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
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

import edu.fsuj.csb.distributedcomputing.tools.Ports;
import edu.fsuj.csb.gui.HorizontalPanel;
import edu.fsuj.csb.gui.StatusPanel;
import edu.fsuj.csb.gui.VerticalPanel;
import edu.fsuj.csb.reactionnetworks.interaction.ActionHandler;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.ParameterSet;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.SubstanceSet;
import edu.fsuj.csb.tools.configuration.Configuration;
import edu.fsuj.csb.tools.newtork.pagefetcher.PageFetcher;
import edu.fsuj.csb.tools.xml.NoTokenException;
import edu.fsuj.csb.tools.xml.Tools;

/**
 * InteractionToolbox is a java program that provides several Tools for metabolic network analyzation
 * @author Stephan Richter
 *
 */
public class InteractionToolbox extends JFrame implements ActionListener, ChangeListener {

	private static final long serialVersionUID = 7;
	//private static PrintStream out;
	private CompartmentsTab compartmentTab;
	private ActionHandler actionHandler;
	private JButton disconnectClients;
	private SubstancesTab substancesTab;
	private JButton calculateProductsButton,calcPotentialAdditionals,searchProcessors,fluxBalanceAnalysis;
	private JTabbedPane taskTabs;
	private JCheckBox onlyOdle,skipUnbalancedReactions;
	private OptimizationParametersTab parametersTab;
	private StatusPanel statusPanel;
	private DatabasePane databasePane;
	private JTabbedPane taskResultPane;
	private VerticalPanel mainPanel;
	private ResultPanel resultPane;
	private HorizontalPanel taskPane;
	private VerticalPanel taskButtonPanel;
	private Configuration configuration;
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
		//System.setOut(out);
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
		
		//***** DATABASE **********//
		System.out.println("- creating panel for database actions...");
		databasePane = new DatabasePane();
		databasePane.addChangeListener(this);
		taskResultPane.add(databasePane,"Database");
		
		//***** INFO *********//
		taskResultPane.add(createInfoPanel(),"Information");
		return taskResultPane;
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
	
	public VerticalPanel graphTaskButtons(){
		VerticalPanel graphTaskButtonPanel=new VerticalPanel("Graph theoretic tasks");
		calculateProductsButton = new JButton("Calculate products");
		calculateProductsButton.setToolTipText("<html>This calculates the set of substances, which can be formed out of the given set of substances <i>directly or indirectly</i></html>");
		calculateProductsButton.addActionListener(this);
		graphTaskButtonPanel.add(calculateProductsButton);

		calcPotentialAdditionals=new JButton("<html>Calc additionals maximizing<br/>the set of products");
		calcPotentialAdditionals.setToolTipText("<html>Calculates the substances, which, thoghether with the given substances, maximize the scope (number of reachable substances) of the given substances</html>");
		calcPotentialAdditionals.addActionListener(this);
		graphTaskButtonPanel.add(calcPotentialAdditionals);
		return graphTaskButtonPanel;
	}
	
	public VerticalPanel optimizationButtons(){
		VerticalPanel optimizationButtonPanel=new VerticalPanel("Optimizations");
		
		fluxBalanceAnalysis=new JButton("<html>perform FBA");
		Tools.notImplemented("fluxBalanceAnalysis.setToolTipText()");
		fluxBalanceAnalysis.addActionListener(this);
		optimizationButtonPanel.add(fluxBalanceAnalysis);		
				
		return optimizationButtonPanel;
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
		
		taskButtons.add(graphTaskButtons());		
		taskButtons.add(optimizationButtons());
		

		
		
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
		//out=System.out;
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
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		Component frame=SwingUtilities.getRoot(this);
		frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		try {
			if (source == calculateProductsButton){
						TreeSet<Integer> nutrients = substancesTab.degradeList();
						nutrients.addAll(substancesTab.ignoreList());
						actionHandler.calculateProducts(compartmentTab.getUserList().getListed(),nutrients);
			}
			if (source == disconnectClients) actionHandler.disconnect(onlyOdle.isSelected());
			if (source == calcPotentialAdditionals) actionHandler.calcPotentialAdditionals(compartmentTab.getUserList().getListed(),substancesTab.degradeList(),substancesTab.ignoreList());
			if (source == searchProcessors) actionHandler.searchProcessors(substancesTab.degradeList());
			if (source == fluxBalanceAnalysis) actionHandler.startFBA(compartmentTab.getUserSpecies(),getSubstanceSet(),parameters());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	private ParameterSet parameters() {
	  return new ParameterSet(false, skipUnbalancedReactions.isSelected());
  }

	private SubstanceSet getSubstanceSet() {		
		return new SubstanceSet(substancesTab.degradeList(),substancesTab.produceList(),null,substancesTab.noOutflowList(),substancesTab.ignoreList());
  }


}
