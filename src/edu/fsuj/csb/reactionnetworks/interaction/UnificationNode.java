package edu.fsuj.csb.reactionnetworks.interaction;

import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UnknownFormatConversionException;
import java.util.Vector;
import java.util.zip.DataFormatException;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import edu.fsuj.csb.reactionnetworks.database.InteractionDB;
import edu.fsuj.csb.reactionnetworks.organismtools.DbSubstance;
import edu.fsuj.csb.stringComparison.Levenshtein;
import edu.fsuj.csb.tools.newtork.pagefetcher.PageFetcher;
import edu.fsuj.csb.tools.organisms.Formula;
import edu.fsuj.csb.tools.organisms.gui.UrnNode;
import edu.fsuj.csb.tools.urn.URN;
import edu.fsuj.csb.tools.xml.NoTokenException;
import edu.fsuj.csb.tools.xml.ObjectComparator;
import edu.fsuj.csb.tools.xml.Tools;
import edu.fsuj.csb.tools.xml.XMLWriter;
import edu.fsuj.csb.tools.xml.XmlToken;

public class UnificationNode extends DefaultMutableTreeNode implements MutableTreeNode {

	private DbSubstance substance;
	private TreeMap<URL,TreeSet<String>> nameMap=new TreeMap<URL, TreeSet<String>>(ObjectComparator.get());
	private TreeMap<URL,String> smilesMap=new TreeMap<URL, String>(ObjectComparator.get());
	private TreeMap<URL,Double> massMap=new TreeMap<URL, Double>(ObjectComparator.get());
	
	
  public UnificationNode(Integer id) throws SQLException {
  	this(DbSubstance.load(id));
  }

	public UnificationNode(DbSubstance substance) {
	  super(substance.mainName());
	  this.substance=substance;
  }

