package edu.fsuj.csb.reactionnetworks.interaction;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

import org.jgap.FitnessFunction;
import org.jgap.IChromosome;

import edu.fsuj.csb.tools.organisms.Compartment;


public class DegradationFitness extends FitnessFunction {

  private static final long serialVersionUID = 1L;

	private static Compartment currentCompartment;
	
	private Collection<Integer> substancesToBeDegraded, substancesToBeFormed, substancesToBeIgnored;
	private Compartment comp;
	
	public DegradationFitness(Collection<Integer> substancesToBeDegraded,Collection<Integer> substancesToBeFormed, TreeSet<Integer> ignore) {
		this.substancesToBeDegraded=substancesToBeDegraded;
		this.substancesToBeFormed=substancesToBeFormed;
		this.substancesToBeIgnored=ignore;
		comp=currentCompartment;
  }

	protected double evaluate(IChromosome chrom) {
		int geneNumber=chrom.size();
		
		TreeSet<Integer> inputSubstances=new TreeSet<Integer>();
		
		for (int i=0; i<geneNumber; i++){
			SubstanceGene sg=(SubstanceGene)chrom.getGene(i);
			if (sg.getAllele().equals(Boolean.TRUE)) inputSubstances.add(sg.substanceId());
		}
		inputSubstances.addAll(substancesToBeIgnored);
		
		try {
	    return degradationSuccess(inputSubstances)*productionSuccess(inputSubstances)/inputSubstances.size();
    } catch (SQLException e) {
	    e.printStackTrace();
	    return 0;
    }
	}
	
	private double productionSuccess(TreeSet<Integer> inputSubstances) throws SQLException {
		Collection<Integer> products = comp.calculateProductsOf(inputSubstances);
		double numberOfTargetsProduced=0;
		double numberOfTargetsSupplied=0;
		for (Iterator<Integer> it = substancesToBeFormed.iterator();it.hasNext();){
			int sid=it.next();
			if (products.contains(sid)) numberOfTargetsProduced+=1.0;
			if (inputSubstances.contains(sid)) numberOfTargetsSupplied+=1.0;
		}
		
	  return Math.pow(numberOfTargetsProduced/substancesToBeFormed.size(),2)/(1+numberOfTargetsSupplied);
  }	
	
	private double degradationSuccess(TreeSet<Integer> inputSubstances) {
		double counter=0;
		for (Iterator<Integer> it = substancesToBeDegraded.iterator();it.hasNext();){
			if (inputSubstances.contains(it.next())) counter+=1.0;
		}
	  return Math.pow(counter/substancesToBeDegraded.size(),2);
  }
	
	public static void setCompartment(Compartment c){
		currentCompartment=c;
	}

}
