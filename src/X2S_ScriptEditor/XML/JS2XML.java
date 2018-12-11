package X2S_ScriptEditor.XML;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import X2S_ScriptEditor.Utils;

public class JS2XML {
	private static boolean interrupts=false, paramsx=false, interrupt_inline=false;
	private static PreXMLNode intnode = null, paramnode = null; 
	
	
	// Quite a few tags were stripped when it was converted to a script. Now, they must be put back in.
	public static String getXML(String js) {
		// First, determine what we are doing.
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n";
		
		// write out debug file
		try {
			BufferedWriter writer2 = new BufferedWriter(new FileWriter(System.getProperty("user.dir")+"/test.xml.script"));
			writer2.write(js);
		    writer2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// preprocess the script, combine lines and flatten.
		js = preprocess(js);
		
		
		// loop through and split up text by brackets into nodes
		PreXMLNode root = new PreXMLNode("root");
		addScriptNodes(js, root, 0);
		
		// report
		if(root.getChildren().size() > 0) {
			for(PreXMLNode n : root.getChildren()) {
				if(n != null)
					n.printnode(0);
				else System.out.println("null node");
			}
		}
		
		// convert nodes to XML
		if(root.getChildren().size() > 0) {
			for(PreXMLNode n : root.getChildren()) {
				if(n != null)
					xml += n.toXML(0);
				else xml += "null node\n";
			}
		}
		
		// verify XML
		
		// write out debug file
		try {
			BufferedWriter writer2 = new BufferedWriter(new FileWriter(System.getProperty("user.dir")+"/test2.xml.script"));
			writer2.write(js);
		    writer2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return xml;
	}
	
	// returns null if syntax error, secondary syntax detector will kick in
	private static void addScriptNodes(String innerjs, PreXMLNode parentnode, int bracket) {
		
		// loop through each character in each line to verify:
		//	A - is a container type?
		//  B - extract information from the tag
		
		boolean mlcomment = false; // used to work with /**/ comments
		LinkedList<String> mlcomment_text = new LinkedList<String>();
		
		String[] lines = innerjs.split("\n");
		for(int l=0;l<lines.length;l++) {
			
			// split line up by parts.
			String line = lines[l].trim();
			String text = line, comment = "", params="";
			
			if(line.length() == 0) continue;
			if(line.equals(";")) continue;
			
			System.out.println(Utils.toTab(bracket)+"process -- "+text);
			try {
				if(mlcomment) {
					if(text.contains("*/")) {
						System.out.println("mlcomment end");
						//int loc = text.indexOf("*/");
						//mlcomment_text.add(text.substring(loc+2,text.length()-1));
						mlcomment = false;
						
						// save node
						PreXMLNode comment1 = new PreXMLNode("#comment");
						for(String cline : mlcomment_text) {
							comment1.value += cline;
							if(!cline.equals(mlcomment_text.get(mlcomment_text.size()-1))) 
								comment1.value+="\n";
						}
						
						mlcomment_text.clear();
						parentnode.addChild(comment1);
					}else {
						mlcomment_text.add(text);
					}
					
					continue;
				}else if(text.startsWith("/*")){
					mlcomment = true;
					System.out.println("mlcomment start");
					mlcomment_text.add(text.substring(0, text.length()));
					
					continue;
				}else {
					String[] stripped = Utils.stripTextParamsComment(text);
					text = stripped[0];
					params = stripped[1];
					comment = stripped[2]; // actual comment, not just a parameter {} comment.
				}
				
				
				if(text.length() > 0) {
					// does this line open a bracket of subnodes?
					
					if(text.endsWith("{")) {
						System.out.println("OPENBRACKET for "+text);
						
						PreXMLNode nodex = PreXMLNodeFactory.toPreXMLNode(text,params);
						if(comment.length() > 0) 
							nodex.putAttrNode("comment", comment);
						interrupt_nodehandle(text, nodex, parentnode);
						param_nodehandle(text, nodex, parentnode);
						
						if(interrupts && text.startsWith("#interrupt")) {
							//parentnode.addChild(intnode);
							//System.out.println("interrupts } add " + nodex.name);
						}else if(paramsx && text.startsWith("param ")) {
							//nodes.add(paramnode);
							System.out.println("params } add " + nodex.name);
						}else {
							parentnode.addChild(nodex);
						}
						
						
						// find the exit bracket, all lines in-between are considered inner script
						LinkedList<String> innerlines = new LinkedList<String>();
						int bcnt = 1; // count brackets
						//int a=l+1, b=l+1;
						for(int j=l+1;j<lines.length;j++) {
							String linex = lines[j];
							String textx = linex.contains("//") ? linex.substring(0,linex.indexOf("//")) : linex; // strip comment temporarily
							
							bcnt = Utils.countbrackets(textx, '{', '}', bcnt);
							//System.out.println("bcnt: "+bcnt+"  ---     "+textx);
							
							
							if((textx.contains("}") && bcnt <= 0) || (bcnt==1 && textx.startsWith("}") && textx.contains("{"))) {
								// ending, send innerjs to recursion
								//b=j;
								String innerlinesx = "";
								for(String s : innerlines) innerlinesx += s+"\n";
								
								
								addScriptNodes(innerlinesx, nodex, bracket+1); 
								
								System.out.println("CLOSEBRACKET LINE: "+j+" "+nodex.name);
								//nodex.children.addAll(nodesx);
								//nodex.addChildren(nodesx);
								
								
								
								
								
								//System.out.println("BRACKET: "+nodex.name+" attrcount:"+nodex.attributes.size());
								
								// jump forward
								if(textx.contains("else")) { // }else{ or }else if(){
									l=j-1;
								}else
									l=j;
								
								break;
							}else {
								//System.out.println("INNERLINE ["+j+"] for parent: "+nodex.name+"      =     "+linex);
								innerlines.add(linex);
							}
						}
					}else {
						
						
						if(!text.equals("}")) {
							//System.out.println("FUNCX("+comment);
							PreXMLNode nodex = PreXMLNodeFactory.toPreXMLNode(text,params);
							if(comment.length() > 0) 
								nodex.putAttrNode("comment", comment);
							
							interrupt_nodehandle_bracket(text, nodex, parentnode);
							param_nodehandle_bracket(text, nodex, parentnode);
							
						}else {
							//System.out.println("BRACKETEND");
						}
					}
				}else if(text.equals("") && !comment.equals("")){ // comment
					PreXMLNode comment1 = new PreXMLNode("#comment");
					comment1.value=comment;
					parentnode.addChild(comment1);
				}else if(text.equals("") && comment.equals("")){
					// nothing
				}else {
					System.out.println("not handled?");
				}
				
				// FUNCLIKE: param("byhostile", "true"); // {name=?, value=?}
				// LOOP: if(this.ship.jobexpired){
				// SETVAL: $debugchance = $init_debugchance;
				// DEL: delete this.$police_cleared;
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	
	
	private static void param_nodehandle_bracket(String text, PreXMLNode nodex, PreXMLNode parentnode) {
		if(parentnode.name.equals("create_order") || parentnode.name.equals("run_script")) {
			parentnode.addChild(nodex);
			return;
		}
		
		if(text.startsWith("param ") && !interrupts) {
			if(paramsx) {
				paramnode.addChild(nodex);
				System.out.println("params add " + nodex.name);
			}else {
				paramnode = new PreXMLNode("params");
				paramnode.addChild(nodex);
				paramsx = true;
				System.out.println("params start " + nodex.name);
			}
		}else if(paramsx) {
			if(!parentnode.name.equals("param")) {
				parentnode.addChild(paramnode);
				if(!interrupts) parentnode.addChild(nodex);
				paramnode=null;
				paramsx = false;
				System.out.println("params end " + nodex.name);
			}else {
				if(!interrupts) parentnode.addChild(nodex);
				System.out.println("params add innernode " + nodex.name);
			}
		}else if(!interrupts && !text.startsWith("#interrupt")) { // prevent dual-copy
			if(!parentnode.getChildren().contains(nodex))
				parentnode.addChild(nodex);
		}
	}

	
	private static void param_nodehandle(String text, PreXMLNode nodex, PreXMLNode parentnode) {
		if(parentnode.name.equals("create_order") || parentnode.name.equals("run_script")) {
			parentnode.addChild(nodex);
			return;
		}
		
		if(text.startsWith("param ") && !interrupts) {
			if(paramsx) {
				paramnode.addChild(nodex);
				System.out.println("params { add " + text);
			}else {
				paramnode = new PreXMLNode("params");
				paramnode.addChild(nodex);
				paramsx = true;
				System.out.println("params { start " + text);
			}
		}else if(paramsx) {
			parentnode.addChild(paramnode);
			paramnode=null;
			paramsx = false;
			System.out.println("params { end " + text);
		}else if(!interrupts && !text.startsWith("#interrupt")) { // prevent dual-copy
			//if(!parentnode.children.contains(nodex))
			//	parentnode.addChild(nodex);
		}
	}
	
	
	
	
	
	private static void interrupt_nodehandle_bracket(String text, PreXMLNode nodex, PreXMLNode parentnode) {
		if(parentnode.name.equals("create_order")) {
			parentnode.addChild(nodex);
			return;
		}
		
		if(text.startsWith("#interrupt_if") || text.startsWith("#interrupt_ref")) 
			interrupt_inline = true;
		
		if(text.startsWith("#interrupt")/* && !paramsx*/) {
			if(interrupts) {
				if(!interrupt_inline) intnode.addChild(nodex);
				else parentnode.addChild(nodex);
				System.out.println("interrupts add " + nodex.name);
			}else {
				if(!interrupt_inline) {
					intnode = new PreXMLNode("interrupts");
					intnode.addChild(nodex);
				}else parentnode.addChild(nodex);
				
				interrupts = true;
				System.out.println("interrupts start " + nodex.name);
			}
		}else if(interrupts) {
			if(!(parentnode.isInterruptAction(parentnode) || parentnode.name.startsWith("interrupt"))) {
				if(!interrupt_inline)
					parentnode.addChild(intnode);
				parentnode.addChild(nodex);
				
				intnode=null;
				interrupts = false;
				interrupt_inline = false;
				System.out.println("interrupts end " + nodex.name+"   PARENT: "+parentnode.name);
			}else {
				parentnode.addChild(nodex);
				System.out.println("interrupts add innernode " + nodex.name);
			}
			
		}else if(!paramsx && !text.startsWith("param ")) { // prevent dual-copy
			parentnode.addChild(nodex);
		}
	}
	
	
	private static void interrupt_nodehandle(String text, PreXMLNode nodex, PreXMLNode parentnode) {
		if(parentnode.name.equals("create_order")) {
			parentnode.addChild(nodex);
			return;
		}
		
		if(text.startsWith("#interrupt_if") || text.startsWith("#interrupt_ref")) 
			interrupt_inline = true;
		
		if(text.startsWith("#interrupt")/* && !paramsx*/) {
			
			if(interrupts) {
				if(interrupt_inline) parentnode.addChild(nodex);
				else intnode.addChild(nodex);
				System.out.println("interrupts { add " + text);
			}else {
				if(interrupt_inline) {
					parentnode.addChild(nodex);
				}else {
					intnode = new PreXMLNode("interrupts");
					intnode.addChild(nodex);
				}
				
				
				interrupts = true;
				System.out.println("interrupts { start " + text);
			}
		}else if(interrupts) {
			if(nodex.isInterruptAction(parentnode)) {
				parentnode.addChild(nodex);
				System.out.println("interrupts { ISINTERRUPT add  " + text);
			}else {
				if(interrupt_inline) parentnode.addChild(nodex);
				else parentnode.addChild(intnode);
				intnode=null;
				interrupts = false;
				interrupt_inline = false;
				System.out.println("interrupts { end " + text);
			}
		}else if(!paramsx && !text.startsWith("param ")) { // prevent dual-copy
			parentnode.addChild(nodex);
			
			System.out.println("interrupts { copy  " + text);
		}
	}
	
	

	private static String preprocess(String injs) {
		injs = injs.replaceAll("^[ ]+", ""); // replace all leading spaces
		injs = injs.replaceAll("\\*\\/([^\n])", "*/\n$1"); // multi-line comment bleeding into same line, split it up
		injs = injs.replaceAll("\n}", "\n}\n"); // prevent combining endbrackets
		injs = injs.replaceAll("\t", "");
		injs = injs.replaceAll("^[ ]+", ""); // replace all leading spaces
		
		injs = preprocess_combineparams(injs);
		
		
		return injs;
	}

	private static String preprocess_combineparams(String injs) {
		// loop through each line and combine lines
		// look for lines that are multiple parts of a single conditional or function. and put them together.
					// i.e: if(a &&
					//			b){
		
		String[] lines = injs.split("\n");
		List<String> newlines = new LinkedList<String>();
		
		String currentline="";
		boolean inparam = false;
		int par=0;
		for(String line : lines) {
			// count parentheses, if the number of parentheses matches, then move on.
			line=line.trim();
			line=line.replace("&& ", "&&"); // matches up
			line=line.replace("&&", "&& ");
			
			if(par > 0) {
				currentline += line;
			}else {
				newlines.add(currentline);
				//System.out.println(currentline);
				currentline = line;
			}
			
			for(char c : line.toCharArray()) {
				if(c == '(') par++;
				else if(c==')') par--;
			}
		}
		
		newlines.add(currentline);
		
		String newl = "";
		for(String s : newlines)
			if(!s.equals(""))
				newl += s+"\n";
		
		return newl;
	}

	

	
	
	public static boolean verifyXML(String xml) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			
			// write a temp file so that pathing works :(
			File ftmp = File.createTempFile("js2xml_tmp-"+Utils.md5(xml), ".xml");
			BufferedWriter writer = new BufferedWriter(new FileWriter(ftmp));
		    writer.write(xml);
		    writer.close();
			
			Document doc = dBuilder.parse(ftmp);
			
			// get the schema file from this
			Element root = doc.getDocumentElement();
			String xsdloc = root.getAttribute("xsi:noNamespaceSchemaLocation");
			
			System.out.println(ftmp.toPath().getParent().getFileName()+xsdloc);
			
			// Get schema file from the file?
			URL schemaFile = new URL(ftmp.toPath().getParent().getFileName()+"/"+xsdloc);
			Source xmlFile = new StreamSource(ftmp);
			SchemaFactory schemaFactory = SchemaFactory
			    .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			try {
			  Schema schema = schemaFactory.newSchema(schemaFile);
			  Validator validator = schema.newValidator();
			  validator.validate(xmlFile);
			  System.out.println(xmlFile.getSystemId() + " is valid");
			} catch (SAXException e) {
			  System.out.println(xmlFile.getSystemId() + " is NOT valid reason:" + e);
			} catch (IOException e) {}
			
		}catch(Exception e) {}
		return false;
	}
}