	private static final long serialVersionUID = -5983158639536627867L;
	
	
	public String analyze(boolean createSVG) throws SQLException, DataFormatException, IOException, NoTokenException {
		//Tools.enableLogging();		
		Tools.startMethod("analyze()");
		
		/* initialize... */
		String filename="/tmp/image.svg";		
		Object o=this.getUserObject();
		this.setUserObject(o+" ---- similarities of urns:");		
		int id=substance.id();
		TreeMap<URN,TreeMap<Integer,TreeSet<URN>>> mapFromURNsToScores = new TreeMap<URN,TreeMap<Integer,TreeSet<URN>>>(ObjectComparator.get());
		TreeMap<Integer,TreeMap<URN,Integer>> mapFromScoresToUrn = new TreeMap<Integer, TreeMap<URN,Integer>>();		

		/* ...initialize */

		Vector<URN> urns = InteractionDB.getURNsFor(id);
		Vector<URL> urls= InteractionDB.getReferencingURLs(id);
		int numberOfUrns=urns.size();
		
		TreeMap<URL,TreeSet<URN>> referencingUrls=new TreeMap<URL, TreeSet<URN>>(ObjectComparator.get());
		
		for (Iterator<URN> urnIterator = urns.iterator(); urnIterator.hasNext();) {
	    URN urn = urnIterator.next();	    
	    TreeSet<URL> newUrls = InteractionDB.getReferencingURLs(urn);
	    for (Iterator<URL> it = newUrls.iterator(); it.hasNext();) {
	      URL url = it.next();
	      if (!referencingUrls.containsKey(url)) referencingUrls.put(url, new TreeSet<URN>(ObjectComparator.get()));
	      referencingUrls.get(url).add(urn);	     
      }
	    Set<URL> dummy = urn.urls();
	    if (dummy!=null) urls.removeAll(dummy);	    
    }		
		
		int numberOfPoints=numberOfUrns+urls.size();
		/* prepare svg... */
		double angle=2*Math.PI/(numberOfPoints);
		int radius=200;
		XMLWriter xml=null;
		XmlToken svg=null;
		if (createSVG){
			xml=new XMLWriter(filename);
			svg=new XmlToken("svg");
			svg.setValue("xmlns", "http://www.w3.org/2000/svg");
			svg.setValue("xmlns:xlink", "http://www.w3.org/1999/xlink");
			svg.setValue("xmlns:ev", "http://www.w3.org/2001/xml-events");
			svg.setValue("version", "1.1");
			svg.setValue("baseProfile", "full");
			svg.setValue("width", (400+20*numberOfPoints)+"mm");
			svg.setValue("height", (400+20*numberOfPoints)+"mm");
		}
		/* ...prepare svg */
		int index1;
		for (index1=0; index1<numberOfUrns; index1++){
			URN urn1=urns.elementAt(index1);			
			
			/* calculate starting positions of lines... */
			double alpha1=index1*angle;
			double x1=Math.sin(alpha1)*radius+radius+20;
			double y1=-Math.cos(alpha1)*radius+radius+20;
			/* ...calculate starting positions of lines */
			
			if (createSVG) {
				/* insert text into svg... */
				XmlToken text=new XmlToken("text");
				text.setContent(""+(index1+1));
				text.setValue("x", ""+x1);
				text.setValue("y", ""+y1);
				svg.add(text);			

				XmlToken urn=new XmlToken("text");
				urn.setContent((index1+1)+": "+urn1);
				urn.setValue("x", "10");
				urn.setValue("y", ""+(radius+radius+20+15*index1));
				svg.add(urn);
			}
			
			/* ...insert text into svg */
			for (int index2=index1+1; index2<numberOfUrns; index2++){
				URN urn2=urns.elementAt(index2);
				
				boolean solid=false;
				
				for (Iterator<URL> it = InteractionDB.getUrls(urn1).iterator(); it.hasNext();){
					URL url = it.next();
					TreeSet<URN> dummy = referencingUrls.get(url);
					if (dummy!=null && dummy.contains(urn2)) solid=true;
				}
				for (Iterator<URL> it = InteractionDB.getUrls(urn2).iterator(); it.hasNext();){
					URL url = it.next();
					TreeSet<URN> dummy = referencingUrls.get(url);
					if (dummy!=null && dummy.contains(urn1)) solid=true;
				}
				
				int score;
				try {
					score=scoreFor(urn1, urn2); /****************************** calculate score */
				} catch (FileNotFoundException fnf){
					continue;
				}
				
				if (createSVG){
					/* calculate line color... */
					int s=Math.max(score,0);
					int r=(s<50)?255:(int)Math.round(255-5.1*(s-50));
					int g=(s<50)?(int)Math.round(5.1*s):(int)Math.round(255-128*(s-50)/50.0);
					Color c=new Color(r,g,0);
				
					c=new Color(0, 255, 0);
					if (score<75) c=new Color(100,200,0);
					if (score<50) c=new Color(230,230,0);
					if (score<25) c=new Color(255,200,150);
					if (score<0) c=Color.red;
				
					/* ...calculate line color */
				
				
					/* insert line into svg... */
					XmlToken line = new XmlToken("line");
					line.setValue("x1", ""+x1);
					line.setValue("y1", ""+y1);
					double alpha2=index2*angle;
					line.setValue("x2", ""+(Math.sin(alpha2)*radius+radius+20));
					line.setValue("y2", ""+(-Math.cos(alpha2)*radius+radius+20));
					line.setValue("stroke", "#" + Integer.toHexString(c.getRGB()).substring(2).toUpperCase());				
					line.setValue("stroke-width", (score<0||score>75)?"2px":"1px");
					if (!solid) line.setValue("style", "stroke-dasharray: 9, 5;");
					svg.add(line);
					/* ...insert line into svg */
				}
				
				if (score>50) System.out.println("Heureka! Score = "+score);
				if (!mapFromURNsToScores.containsKey(urn1)) mapFromURNsToScores.put(urn1, new TreeMap<Integer, TreeSet<URN>>());
				if (!mapFromURNsToScores.containsKey(urn2)) mapFromURNsToScores.put(urn2, new TreeMap<Integer, TreeSet<URN>>());				
				
				TreeSet<URN> set = mapFromURNsToScores.get(urn1).get(score);
				if (set==null) {
					set=new TreeSet<URN>(ObjectComparator.get());
					mapFromURNsToScores.get(urn1).put(score, set);
				}
				set.add(urn2);
				
				
				set = mapFromURNsToScores.get(urn2).get(score);
				if (set==null) {
					set=new TreeSet<URN>(ObjectComparator.get());
					mapFromURNsToScores.get(urn2).put(score, set);
				}
				set.add(urn1);
				
				TreeMap<URN, Integer> urnMap = mapFromScoresToUrn.get(score);
				if (urnMap==null){
					urnMap=new TreeMap<URN, Integer>(ObjectComparator.get());
					mapFromScoresToUrn.put(score, urnMap);
				}
				
				Integer count=urnMap.get(urn1);
				if (count==null){
					count=1;
				} else count+=1;

				
				urnMap.put(urn1, count);
				
				count=urnMap.get(urn2);
				
				if (count==null){
					count=1;
				} else count+=1;
				
				urnMap.put(urn2, count);
			}		
		}
		for (Iterator<URL> it = urls.iterator(); it.hasNext();){
			URL url = it.next();
			TreeSet<URN> linkedUrns = referencingUrls.get(url);
			
			double x1=0,y1=0;
			if (createSVG){
				/* calculate starting positions of lines... */
				double alpha1=index1*angle;
				x1=Math.sin(alpha1)*radius+radius+20;
				y1=-Math.cos(alpha1)*radius+radius+20;
				/* ...calculate starting positions of lines */
			
				/* insert text into svg... */
				XmlToken text=new XmlToken("text");
				text.setContent(""+(index1+1));
				text.setValue("x", ""+x1);
				text.setValue("y", ""+y1);
				svg.add(text);			

				XmlToken urn=new XmlToken("text");
				String dummy=url.toString();
				int i=dummy.lastIndexOf("/");
				i=dummy.lastIndexOf("/", i-1);
			
				urn.setContent((index1+1)+": "+dummy.substring(i+1));
				urn.setValue("x", "10");
				urn.setValue("y", ""+(radius+radius+20+15*index1));
				svg.add(urn);

				for (int index2=0; index2<numberOfUrns; index2++){
					URN urn2 = urns.elementAt(index2);
					if (linkedUrns.contains(urn2)){
						/* insert line into svg... */
						XmlToken line = new XmlToken("line");
						line.setValue("x1", ""+x1);
						line.setValue("y1", ""+y1);
						double alpha2=index2*angle;
						line.setValue("x2", ""+(Math.sin(alpha2)*radius+radius+20));
						line.setValue("y2", ""+(-Math.cos(alpha2)*radius+radius+20));
						line.setValue("stroke", "black");
						svg.add(line);
						/* ...insert line into svg */
					}
				}
			}
			index1++;

		}
		if (createSVG){
			xml.write(svg);
			xml.close();
		}
		Stack<Integer> stack=new Stack<Integer>();
		for (Iterator<Integer> it = mapFromScoresToUrn.keySet().iterator(); it.hasNext();) {
			stack.push(it.next());
		}
		while (!stack.isEmpty()){
			Integer score=stack.pop();
			
			DefaultMutableTreeNode scoreNode=null;
			if (score>=0) scoreNode=new DefaultMutableTreeNode(score+"% similarity:");
			if (score==-1) scoreNode=new DefaultMutableTreeNode("different masses:"); 
			if (score==-2) scoreNode=new DefaultMutableTreeNode("different sum formulas:"); 
			if (score==-4) scoreNode=new DefaultMutableTreeNode("different SMILES strings:"); 
	    TreeMap<URN, Integer> map = mapFromScoresToUrn.get(score);
	    while (!map.isEmpty()){
	    	int maxCount=0;
	    	URN maxCountURN=null;
	    	for (Iterator<Entry<URN,Integer>> it2 = map.entrySet().iterator(); it2.hasNext();) {
	    		Entry<URN,Integer> dummy = it2.next();
	    		if (dummy.getValue()>maxCount){
	    			maxCount=dummy.getValue();
	    			maxCountURN=dummy.getKey();
	    		}
        }
	    	
	    	DefaultMutableTreeNode urnNode=new UrnNode(maxCountURN);
	    	scoreNode.add(urnNode);
	    	for (Iterator<URN> it2 = mapFromURNsToScores.get(maxCountURN).get(score).iterator(); it2.hasNext();){
	    		URN urn2=it2.next();
	    		mapFromURNsToScores.get(urn2).get(score).remove(maxCountURN);
	    		urnNode.add(new UrnNode(urn2));
	    		
	    		int count=map.get(maxCountURN)-1;
	    		if (count==0) {
	    			map.remove(maxCountURN);
	    		} else map.put(maxCountURN, count);
	    		
	    			count=map.get(urn2)-1;
	    			if (count==0) {
	    				map.remove(urn2);
	    			} else map.put(urn2, count);
	    		
	    	}
	    	
				add(scoreNode);
	    }
    }


		nameMap=new TreeMap<URL, TreeSet<String>>(ObjectComparator.get()); // free space
		smilesMap=new TreeMap<URL, String>(ObjectComparator.get());
		massMap=new TreeMap<URL, Double>(ObjectComparator.get());

		Tools.endMethod();
		return "file://"+filename;
	}

