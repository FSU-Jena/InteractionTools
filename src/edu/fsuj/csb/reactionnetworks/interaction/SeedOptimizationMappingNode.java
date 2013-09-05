package edu.fsuj.csb.reactionnetworks.interaction;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.fsuj.csb.reactionnetworks.interaction.results.SeedOptimizationResult;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.OptimizeBuildTask;
import edu.fsuj.csb.reactionnetworks.organismtools.DbCompartment;
import edu.fsuj.csb.reactionnetworks.organismtools.DbReaction;
import edu.fsuj.csb.reactionnetworks.organismtools.gui.DbComponentNode;
import edu.fsuj.csb.tools.organisms.Compartment;
import edu.fsuj.csb.tools.organisms.Reaction;
import edu.fsuj.csb.tools.organisms.Substance;
import edu.fsuj.csb.tools.xml.ObjectComparator;
import edu.fsuj.csb.tools.xml.XMLWriter;
import edu.fsuj.csb.tools.xml.XmlObject;
import edu.fsuj.csb.tools.xml.XmlToken;

public class SeedOptimizationMappingNode extends DefaultMutableTreeNode implements XmlObject {

	private static final long serialVersionUID = -2570509750611022372L;

	private class SBMLModel implements XmlObject {

		public void getCode(StringBuffer buffer) {
			buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			try {
				buffer.append("<sbml xmlns=\"http://www.sbml.org/sbml/level2\" level=\"2\" version=\"1\">");
				buffer.append("\n\t<model id=\"Result\" name=\"Result\">");
				notes(buffer);
				compartmentList(buffer);
				speciesList(buffer);
				reactionList(buffer);
				buffer.append("\n\t</model>\n</sbml>");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		private void reactionList(StringBuffer buffer) throws SQLException {
			buffer.append("\n<listOfReactions>");

			for (Integer reactionId : solution.forwardReactions().keySet()) {
				Reaction react = Reaction.get(reactionId);
				react.getCode(buffer,false);
			}

			for (Integer reactionId : solution.backwardReactions().keySet()) {
				Reaction react = Reaction.get(reactionId);
				react.getCode(buffer,true);
			}

			buffer.append("\n</listOfReactions>");
		}

		private void speciesList(StringBuffer sb) throws SQLException {
			XmlToken result=new XmlToken("listOfSpecies");
			TreeSet<Integer> ignored = task.ignoredSubstances();
			for (Integer speciesId : getAllSpecies()) {
				Substance subs = Substance.get(speciesId);
				boolean boundary=ignored.contains(subs.id());
				subs.setValue("boundary", boundary);
				subs.setValue("compartment", "c" + task.getCompartmentId());
				result.add(subs);
			}
			result.getCode(sb);
		}

		private TreeSet<Integer> getAllSpecies() {

			TreeSet<Integer> result = new TreeSet<Integer>(ObjectComparator.get());
			for (Integer rit : solution.forwardReactions().keySet()) {
				Reaction r = Reaction.get(rit);
				result.addAll(r.substrateIds());
				result.addAll(r.productIds());
			}
			for (Integer rit : solution.backwardReactions().keySet()) {
				Reaction r = Reaction.get(rit);
				result.addAll(r.substrateIds());
				result.addAll(r.productIds());
			}
			return result;
		}

		private void compartmentList(StringBuffer buffer) {
			buffer.append("\n<listOfCompartments>\n\t<compartment id=\"c" + task.getCompartmentId() + "\" name=\"Compartment 1\" size=\"1\"></compartment>\n</listOfCompartments>");
		}

		private void notes(StringBuffer buffer) {

		}

	}

	private OptimizeBuildTask task;
	private SeedOptimizationSolution solution;

	public SeedOptimizationMappingNode(SeedOptimizationResult seedOptimizationResult) throws SQLException {
		super("Result mapping");
		task = (OptimizeBuildTask) seedOptimizationResult.getTask();
		solution = seedOptimizationResult.result();

		DefaultMutableTreeNode inputs = new SubstanceListNode("Consumed substances", solution.inflows().keySet());
		DefaultMutableTreeNode outputs = new SubstanceListNode("Produced substances", solution.outflows().keySet());
		DefaultMutableTreeNode reactions = new DefaultMutableTreeNode("Reactions (" + (solution.forwardReactions().size() + solution.backwardReactions().size()) + ")");

		for (Integer reactionId: solution.forwardReactions().keySet()){
			DbReaction.load(reactionId);
			reactions.add(DbComponentNode.create(reactionId));
		}
		for (Integer reactionId : solution.backwardReactions().keySet()){
			DbReaction.load(reactionId);
			reactions.add(DbComponentNode.create(reactionId));
		}
		add(inputs);
		add(reactions);
		add(outputs);
	}

	public void getCode(StringBuffer sb) {
		(new SBMLModel()).getCode(sb);
	}

	public void writeDotFile(String filename) throws IOException, URISyntaxException, SQLException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		TreeSet<Integer> substanceIds = new TreeSet<Integer>(ObjectComparator.get());
		for (Integer rit : solution.forwardReactions().keySet()) {
			Reaction r = Reaction.get(rit);
			substanceIds.addAll(r.substrateIds());
			substanceIds.addAll(r.productIds());
		}
		for (Integer rit : solution.backwardReactions().keySet()) {
			Reaction r = Reaction.get(rit);
			substanceIds.addAll(r.substrateIds());
			substanceIds.addAll(r.productIds());
		}
		Compartment c = DbCompartment.load(solution.compartmentId());
		writer.write("digraph " + c.mainName().replace(" ", "_").replace(".", ".") + " {\n");
		for (Integer sid : substanceIds) {
			Substance s = Substance.get(sid);
			writer.write("S" + sid + " [shape=ellipse, label=\"" + s.mainName().replace("\"", "'") + "\"];\n");
		}
		for (Integer rid : solution.forwardReactions().keySet()) {
			Reaction r = Reaction.get(rid);
			writer.write("R" + rid + " [shape=box, label=\"" + r.mainName().replace("\"", "'") + "\"];\n");
			for (Iterator<Entry<Integer, Integer>> substrateEntries = r.substrates().entrySet().iterator(); substrateEntries.hasNext();) {
				Entry<Integer, Integer> substrateEntry = substrateEntries.next();
				Substance s = Substance.get(substrateEntry.getKey());
				int stoich = substrateEntry.getValue();
				writer.write("S" + s.id() + " -> R" + r.id());
				if (stoich > 1) writer.write(" [label=\"" + stoich + "\"]");
				writer.write("\n");
			}
			for (Iterator<Entry<Integer, Integer>> productEntries = r.products().entrySet().iterator(); productEntries.hasNext();) {
				Entry<Integer, Integer> productEntry = productEntries.next();
				Substance s = Substance.get(productEntry.getKey());
				int stoich = productEntry.getValue();
				writer.write("R" + r.id() + " -> S" + s.id());
				if (stoich > 1) writer.write(" [label=\"" + stoich + "\"]");
				writer.write("\n");
			}
		}
		for (Iterator<Integer> rit = solution.backwardReactions().keySet().iterator(); rit.hasNext();) {
			Reaction r = Reaction.get(rit.next());
			writer.write("R" + r.id() + " [shape=box, label=\"" + r.mainName().replace("\"", "'") + "\"];\n");
			for (Iterator<Entry<Integer, Integer>> substrateEntries = r.substrates().entrySet().iterator(); substrateEntries.hasNext();) {
				Entry<Integer, Integer> substrateEntry = substrateEntries.next();
				Substance s = Substance.get(substrateEntry.getKey());
				int stoich = substrateEntry.getValue();
				writer.write("R" + r.id() + " -> S" + s.id());
				if (stoich > 1) writer.write(" [label=\"" + stoich + "\"]");
				writer.write("\n");
			}
			for (Iterator<Entry<Integer, Integer>> productEntries = r.products().entrySet().iterator(); productEntries.hasNext();) {
				Entry<Integer, Integer> productEntry = productEntries.next();
				Substance s = Substance.get(productEntry.getKey());
				int stoich = productEntry.getValue();
				writer.write("S" + s.id() + " -> R" + r.id());
				if (stoich > 1) writer.write(" [label=\"" + stoich + "\"]");
				writer.write("\n");
			}
		}
		writer.write("}\n");
		writer.close();
	}
}
