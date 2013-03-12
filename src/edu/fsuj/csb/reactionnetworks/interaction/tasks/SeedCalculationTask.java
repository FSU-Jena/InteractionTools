package edu.fsuj.csb.reactionnetworks.interaction.tasks;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.CalculationClient;
import edu.fsuj.csb.reactionnetworks.interaction.SubstanceListNode;
import edu.fsuj.csb.reactionnetworks.interaction.results.SeedCalculationResult;
import edu.fsuj.csb.reactionnetworks.organismtools.DbCompartment;
import edu.fsuj.csb.reactionnetworks.organismtools.SeedCalculationIntermediate;
import edu.fsuj.csb.reactionnetworks.organismtools.gui.DbComponentNode;
import edu.fsuj.csb.tools.LPSolverWrapper.CplexWrapper;
import edu.fsuj.csb.tools.LPSolverWrapper.LPCondition;
import edu.fsuj.csb.tools.LPSolverWrapper.LPConditionLessThan;
import edu.fsuj.csb.tools.LPSolverWrapper.LPSum;
import edu.fsuj.csb.tools.LPSolverWrapper.LPTerm;
import edu.fsuj.csb.tools.LPSolverWrapper.LPVariable;
import edu.fsuj.csb.tools.organisms.CalculationIntermediate;
import edu.fsuj.csb.tools.organisms.Compartment;
import edu.fsuj.csb.tools.xml.NoTokenException;
import edu.fsuj.csb.tools.xml.ObjectComparator;

public class SeedCalculationTask extends StructuredTask {

	private TreeSet<Integer> potentialPrecursors=new TreeSet<Integer>(ObjectComparator.get());
	private TreeSet<TreeSet<Integer>> continouslyAvailableSubstances=new TreeSet<TreeSet<Integer>>(ObjectComparator.get());
  private static final long serialVersionUID = -8131331284620004914L;
	private int compartmentId;
	private int numberOfSubtasks=0;
	private TreeSet<Integer> ignoredSubstances,targets;
	private boolean ignoreUnbalanced;
	
	/**
	 * This clas represents a Task for calculation the seedset of a certain set of substances in a given compartment 
	 * @param compartmentId the database id of the compartment
	 * @param targetSubstanceIds the set of (database related) substance ids, which shall be built
	 * @param ignoreUnbalanced 
	 */
	public SeedCalculationTask(int compartmentId, TreeSet<Integer> targetSubstanceIds,TreeSet<Integer> ignoredSubstances, boolean ignoreUnbalanced) {
		super();
		this.compartmentId=compartmentId;
		this.targets=targetSubstanceIds;
		this.ignoredSubstances=ignoredSubstances;
		this.ignoreUnbalanced=ignoreUnbalanced;
  }


	/** adds an precalculation solution (a set of potential precursors and continously available substances to the seed calculation task
	 * @param result should be an instance of SeedCalculationIntermediate
	 */
	public void addIntermediateResult(Object result) {
	  if (!(result instanceof SeedCalculationIntermediate)) throw new ClassCastException("Result is not an Seed Calculation Intermediate!");
	  SeedCalculationIntermediate intermediate = (SeedCalculationIntermediate)result;
	  potentialPrecursors.addAll(intermediate.potentialPrecursors());
	  potentialPrecursors.removeAll(intermediate.continouslyAvailableSubstances());
	  continouslyAvailableSubstances.addAll(intermediate.continouslyAvailableSubstances());
	  numberOfPendingSubtasks--;	  
	  numberOfSubtasks++;	
  }
	
	/* (non-Javadoc)
	 * @see edu.fsuj.csb.reactionnetworks.interaction.StructuredTask#readyToRun()
	 */
	public boolean readyToRun(){
		return numberOfPendingSubtasks==0;
	}	
	