	TreeSet<UrnNode> extendedNodes=new TreeSet<UrnNode>(ObjectComparator.get());

	private int scoreFor(URN urn1, URN urn2) throws IOException, NoTokenException, DataFormatException, SQLException {
		Tools.startMethod("scoreFor("+urn1+", "+urn2+")");
		double score=0;

		String smilesA = getSmiles(urn1); /********************************************/
		
		if (smilesA!=null){
			String smilesB=getSmiles(urn2);
			if (smilesB!=null){
				if (!smilesA.equals(smilesB)) {
					Tools.endMethod(-4);
					return -4;
				}
				Tools.indent("score +4");
				score+=4;
			}
		}
		
		Formula sumFormulaA = getSumFormula(urn1); /**********************************************/
		
		if (sumFormulaA!=null){
			Formula sumFormulaB=getSumFormula(urn2);
			if (sumFormulaB!=null){
				if (!sumFormulaA.equals(sumFormulaB)) {
					Tools.endMethod(-2);
					return -2;
				}
				Tools.indent("score +2");
				score+=2;
			}
		}
		
		Double massA=getMass(urn1); /******************************************/
		
		if (massA!=null){
			Double massB=getMass(urn2); 			
			if (massB!=null){
				double diff = Math.abs(massA-massB);
				int thresh=5;
				if (diff>thresh) {
					Tools.endMethod(-1);
					return -1;
				}
				double value=(5-diff)/5;
				Tools.indent("score +"+value);

				score=score+value;
			}
		}
		
		TreeSet<String>names1=getNames(urn1);  /*************************************/
		
		
		if (names1!=null){
			TreeSet<String> names2 = getNames(urn2);
			if (names2!=null){
				double value=scoreForNames(names1,names2);
				Tools.indent("score +"+value);
				score=score+value;
			}
		}
		int result=(int) Math.round(score*100/8);
		Tools.endMethod("score: "+score+"/8 => "+result+"%");
	  return result;
  }

	private double scoreForNames(TreeSet<String> names1, TreeSet<String> names2) throws DataFormatException {
		Tools.startMethod("scoreForNames("+names1+" / "+names2+")");
		double score=0;
		for (Iterator<String> it1 = names1.iterator(); it1.hasNext();) {
	    String name1 = it1.next();
	    if (name1.contains("e")) throw new DataFormatException("Name \""+name1+"\" contains lowercase characters!");
			int len1=name1.length();			
			for (Iterator<String> it2 = names2.iterator(); it2.hasNext();) {
	      String name2 = it2.next();
		    if (name2.contains("e")) throw new DataFormatException("Name \""+name2+"\" contains lowercase characters!");
      	int len=Math.max(len1, name2.length());
				int dist=Levenshtein.distance(name1, name2);
				double pairScore=((double)len-dist)/((double)len);
				score=Math.max(score, pairScore);
			}
		}
		Tools.endMethod(score);
	  return score;
  }

