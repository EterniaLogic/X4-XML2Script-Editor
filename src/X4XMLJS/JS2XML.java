package X4XMLJS;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class JS2XML {
	
	// Quite a few tags were stripped when it was converted to a script. Now, they must be put back in.
	public static String getXML(String js) {
		// First, determine what we are doing.
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
		
		// write out debug file
		try {
			BufferedWriter writer2 = new BufferedWriter(new FileWriter(System.getProperty("user.dir")+"/test.xml"));
			writer2.write(js);
		    writer2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// preprocess the script, combine lines and flatten.
		js = preprocess(js);
		
		
		// loop through and split up text by brackets into nodes
		List<SubNode> nodes = toNodes(js,0);
		
		// report
		if(nodes.size() == 0) {
			// report syntax error and location/line.
		}else {
			for(SubNode n : nodes) {
				if(n != null)
					n.printnode(0);
				else System.out.println("null node");
			}
		}
		
		// convert nodes to XML
		
		
		// write out debug file
		try {
			BufferedWriter writer2 = new BufferedWriter(new FileWriter(System.getProperty("user.dir")+"/test2.xml"));
			writer2.write(js);
		    writer2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return xml;
	}
	
	private static String preprocess(String injs) {
		injs = injs.replaceAll("\t", "");
		
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
	
	// returns null if syntax error, secondary syntax detector will kick in
	private static List<SubNode> toNodes(String innerjs, int bracket) {
		LinkedList<SubNode> nodes = new LinkedList<SubNode>();
		
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
			
			System.out.println(XML2JS.toTab(bracket)+"process -- "+text);
			try {
				if(mlcomment) {
					if(text.contains("*/")) {
						System.out.println("mlcomment end");
						int loc = text.indexOf("*/");
						mlcomment_text.add(text.substring(loc+2,text.length()-1));
						mlcomment = false;
						
						// save node
						SubNode comment1 = new SubNode("#comment");
						for(String cline : mlcomment_text) {
							comment1.value += cline;
							if(!cline.equals(mlcomment_text.get(mlcomment_text.size()-1))) 
								comment1.value+="\n";
						}
						
						nodes.add(comment1);
					}else {
						mlcomment_text.add(text);
					}
				}else {
					String[] stripped = stripParams(text);
					text = stripped[0];
					params = stripped[1];
					comment = stripped[2];
				}
				
				
				if(text.length() > 0) {
					// does this line open a bracket of subnodes?
					
					if(text.contains("{")) {
						System.out.println("OPENBRACKET for "+text);
						
						// find the exit bracket, all lines in-between are considered inner script
						LinkedList<String> innerlines = new LinkedList<String>();
						int bcnt = 1; // count brackets
						//int a=l+1, b=l+1;
						for(int j=l+1;j<lines.length;j++) {
							String linex = lines[j];
							String textx = linex.contains("//") ? linex.substring(0,linex.indexOf("//")) : linex; // strip comment temporarily
							
							bcnt = countbrackets(textx, '{', '}', bcnt);
							//System.out.println("bcnt: "+bcnt+"  ---     "+textx);
							
							if(textx.contains("}") && bcnt <= 0) {
								// ending, send innerjs to recursion
								//b=j;
								String innerlinesx = "";
								for(String s : innerlines) innerlinesx += s+"\n";
								
								SubNode nodex = setSubNode(text,params);
								List<SubNode> nodesx = toNodes(innerlinesx, bracket+1); 
								
								System.out.println("CLOSEBRACKET LINE: "+j);
								System.out.println("CLOSEBRACKET NODE: "+nodex.name);
								nodex.children.addAll(nodesx);
								nodes.add(nodex);
								
								//System.out.println("BRACKET: "+nodex.name+" attrcount:"+nodex.attributes.size());
								
								// jump forward
								if(textx.contains("else")) { // }else{ or }else if(){
									l=j-1;
								}else
									l=j;
								
								break;
							}else {
								innerlines.add(linex);
							}
						}
					}else {
						
						
						if(!text.equals("}")) {
							//System.out.println("FUNCX("+comment);
							SubNode nodex = setSubNode(text,params);
							nodes.add(nodex);
						}else {
							//System.out.println("BRACKETEND");
						}
					}
				}else if(text.equals("") && !comment.equals("")){
					SubNode comment1 = new SubNode("#comment");
					comment1.value=comment;
					nodes.add(comment1);
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
		
		// convert // and /* */ to comment tags (<!-- -->)
		
		return nodes;
	}
	
	private static SubNode setSubNode(String text, String params) {
		SubNode node=null;
		
		System.out.println("setSubNode "+text+"|"+params);
		
		// split up parameters into a list
		List<String> paramlist = new LinkedList<String>();
		
		params = params.replace(", ", ",");
		if(params.contains(",")) {
			for(String s : params.split(","))
				paramlist.add(s);
		}else if(params.length()>2) { // single
			paramlist.add(params);
		}
		
		List<String> valuelist = new LinkedList<String>();
		
		
		// get name and attributes from item
		if(text.startsWith("aiscript")) { // aiscript name{ // {param=pvalue}
			node = setSubNode_aiscript(text);
		}else if(text.contains("input_param")) { // varname=b;
			node = setSubNode_inputparam(text);
		}else if(text.startsWith("#interrupt_handler_ref")) { // {default}
			node = setSubNode_interrupt_ref(text);
		}else if(text.startsWith("#interrupt_handler_if")) { // #interrupt_handler_if(conds){}
			node = setSubNode_interrupt_if(text,valuelist);
		}else if(text.startsWith("create ")) { // create varname;
			node = setSubNode_create(text);
		}else if(text.startsWith("delete ")) { // delete varname;
			node = setSubNode_delete(text);
		}else if(text.startsWith("param")) { // {default}
			node = setSubNode_param(text, valuelist);
		}else if(text.startsWith("function ")) { // function name(value1, value2){ // {param1, param2}
			node = setSubNode_function(text, valuelist);
		}else if(text.startsWith("if(") || text.startsWith("while(") || text.startsWith("do_any")
				 || text.startsWith("any")) { // if(value){
			node = setSubNode_IfWhile(text);
		}else if(text.startsWith("else{")){ // if(value){
			System.out.println("setSubNode else{");
			String name = text.substring(0,text.indexOf("{"));
			node = new SubNode("else");
		}else if(text.startsWith("else if(")){ // if(value){
			text=text.replace("else if(", "elseif(");
			node = setSubNode_IfWhile(text);
		}else if(text.contains(");")) { // func(value, value); // {param, param}
			node = setSubNode_func_nobracket(text, valuelist);
		}else if(text.contains("=")) { // varname=b;
			node = setSubNode_setvalue(text);
		}else if(text.contains("){")) { // func(value, value){ // {param, param}
			node = setSubNode_func_bracket(text, valuelist);
		}else {
			System.out.println("setSubNode name attribute not handled!");
		}
		
		// generate subnodes for parameters
		List<SubNode> parameters = new LinkedList<SubNode>();
		for(int i=0;i<paramlist.size();i++) {
			String param = paramlist.get(i);
			String val="";
			if(valuelist.size() > i)
				val = valuelist.get(i);
			
			if(param.contains("=")) { // {name=value}
				// full parameters including values
				String[] p = param.split("=");
				SubNode n = new SubNode(p[0]);
				n.value = p[1].replace("\"", "");
				parameters.add(n);
			}else { // {name}
				SubNode n = new SubNode(param);
				n.value = val;
				parameters.add(n);
			}
		}
		
		// put parameters in
		if(node != null) {
			for(SubNode pn : parameters) {
				//System.out.println("add parameter");
				node.attributes.put(pn.name, pn);
			}
		}
		
		return node;
	}

	private static SubNode setSubNode_func_bracket(String text, List<String> valuelist) {
		SubNode node;
		System.out.println("setSubNode ){");
		String name = text.substring(0,text.indexOf("("));
		node = new SubNode(name);
		System.out.println("	setSubNode "+name+"(???){");
		String vals = text.substring(text.indexOf("(")+1, text.lastIndexOf(")"));
		System.out.println("	setSubNode FUNC("+vals+"){");
		
		String svals = vals.replace(", ", ",");
		if(svals.contains("\",\"")) {
			for(String s : svals.split("\",\""))
				valuelist.add(s.replace("\"", ""));
		}else {
			if(vals.length() > 0)
				valuelist.add(vals.replace("\"", ""));
		}
		
		System.out.println("	setSubNode FUNC("+svals+"){");
		return node;
	}

	private static SubNode setSubNode_func_nobracket(String text, List<String> valuelist) {
		SubNode node;
		System.out.println("setSubNode );");
		String name = text.substring(0,text.indexOf("("));
		node = new SubNode(name);
		System.out.println("	setSubNode "+name+"(???);");
		String vals = text.substring(text.indexOf("(")+1, text.lastIndexOf(")"));
		
		String svals = vals.replace(", ", ",");
		if(svals.contains("\",\"")) {
			for(String s : svals.split("\",\"")) 
				valuelist.add(s.replace("\"", ""));
		}else {
			if(vals.length() > 0)
				valuelist.add(vals.replace("\"", ""));
		}
		
		
		System.out.println("	setSubNode FUNC("+vals+");");
		return node;
	}

	private static SubNode setSubNode_delete(String text) {
		SubNode node;
		System.out.println("setSubNode setValue (delete)");
		String name = text.substring(8, text.lastIndexOf(";")).replace("\"", "");
		
		node = new SubNode("remove_value");
		
		SubNode namen = new SubNode("name");
		namen.value = name;
		node.attributes.put("name", namen);
		return node;
	}

	private static SubNode setSubNode_create(String text) {
		SubNode node;
		System.out.println("setSubNode setValue (create)");
		String name = text.substring(8, text.lastIndexOf(";")).replace("\"", "");
		
		node = new SubNode("set_value");
		
		SubNode namen = new SubNode("name");
		namen.value = name;
		node.attributes.put("name", namen);
		return node;
	}

	private static SubNode setSubNode_interrupt_ref(String text) {
		SubNode node;
		System.out.println("setSubNode #interrupt ref");
		String val = text.substring(text.indexOf("(")+1, text.lastIndexOf(")")).replace("\"", "");
		
		node = new SubNode("handler");
		SubNode ref = new SubNode("ref");
		ref.value = val;
		node.attributes.put("ref", ref);
		
		System.out.println("	setSubNode ref="+val);
		return node;
	}

	private static SubNode setSubNode_inputparam(String text) {
		SubNode node;
		//System.out.println("setSubNode inputparam name =    "+text);
		String[] vv = text.split("=");
		String name = vv[0].trim();
		String exact = vv[1].trim().replace(";", "").replace("\"", "");
		name = name.substring(12,name.length());
		
		node = new SubNode("input_param");
		SubNode namen = new SubNode("name");
		namen.value = name;
		node.attributes.put("name", namen);
		SubNode valuen = new SubNode("value");
		valuen.value = exact;
		node.attributes.put("value", valuen);
		
		System.out.println("setSubNode inputparam name =    "+name+"="+exact);
		return node;
	}

	private static SubNode setSubNode_aiscript(String text) {
		SubNode node;
		System.out.println("setSubNode aiscript");
		String name = text.substring(9,text.indexOf("{"));
		
		node = new SubNode("aiscript");
		SubNode namen = new SubNode("name");
		namen.value = name;
		node.attributes.put("name", namen);
		
		System.out.println("	setSubNode aiscript "+name+"{");
		return node;
	}

	private static SubNode setSubNode_interrupt_if(String text, List<String> valuelist) {
		System.out.println("setSubNode #interrupt if");
		
		// #interrupt_handler_if(check_any(event_object_attacked(group="$localtargetgroup") && event_object_signalled(check="false", object="this.sector", param="'police'")) && set_value(exact="if (event.name == 'event_object_attacked') then event.param else event.param2", name="$attacker") && $police && this.sector.exists && $attacker.isoperational && $attacker.zone.policefaction && not this.hasrelation.enemy.{$attacker.zone.policefaction} && $attacker.owner != this.owner && $attacker.owner != event.param3.owner){
		
		SubNode handler = new SubNode("interrupt_handler_if");
		
		// conditions
		handler.children.add(handleConds(text));
		
		// actions
		SubNode actn = new SubNode("actions");
		
		// listem
		//
		
		//handler.children.add(actn);
		
		return handler;
	}
	
	private static SubNode handleConds(String text) {
		String conds = text.substring(22, text.lastIndexOf(")"));
		System.out.println("CONDS: "+conds);
		
		SubNode conn = new SubNode("conditions");
		
		if(conds.contains("&")) {
			conds = conds.replaceAll("&&", "&");
			List<String> condl = new LinkedList<String>();
			
			int len = conds.length();
			for(int i=0;i<len;i++) {
				if(countbrackets(conds.substring(0,i), '(', ')', 0) <= 0) {
					if(conds.charAt(i) == '&' || i==len-1) {	
						String x = conds.substring(0,i+1).trim();
						conds = conds.substring(i+1,len);
						len = conds.length();
						i=0;
						
						if(x.endsWith("&")) {
							x = x.substring(0,x.length()-1).trim();
						}
						
						System.out.println("COND: "+x);
						if(x.contains("&") && x.contains("(")) { // still contains at least 3 elements?
							String cname = x.substring(0,x.indexOf('('));
							
							SubNode condx = new SubNode(cname);
							String act = x.substring(x.indexOf('(')+1, x.lastIndexOf(')'));
							
							for(String a : act.split("&")) {
								SubNode n = condToNode(a);
								if(n != null)
									condx.children.add(n);
							}
							
							conn.children.add(condx);
						}else {
							SubNode n = condToNode(x);
							if(n != null)
								conn.children.add(n);
						}
					}
				}
			}
		}else {
			SubNode n = condToNode(conds);
			if(n != null)
				conn.children.add(n);
		}
		
		return conn;
	}

	private static SubNode condToNode(String text) {
		System.out.println("CONDTONODE: "+text);
		SubNode condx2 = null;
		
		if(text.contains("(")) {
			String cname2 = text.substring(0,text.indexOf('('));
			String cval2 = text.substring(text.indexOf('(')+1, text.lastIndexOf(')'));
			condx2 = new SubNode(cname2.trim());
			/// TODO FIX, this is a function(blah="blah", blah="blah")
			
			
			if(cval2.contains(",")) {
				String[] p = cval2.split(",");
				
				// Attributes
				for(String pa : p) {
					if(pa.contains("=")) {
						String[] nvpair = pa.split("=",2); // ,2
						String name = nvpair[0].trim();
						
						SubNode valn = new SubNode(name);
						valn.value = nvpair[1].replaceAll("\"", "").trim();
						condx2.attributes.put(name, valn);
					}
				}
			}else { // ?
				if(cval2.contains("=")) {
					String[] nvpair = cval2.split("=",2);
					String name = nvpair[0].trim();
					
					SubNode valn = new SubNode(name);
					valn.value = nvpair[1].replaceAll("\"", "").trim();
					condx2.attributes.put(name, valn);
				}else {
					SubNode valn = new SubNode("check_value");
					SubNode exact = new SubNode("exact");
					exact.value = cval2.replaceAll("\"", "").trim();
					valn.attributes.put("exact", exact);
					condx2.children.add(valn);
				}
			}
		}else {
			SubNode valn = new SubNode("check_value");
			SubNode exact = new SubNode("exact");
			exact.value = text.replaceAll("\"", "").trim();
			valn.attributes.put("exact", exact);
			condx2 = valn; //.children.add(valn);
		}
		
		/*
		 * <check_any>
	          <event_object_attacked group="$localtargetgroup"/>
	          <event_object_signalled object="this.sector" param="'police'" check="false"/>
	        </check_any>
	        <set_value name="$attacker" exact="if (event.name == 'event_object_attacked') then event.param else event.param2"/>
	        <check_value value="$police"/>
	        <check_value value="this.sector.exists"/>
	        <check_value value="$attacker.isoperational"/>
	        <check_value value="$attacker.zone.policefaction"/>
	        <check_value value="not this.hasrelation.enemy.{$attacker.zone.policefaction}" comment="Check that the police faction is not an enemy"/>
	        <check_value value="$attacker.owner != this.owner" />
	        <check_value value="$attacker.owner != event.param3.owner"/>
		 */
		
		return condx2;
	}
	
	
	
	private static int countbrackets(String line, char a, char b, int bcnt) {
		int initial = bcnt;
		boolean wasabove=false;
		for(char c : line.toCharArray()) {
			if(bcnt <= 0 && wasabove) break;
			if(bcnt > initial) wasabove=true;
			if(c == a) bcnt ++;
			else if(c == b) bcnt --;
		}
		
		return bcnt;
	}


	private static SubNode setSubNode_setvalue(String text) {
		SubNode node;
		System.out.println("setSubNode setValue=    "+text);
		String[] vv = text.split("=");
		String name = vv[0].trim();
		String exact = vv[1].trim().replace(";", "");
		
		node = new SubNode("set_value");
		SubNode namen = new SubNode("name");
		namen.value = name;
		node.attributes.put("name", namen);
		SubNode exactn = new SubNode("exact");
		exactn.value = exact;
		node.attributes.put("exact", exactn);
		return node;
	}

	private static SubNode setSubNode_function(String text, List<String> valuelist) {
		SubNode node;
		System.out.println("setSubNode function");
		String name = text.substring(9,text.indexOf("("));
		node = new SubNode(name);
		String vals = text.substring(text.indexOf("(")+1, text.lastIndexOf(")"));
		vals = vals.replace(", ", ",");
		
		if(vals.contains("\",\"")) {
			for(String s : vals.split("\",\"")) 
				valuelist.add(s.replace("\"", ""));
		}else {
			if(vals.length() > 0) {
				if(name.equals("attention")) {
					SubNode attrn = new SubNode("min");
					attrn.value = vals;
					node.attributes.put("min", attrn);
				}else valuelist.add(vals.replace("\"", ""));
			}
		}
		
		System.out.println("	setSubNode ! function "+name+"("+vals+"){");
		return node;
	}

	private static SubNode setSubNode_param(String text, List<String> valuelist) {
		SubNode node;
		System.out.println("setSubNode param {type} name(def?)");
		String name = text.substring(6,text.indexOf("("));
		String val = text.substring(text.indexOf("(")+1, text.lastIndexOf(")")); //.replace("\"", "");
		String type="";
		
		if(name.contains(" ")) {
			String[] t = name.split(" ");
			name = t[0];
			type = t[1];
		}
		
		System.out.println("setSubNode param "+type+" "+name+"("+val+")");
		
		node = new SubNode("param");
		SubNode namen = new SubNode("name");
		namen.value = name;
		node.attributes.put("name", namen);
		
		if(!type.equals("")) {
			SubNode typen = new SubNode("type");
			typen.value = type;
			node.attributes.put("type", typen);
		}
		
		String svals = val.replace("\", \"", "\",\"");
		if(svals.contains("\",\"")) {
			for(String s : svals.split("\",\""))
				valuelist.add(s.replace("\"", ""));
		}else {
			if(val.length() > 0)
				valuelist.add(val.replace("\"", ""));
		}
		
		if(text.contains("{")) {
			// uh... it works w/o this?
		}
		return node;
	}

	private static SubNode setSubNode_IfWhile(String text) {
		SubNode node;
		
		if(text.startsWith("do_any") || text.startsWith("any")) {
			node = new SubNode("any");
		}else {		
			String name = text.substring(0,text.indexOf("("));
			node = new SubNode(name);
			String val = text.substring(text.indexOf("(")+1, text.lastIndexOf(")")).replace("\"", "");;
			SubNode value = new SubNode("value");
			value.value = val;
			node.attributes.put("value", value);
			//System.out.println("	setSubNode if("+val+"){");
		}
		
		return node;
	}
	
	// returns text, params, comment
	private static String[] stripParams(String text) {
		List<String> list = new LinkedList<String>();
		
		String newtext=text.trim();
		String params="";
		String comment="";
		
		if(newtext.startsWith("}")) {
			//System.out.println(newtext);
			//System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			
			newtext = newtext.replace("}", "").trim();
			
		}
		
		
		if(newtext.startsWith("//")) { // direct single-line comment
			newtext = newtext.replaceAll("//", "");
			comment = newtext.trim();
			newtext = "";
			System.out.println("SLComment "+comment);
		}else if(newtext.contains("//")) { // strip out comment and use it for naming guidelines if using it
			int loc = newtext.indexOf("//");
			comment = newtext.substring(loc+2,newtext.length()).trim();
			newtext = newtext.substring(0,loc).trim();
			
			//System.out.println(newtext+"|"+comment);
			
			// determine if an actual user-based comment or a generated one
			if(comment.contains("{") && comment.contains("}")) {
				// strip out {parametername=?, parametername=?} from comment
				int ba = comment.indexOf("{"), bb = comment.indexOf("}");
				params = comment.substring(ba+1,bb);
				params = params.replaceAll(", ", ","); // trim inner spaces
				
				//System.out.println("0 < "+ba+" < "+ bb + " < "+comment.length());
				
				// fix comment with removed params
				if(bb+1 < comment.length() && ba != 0)
					comment = comment.substring(0,ba) + comment.substring(bb+1, comment.length());
				else if(bb+1 >= comment.length() && ba > 0)
					comment = comment.substring(0,ba);
				else if(bb+1 < comment.length() && ba == 0)
					comment = comment.substring(bb+1, comment.length());
				else comment="";
				
				
				//System.out.println("{} PARAMS: "+params+"   stripped from: "+text);
			}
			
			if(comment.length() > 2) { // there is still a comment="" attribute here.
				//System.out.println("STILL COMMENT: "+comment);
			}
		}else if(newtext.contains("/*")) {
			System.out.println("mlcomment start");
		}else {
			System.out.println("LINE: "+newtext);
		}
		
		list.add(newtext);
		list.add(params);
		list.add(comment);
		
		return new String[] {newtext, params, comment};
	}
	

	public static boolean verifyXML(String xml) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			
			// write a temp file so that pathing works :(
			File ftmp = File.createTempFile("js2xml_tmp-"+md5(xml), ".xml");
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
	
	private static String tag() {
		return "";
	}
	
	private static String md5(String ins) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			md.update(ins.getBytes());
		    byte[] digest = md.digest();
		    return DatatypeConverter.printHexBinary(digest).toLowerCase();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
	}
}

class SubNode{
	public String name="", value="";
	public HashMap<String,SubNode> attributes = new HashMap<String,SubNode>();
	public List<SubNode> children = new LinkedList<SubNode>();
	
	public SubNode(String name) {
		this.name=name;
	}
	
	public void printnode(int tab) {
		// this node
		System.out.print(XML2JS.toTab(tab+1)+"<"+name);
		for(SubNode n : attributes.values()) {
			System.out.print(" "+n.name+"=\""+n.value+"\"");
		}
		
		if(!value.equals(""))
			System.out.print(" \""+value+"\"");
		
		if(children.size() == 0) System.out.println(" />"); 
		else System.out.println(">");
		
		// subnodes
		for(SubNode n : children) {
			if(n != null)
				n.printnode(tab+1);
			else System.out.println("null node");
		}
	}
}
