package edu.fsuj.csb.reactionnetworks.interaction.tasks.lp;

import java.util.TreeSet;

import edu.fsuj.csb.tools.LPSolverWrapper.LPCondition;
import edu.fsuj.csb.tools.LPSolverWrapper.LPDiff;
import edu.fsuj.csb.tools.LPSolverWrapper.LPSolveWrapper;
import edu.fsuj.csb.tools.LPSolverWrapper.LPVariable;
import edu.fsuj.csb.tools.xml.ObjectComparator;
import edu.fsuj.csb.tools.xml.Tools;

	public class Binding {
		private static Double limit = 100000.0;

		private LPCondition lowerLimit;
		private LPCondition upperLimit;
		private LPVariable switchVar;
		
		public Binding(LPVariable flow, LPVariable flowSwitch, LPSolveWrapper solver) {
			Tools.startMethod("new Binding("+flow+", "+flowSwitch+")");
			
			lowerLimit = new LPCondition(new LPDiff(flowSwitch, flow),LPCondition.LESS_OR_EQUAL, 0.0);
			lowerLimit.setComment("force velocity>1 if switch=1");
			solver.addCondition(lowerLimit);
			
			upperLimit = new LPCondition(new LPDiff(flow, limit,flowSwitch),LPCondition.LESS_OR_EQUAL,0.0);
			upperLimit.setComment("force velocity=0 if switch==0 ");
			solver.addCondition(upperLimit);
			
			switchVar=flowSwitch;
			
			solver.addBinVar(flowSwitch);

			Tools.endMethod();
		}

		public LPCondition upperLimit() {
	    return upperLimit;
    }

		public LPCondition lowerLimit() {
	    return lowerLimit;
    }
		
		public LPVariable switchVar(){
			return switchVar;
		}

		public static TreeSet<Binding> set() {
			return new TreeSet<Binding>(ObjectComparator.get());
		}
	}