	private TreeSet<String> getNames(URN urn) throws IOException {
		Tools.startMethod("getNames("+urn+")");
		Set<URL> urls = urn.urls();		
		TreeSet<String> result=null;
		if (urls==null) return result;
		for (Iterator<URL> it = urls.iterator(); it.hasNext();) {
	    URL url = it.next();
	    
	    
	    result=getNames(url);  /***********************************/
	    
	    
	    if (result!=null) break;
    }		
		Tools.endMethod(result);
	  return result;
  }

	
	private TreeSet<String> getNames(URL url) throws IOException {		
		Tools.startMethod("getNames("+url+")");
		TreeSet<String> result=null;
		if (nameMap.containsKey(url)){
		  result=nameMap.get(url);
			Tools.endMethod(result);
		  return result;
		}

		if (url.toString().contains("genome.jp/dbget-bin/www_bget")){
			result=getKeggNames(url);
		} else if (url.toString().contains("3dmet.dna.affrc.go.jp/cgi/show_data.php")){
			result=get3dMetNames(url);
		} else if (url.toString().contains("bioportal.bioontology.org/ontologies")){
			result=getGeneOntologyNames(url);
		} else if (url.toString().contains("drugbank.ca/drugs")){
			result=getDrugBankNames(url);
		} else if (url.toString().contains("kanaya.naist.jp/knapsack_jsp/information.jsp")){
			result=getKnapsackNames(url);
		} else if (url.toString().contains("commonchemistry.org/ChemicalDetail.aspx")){
			result=getCasNames(url);
		} else if (url.toString().contains("ncbi.nlm.nih.gov/sites/entrez?db=pccompound")){
			result=getPubChemCompoundNames(url);
		} else if (url.toString().contains("pubchem.ncbi.nlm.nih.gov/summary/summary.cgi")){
			result=getPubChemSubstanceNames(url);
		} else if (url.toString().contains("lipidmaps.org/data/get_lm_lipids_dbgif.php")){
			result=getLipidMapsNames(url);
		} else if (url.toString().contains("lipidbank.jp/cgi-bin/detail.cgi")){
			result=getLipidBankNames(url);
		} else if (url.toString().contains("ebi.ac.uk/chebi/searchId.do")){
			result=getChebiNames(url);
		} else if (url.toString().contains("nikkajiweb.jst.go.jp/nikkaji_web/pages")){
			result=getJCSDNames(url);
		} else if (url.toString().contains("purl.uniprot.org/uniprot")){
			result=getUniprotNames(url);
		} else {
			String code=PageFetcher.fetch(url).toString();
			code=Tools.removeHtml(code).toUpperCase();
			if (code.contains("SYNONYM")) throw new UnknownFormatConversionException(url+" contains string SYNONYM:"+code);
		}
		nameMap.put(url, result);
		Tools.endMethod(result);
	  return result;
  }

	private TreeSet<String> getUniprotNames(URL url) throws IOException {
		Tools.startMethod("getUniprotNames("+url+")");
	  String[] lines= PageFetcher.fetchLines(url);
	  TreeSet<String> names=null;
		for (int i=0; i<lines.length; i++){
			String line=lines[i];
			String code=line.toUpperCase();
			int start=code.indexOf("PROTEIN NAMES");
			if (start>0){
				int end=code.indexOf("GENE NAMES");
				if (end<start){
					System.err.println("Something is wrong here.");
					System.exit(0);
				} else {
					String dummy=Tools.removeHtml(line.substring(start, end).replace("<h3>", "\n").replace("</span>", "\n").replace("<br>", "\n").replace("<br/>", "\n").replace("</acronym>", "\n").replace("\n\n","\n").trim());
					System.out.println(dummy);
					String [] arr=dummy.split("\n");
					for (int arrIndex=0; arrIndex<arr.length; arrIndex++){
						String name=arr[arrIndex].trim();
						if (name.startsWith("Protein name")) continue;
						if (name.startsWith("Recommended name")) continue;
						if (name.startsWith("Submitted name")) continue;
						if (name.startsWith("Short name")) continue;
						if (name.startsWith("Cleaved into")) continue;
						if (name.startsWith("EMBL")) continue;
						if (name.startsWith("Alternative name")) continue;
						if (name.startsWith("EC=")||name.startsWith("CD_antigen=")) {
							arrIndex++;
							continue;
						}
						if (name==null || name.equals("")) continue;
						if (names==null) names=Tools.StringSet();
						names.add(name.toUpperCase());
					}
				}
			}
		}
		Tools.warn("getUniprotNames lists the following names (which may be wrong):");
		System.out.println("Names: "+names);
		if (names==null) System.exit(-2);
		try {
	    Thread.sleep(20000);
    } catch (InterruptedException e) {
	    e.printStackTrace();
    }
		Tools.endMethod(names);
	  return names;
	  
  }

	private TreeSet<String> getDrugBankNames(URL url) throws IOException {
		Tools.startMethod("getDrugBankNames("+url+")");
	  String[] lines= PageFetcher.fetchLines(url);
	  TreeSet<String> names=null;
		for (int i=0; i<lines.length; i++){
			String line=lines[i];
			if (line.contains(">Name<")){
				if (names==null) names=Tools.StringSet();
				line=lines[++i];
				names.add(Tools.removeHtml(line).trim().toUpperCase());
			}
			if (line.contains(">Synonyms<")){
				if (names==null) names=Tools.StringSet();
				while (true){
					line=lines[++i];
					if (line.contains("</ul>") || line.contains("Not Available")) break;
					line=Tools.removeHtml(line).trim().toUpperCase();
					if (!line.isEmpty()) names.add(line);				
				}
			}
		}
		Tools.endMethod(names);
	  return names;
	  
  }

