package edu.fsuj.csb.reactionnetworks.interaction;
import java.io.IOException;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.sql.SQLException;

import edu.fsuj.csb.distributedcomputing.tools.Client;
import edu.fsuj.csb.distributedcomputing.tools.Ports;
import edu.fsuj.csb.distributedcomputing.tools.Signal;
import edu.fsuj.csb.reactionnetworks.interaction.tasks.CalculationTask;
import edu.fsuj.csb.tools.xml.NoTokenException;
import edu.fsuj.csb.tools.xml.Tools;


/**
 * executable program, whicht connects to the InteractionToolbox and performs calculations for it
 * @author Stephan Richter
 *
 */
public class CalculationClient extends Client{

	/**
	 * launches a new isntance of the calculation program
	 * @param hostname the name of the host, on which the interaction toolbox is executed
	 * @param port the port, on which the interaction toolbox can be reached
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ClassNotFoundException
	 * @throws NoTokenException
	 */
	public CalculationClient(String hostname,int port) throws UnknownHostException, IOException, InterruptedException, ClassNotFoundException, NoTokenException {
	  super(hostname,port);
  }
	
	/**
	 * instantiates and launches a new instance of the calculation client
	 * @param args
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ClassNotFoundException
	 */
	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException, ClassNotFoundException {
		Tools.disableLogging();
		String host=(args.length>0)?args[0]:"quad";
		for (int i=0; i<args.length; i++) System.out.println(args[i]);
		int port=Ports.registrationPort();
		
	  try {
	    new CalculationClient(host,port);
    } catch (NoTokenException e) {
	    e.printStackTrace();
    }
  }
	
	/* (non-Javadoc)
	 * @see edu.fsuj.csb.distributedcomputing.tools.Client#handleObject(java.lang.Object)
	 */
	public void handleObject(Object o) throws IOException {		
		System.out.println("recieved " + o.getClass());
    if (o instanceof CalculationTask) {
	    CalculationTask calcTask = (CalculationTask) o;
	    try {
	      calcTask.start(this);
      } catch (NoTokenException e) {
	      e.printStackTrace();
      } catch (AlreadyBoundException e) {
	      e.printStackTrace();
      } catch (SQLException e) {
	      e.printStackTrace();
      }
	    sendObject(new Signal(Signal.IDLE));
	    System.out.println("Task done, idling around...\n");
    }
	}
}
