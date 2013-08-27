package edu.fsuj.csb.reactionnetworks.interaction.gui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import edu.fsuj.csb.gui.HorizontalPanel;
import edu.fsuj.csb.gui.PanelTools;
import edu.fsuj.csb.gui.VerticalPanel;
import edu.fsuj.csb.reactionnetworks.organismtools.DbCompartment;
import edu.fsuj.csb.tools.organisms.Compartment;
import edu.fsuj.csb.tools.organisms.Reaction;
import edu.fsuj.csb.tools.organisms.Substance;
import edu.fsuj.csb.tools.organisms.gui.CompartmentNode;
import edu.fsuj.csb.tools.organisms.gui.ReactionNode;
import edu.fsuj.csb.tools.organisms.gui.SubstanceNode;
import edu.fsuj.csb.tools.organisms.gui.URLNode;
import edu.fsuj.csb.tools.xml.ObjectComparator;

public class ResultPanel extends VerticalPanel implements ActionListener, TreeSelectionListener, MouseListener {

	private ResultTreeRoot resultTreeRoot;
	private JScrollPane scrollpane;
	private JButton clearButton, exportButton;
	private ResultTree resultTree;
	private HorizontalPanel buttonPanel;
	private static final Dimension initialSize=new Dimension(600, 600);
	private TreeSet<ActionListener> listeners;

	public ResultPanel() {
		resultTreeRoot = new ResultTreeRoot(this);
		resultTree = new ResultTree(resultTreeRoot);
		resultTree.addTreeSelectionListener(this);
		resultTree.addMouseListener(this);
		resultTree.addActionListener(this);
		scrollpane = new JScrollPane(resultTree);
		scrollpane.setPreferredSize(initialSize);
		add(scrollpane);
		buttonPanel = new HorizontalPanel();

		clearButton = new JButton("clear list");
		clearButton.addActionListener(this);
		buttonPanel.add(clearButton);

		exportButton = new JButton("export");
		exportButton.addActionListener(this);
		buttonPanel.add(exportButton);
		buttonPanel.scale();
		add(buttonPanel);
		scale();
	}

	private static final long serialVersionUID = 7742166075954843594L;

	public ResultTree getTree() {
		return resultTree;
	}

	public void actionPerformed(ActionEvent arg0) {
		Object source = arg0.getSource();
		if (source == clearButton) resultTreeRoot.removeAllChildren();
		if (source == exportButton) export();
		if (source == resultTree) activate();
	}
	
	public void activate() {
		fireEvent(new ActionEvent(this, 0, "activate"));
  }
	
	public void addActionListener(ActionListener l){
		if (listeners==null) listeners=new TreeSet<ActionListener>(ObjectComparator.get());
		listeners.add(l);
	}

	private void fireEvent(ActionEvent actionEvent) {
		if (listeners==null) return;
		for (ActionListener listener:listeners){
			listener.actionPerformed(actionEvent);
		}
  }

	private void export()  {
		URL filename = PanelTools.showSelectFileDialog("SELECT PREFIX", null, null, this);
		Component root = SwingUtilities.getRoot(this);
		root.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		try {
			export(filename.toString().replace("file:", ""), resultTreeRoot, null);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
	    e.printStackTrace();
    } catch (SQLException e) {
	    e.printStackTrace();
    } catch (DataFormatException e) {
	    e.printStackTrace();
    }
		root.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

	}