	private TreeSet<String> getGeneOntologyNames(URL url) throws IOException {
		Tools.startMethod("getGeneOntologyNames("+url+")");
	  String[] lines= PageFetcher.fetchLines(url);
	  TreeSet<String> names=null;
		for (int i=0; i<lines.length; i++){
			String line=Tools.removeHtml(lines[i]).trim();
			if (line.equals("Preferred Name")){
				while (true){
					i++;
					line=Tools.removeHtml(lines[i]).trim();
					if (line.length()>0) {
						if (names==null) names=Tools.StringSet();
						names.add(line.toUpperCase());						
						break;
					}
				}
			}
			if (line.equals("Narrow Synonym")){
				while (true){
					i++;
					line=Tools.removeHtml(lines[i]).trim();
					if (line.length()>0) {
						if (names==null) names=Tools.StringSet();
						String[] dummy = lines[i].split("</p><p>");
						for (int index=0; index<dummy.length; index++){
							names.add(Tools.removeHtml(dummy[index]).trim().toUpperCase());
						}
						break;
					}
				}
			}
		}

		Tools.endMethod(names);
		Tools.warnOnce("getGeneOntologyNames lists "+names+" as names for "+url+". Please check!!!");
	  return names;
	  
  }

	private TreeSet<String> getPubChemSubstanceNames(URL url) throws IOException {
		Tools.startMethod("getPubChemSubstanceNames("+url+")");
	  String[] lines= PageFetcher.fetchLines(url);
	  TreeSet<String> names=null;
		for (int i=0; i<lines.length; i++){
			String line=lines[i];
			if (line.contains(">Substance Summary<")){
				if (names==null) names=Tools.StringSet();
				String dummy=Tools.removeHtml(line);
				int index=dummy.indexOf("Summary");
				dummy=dummy.substring(0,index);
				index=dummy.lastIndexOf('-');
				if (index>0){
					dummy=dummy.substring(0,index);
					dummy=dummy.trim().toUpperCase();
					names.add(dummy);
				}
				index=line.indexOf("Also known as:");
				if (index>0){
					dummy=line.substring(index+14);
					index=dummy.indexOf("</div>");
					dummy=Tools.removeHtml(dummy.substring(0,index)).trim();
					String[] arr = dummy.split(", ");
					for (index=0; index<arr.length; index++) names.add(arr[index].trim().toUpperCase());
				}				
			}
		}

		Tools.endMethod(names);
	  return names;
	  
  }

	private TreeSet<String> getKnapsackNames(URL url) throws IOException {
		Tools.startMethod("getKnapsackNames("+url+")");
	  String[] lines= PageFetcher.fetchLines(url);
	  TreeSet<String> names=null;
		for (int i=0; i<lines.length; i++){
			String line=lines[i];
			if (line.contains(">Name<")){
				if (names==null) names=Tools.StringSet();
				line=lines[++i];
				String[] dummy = line.split("<br>");
				for (int index=0; index<dummy.length; index++){
					names.add(Tools.removeHtml(dummy[index]).trim().toUpperCase());
				}
			}
		}

		Tools.endMethod(names);
	  return names;
	  
  }

	private TreeSet<String> getPubChemCompoundNames(URL url) throws IOException {
		Tools.startMethod("getPubChemCompoundNames("+url+")");
	  String[] lines= PageFetcher.fetchLines(url);
	  TreeSet<String> names=null;
		for (int i=0; i<lines.length; i++){
			String line=lines[i];
			if (line.contains(">Compound Summary<")){
				if (names==null) names=Tools.StringSet();
				String dummy=Tools.removeHtml(line);
				int index=dummy.indexOf("Summary");
				dummy=dummy.substring(0,index);
				index=dummy.lastIndexOf('-');
				if (index>0){
					dummy=dummy.substring(0,index);
					dummy=dummy.trim().toUpperCase();
					names.add(dummy);
				}
				index=line.indexOf("Also known as:");
				if (index>0){
					dummy=line.substring(index+14);
					index=dummy.indexOf("</div>");
					dummy=Tools.removeHtml(dummy.substring(0,index)).trim();
					String[] arr = dummy.split(", ");
					for (index=0; index<arr.length; index++) names.add(arr[index].trim().toUpperCase());
				}				
			}
		}
		Tools.endMethod(names);
	  return names;
	  
  }

	private TreeSet<String> getCasNames(URL url) throws IOException {
		Tools.startMethod("getCasNames("+url+")");
	  String[] lines= PageFetcher.fetchLines(url);
	  TreeSet<String> names=null;
		for (int i=0; i<lines.length; i++){
			String line=lines[i];
			if (line.contains("No CAS Registry Number<sup>&#174;</sup> matched your search")) return null;
			if (line.contains("CA Index Name")){
				if (names==null) names=Tools.StringSet();
				names.add(Tools.removeHtml(lines[++i]).trim().toUpperCase());
			}
			if (line.contains("Synonyms")){
				if (names==null) names=Tools.StringSet();
				line=lines[++i];
				String[] dummy = line.split("</li><li>");
				for (int index=0; index<dummy.length; index++){
					names.add(Tools.removeHtml(dummy[index]).trim().toUpperCase());
				}
			}
		}
		Tools.endMethod(names);
		return names;
	  
  }

	private TreeSet<String> get3dMetNames(URL url) throws IOException {
		Tools.startMethod("get3dMetNames("+url+")");
	  String[] lines= PageFetcher.fetchLines(url);
	  TreeSet<String> names=null;
		for (int i=0; i<lines.length; i++){
			String line=lines[i];
			if (line.contains(">Name<")){
				if (names==null) names=Tools.StringSet();
				names.add(Tools.removeHtml(lines[++i]).trim().toUpperCase());
			}
		}

		Tools.endMethod(names);
	  return names;
	  
  }

	private TreeSet<String> getLipidBankNames(URL url) throws IOException {
		Tools.startMethod("getLipidBankNames("+url+")");
	  String[] lines= PageFetcher.fetchLines(url);
	  TreeSet<String> names=null;
		for (int i=0; i<lines.length; i++){
			String line=lines[i];
			if (line.contains("NAME</b>")) {
				if (names==null) names=Tools.StringSet();
				String dummy=Tools.removeHtml(line).split(":")[1].trim();
				if (dummy.length()>0)	names.add(dummy.toUpperCase());
			}
		}

		Tools.endMethod(names);
		return names;	  
  }
	