  /* (non-Javadoc)
   * @see edu.fsuj.csb.reactionnetworks.interaction.CalculationTask#run(edu.fsuj.csb.reactionnetworks.interaction.CalculationClient)
   */
  public void run(CalculationClient calculationClient) throws IOException, NoTokenException, AlreadyBoundException{
  	//System.out.println("Potential precursors: "+potentialPrecursors);
  	//System.out.println("continously available:"+continouslyAvailableSubstances);
  	//System.out.println("targets: "+targets);
  	try {
  		TreeSet<TreeSet<Integer>> solutions=new TreeSet<TreeSet<Integer>>(ObjectComparator.get());
  		int solutionSize=Integer.MAX_VALUE; // for monitoring the size of the solutions
  		int number=1; // for enumerating the solutions
  		while (true){
    		TreeMap<Integer, Double> solution = runInternal(solutions); // start the actual calculation
    		if (solution==null) {
    			System.out.println("Solution #"+number+": no solution found!");
    			break;
    		}
    		if (solution.size()>solutionSize) { // if the size of the solution increases, we're getting suboptimal solutions. so: quit!
      		System.out.println("found no more solution with size "+solutionSize);
    			break;
    		}
    		if (solution.size()<solutionSize) { // normaly the size of the solutions should not decrease, as this would mean, we have found suboptimal solutions before...
    			if (solutionSize!=Integer.MAX_VALUE) {
    				System.err.println("uh oh! found smaller solution ("+solution+"). this was not expected.");
    				solutions=new TreeSet<TreeSet<Integer>>(ObjectComparator.get());
    				number=1;
    			}
  				solutionSize=solution.size();
    		}
    		System.out.println("Solution #"+(number++)+": "+solution); // show the solution
  			solutions.add(new TreeSet<Integer>(solution.keySet())); // don't allow inflow of the substances in the solution in the next turn
  		}
  		calculationClient.sendObject(new SeedCalculationResult(this, solutions,continouslyAvailableSubstances));
    } catch (SQLException e) {
	    e.printStackTrace();
    } catch (InterruptedException e) {
	    e.printStackTrace();
    } catch (DataFormatException e) {
	    e.printStackTrace();
    }
  }
  
  /**********************************************************************************************************/
  /*																																																				*/
  /*																																																				*/
  /*			Problem ist im Moment, dass zwar das lp-file korrekt geschrieben wird (oder doch nicht?),					*/
  /*																																																				*/
  /*			dennoch keine Lösungen ausgegeben werden.                                         								*/
  /*																																																				*/
  /*																																																				*/
  /*																																																				*/
  /*			offenbar werden bei der Berechnung der Precursor die Loops nicht richtig gespeichert 							*/
  /*																																																				*/
  /*																																																				*/
  /*																																																				*/
  /*																																																				*/
  /*																																																				*/
  /*																																																				*/
  /*																																																				*/
  /*																																																				*/
  /*																																																				*/
  /*																																																				*/
  /*																																																				*/
  /*																																																				*/
  /*																																																				*/
  /*																																																				*/
  /*																																																				*/
  /*																																																				*/
  /*																																																				*/
  /*																																																				*/
  /*																																																				*/
  /*																																																				*/
  /**********************************************************************************************************/


