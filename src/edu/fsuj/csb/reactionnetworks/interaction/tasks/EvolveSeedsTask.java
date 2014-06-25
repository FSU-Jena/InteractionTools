package edu.fsuj.csb.reactionnetworks.interaction.tasks;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.DefaultFitnessEvaluator;
import org.jgap.FitnessFunction;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.event.EventManager;
import org.jgap.impl.BestChromosomesSelector;
import org.jgap.impl.ChromosomePool;
import org.jgap.impl.CrossoverOperator;
import org.jgap.impl.GABreeder;
import org.jgap.impl.MutationOperator;
import org.jgap.impl.StockRandomGenerator;

import edu.fsuj.csb.reactionnetworks.interaction.CalculationClient;
import edu.fsuj.csb.reactionnetworks.interaction.DegradationFitness;
import edu.fsuj.csb.reactionnetworks.interaction.SubstanceGene;
import edu.fsuj.csb.reactionnetworks.interaction.SubstanceListNode;
import edu.fsuj.csb.reactionnetworks.interaction.results.EvolveSeedsResult;
import edu.fsuj.csb.reactionnetworks.organismtools.DbCompartment;
import edu.fsuj.csb.tools.organisms.Compartment;
import edu.fsuj.csb.tools.xml.NoTokenException;

public class EvolveSeedsTask extends TaskContainingCompartmentAndSubtances {

	private static final long serialVersionUID = 1095552689438604226L;
	private TreeSet<Integer> build,ignore;
	private static final int populationDivisor=1;
	

	private static class MyConfig extends Configuration implements Cloneable {

		private static final long serialVersionUID = 1012692729256509984L;
		private static final int divisor = 6; // derived from experiments

		public MyConfig(int networkSize) {
			super("", "");
			try {
				setBreeder(new GABreeder());
				setRandomGenerator(new StockRandomGenerator());
				setEventManager(new EventManager());
				BestChromosomesSelector bestChromsSelector = new BestChromosomesSelector(this, 0.90d);
				bestChromsSelector.setDoubletteChromosomesAllowed(true);
				addNaturalSelector(bestChromsSelector, false);
				setMinimumPopSizePercent(0);
				setSelectFromPrevGen(1.0d);
				setKeepPopulationSizeConstant(true);
				setFitnessEvaluator(new DefaultFitnessEvaluator());
				setChromosomePool(new ChromosomePool());
				addGeneticOperator(new CrossoverOperator(this, 0.35d));
				addGeneticOperator(new MutationOperator(this, networkSize / divisor));
			} catch (InvalidConfigurationException e) {
				throw new RuntimeException("Fatal error: DefaultConfiguration class could not use its " + "own stock configuration values. This should never happen. " + "Please report this as a bug to the JGAP team.");
			}
		}

		/**
		 * @return deep clone of this instance
		 * 
		 * @author Klaus Meffert
		 * @since 3.2
		 */
		public Object clone() {
			return super.clone();
		}

	}

	public EvolveSeedsTask(int compartmentId, TreeSet<Integer> decompose, TreeSet<Integer> substancesThatShallBeBuilt, TreeSet<Integer> substancesThatCanBeIgnored) {
		super(compartmentId, decompose);
		build = substancesThatShallBeBuilt;
		ignore = substancesThatCanBeIgnored;
	}

	@Override
	public void run(CalculationClient calculationClient) throws IOException, NoTokenException, SQLException {
		try {
			Compartment comp = DbCompartment.load(compartmentId);
			int speciesCount = comp.utilizedSubstances().size();
			DegradationFitness.setCompartment(comp);
			Configuration.reset();
			Configuration configuration = new MyConfig(speciesCount);

			FitnessFunction degradationFitness = new DegradationFitness(substanceIds, build,ignore); // this actually describes the evolution goal
			configuration.setFitnessFunction(degradationFitness);
			
			
			SubstanceGene[] genes = new SubstanceGene[comp.utilizedSubstances().size()];
			int index = 0;

			for (Iterator<Integer> it = comp.utilizedSubstances().iterator(); it.hasNext();) {
				genes[index++] = new SubstanceGene(configuration, it.next());
			}
			
			Chromosome sampleChromosome = new Chromosome(configuration, genes);
			configuration.setSampleChromosome(sampleChromosome);
			configuration.setPopulationSize(speciesCount/populationDivisor);
			

			Genotype genotype = Genotype.randomInitialGenotype(configuration);
			
			int leftSteps=(int) (speciesCount*Math.round(10/Math.log(speciesCount)));
			System.out.println("Evolution time: "+leftSteps+" cycles");
			long startTime = Calendar.getInstance().getTimeInMillis();
			int elapsedSteps=0;
			int step=42; // thank you, Douglas Adams
			while (leftSteps>0){
				
				int nextSteps=Math.min(step, leftSteps);
				genotype.evolve(nextSteps);
				leftSteps-=nextSteps;
				elapsedSteps+=nextSteps;
				
				long elapsedTime = Calendar.getInstance().getTimeInMillis()-startTime;
				long leftTime = (leftSteps*elapsedTime)/elapsedSteps;
				String t="."+(leftTime%1000);
				leftTime/=1000;
				/* left time has seconds here */
				t=":"+(leftTime%60)+t;
				leftTime/=60;
				/* left time has minutes here */
				t=":"+(leftTime%60)+t;
				leftTime/=60;
				/* left time has hours here */
				t=" days, "+(leftTime%24)+t;
				leftTime/=24;
				/* left time has day here */
				t=leftTime+t;
				System.out.println((100*elapsedSteps)/(elapsedSteps+leftSteps)+"% ("+t+" left)");
				
			}
			IChromosome bestSolutionSoFar = genotype.getFittestChromosome();
			elapsedSteps = bestSolutionSoFar.size();
			
			TreeSet<Integer> seeds=new TreeSet<Integer>();
			
			for (; elapsedSteps > 0; elapsedSteps--) {
				SubstanceGene g = (SubstanceGene) bestSolutionSoFar.getGene(elapsedSteps - 1);
				if (g.getAllele() == Boolean.TRUE) seeds.add(g.substanceId());
			}
			calculationClient.sendObject(new EvolveSeedsResult(this,seeds));
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}

	}
	
	public DefaultMutableTreeNode treeRepresentation() throws IOException, NoTokenException, AlreadyBoundException, SQLException {
		DefaultMutableTreeNode result=treeRepresentation("Task: Calculate additionals with evolutionary algorithm ["+this.getClass().getSimpleName()+"]");
		result.add(new SubstanceListNode("substances that shall be built",build));
		result.add(new SubstanceListNode("ignored substances", ignore));
		return result;
	}
}