	TreeSet<String> thrownMessages=Tools.StringSet();


	private TreeSet<String> getJCSDNames(URL url) throws IOException {
		Tools.startMethod("getJCSDNames("+url+")");
	  String[] lines= PageFetcher.fetchLines(url);
	  TreeSet<String> names=null;
		for (int i=0; i<lines.length; i++){
			String line=lines[i];
			if (line.contains(">Names<")){
				while (true){
					i++;
					line=lines[i];
					String trimmed=Tools.removeHtml(line).trim();
					if (trimmed.length()>0) {
						if (names==null) names=Tools.StringSet();
						String[] dummy = line.split("<BR>");
						for (int index=0; index<dummy.length; index++) names.add(Tools.removeHtml(dummy[index]).trim().toUpperCase());
						break;
					}
				}
			}
		}

		Tools.endMethod(names);
	  return names;
	  
  }

	private TreeSet<String> getChebiNames(URL url) throws IOException {
		Tools.startMethod("getChebiNames("+url+")");
	  String[] lines= PageFetcher.fetchLines(url);
	  TreeSet<String> names=null;
		for (int i=0; i<lines.length; i++){
			String line=lines[i];
			if (line.contains(">ChEBI Name<")||line.contains(">ChEBI ASCII Name<")||line.contains(">IUPAC Name<")){
				while (true){
					i++;
					line=Tools.removeHtml(lines[i]).trim();
					if (line.length()>0) {
						if (names==null) names=Tools.StringSet();
						names.add(line.trim().toUpperCase());
						break;
					}
				}
			}
		}

		Tools.endMethod(names);
	  return names;
	  
  }

	private TreeSet<String> getLipidMapsNames(URL url) throws IOException {
		Tools.startMethod("getLipidMapsNames("+url+")");
	  String[] lines= PageFetcher.fetchLines(url);
	  TreeSet<String> names=null;
		for (int i=0; i<lines.length; i++){
			if (lines[i].toUpperCase().contains("COMMON NAME")){
				String name=Tools.removeHtml(lines[i]).trim().substring(11).trim().toUpperCase();
				if (names==null) names=Tools.StringSet();
				names.add(name);
			}
			if (lines[i].toUpperCase().contains("SYSTEMATIC NAME")){
				String name=Tools.removeHtml(lines[i]).trim().substring(15).trim().toUpperCase();
				if (names==null) names=Tools.StringSet();
				names.add(name);
			}
			if (lines[i].toUpperCase().contains("SYNONYMS")){
				String[] nameArray=Tools.removeHtml(lines[i]).trim().substring(8).split(";");
				for (int ai=0; ai<nameArray.length; ai++){
					if (names==null) names=Tools.StringSet();
					String name=nameArray[ai].trim().toUpperCase();
					if (!name.equals("'")) names.add(name);
				}
			}
		}
		Tools.endMethod(names);
	  return names;
	  
  }

	private TreeSet<String> getKeggNames(URL url) throws IOException {
		Tools.startMethod("getKeggNames("+url+")");
	  String[] lines= PageFetcher.fetchLines(url);
	  TreeSet<String> names=null;
		for (int i=0; i<lines.length; i++){
			if (lines[i].contains("<nobr>Name</nobr>")) {
				while (!lines[++i].contains("</div>")) {
					String name = Tools.removeHtml(lines[i]);
					if (names==null) names=Tools.StringSet();
					names.add(name.endsWith(";") ? (name.substring(0, name.length() - 1).toUpperCase()) : name.toUpperCase()); // only remove endo-of-line semicolons, preserve in-string semicolons
				}
			}
	  }
		Tools.endMethod(names);
	  return names;
	  
  }

	private Double getMass(URN urn) throws IOException, DataFormatException {
		Tools.startMethod("getMass("+urn+")");
		Set<URL> urls = urn.urls();		
		Double result=null;
		if (urls==null) return result;
		for (Iterator<URL> it = urls.iterator(); it.hasNext();) {
	    URL url = it.next();
	    
	    
	    result=getMass(url); /************************************/
	    
	    
	    if (result!=null) break;
    }		
		Tools.endMethod(result);
	  return result;
  }