  /**
   * calculates a minimal seed set for the given target substances, excluding solutions given by parameter
   * 
   * @param solutions those solutions shall be forbidden during the search for new solutions
   * @return a mapping from substance ids (keys) to solution values. The values should all be greater then zero, the respective substances are those needed to form the target substances.
   * @throws SQLException if any database error occurs
   * @throws IOException
   * @throws InterruptedException 
   * @throws DataFormatException 
   */
  private TreeMap<Integer, Double> runInternal(TreeSet<TreeSet<Integer>> solutions) throws SQLException, IOException, InterruptedException, DataFormatException{  	
  	System.out.println("running seed calculation:");  	
  	System.out.println("Potential precursors: "+potentialPrecursors);
  	System.out.println("continously available:"+continouslyAvailableSubstances);
  	System.out.println("targets: "+targets);
  	if (ignoreUnbalanced) System.out.println("Not taking unbalanced reactions into account.");
  	System.out.print("Creating solver input file...");
  	
  	double inflowOutflowWeight = 10.0;

  	
  	CplexWrapper cpw=new CplexWrapper(); // create program wrapper
  	Compartment compartment=DbCompartment.load(compartmentId); // load the compartment

  	TreeMap<Integer, LPTerm> balances = OptimizationTask.createBasicBalances(cpw, ignoredSubstances, ignoreUnbalanced, compartment); // create balances for all substances in the compartment
  	TreeSet<Integer> utilizedSubstances = compartment.utilizedSubstances(); // get the list of all substances utilized by this compartment
		OptimizationTask.addInflowReactionsFor(potentialPrecursors, balances, cpw);
		OptimizationTask.addOutflowReactionsFor(targets, balances, cpw);
		OptimizationTask.writeBalances(balances, cpw);
		
		LPTerm termToMinimize = null; // start creating the directive
		
		for (Iterator<Integer> it = utilizedSubstances.iterator(); it.hasNext();) { // iterate through all the compartment's substances:
			int sid = it.next(); // get substance id

			LPVariable inflow = new LPVariable("sRi_" + sid);
			LPVariable outflow = new LPVariable("sRo_" + sid);

			OptimizationTask.addCondition(sid, cpw, OptimizationTask.INFLOW); // connect inflow switch and velocity
			OptimizationTask.addCondition(sid, cpw, OptimizationTask.OUTFLOW); // connect outflow switch and velocity

			if (targets.contains(sid)) { // activate outflow for all substances, that shall be produced
				cpw.setEqual(outflow, 1.0, "force outflow for substance to be built");
				cpw.setEqual(inflow, 0.0, "forbid inflow of substances to be built");
			} else termToMinimize = new LPSum(termToMinimize, inflowOutflowWeight, outflow); // minimize other outflows

		}
		
		int solutionNumber = 0;
		for (Iterator<TreeSet<Integer>> solutionIterator = solutions.iterator(); solutionIterator.hasNext();) {
			solutionNumber++;
			Double sum = 0.0;
			TreeSet<Integer> solution = solutionIterator.next();
			LPTerm inflowSwitchSum = null;
			for (Iterator<Integer> inflowIterator = solution.iterator(); inflowIterator.hasNext();) {
				if (inflowSwitchSum == null) inflowSwitchSum = new LPVariable("sRi_" + inflowIterator.next());
				else inflowSwitchSum = new LPSum(inflowSwitchSum, new LPVariable("sRi_" + inflowIterator.next()));
				sum++;
			}
			LPCondition lpc = new LPConditionLessThan(inflowSwitchSum, sum);
			lpc.setComment("Forbid solution " + solutionNumber);
			cpw.addCondition(lpc);
		}
		
  	cpw.minimize(termToMinimize);
  	
  	System.out.print("done.\nStarting solver: ");
  	SimpleDateFormat formatter = new SimpleDateFormat("yy-MM-dd HH.mm.ss");
  	String filename=("seedCalculation " + getNumber() + " " + formatter.format(new Date()) + ".lp").replace(" ", "_").replace(":", ".");
		cpw.setTaskfileName(filename);
  	cpw.start("sR*");  	
  	TreeMap<Integer,Double> result=new TreeMap<Integer, Double>(ObjectComparator.get()); // mapping from substance id to value for substance in the solution

  	TreeMap<LPVariable, Double> solution = cpw.getSolution();
  	if (solution==null) return null;
  	for (Iterator<Entry<LPVariable, Double>> values = solution.entrySet().iterator();values.hasNext();){
  		Entry<LPVariable, Double> entry = values.next();
  		String name=entry.getKey().toString();
			if (entry.getValue() >= 0) {
				if (name.startsWith("sRi")) result.put(Integer.parseInt(name.substring(4)), entry.getValue());
			}

  	}
  	return result;
  }

	@Override
  void handleIntermediateResultData(CalculationIntermediate cim) {
	  SeedCalculationIntermediate intermediate = (SeedCalculationIntermediate)cim;
	  potentialPrecursors.addAll(intermediate.potentialPrecursors());
	  potentialPrecursors.removeAll(intermediate.continouslyAvailableSubstances());
	  continouslyAvailableSubstances.addAll(intermediate.continouslyAvailableSubstances());	  
  }
	
	public MutableTreeNode treeRepresentation() throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		DefaultMutableTreeNode result = new DefaultMutableTreeNode("Task: Calculate Seeds for given Substances with MILP ["+this.getClass().getSimpleName()+"]");
		result.add(DbComponentNode.create(DbCompartment.load(compartmentId)));
		result.add(new SubstanceListNode("given target substances",targets));
		result.add(new SubstanceListNode("ignored substances",ignoredSubstances));
		return result;
	}
	
	public int maxTaskNumber(){
		return getNumber()+numberOfSubtasks;
	}
}
