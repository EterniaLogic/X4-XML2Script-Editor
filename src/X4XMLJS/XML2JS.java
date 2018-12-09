package X4XMLJS;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.text.html.parser.AttributeList;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class XML2JS {
	public String floc;
	public boolean saved=false;
	public String saveloc="";
	
	public XML2JS(String fileloc) {
		floc = fileloc;
	}
	
	public XML2JS() {
		floc = ""; // no reference XML file
	}
	
	public String getJS() {
		String text="";
		
		// <?xml version="1.0" encoding="iso-8859-1" ?>
		// <??? name="boarding.pod.return" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="aiscripts.xsd">
		// </???>
		
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new File(floc));
			
			doc.getDocumentElement().normalize();
			//System.out.println("Root element :" + doc.getDocumentElement().getNodeName()+"\n\n");
			
			org.w3c.dom.Element root = doc.getDocumentElement();
			
			
			
			
			// loop through parameters for root
			String[] v = Utils.strAttrs(root,true,new String[] {"name"});
			text += root.getNodeName()+" "+Utils.getAttrValue(root,"name")+"{ // "+v[1]+"{"+v[0]+"}";
			
			text += "\n";
			
			text += processInnerJS(root);
			
			// REGEX: prevent endbrackets on the same line
			//text = text.replaceAll("$}}", "}\n}");
			text = text.replaceAll("}([ ]+})", "}\n$1");// prevent }      }
			text = text.replaceAll("}([ ]+else)", "}\n$1"); // prevent }     else{
			text = text.replaceAll("}\n[ ]+(else)", "}$1");
			text = text.replaceAll("}([ ]+[^e].*)", "}\n$1");
			
			for(int i=0;i<20;i++) // remove excessive spaces within statements
				text = text.replaceAll("(\n\\s{2,})(([^ \n]+ )+)(\\s{4,})", "$1$2");
			
			text += "\n}";
			
		}catch(Exception e) {
			e.printStackTrace();
			
		}
		
		//System.out.println(text);
		return text;
	}
	
	public boolean setJS(String js) {
		// convert js back to xml
		String[] lines = js.split("\n");
		
		return true; // return false if the js cannot validate XML
	}
	
	
	
	public String processInnerJS(Node root) {
		String text="";
		// inner elements
		for(Node child : Utils.getChildren(root)) {
			if(child.getNodeName().equals("#comment")) text += Utils.toTab(1)+Utils.handleComment(child.getNodeValue(),1,false);
			
			// aiscripts
			if(child.getNodeName().equals("order")) {
				text += processOrder(child,1)+"\n";
			}else if(child.getNodeName().equals("params")) {
				text += processParams(child,1)+"\n";
			}else if(child.getNodeName().equals("param ")) {
				//text += processParams_param(1,"",child)+"\n";
			}else if(child.getNodeName().equals("interrupts")) {
				text += processInterrupts(child,1)+"\n";
			}else if(child.getNodeName().equals("init")) {
				text += Utils.toTab(1)+"function init(){\n";
					text += handleActions(child,2);
				text += Utils.toTab(1)+"}\n";
			}else if(child.getNodeName().equals("attention")) {
				text += processAttention(child,1)+"\n";
			}else if(child.getNodeName().equals("on_abort")) {
				text += Utils.toTab(1)+"function on_abort(){\n";
				text += handleActions(child,2);
				text += Utils.toTab(1)+"}\n";
				
			// mdscripts
			}else if(child.getNodeName().equals("cues")) { // md script cue
				
			}else if(child.getNodeName().equals("library")) { // Libraries act kind of like functions that are referenced in the cues (via <include_actions ref="BoardShip__Standard_FindMilitaryTarget"/>)
				
			}else {}
		}
		return text;
	}
	
	
	// Process <order > tag
	private String processOrder(Node child, int tabulation) {
		String text = Utils.toTab(tabulation)+"order("+Utils.strAttrsValOnly(child)+"){"+Utils.strAttrsComment(child)+"\n";
		
		for(Node child2 : Utils.getChildren(child)) {
			if(child2.getNodeName().equals("#comment")) text += Utils.toTab(tabulation)+Utils.handleComment(child2.getNodeValue(),tabulation,false);
			
			if(child2.getNodeName().equals("params")) {
				text += processParams(child2,tabulation+1);
			}else if(child2.getNodeName().equals("skill")) {
				text += Utils.toTab(tabulation+1)+"skill("+Utils.strAttrsValOnly(child2)+");"+Utils.strAttrsComment(child2)+"\n"; //+"("+handleConds(child2)+")";
			}
		}
		
		return text+Utils.toTab(tabulation)+"}\n\n";
	}
	
	// Process basic parameters
	private String processParams(Node child, int tabulation) {
		String text="";
		
		for(Node child2 : Utils.getChildren(child)) {
			if(child2.getNodeName().equals("#comment")) text += Utils.toTab(tabulation)+Utils.handleComment(child2.getNodeValue(),tabulation,false);
			
			if(child2.getNodeName().equals("param")) {
				text = processParams_param(tabulation, text, child2);
			}
		}
		
		return text;
	}

	private String processParams_param(int tabulation, String text, Node child2) {
		// single <param name="???" />
		String name = Utils.getAttrValue(child2,"name");
		String type = Utils.getAttrValue(child2,"type").equals("") ? "" : Utils.getAttrValue(child2,"type")+" ";
		//String value = getAttrValue(child2,"value");
		List<Node> attrs = Utils.getAttrs(child2);
		String paramnamescomment="";
		String paramvals="";
		String paramcom="";
		String comment="";
		
		for(Node n : attrs) {
			if(n.getNodeName().equals("comment")) {
				comment = n.getNodeValue()+" ";
			}else if(!n.getNodeName().equals("name") && !n.getNodeName().equals("type")) {
				paramnamescomment += n.getNodeName()+", ";
				paramvals += "\""+n.getNodeValue()+"\", ";
			}
		}
		
		// trim text, the lazy way
		if(paramnamescomment.length() > 2)	paramnamescomment=paramnamescomment.substring(0, paramnamescomment.length()-2);
		if(paramnamescomment.length() > 2) paramvals=paramvals.substring(0, paramvals.length()-2);
		
		//if(!value.equals("")) paramnamescomment+="value";
		if(paramnamescomment.length() > 0) paramcom=" // "+comment+"{"+paramnamescomment+"}";
		if(paramcom.length()==0 && comment.length()>0) paramcom = " // "+comment; 
		
		
		
		if(child2.hasChildNodes()) {
			// handle... input_param nodes
			text += Utils.toTab(tabulation)+"param "+type+name+"("+paramvals+"){"+paramcom+"\n";
			for(Node n : Utils.getChildren(child2)) {
				String c = "";
				if(!Utils.getAttrValue(n,"comment").equals("")) c = " // "+Utils.getAttrValue(n,"comment"); 
				text += Utils.toTab(tabulation+1)+n.getNodeName()+" "+Utils.getAttrValue(n,"name")+" = \""+Utils.getAttrValue(n,"value")+"\";"+c+"\n";
			}
			text += Utils.toTab(tabulation)+"}\n";
		}else {
			text += Utils.toTab(tabulation)+"param "+type+name+"("+paramvals+");"+paramcom+"\n";
		}
		
		System.out.println("PRPCESS PARAM: "+name);
		
		return text;
	}
	
	// process interrupts for aiscripts
	private String processInterrupts(Node child, int tabulation) {
		String text="";
		
		// Standard layout
		// <interrupts>
		//	<handler ... />
		//  <handler ...> <conditions></conditions> <actions></actions>  </handler>
		// </interrupts>
		
		// Single interrupt inline with code
		// <interrupt> <conditions></conditions> <actions></actions> </interrupt>
		
		if(child.getNodeName().equals("interrupt")) {
			text = processSingleInterrupt(child.getParentNode(), tabulation, text, child, true);
		}else {
			for(Node child2 : Utils.getChildren(child)) {
				if(child2.getNodeName().equals("#comment")) text += Utils.toTab(tabulation)+Utils.handleComment(child2.getNodeValue(),tabulation,false);
				
				if(child2.getNodeName().equals("handler")) {
					text = processSingleInterrupt(child, tabulation, text, child2, false);
				}
			}
		}
		
		return text;
	}

	private String processSingleInterrupt(Node parent, int tabulation, String text, Node child, boolean inline) {
		if(child.getChildNodes().getLength() > 0) {
			String conds = "";
			String actions = "";
			// handler <conditions> or <actions>
			for(Node child2 : Utils.getChildren(child)) {
				if(child2.getNodeName().equals("#comment")) text += Utils.toTab(tabulation)+Utils.handleComment(child2.getNodeValue(),tabulation,false);
				//System.out.println("interrupt handler - "+child3.getNodeName());
				
				if(child2.getNodeName().equals("conditions")) {
					System.out.println("conds");
					String c = handleConds(child2,true);
					if(!c.equals("")) {
						conds += c;
					}
				}else if(child2.getNodeName().equals("actions")) {
					String c = handleActions(child2,tabulation+1);
					if(!c.equals("")) {
						actions += c;
					}
				}
			}
			
			String comment = Utils.getAttrValue(parent,"comment");
			
			if(!Utils.getAttrValue(parent,"comment").equals(""))
				text += "// "+Utils.handleComment(comment,tabulation,false)+"\n";
			if(conds.equals("")) conds="TRUE";
			if(inline) 	text += Utils.toTab(tabulation)+"#interrupt_if("+conds+"){"+comment+"\n"+actions+"\n"+Utils.toTab(tabulation)+"};\n";
			else 		text += Utils.toTab(tabulation)+"#interrupt_handler_if("+conds+"){"+comment+"\n"+actions+"\n"+Utils.toTab(tabulation)+"};\n";
		}else {
			String[] atcomment = Utils.strAttrs(child,true,new String[] {"ref"});
			String ref=Utils.getAttrValue(child,"ref");
			if(!ref.equals("")) {
				String comment = !atcomment[1].equals("") ? " // "+atcomment[1] : "";
				if(inline) 	text += Utils.toTab(tabulation)+"#interrupt_ref("+ref+");"+comment+"\n";
				else		text += Utils.toTab(tabulation)+"#interrupt_handler_ref("+ref+");"+comment+"\n";
			}else {
				if(inline) 	text += Utils.toTab(tabulation)+"#interrupt_ ?; // "+atcomment[1]+"{"+atcomment[0]+"}\n";
				else		text += Utils.toTab(tabulation)+"#interrupt_handler ?; // "+atcomment[1]+"{"+atcomment[0]+"}\n";
			}
		}
		return text;
	}
	
	// conditionals for interrupts
	private String handleConds(Node child, boolean topmost) {
		String text="";
		
		//System.out.println(child.getNodeName());
		List<Node> list = Utils.getChildren(child);
		for(Node child2 : list) {
			String com="";
			if(child2.getNodeName().equals("#comment")) text += Utils.toTab(1)+Utils.handleComment(child2.getNodeValue(),1,true);
			else {
				if(child2.hasChildNodes()) {
					text += child2.getNodeName()+"("+handleConds(child2,false)+")";
				}else if(child2.getNodeName().equals("check_value")){
					text += Utils.getAttrValue(child2,"value");
					if(!Utils.getAttrValue(child2, "comment").equals("")) 
						text += " /* "+Utils.getAttrValue(child2, "comment")+" */";
				}else {
					String[] atcomment = Utils.strAttrs(child2,false,null);
					text += child2.getNodeName()+"("+atcomment[0]+")";
				}
				
				if(list.get(list.size()-1) != child2) {
					text += " && ";
					if(topmost) { 
						if(!com.equals("")) text += com;
						text += "\n"+Utils.toTab(7);
					}
					
				}
			}
		}
		
		// remove ... 'extras'
		
		return text;
	}
	
	// Actual code
	private String handleActions(Node child, int tabulation) {
		String text="";
		
		//if(child.getChildNodes().getLength() > 0) text += "\n";
		
		
		//System.out.println(child.getNodeName());
		boolean last_if=false;
		// Standardized scripting language
		List<Node> list = Utils.getChildren(child);
		for(Node child2 : list) {
			String nodeName = child2.getNodeName();
			if(nodeName.equals("#text")) continue;
			if(last_if && !(nodeName.equals("do_else") || nodeName.equals("do_elseif"))) text += "\n";
			String comment = Utils.getAttrValue(child2, "comment");
			String commentx = comment.length() > 0 ? " // "+comment : "";
			
			// "+Utils.strAttrsComment(child2)+"
			
			
			
			// loops
			if(nodeName.equals("do_if")) {
				// pre-space it out   getTabs(tabulation)+"\n"+
				String s = Utils.strAttrs_tocomment(child2,true,new String[] {"value"});
				text += Utils.toTab(tabulation)+"if("+Utils.getAttrValue(child2,"value")+"){"+s+"\n";
				text += handleActions(child2,tabulation+1);
				text += Utils.toTab(tabulation)+"}";// errors sometimes!
				last_if=true;
			}else if(nodeName.equals("do_elseif")) {
				if(!last_if) text += Utils.toTab(tabulation);
				String s = Utils.strAttrs_tocomment(child2,true,new String[] {"value"});
				text += "else if("+Utils.getAttrValue(child2,"value")+"){"+s+"\n";
				text += handleActions(child2,tabulation+1);
				text += Utils.toTab(tabulation)+"}"; // errors sometimes!
				last_if=true;
			}else if(nodeName.equals("param")) {// <param name="?" value=""
				text += processParams_param(tabulation, "", child2);
			}else if(nodeName.equals("do_else")) { // keeps on messing up?
				if(!last_if) text += Utils.toTab(tabulation);
				String s = Utils.strAttrs_tocomment(child2,true,null);
				text += "else{"+s+"\n";
				text += handleActions(child2,tabulation+1);
				text += Utils.toTab(tabulation)+"}\n";
			}else if(nodeName.equals("do_while")) {
				String s = Utils.strAttrs_tocomment(child2,true,new String[] {"value"});
				text += Utils.toTab(tabulation)+"while("+Utils.getAttrValue(child2,"value")+"){"+s+"\n";
				text += handleActions(child2,tabulation+1);
				text += Utils.toTab(tabulation)+"}\n";
			}else if(nodeName.equals("do_any")) { // uh... ok
				String s = Utils.strAttrs_tocomment(child2,true,null);
				text += Utils.toTab(tabulation)+"any{"+s+"\n";
				text += handleActions(child2,tabulation+1);
				text += Utils.toTab(tabulation)+"}\n";
			}else if(nodeName.equals("interrupt")) {
				text += processInterrupts(child2,tabulation);
			}else if(nodeName.equals("interrupts")) {
				//text += processInterrupts(child2,tabulation);
			}else if(nodeName.equals("set_value")) {
				
				if(Utils.getAttrValue(child2,"exact").equals("") && Utils.getAttrValue(child2,"exact").equals("")) {
					String s = Utils.strAttrs_tocomment(child2,true,new String[] {"name"});
					text += Utils.toTab(tabulation)+"create "+Utils.getAttrValue(child2,"name")+";"+s+"\n";
				}else {
					String s = Utils.strAttrs_tocomment(child2,true,new String[] {"name","exact"});
					text += Utils.toTab(tabulation)+Utils.getAttrValue(child2,"name")+" = "+Utils.getAttrValue(child2,"exact")+";"+s+"\n";
				}
			}else if(nodeName.equals("remove_value")) {
				String s = Utils.strAttrs_tocomment(child2,true,new String[] {"name"});
				text += Utils.toTab(tabulation)+"delete "+Utils.getAttrValue(child2,"name")+";"+s+"\n";
			}else if(nodeName.equals("run_script")) { // Script?
				String s = Utils.strAttrs_tocomment(child2,true,new String[] {"name"});
				text += Utils.toTab(tabulation)+"run_script "+Utils.getAttrValue(child2,"name")+"{"+s+"\n";
				text += handleActions(child2,tabulation+1);
				text += Utils.toTab(tabulation)+"}\n";
			}else if(nodeName.equals("#comment")) { 
				text += Utils.toTab(tabulation)+"\n"+Utils.handleComment(child2.getNodeValue(),tabulation,false)+"\n";
			}else{
				if(child2.hasChildNodes()) {
					text += Utils.toTab(tabulation)+nodeName+"("+Utils.strAttrsValOnly(child2)+"){"+Utils.strAttrsComment(child2)+"\n";
					text += handleActions(child2,tabulation+1);
					text += Utils.toTab(tabulation)+"}\n";
				}else if(nodeName.equals("break") || nodeName.equals("return") || nodeName.equals("continue")) {
					text += Utils.toTab(tabulation)+nodeName+";"+Utils.strAttrs_tocomment(child2,false,null)+"\n"; //+"("+handleConds(child2)+")";
				}else {
					text += Utils.toTab(tabulation)+nodeName+"("+Utils.strAttrsValOnly(child2)+");"+Utils.strAttrsComment(child2)+"\n"; //+"("+handleConds(child2)+")";
				}
			}
			
			if(!nodeName.equals("do_if")) last_if=false;
			if(list.get(list.size()-1) == child2 && nodeName.equals("do_if"))	text += "\n";
		}
		
		//if(!text.equals("")) text += getTabs(tabulation-1);
		
		return text;
	}
	
	// attention unknown?
	private String processAttention(Node child, int tabulation) {
		String text="";
		
		text += Utils.toTab(tabulation)+"function attention("+Utils.getAttrValue(child,"min")+"){\n";
		
		for(Node child2 : Utils.getChildren(child)) {
			if(child2.getNodeName().equals("#comment")) text += Utils.toTab(tabulation)+Utils.handleComment(child2.getNodeValue(),tabulation,false);
			if(child2.getNodeName().equals("actions")) {
				text += handleActions(child2, tabulation+1);
			}
		}
		text += Utils.toTab(tabulation)+"}\n";
		
		return text;
	}
}