	private Double getMass(URL url) throws IOException, DataFormatException {
		Tools.startMethod("getMass("+url+")");
		Double result=null;
		if (massMap.containsKey(url)){
		  result=massMap.get(url);
			Tools.endMethod(result);
		  return result;
		}
		if (url.toString().contains("genome.jp/dbget-bin/www_bget")) {
			result=getKeggMass(url);
		} else if (url.toString().contains("nikkajiweb.jst.go.jp/nikkaji_web/pages/top_e.js")){
			result=getJCSDMass(url);
		} else if (url.toString().contains("ebi.ac.uk/chebi/searchId.do")){
			result=getChebiMass(url);
		} else if (url.toString().contains("kanaya.naist.jp/knapsack_jsp/information.jsp")){
			result=getKnapsackMass(url);
		} else if (url.toString().contains("lipidbank.jp/cgi-bin/detail.cgi")){
			result=getLipidBankMass(url);
		} else if (url.toString().contains("drugbank.ca/drugs")){
			result=getDrugBankMass(url);
		} else if (url.toString().contains("lipidmaps.org/data/get_lm_lipids_dbgif.php")){
			result=getLipidMapsMass(url);
		} else if (url.toString().contains("3dmet.dna.affrc.go.jp/cgi/show_data.php")){
			result=get3DmetMass(url);
		} else if (url.toString().contains("ncbi.nlm.nih.gov/sites/entrez?db=pccompound")){
			result=getPubchemCompoundMass(url);
		} else if (url.toString().contains("pubchem.ncbi.nlm.nih.gov/summary/summary.cgi")){
			result=getPubchemCompoundMass(url);
		} else {
			String code=PageFetcher.fetch(url).toString();
			code=code.toUpperCase().replace("MASS SPEC", "").replace("MASS-SPEC", "").replace("LIGHTWEIGHT", "").replace("FONT-WEIGHT", "").replace("MASSENGILL", "").replace("MASSIVE", "").replace("HIGH MOLECULAR WEIGHT", "").replace("THE MASS OF THE UNPROCESSED PROTEIN.\">MASS", "").replace("PEPTIDE_MASS", "").replace("PEPTIDE-MASS", "").replace("PEPTIDEMASS", "").replace("CHIMASSORB", "").replace("HIGHMASS", "");
			if (code.contains("MASS")) {
				 Exception ex = new UnknownFormatConversionException(url+" probably contains MASS value:\n");
				ex.printStackTrace();
				if (!url.toString().contains("glycome-db.org/database")) System.exit(0);
			}		
			if (code.contains("WEIGHT")) {
				 Exception ex = new UnknownFormatConversionException(url+" contains string WEIGHT");
				ex.printStackTrace();
				if (!url.toString().contains("glycome-db.org/database")) System.exit(0);
			}		
		}
		massMap.put(url, result);
		Tools.endMethod(result);
	  return result;
  }

	private Double getDrugBankMass(URL url) throws IOException {
		Tools.startMethod("getDrugBankMass("+url+")");
		String[] lines=PageFetcher.fetchLines(url);
		Double mass=null;
		for (int i=0; i<lines.length; i++){
			
			if (lines[i].contains(">Weight<")) {
				while (true){
					String line=Tools.removeHtml(lines[++i]).trim();					
					if (!line.isEmpty()) {						
						if (line.equals("Not Available")) break;
						line=Tools.firstNumber(line);
						mass=Double.parseDouble(line);
						break;
					}
				}
			}

		}
		Tools.endMethod(mass);
	  return mass;
  }

	private Double getPubchemCompoundMass(URL url) throws IOException {
		Tools.startMethod("getPubchemCompoundMass("+url+")");
		String[] lines=PageFetcher.fetchLines(url);
		Double mass=null;
		for (int i=0; i<lines.length; i++){
			String line=lines[i];
			int index=line.indexOf("Weight:<");
			if (index>0){
				index=line.indexOf(">",index)+1;
				int end=line.indexOf("<",index);
				line=Tools.removeHtml(line.substring(index,end)).trim();
				try {
					mass=Double.parseDouble(line);
				} catch (NumberFormatException nfe){
					Tools.warnOnce("NumberFormatException "+nfe.getMessage());
				}
				break;
			}
		}
		Tools.endMethod(mass);		
	  return mass;
  }

	private Double getLipidMapsMass(URL url) throws IOException {
		Tools.startMethod("getLipidMapsMass("+url+")");
		String[] lines=PageFetcher.fetchLines(url);
		Double mass=null;
		for (int i=0; i<lines.length; i++){
			
			if (lines[i].contains("Mass<")) {
				String line=Tools.removeHtml(lines[++i]).trim();			
				int index=line.indexOf(" ");
				if (index>0) line=line.substring(0,index);
				if (line.equals("Formula-")) break;
				mass=Double.parseDouble(line);
				break;
			}

		}
		Tools.endMethod(mass);
	  return mass;
  }

	private Double getChebiMass(URL url) throws IOException {
		Tools.startMethod("getChebiMass("+url+")");
		String[] lines=PageFetcher.fetchLines(url);
		Double mass=null;
		for (int i=0; i<lines.length; i++){
			
			if (lines[i].contains("Mass<")) {
				String line=Tools.removeHtml(lines[++i]).trim();
				while (line.length()==0){
					line=Tools.removeHtml(lines[++i]).trim();					
				}
			
				try {
					mass=Double.parseDouble(line);
				} catch (NumberFormatException nfe){
					Tools.warnOnce("NumberFormatException "+nfe.getMessage());
				}
				break;
			}

		}
		Tools.endMethod(mass);
	  return mass;
  }

	private Double getKnapsackMass(URL url) throws IOException {
		Tools.startMethod("getKnapsackMass("+url+")");
		String[] lines=PageFetcher.fetchLines(url);
		Double mass=null;
		for (int i=0; i<lines.length; i++){
			if (lines[i].contains(">Mw<")) {
				String dummy = Tools.removeHtml(lines[++i]).trim();
				try {
					mass=Double.parseDouble(dummy);
				} catch (NumberFormatException nfe){
					Tools.warnOnce("NumberFormatException "+nfe.getMessage());
				}
				break;
			}
		}
		Tools.endMethod(mass);
	  return mass;
  }

	private Double get3DmetMass(URL url) throws IOException {
		Tools.startMethod("get3DmetMass("+url+")");
		String[] lines=PageFetcher.fetchLines(url);
		Double mass=null;
		for (int i=0; i<lines.length; i++){
			if (lines[i].contains(">Weight<")) {
				String dummy = Tools.removeHtml(lines[++i]).trim();
				try {
					mass=Double.parseDouble(dummy);
				} catch (NumberFormatException nfe){
					Tools.warnOnce("NumberFormatException "+nfe.getMessage());
				}
				break;
			}
		}
		Tools.endMethod(mass);
	  return mass;
  }

