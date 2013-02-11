package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintStream;
import java.rmi.AlreadyBoundException;
import java.sql.SQLException;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.fsuj.csb.gui.HorizontalPanel;
import edu.fsuj.csb.gui.StatusPanel;
import edu.fsuj.csb.gui.VerticalPanel;
import edu.fsuj.csb.reactionnetworks.interaction.ActionHandler;
import edu.fsuj.csb.reactionnetworks.interaction.CompartmentList;
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
	private static PrintStream out;
	private JButton calculateSeedsButton;
	private CompartmentsTab compartmentTab;
	private ActionHandler actionHandler;
	//private ResultPanel resultPane;
	private int statusHeight = 200;
	private JButton disconnectClients;
	private SubstancesTab substancesTab;
	private JButton calculateProductsButton,calcPotentialAdditionals,searchProcessors;
	private JTabbedPane taskTabs;
	private JButton optimizeSeeds,evolveSeeds;
	private JCheckBox onlyOdle,skipUnbalancedReactions;
	private OptimizationParametersTab parametersTab;

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
		System.out.println("Creating GUI components:");
		createComponents(1200, 800);
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
	private void createComponents(int width, int height) throws IOException, NoTokenException, AlreadyBoundException, SQLException, DataFormatException {
		JTabbedPane taskResultPane = new JTabbedPane();

		//***** TASKS *******//
		System.out.println("- creating panel for tasks...");
		taskResultPane.add(createTaskPane(width - 20, height - statusHeight-70), "Tasks");

		//***** RESULTS *****//
		System.out.println("- creating panel for results...");
		taskResultPane.add(createResultPane(width - 40, height - statusHeight-70), "Results");
		
		//***** DATABASE **********//
		System.out.println("- creating panel for database actions...");
		DatabasePane databasePane = new DatabasePane();
		databasePane.addChangeListener(this);
		taskResultPane.add(databasePane,"Database");
		
		//***** INFO *********//
		taskResultPane.add(createInfoPanel(),"Information");
		
		//***** MAIN PANEL ********//
		VerticalPanel panel = new VerticalPanel();
		panel.add(taskResultPane);
		panel.add(new StatusPanel(width - 40, statusHeight));
		System.setOut(out);
		panel.skalieren();
		add(panel);
		pack();
	}

	/**
	 * creates a panel showing some short info related to the program
	 * @return the created panel
	 */
	private Component createInfoPanel() {
		VerticalPanel result = new VerticalPanel();
		JLabel text=new JLabel("<html>This is the <i>Interaction Toolbox</i><br/><br/><br/>Author: <b>Stephan Richter</b><br/>Bio System Analysis Group<br/><br/>Get more info at http://www.biosys.uni-jena.de<br/><br/>Bitte beachten sie bei den hiermit untersuchten Modellen, dass evtl. falsche Daten durch uneindeutige Annotation entstehen können.<br/><br/>(siehe Genome annotation errors in pathway databases due to semantic abiguity in partial EC numbers)");
		result.add(text);
	  return result;
  }

	/**
	 * creates the ResultPanel, which is used to display calculation results
	 * @param width the width which the panel shall have
	 * @param height the height, which the panel shall have
	 * @return the newly creaed result panel
	 * @throws IOException 
	 */
	private ResultPanel createResultPane(int width, int height) throws IOException {
		ResultPanel resultPane=new ResultPanel(width, height-70);
		System.out.print("- trying to start server: ");
		actionHandler = new ActionHandler(resultPane.getTree());
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
	JComponent taskButtonPanel = createTaskButtons();
		
	private HorizontalPanel createTaskPane(int width, int height) throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		HorizontalPanel taskPane = new HorizontalPanel();
		taskPane.add(createTaskTabs(width - taskButtonPanel.getWidth() - 60, height-50));
		taskPane.add(taskButtonPanel);
		taskPane.skalieren();
		return taskPane;
	}

	/**
	 * creates the sidebar of the task panel, which holds the buttons to submit tasks
	 * @return the button panel after it has been created
	 */
	private JComponent createTaskButtons() {
		VerticalPanel buttonPane = new VerticalPanel();
		VerticalPanel taskButtons=new VerticalPanel("Tasks");

		calculateSeedsButton = new JButton("<html>Calculate Seeds<br/>with MILP (buggy)");
		calculateSeedsButton.setToolTipText("<html>This method should calculate the minimum sets of substances which can be supplied to form<ul><li>all Substances in the compartment (organism) <i>if no target substances are specified</i></li><li>for the specified target substances <i>otherwise</i></li></ul><font color=\"red\">not implemented, yet.</font></html>");
		calculateSeedsButton.addActionListener(this);
		taskButtons.add(calculateSeedsButton);
		
		optimizeSeeds=new JButton("<html>Calculate additionals<br/>with MILP (buggy)");
		optimizeSeeds.setToolTipText("<html>Takes one substance list as targets<br/>and the other as \"desired nutrients\"<br/>and tries to optimize (maximize) flow<br/>towards targets and decomposition<br/>of the <i>desired nutrients</i> while keeping<br/>all other inflow reactions low.</html>");
		optimizeSeeds.addActionListener(this);
		taskButtons.add(optimizeSeeds);
		
		evolveSeeds=new JButton("<html>Calculate additionals<br/>with evolutionary Algorithm");
		evolveSeeds.setToolTipText("<html>Takes one substance list as targets<br/>and the other as \"desired nutrients\"<br/>and tries to optimize (maximize) flow<br/>towards targets and decomposition<br/>of the <i>desired nutrients</i> while keeping<br/>all other inflow reactions low.</html>");
		evolveSeeds.addActionListener(this);
		taskButtons.add(evolveSeeds);

		calculateProductsButton = new JButton("Calculate products");
		calculateProductsButton.setToolTipText("<html>This calculates the set of substances, which can be formed out of the given set of substances <i>directly or indirectly</i></html>");
		calculateProductsButton.addActionListener(this);
		taskButtons.add(calculateProductsButton);

		calcPotentialAdditionals=new JButton("Calc best additionals");
		calcPotentialAdditionals.setToolTipText("<html>Calculates the substances, which, thoghether with the given substances, maximize the scope (number of reachable substances) of the given substances</html>");
		calcPotentialAdditionals.addActionListener(this);
		taskButtons.add(calcPotentialAdditionals);

		searchProcessors=new JButton("<html>Search for<br/>processing species");
		searchProcessors.setToolTipText("<html>Search for species, which enable reactions that have at least one of the given substances as substrate</html>");
		searchProcessors.addActionListener(this);
		taskButtons.add(searchProcessors);
		
		skipUnbalancedReactions=new JCheckBox("<html>Skip unbalanced reactions");
		skipUnbalancedReactions.setToolTipText("<html>Unbalanced reactions wil not be taken into account, when using methods which use stoichiometry.");
		taskButtons.add(skipUnbalancedReactions);
		
		
		taskButtons.skalieren();
		buttonPane.add(taskButtons);
		
		VerticalPanel clientManager = new VerticalPanel("Client actions");
		
		disconnectClients = new JButton("Disconnect clients");
		disconnectClients.addActionListener(this);
		clientManager.add(disconnectClients);
		
		onlyOdle=new JCheckBox("only idle clients");
		onlyOdle.setSelected(true);
		clientManager.add(onlyOdle);
		
		
		clientManager.skalieren();
		buttonPane.add(clientManager);
		
		buttonPane.skalieren();
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
	private JComponent createTaskTabs(int width, int height) throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		taskTabs = new JTabbedPane();
		System.out.println("    |`- creating compartments panel");
		compartmentTab = new CompartmentsTab(width - 60, height-60);
		System.out.println("    |`- creating substances panel");
		substancesTab = new SubstancesTab(width-60,height-50);
		System.out.println("    `-- creating parameters panel");		
		parametersTab = new OptimizationParametersTab(width-60,height-60);
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
		out=System.out;
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
					actionHandler.optimizeSeeds(compartmentTab.getUserList().getListed(),decompositionList,buildList,substancesTab.ignoreList(),parametersTab.optimizationParameterSet(),skipUnbalancedReactions.isSelected());
				} else {
					actionHandler.evovleSeeds(compartmentTab.getUserList().getListed(), decompositionList, buildList,substancesTab.ignoreList());
				}
			}
		}
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

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		Component frame=SwingUtilities.getRoot(this);
		frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		try {
			if (source == calculateSeedsButton) actionHandler.calcSeeds(compartmentTab.getUserList().getListed(),substancesTab.produceList(),substancesTab.ignoreList(),skipUnbalancedReactions.isSelected());
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
			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}
}
