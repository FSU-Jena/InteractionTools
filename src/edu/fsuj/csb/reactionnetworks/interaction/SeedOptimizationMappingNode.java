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
import edu.fsuj.csb.tools.organisms.Compartment;
import edu.fsuj.csb.tools.organisms.Reaction;
import edu.fsuj.csb.tools.organisms.Substance;
import edu.fsuj.csb.tools.organisms.gui.ComponentNode;
import edu.fsuj.csb.tools.xml.ObjectComparator;
import edu.fsuj.csb.tools.xml.XMLWriter;
import edu.fsuj.csb.tools.xml.XmlObject;

public class SeedOptimizationMappingNode extends DefaultMutableTreeNode implements XmlObject {

	private static final long serialVersionUID = -2570509750611022372L;

	private class SBMLModel implements XmlObject {

		public StringBuffer getCode() {
			StringBuffer buffer = new StringBuffer("\n");
			try {
				buffer.append("<sbml xmlns=\"http://www.sbml.org/sbml/level2\" level=\"2\" version=\"1\">");
				buffer.append("\n\t<model id=\"Result\" name=\"Result\">");
				buffer.append(XMLWriter.shift(notes(), 2));
				buffer.append(XMLWriter.shift(compartmentList(), 2));
				buffer.append(XMLWriter.shift(speciesList(), 2));
				buffer.append(XMLWriter.shift(reactionList(), 2));
				buffer.append("\n\t</model>\n</sbml>");
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return buffer;
		}

		private StringBuffer reactionList() throws SQLException {
			StringBuffer buffer = new StringBuffer();
			buffer.append("\n<listOfReactions>");

			for (Iterator<Integer> reactionIterator = solution.forwardReactions().iterator(); reactionIterator.hasNext();) {
				Reaction react = Reaction.get(reactionIterator.next());
				buffer.append(XMLWriter.shift(react.getCode(false), 1));
			}

			for (Iterator<Integer> reactionIterator = solution.backwardReactions().iterator(); reactionIterator.hasNext();) {
				Reaction react = Reaction.get(reactionIterator.next());
				buffer.append(XMLWriter.shift(react.getCode(true), 1));
			}

			buffer.append("\n</listOfReactions>");
			return buffer;
		}

		private StringBuffer speciesList() throws SQLException {
			StringBuffer buffer = new StringBuffer();
			buffer.append("\n<listOfSpecies>");

			for (Iterator<Integer> speciesIterator = getAllSpecies().iterator(); speciesIterator.hasNext();) {
				Substance subs = Substance.get(speciesIterator.next());
				buffer.append(XMLWriter.shift(subs.getCode("c" + task.getCompartmentId()), 1));
			}

			buffer.append("\n</listOfSpecies>");
			return buffer;
		}

		private TreeSet<Integer> getAllSpecies() {

			TreeSet<Integer> result = new TreeSet<Integer>(ObjectComparator.get());
			for (Iterator<Integer> rit = solution.forwardReactions().iterator(); rit.hasNext();) {
				Reaction r = Reaction.get(rit.next());
				result.addAll(r.substrateIds());
				result.addAll(r.productIds());
			}
			for (Iterator<Integer> rit = solution.backwardReactions().iterator(); rit.hasNext();) {
				Reaction r = Reaction.get(rit.next());
				result.addAll(r.substrateIds());
				result.addAll(r.productIds());
			}
			return result;
		}

		private StringBuffer compartmentList() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("\n<listOfCompartments>\n\t<compartment id=\"c" + task.getCompartmentId() + "\" name=\"Compartment 1\"></compartment>\n</listOfCompartments>");
			return buffer;
		}

		private StringBuffer notes() {
			return new StringBuffer();
		}

	}

	private OptimizeBuildTask task;
	private SeedOptimizationSolution solution;

	public SeedOptimizationMappingNode(SeedOptimizationResult seedOptimizationResult) throws SQLException {
		super("Result mapping");
		task = (OptimizeBuildTask) seedOptimizationResult.getTask();
		solution = seedOptimizationResult.result();

		DefaultMutableTreeNode inputs = new SubstanceListNode("Consumed substances", solution.inflows());
		DefaultMutableTreeNode outputs = new SubstanceListNode("Produced substances", solution.outflows());
		DefaultMutableTreeNode reactions = new DefaultMutableTreeNode("Reactions (" + (solution.forwardReactions().size() + solution.backwardReactions().size()) + ")");

		for (Iterator<Integer> reactionIterator = solution.forwardReactions().iterator(); reactionIterator.hasNext();){
			Integer rId = reactionIterator.next();
			DbReaction.load(rId);
			reactions.add(ComponentNode.create(rId));
		}
		for (Iterator<Integer> reactionIterator = solution.backwardReactions().iterator(); reactionIterator.hasNext();){
			Integer rId = reactionIterator.next();
			DbReaction.load(rId);
			reactions.add(ComponentNode.create(rId));
		}
		add(inputs);
		add(reactions);
		add(outputs);
	}

	public StringBuffer getCode() {
		return (new SBMLModel()).getCode();
	}

	public void writeDotFile(String filename) throws IOException, URISyntaxException, SQLException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		TreeSet<Integer> substanceIds = new TreeSet<Integer>(ObjectComparator.get());
		for (Iterator<Integer> rit = solution.forwardReactions().iterator(); rit.hasNext();) {
			Reaction r = Reaction.get(rit.next());
			substanceIds.addAll(r.substrateIds());
			substanceIds.addAll(r.productIds());
		}
		for (Iterator<Integer> rit = solution.backwardReactions().iterator(); rit.hasNext();) {
			Reaction r = Reaction.get(rit.next());
			substanceIds.addAll(r.substrateIds());
			substanceIds.addAll(r.productIds());
		}
		Compartment c = DbCompartment.load(solution.compartmentId());
		writer.write("digraph " + c.mainName().replace(" ", "_").replace(".", ".") + " {\n");
		for (Iterator<Integer> sit = substanceIds.iterator(); sit.hasNext();) {
			Substance s = Substance.get(sit.next());
			writer.write("S" + s.id() + " [shape=ellipse, label=\"" + s.mainName().replace("\"", "'") + "\"];\n");
		}
		for (Iterator<Integer> rit = solution.forwardReactions().iterator(); rit.hasNext();) {
			Reaction r = Reaction.get(rit.next());
			writer.write("R" + r.id() + " [shape=box, label=\"" + r.mainName().replace("\"", "'") + "\"];\n");
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
		for (Iterator<Integer> rit = solution.backwardReactions().iterator(); rit.hasNext();) {
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