	private Double getLipidBankMass(URL url) throws IOException {
		Tools.startMethod("getLipidBankMass("+url+")");
		String[]lines=PageFetcher.fetchLines(url);
		Double mass=null;
		for (int i=0; i<lines.length; i++){
			if (lines[i].contains("MOL.WT")){
				String weight=Tools.removeHtml(lines[i]).trim();
				int index=weight.indexOf("MOL.WT");
				index=weight.indexOf(":",index);
				weight=weight.substring(index+1).trim();
				if (weight.length()!=0) mass=Double.parseDouble(weight);
			}
		}
		Tools.endMethod(mass);
		return mass;
  }

	private Double getJCSDMass(URL url) throws IOException, DataFormatException {
		Tools.startMethod("getJCSDFormula("+url+")");
		String[]lines=PageFetcher.fetchLines(url);
		String mw=null;
		Double mass=null;
		for (int i=0; i<lines.length; i++){
			if (lines[i].contains(">MW</span>")){
				while (!lines[++i].contains("span")){
					if (i>lines.length) throw new DataFormatException("Formula tag found in "+url+", but no formula reckognized!");
				}
				mw=Tools.removeHtml(lines[i]).trim();
			}
		}
		if (!mw.isEmpty()) mass=Double.parseDouble(mw);
		Tools.endMethod(mass);
		return mass;
  }

	private Double getKeggMass(URL url) throws IOException {
		Tools.startMethod("getKeggMass("+url+")");
		String[] lines=PageFetcher.fetchLines(url);
		Double mass=null;
		for (int i=0; i<lines.length; i++){
			if (lines[i].contains("<nobr>Mass</nobr>")) {
				String dummy = Tools.removeHtml(lines[++i]);
				try {
					mass=Double.parseDouble(Tools.firstNumber(dummy));
				} catch (NumberFormatException nfe){
					Tools.warnOnce("NumberFormatException "+nfe.getMessage());
				}
				break;
			}
		}
		Tools.endMethod(mass);
	  return mass;
  }

	private Formula getSumFormula(URN urn) throws IOException, NoTokenException, DataFormatException, SQLException {
		Tools.startMethod("getSumFormula("+urn+")");		
		Formula result=null;		
		Set<URL> urls = urn.urls();
		if (urls==null) return null;
		for (Iterator<URL> it = urls.iterator(); it.hasNext();) {
	    URL url = it.next();
	    
	    result=InteractionDB.getFormulaFrom(url); /*******************************/
	    

    }
		
		Tools.endMethod(result);
	  return result;
  }


	private String getSmiles(URN urn) throws IOException {
		Tools.startMethod("getSmiles("+urn+")");
		Set<URL> urls = urn.urls();		
		String result=null;		
		if (urls==null) return result;
		for (Iterator<URL> it = urls.iterator(); it.hasNext();) {
	    URL url = it.next();
	    
	    result=getSmiles(url); /**************************************/
	    
	    if (result!=null) break;	    
    }		
		Tools.endMethod(result);
	  return result;
  }

	private String getSmiles(URL url) throws IOException {
		Tools.startMethod("getSmiles("+url+")");
		
		String result=null;
		if (smilesMap!=null && smilesMap.containsKey(url)){
		  result=smilesMap.get(url);
			Tools.endMethod(result);
		  return result;
		}
		if (url.toString().contains("3dmet.dna.affrc.go.jp/cgi/show_data.php")){
			result=get3dMetSmiles(url);
		} else if (url.toString().contains("ebi.ac.uk/chebi/searchId.do")){
			result=getChEBISmiles(url);
		} else if (url.toString().contains("drugbank.ca/drugs")){
			result=getDrugbankSmiles(url);
		} else {
			String code=PageFetcher.fetch(url).toString();
			code=code.toUpperCase();
		 	if (code.contains("SMILE")) throw  new UnknownFormatConversionException(url+" contains string SMILE");
		}
		smilesMap.put(url,result);
		Tools.endMethod(result);
	  return result;
  }

	private String getDrugbankSmiles(URL url) throws IOException {
		Tools.startMethod("getDrugbankSmiles("+url+")");
		String[] lines=PageFetcher.fetchLines(url);
		String smiles=null;
		for (int i=0; i<lines.length; i++){
			if (lines[i].contains("<td>SMILES<")) {
				String line=Tools.removeHtml(lines[++i]).trim();
				while (line.length()==0){
					line=Tools.removeHtml(lines[++i]).trim();					
				}
				smiles = line;;
				break;
			}
		}
		Tools.endMethod(smiles);
		return smiles;
  }

	private String getChEBISmiles(URL url) throws IOException {
		Tools.startMethod("getChEBISmiles("+url+")");
		String[] lines=PageFetcher.fetchLines(url);
		String smiles=null;
		for (int i=0; i<lines.length; i++){
			if (lines[i].contains(">SMILES<")) {
				String line=Tools.removeHtml(lines[++i]).trim();
				while (line.length()==0){
					line=Tools.removeHtml(lines[++i]).trim();					
				}
				smiles = line;;
				Tools.note("Smiles found: "+smiles+" on "+url);
				break;
			}
		}
		Tools.endMethod(smiles);
		return smiles;
  }

	private String get3dMetSmiles(URL url) throws IOException {
		Tools.startMethod("get3dMetSmiles("+url+")");
		String[] lines=PageFetcher.fetchLines(url);
		String smiles=null;
		for (int i=0; i<lines.length; i++){
			if (lines[i].contains("SMILES<")) {
				smiles = Tools.removeHtml(lines[++i]).trim();
				break;
			}
		}
		Tools.endMethod(smiles);
	  return smiles;
  }
}