	private void writeHead(BufferedWriter bw) throws IOException {
		bw.write("<html><head>\n<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n");
		bw.write("<style type=\"text/css\">\n");
		bw.write("li.collapsibleListOpen{\n");
		bw.write("list-style-image:url('http://code.stephenmorley.org/javascript/collapsible-lists/button-open.png');\n");
		bw.write("cursor:pointer;\n");
		bw.write("}\n");
		bw.write("li.collapsibleListClosed{\n");
		bw.write("list-style-image:url('http://code.stephenmorley.org/javascript/collapsible-lists/button-closed.png');\n");
		bw.write("cursor:pointer;\n");
		bw.write("}\n");
		bw.write("</style>\n");
		bw.write("<script type=\"text/javascript\">\n");
		bw.write("var CollapsibleLists=new function(){\n");
		bw.write("  this.apply=function(_1){\n");
		bw.write("    var _2=document.getElementsByTagName(\"ul\");\n");
		bw.write("    for(var _3=0;_3<_2.length;_3++){\n");
		bw.write("      if(_2[_3].className.match(/(^| )collapsibleList( |$)/)){\n");
		bw.write("        this.applyTo(_2[_3],true);\n");
		bw.write("        if(!_1){\n");
		bw.write("          var _4=_2[_3].getElementsByTagName(\"ul\");\n");
		bw.write("          for(var _5=0;_5<_4.length;_5++){\n");
		bw.write("            _4[_5].className+=\" collapsibleList\";\n");
		bw.write("          }\n");
		bw.write("        }\n");
		bw.write("      }\n");
		bw.write("    }\n");
		bw.write("  };\n");
		bw.write("  this.applyTo=function(_6,_7){\n");
		bw.write("    var _8=_6.getElementsByTagName(\"li\");\n");
		bw.write("    for(var _9=0;_9<_8.length;_9++){\n");
		bw.write("      if(!_7||_6==_8[_9].parentNode){\n");
		bw.write("        if(_8[_9].addEventListener){\n");
		bw.write("          _8[_9].addEventListener(\"mousedown\",function(e){\n");
		bw.write("            e.preventDefault();\n");
		bw.write("          },false);\n");
		bw.write("        }else{\n");
		bw.write("          _8[_9].attachEvent(\"onselectstart\",function(){\n");
		bw.write("            event.returnValue=false;\n");
		bw.write("          });\n");
		bw.write("        }\n");
		bw.write("        if(_8[_9].addEventListener){\n");
		bw.write("          _8[_9].addEventListener(\"click\",_a(_8[_9]),false);\n");
		bw.write("        }else{\n");
		bw.write("          _8[_9].attachEvent(\"onclick\",_a(_8[_9]));\n");
		bw.write("        }\n");
		bw.write("        _b(_8[_9]);\n");
		bw.write("      }\n");
		bw.write("    }\n");
		bw.write("  };\n");
		bw.write("  function _a(_c){\n");
		bw.write("    return function(e){\n");
		bw.write("      if(!e){\n");
		bw.write("        e=window.event;\n");
		bw.write("      }\n");
		bw.write("      if(_c==(e.target?e.target:e.srcElement)){\n");
		bw.write("        _b(_c);\n");
		bw.write("      }\n");
		bw.write("    };\n");
		bw.write("  };\n");
		bw.write("  function _b(_d){\n");
		bw.write("    var _e=_d.className.match(/(^| )collapsibleListClosed( |$)/);\n");
		bw.write("    var _f=_d.getElementsByTagName(\"ul\");\n");
		bw.write("    for(var _10=0;_10<_f.length;_10++){\n");
		bw.write("      var li=_f[_10];\n");
		bw.write("      while(li.nodeName!=\"LI\"){\n");
		bw.write("        li=li.parentNode;\n");
		bw.write("      }\n");
		bw.write("      if(li==_d){\n");
		bw.write("        _f[_10].style.display=(_e?\"block\":\"none\");\n");
		bw.write("      }\n");
		bw.write("    }\n");
		bw.write("    _d.className=_d.className.replace(/(^| )collapsibleList(Open|Closed)( |$)/,\"\");\n");
		bw.write("    if(_f.length>0){\n");
		bw.write("      _d.className+=\" collapsibleList\"+(_e?\"Open\":\"Closed\");\n");
		bw.write("    }\n");
		bw.write("  };\n");
		bw.write("}();\n</script>\n");
		bw.write("</head>");
	}

	private String export(String filePrefix, DefaultMutableTreeNode treeRoot, String backlink) throws IOException, URISyntaxException, SecurityException, SQLException, DataFormatException {
		String filename = filePrefix + ".html";
		BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
		writeHead(bw);
		writeBody(bw, filePrefix, treeRoot, backlink);
		closeBody(bw);
		return filename;
	}

	private void writeBody(BufferedWriter bw, String filename, DefaultMutableTreeNode treeRoot, String backlink) throws IOException, SecurityException, URISyntaxException, SQLException, DataFormatException {
		bw.write("<body>");
		writeParseTree(bw, treeRoot, filename, backlink);
	}

	private void writeParseTree(BufferedWriter bw, DefaultMutableTreeNode treeRoot, String filename, String backlink) throws IOException, SecurityException, URISyntaxException, SQLException, DataFormatException {
		bw.write(treeRoot + "\n");
		if (backlink != null) bw.write(backlink + "\n");
		appendChildren(treeRoot, bw, filename);
	}

	private void appendChildren(DefaultMutableTreeNode treeRoot, BufferedWriter bw, String filename) throws IOException, SecurityException, URISyntaxException, SQLException, DataFormatException {
		@SuppressWarnings("unchecked")
		Enumeration<DefaultMutableTreeNode> children = treeRoot.children();
		if (children.hasMoreElements()) {
			bw.write("<ul class=\"collapsibleList\">\n");
			while (children.hasMoreElements()) {
				DefaultMutableTreeNode child = children.nextElement();
				bw.write("<li>\n");

				String alternateText = null;
				if (child.toString().startsWith("Task ")) alternateText = exportTask(child, filename);
				if (child instanceof SubstanceNode) alternateText = exportSubstance(((SubstanceNode) child).substance().id());
				if (child instanceof CompartmentNode) alternateText = exportCompartment((CompartmentNode) child);
				if (child instanceof ReactionNode) alternateText = exportReaction((ReactionNode) child);
				if (child instanceof URLNode) alternateText = exportUrlNode((URLNode) child);
				if (alternateText == null) {
					writeParseTree(bw, child, filename, null);
				} else bw.write(alternateText);
				bw.write("</li>\n");
			}
			bw.write("</ul>\n");
		}

	}

