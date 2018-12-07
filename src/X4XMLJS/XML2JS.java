package X4XMLJS;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
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
import org.w3c.dom.NamedNodeMap;
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
			
			text += root.getNodeName()+" "+getAttrValue(root,"name")+"{ // {"+strAttrs(root)+"}";
			
			text += "\n";
			
			text += processInnerJS(root);
			
			
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
		for(Node child : getChildren(root)) {
			if(child.getNodeName().equals("#comment")) text += toTab(1)+handleComment(child.getNodeValue(),1);
			
			// aiscripts
			if(child.getNodeName().equals("order")) {
				text += processOrder(child,1)+"\n";
			}else if(child.getNodeName().equals("params")) {
				text += processParams(child,1)+"\n";
			}else if(child.getNodeName().equals("interrupts")) {
				text += processInterrupts(child,1)+"\n";
			}else if(child.getNodeName().equals("init")) {
				text += toTab(1)+"function init(){\n";
				text += handleActions(child,2);
				text += toTab(1)+"}\n";
			}else if(child.getNodeName().equals("attention")) {
				text += processAttention(child,1)+"\n";
			}else if(child.getNodeName().equals("on_abort")) {
				text += toTab(1)+"function on_abort(){\n";
				text += handleActions(child,2);
				text += toTab(1)+"}\n";
				
			// mdscripts
			}else if(child.getNodeName().equals("cues")) {
				
			}else if(child.getNodeName().equals("library")) { // Libraries act kind of like functions that are referenced in the cues (via <include_actions ref="BoardShip__Standard_FindMilitaryTarget"/>)
				
			}else {}
		}
		return text;
	}
	
	
	// Process <order > tag
	private String processOrder(Node child, int tabulation) {
		String text = toTab(tabulation)+"order("+strAttrsOnly(child)+"){"+strAttrsComment(child)+"\n";
		
		for(Node child2 : getChildren(child)) {
			if(child2.getNodeName().equals("#comment")) text += toTab(tabulation)+handleComment(child2.getNodeValue(),tabulation);
			
			if(child2.getNodeName().equals("params")) {
				text += processParams(child2,tabulation+1);
			}else if(child2.getNodeName().equals("skill")) {
				text += toTab(tabulation+1)+"skill("+strAttrsOnly(child2)+");"+strAttrsComment(child2)+"\n"; //+"("+handleConds(child2)+")";
			}
		}
		
		return text+toTab(tabulation)+"}\n\n";
	}
	
	// Process basic parameters
	private String processParams(Node child, int tabulation) {
		String text="";
		
		for(Node child2 : getChildren(child)) {
			if(child2.getNodeName().equals("#comment")) text += toTab(tabulation)+handleComment(child2.getNodeValue(),tabulation);
			
			if(child2.getNodeName().equals("param")) {
				// single <param name="???" />
				String name = getAttrValue(child2,"name");
				String type = getAttrValue(child2,"type").equals("") ? "" : getAttrValue(child2,"type")+" ";
				String value = getAttrValue(child2,"value");
				List<Node> attrs = getAttrs(child2);
				String paramnamescomment="";
				String paramvals="";
				for(Node n : attrs) {
					if(!n.getNodeName().equals("name") && !n.getNodeName().equals("type") && !n.getNodeName().equals("value")) {
						paramnamescomment += n.getNodeName()+", ";
						paramvals += "\""+n.getNodeValue()+"\", ";
					}
				}
				
				// trim text, the lazy way
				if(paramnamescomment.length() > 2)	paramnamescomment=paramnamescomment.substring(0, paramnamescomment.length()-2);
				if(paramnamescomment.length() > 2) paramvals=paramvals.substring(0, paramvals.length()-2);
				if(paramnamescomment.length() > 0) paramnamescomment=" // {"+paramnamescomment+"}";
				
				if(child2.hasChildNodes()) {
					text += toTab(tabulation)+"param "+type+name+"("+value+paramvals+"){"+paramnamescomment+"\n";
					for(Node n : getChildren(child2)) {
						text += toTab(tabulation+1)+n.getNodeName()+" "+getAttrValue(n,"name")+" = \""+getAttrValue(n,"value")+"\";\n";
					}
					text += toTab(tabulation)+"}\n";
				}else {
					text += toTab(tabulation)+"param "+type+name+"("+value+paramvals+");"+paramnamescomment+"\n";
				}
			}
		}
		
		return text;
	}
	
	// process interrupts for aiscripts
	private String processInterrupts(Node child, int tabulation) {
		String text="";
		
		for(Node child2 : getChildren(child)) {
			if(child2.getNodeName().equals("#comment")) text += toTab(tabulation)+handleComment(child2.getNodeValue(),tabulation);
			
			if(child2.getNodeName().equals("handler")) {
				if(child2.getChildNodes().getLength() > 0) {
					String conds = "";
					String actions = "";
					// handler <conditions> or <actions>
					for(Node child3 : getChildren(child2)) {
						if(child3.getNodeName().equals("#comment")) text += toTab(tabulation)+handleComment(child3.getNodeValue(),tabulation);
						//System.out.println("interrupt handler - "+child3.getNodeName());
						
						if(child3.getNodeName().equals("conditions")) {
							System.out.println("conds");
							String c = handleConds(child3,true);
							if(!c.equals("")) {
								conds += c;
							}
						}else if(child3.getNodeName().equals("actions")) {
							String c = handleActions(child3,tabulation+1);
							if(!c.equals("")) {
								actions += c;
							}
						}
					}
					
					if(!getAttrValue(child,"comment").equals(""))
						text += "// "+handleComment(getAttrValue(child,"comment"),tabulation)+"\n";
					if(conds.equals("")) conds="TRUE";
					text += toTab(tabulation)+"#interrupt_handler_if("+conds+"){\n"+actions+"\n"+toTab(tabulation)+"};\n";
				}else {
					String ref=getAttrValue(child2,"ref");
					if(!ref.equals("")) {
						text += toTab(tabulation)+"#interrupt_handler_ref("+ref+");\n";
					}else {
						text += toTab(tabulation)+"#interrupt_handler ?; // {"+strAttrs(child2)+"}\n";
					}
				}
			}
		}
		
		return text;
	}
	
	// conditionals for interrupts
	private String handleConds(Node child, boolean topmost) {
		String text="";
		
		//System.out.println(child.getNodeName());
		List<Node> list = getChildren(child);
		for(Node child2 : list) {
			String com="";
			if(child2.getNodeName().equals("#comment")) text += toTab(1)+handleComment(child2.getNodeValue(),1);
			else {
				if(child2.hasChildNodes()) {
					text += child2.getNodeName()+"("+handleConds(child2,false)+")";
				}else if(child2.getNodeName().equals("check_value")){
					text += getAttrValue(child2,"value");
				}else {
					text += child2.getNodeName()+"("+strAttrsOnly(child2)+")";
					com += strAttrsComment(child2)+"";
				}
				
				if(list.get(list.size()-1) != child2) {
					text += " && ";
					if(topmost) { 
						if(!com.equals("")) text += com;
						text += "\n"+toTab(7);
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
		List<Node> list = getChildren(child);
		for(Node child2 : list) {
			if(child2.getNodeName().equals("#text")) continue;
			if(last_if && !(child2.getNodeName().equals("do_else") || child2.getNodeName().equals("do_elseif"))) text += "\n";
			
			
			// loops
			if(child2.getNodeName().equals("do_if")) {
				// pre-space it out   getTabs(tabulation)+"\n"+
				text += toTab(tabulation)+"if("+getAttrValue(child2,"value")+"){\n";
				text += handleActions(child2,tabulation+1);
				text += toTab(tabulation)+"}";
				last_if=true;
			}else if(child2.getNodeName().equals("do_elseif")) {
				if(!last_if) text += toTab(tabulation);
				text += "else if("+getAttrValue(child2,"value")+"){\n";
				text += handleActions(child2,tabulation+1);
				text += toTab(tabulation)+"}";
				last_if=true;
			}else if(child2.getNodeName().equals("do_else")) { // keeps on messing up?
				if(!last_if) text += toTab(tabulation);
				text += "else{\n";
				text += handleActions(child2,tabulation+1);
				text += toTab(tabulation)+"}\n";
			}else if(child2.getNodeName().equals("do_while")) {
				text += toTab(tabulation)+"while("+getAttrValue(child2,"value")+"){\n";
				text += handleActions(child2,tabulation+1);
				text += toTab(tabulation)+"}\n";
			}else if(child2.getNodeName().equals("do_any")) { // uh... ok
				text += toTab(tabulation)+"any{\n";
				text += handleActions(child2,tabulation+1);
				text += toTab(tabulation)+"}\n";
			}else if(child2.getNodeName().equals("interrupt")) {
				processInterrupts(child2,tabulation);
			}else if(child2.getNodeName().equals("set_value")) {
				if(getAttrValue(child2,"exact").equals("") && getAttrValue(child2,"exact").equals("")) {
					text += toTab(tabulation)+"create "+getAttrValue(child2,"name")+";\n";
				}else {
					text += toTab(tabulation)+getAttrValue(child2,"name")+" = "+getAttrValue(child2,"exact")+";\n";
				}
			}else if(child2.getNodeName().equals("remove_value")) {
				text += toTab(tabulation)+"delete "+getAttrValue(child2,"name")+";\n";
			}else if(child2.getNodeName().equals("run_script")) { // Script?
				text += toTab(tabulation)+"run_script("+getAttrValue(child2,"name")+"){\n";
				text += handleActions(child2,tabulation+1);
				text += toTab(tabulation)+"}\n";
			}else if(child2.getNodeName().equals("#comment")) { 
				text += toTab(tabulation)+"\n"+handleComment(child2.getNodeValue(),tabulation)+"\n";
			}else{
				if(child2.hasChildNodes()) {
					text += toTab(tabulation)+child2.getNodeName()+"("+strAttrsOnly(child2)+"){"+strAttrsComment(child2)+"\n";
					text += handleActions(child2,tabulation+1);
					text += toTab(tabulation)+"}\n";
				}else {
					text += toTab(tabulation)+child2.getNodeName()+"("+strAttrsOnly(child2)+");"+strAttrsComment(child2)+"\n"; //+"("+handleConds(child2)+")";
				}
			}
			
			if(!child2.getNodeName().equals("do_if")) last_if=false;
			if(list.get(list.size()-1) == child2 && child2.getNodeName().equals("do_if"))	text += "\n";
		}
		
		//if(!text.equals("")) text += getTabs(tabulation-1);
		
		return text;
	}
	
	// attention unknown?
	private String processAttention(Node child, int tabulation) {
		String text="";
		
		text += toTab(tabulation)+"function attention("+getAttrValue(child,"min")+"){\n";
		
		for(Node child2 : getChildren(child)) {
			if(child2.getNodeName().equals("#comment")) text += toTab(tabulation)+handleComment(child2.getNodeValue(),tabulation);
			if(child2.getNodeName().equals("actions")) {
				text += handleActions(child2, tabulation+1);
			}
		}
		text += toTab(tabulation)+"}\n";
		
		return text;
	}
	
	
	
	
	
	
	
// HELPERS
	private String getAttrValue(Node child, String attrname) {
		NamedNodeMap m = child.getAttributes();
		String name="";
		String def="";
		
		// parameters for node
		for(Node n : getAttrs(child)) {
			if(n.getNodeName().equals(attrname)) {
				return n.getNodeValue();
			}
		}
		
		return "";
	}
	
	
	// Print attributes with a space between
	private String handleComment(String com, int tabs) {
		if(com.contains("\n")) {
			String[] lines = com.split("\n");
			String text="/* "+lines[0]+"\n";
			
			for(int i=1;i<lines.length;i++) {
				text += toTab(tabs+1)+lines[i]+"\n";
			}
			
			return text+toTab(tabs)+" */";
		}else return toTab(tabs)+"// "+com+"";
	}
	
	private String strAttrs(Node node) {
		String t = "";
		
		if(node.getNodeName().equals("#text")) return "";
		if(node == null) return "";
		
		
		NamedNodeMap m = node.getAttributes();
		for(int i=0;i<m.getLength();i++) {
			Node n = m.item(i);
			t += n.getNodeName()+"=\""+n.getNodeValue()+"\"";
			if(i < m.getLength()-1) t += ", ";
		}
		
		return t;
	}
	
	private String strAttrsOnly(Node node) {
		String t = "";
		
		if(node.getNodeName().equals("#text")) return "";
		if(node == null) return "";
		
		
		NamedNodeMap m = node.getAttributes();
		for(int i=0;i<m.getLength();i++) {
			Node n = m.item(i);
			t += "\""+n.getNodeValue()+"\"";
			if(i < m.getLength()-1) t += ", ";
		}
		
		return t;
	}
	
	private String strAttrsComment(Node node) {
		String t = "";
		
		if(node.getNodeName().equals("#text")) return "";
		if(node == null) return "";
		
		
		NamedNodeMap m = node.getAttributes();
		for(int i=0;i<m.getLength();i++) {
			Node n = m.item(i);
			t += n.getNodeName();
			if(i < m.getLength()-1) t += ", ";
		}
		
		if(t.length() > 0) t=" // {"+t+"}";
		
		return t;
	}
	
	public static String toTab(int tabulation) {
		String text="";
		for(int i=0;i<tabulation;i++) text += "    ";
		return text;
	}
	
	private List<Node> getAttrs(Node node){
		LinkedList<Node> list = new LinkedList<Node>();
		if(node.getNodeName().equals("#text")) return list;
		if(node == null) return list;
		
		
		NamedNodeMap m = node.getAttributes();
		for(int i=0;i<m.getLength();i++) {
			list.add(m.item(i));
		}
		
		return list;
	}
	
	private List<Node> getChildren(Node n){
		LinkedList<Node> list = new LinkedList<Node>();
		
		for(int j=0;j<n.getChildNodes().getLength();j++) {
			Node child = n.getChildNodes().item(j);
			if(child.getNodeName().equals("#text")) continue;
			
			list.add(child);
			
				
			/*if(child.getNodeName().equals("#comment")){
				// list.add(handleComment(child.getNodeValue()));
			}else {}*/
		}
		
		return list;
	}
}