	private String exportUrlNode(URLNode child) {
		return "<a href=\"" + child + "\">" + child + "</a>\n";
	}

	private String exportTask(DefaultMutableTreeNode treeRoot, String filename) throws IOException, URISyntaxException, SecurityException, SQLException, DataFormatException {
		return "<a href=\"" + export(filename + " - " + treeRoot, treeRoot, null) + "\">" + treeRoot + "</a>";
	}

	private void closeBody(BufferedWriter bw) throws IOException {
		bw.write("<script type=\"text/javascript\">\n");
		bw.write("CollapsibleLists.apply();");
		bw.write("</script>\n</body></html>");
		bw.close();
	}

	private String exportReaction(ReactionNode rn) throws MalformedURLException, SQLException, DataFormatException {
		Reaction r = Reaction.get(rn.reactionId());
		Iterator<URL> it = r.urls().iterator();
		StringBuffer output;

		if (it.hasNext()) {
			output = new StringBuffer("<a href=\"" + it.next() + "\">" + r.mainName() + "</a> (internal id: " + r.id() + ")");
		} else output = new StringBuffer(r.mainName());
		while (it.hasNext())
			output.append(", <a href=\"" + it.next() + "\">Link</a>");
		output.append("<br/>\nSubstrates:<br/>\n<ul>\n");
		for (Iterator<Integer> it2 = r.substrateIds().iterator(); it2.hasNext();) {
			output.append("<li>" + exportSubstance(it2.next()) + "</li>\n");
		}
		output.append("</ul>\n");
		output.append("Products:\n<ul>\n");
		for (Iterator<Integer> it2 = r.productIds().iterator(); it2.hasNext();) {
			output.append("<li>" + exportSubstance(it2.next()) + "</li>\n");
		}
		output.append("</ul>\n");
		return output.toString();
	}

	private String exportSubstance(int sid) throws MalformedURLException, SQLException, DataFormatException {
		Substance s = Substance.get(sid);
		Iterator<URL> it = s.urls().iterator();
		if (!it.hasNext()) return s.mainName() + " (internal id: " + s.id() + ")";
		StringBuffer output = new StringBuffer("<a href=\"" + it.next() + "\">" + s.mainName() + "</a> (internal id: " + s.id() + ")");
		while (it.hasNext())
			output.append(", <a href=\"" + it.next() + "\">Link</a>");
		return output.toString();
	}

	private String exportCompartment(CompartmentNode cn) throws MalformedURLException, SQLException, DataFormatException {
		Compartment c = DbCompartment.load(cn.compartment().id());
		Iterator<URL> it = c.urls().iterator();
		if (!it.hasNext()) return c.mainName() + " (internal id: " + c.id() + ")";
		StringBuffer output = new StringBuffer("<a href=\"" + it.next() + "\">" + c.mainName() + "</a> (internal id: " + c.id() + ")");
		while (it.hasNext())
			output.append(", <a href=\"" + it.next() + "\">Link</a>");
		return output.toString();
	}

	public void valueChanged(TreeSelectionEvent event) {
		Object lastSelected = resultTree.getLastSelectedPathComponent();
		try {
			if (lastSelected instanceof SubstanceNode) {
				SubstanceNode sn = ((SubstanceNode) lastSelected);
				sn.loadDetails();
			}
			if (lastSelected instanceof ReactionNode) {
				ReactionNode rn = ((ReactionNode) lastSelected);
				rn.loadDetails();
			}
			if (lastSelected instanceof CompartmentNode) {
				CompartmentNode cn = ((CompartmentNode) lastSelected);
				cn.loadDetails();
			}
			resultTreeRoot.update();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (DataFormatException e) {
	    e.printStackTrace();
    } catch (IOException e) {
	    e.printStackTrace();
    }
	}

	@Override
	public void setSize(Dimension d) {
	  super.setSize(d);
	  d=new Dimension(d.width-20,d.height-90);
	  scrollpane.setPreferredSize(d);
	}
	
	public void mouseClicked(MouseEvent e) {
		if (e.getButton()==MouseEvent.BUTTON3) showPopupForComponentAt(e.getPoint(),e.getLocationOnScreen());
  }
	
	private void showPopupForComponentAt(Point pos1,Point pos2) {
    TreePath path = resultTree.getPathForLocation(pos1.x,pos1.y);
		if (path == null) return;
		try {
	    PopupMenu.showPopupFor(path.getLastPathComponent(),pos2,this);
    } catch (SQLException e) {
	    e.printStackTrace();
    } catch (DataFormatException e) {
	    e.printStackTrace();
    } catch (IOException e) {
	    e.printStackTrace();
    }
	}  

	public void mouseEntered(MouseEvent arg0) {
	  // TODO Auto-generated method stub
	  
  }

	public void mouseExited(MouseEvent arg0) {
	  // TODO Auto-generated method stub
	  
  }

	public void mousePressed(MouseEvent arg0) {
	  // TODO Auto-generated method stub
	  
  }

	public void mouseReleased(MouseEvent arg0) {
	  // TODO Auto-generated method stub
	  
  }
}
