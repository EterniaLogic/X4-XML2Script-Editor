package X4XMLJS;

import java.util.LinkedList;
import java.util.List;

public class PreXMLNodeFactory {

	// convert a line of code and convert it to a PreXMLNode when converting to XML
	static PreXMLNode toPreXMLNode(String text, String params) {
		PreXMLNode node=null;
		
		//System.out.println("toPreXMLNode "+text+"|"+params);
		
		// split up parameters into a list
		List<String> paramlist = new LinkedList<String>();
		
		params = params.replace(", ", ",");
		if(params.contains(",")) {
			for(String s : params.split(","))
				paramlist.add(s);
		}else if(params.length()>=1) { // single paramater
			paramlist.add(params);
		}
		
		List<String> valuelist = new LinkedList<String>();
		
		
		// get name and attributes from item
		if(text.startsWith("aiscript")) { // aiscript name{ // {param=pvalue}
			node = toPreXMLNode_aiscript(text);
		}else if(text.contains("input_param")) { // varname=b;
			node = toPreXMLNode_inputparam(text);
		}else if(text.startsWith("#interrupt_handler_ref")) { // {default}
			node = toPreXMLNode_interrupt_handler_ref(text);
		}else if(text.startsWith("#interrupt_handler_if")) { // #interrupt_handler_if(conds){}
			node = toPreXMLNode_interrupt_handler_if(text,valuelist);
		}else if(text.startsWith("#interrupt_ref")) { // {default} INLINE, not at the beginning of the file
			node = toPreXMLNode_interrupt_ref(text);
		}else if(text.startsWith("#interrupt_if")) { // INLINE, not at the beginning of the file
			node = toPreXMLNode_interrupt_if(text,valuelist);
		}else if(text.startsWith("create ")) { // create varname;
			node = toPreXMLNode_create(text);
		}else if(text.startsWith("delete ")) { // delete varname;
			node = toPreXMLNode_delete(text);
		}else if(text.startsWith("param")) { // {default}
			node = toPreXMLNode_param(text, valuelist);
		}else if(text.startsWith("function ")) { // function name(value1, value2){ // {param1, param2}
			node = toPreXMLNode_function(text, valuelist);
			
		}else if(text.startsWith("if(") || text.startsWith("while(") || text.startsWith("do_any")
				 || text.startsWith("any")) { // if(value){
			node = toPreXMLNode_IfWhile(text);
		}else if(text.startsWith("else{")){ // if(value){
			System.out.println("toPreXMLNode else{");
			node = new PreXMLNode("else");
		}else if(text.startsWith("else if(")){ // if(value){
			text=text.replace("else if(", "elseif(");
			node = toPreXMLNode_IfWhile(text);
		}else if(text.startsWith("run_script")) { // aiscript name{ // {param=pvalue}
			node = toPreXMLNode_run_script(text);
		}else if(text.contains(");")) { // func(value, value); // {param, param}
			node = toPreXMLNode_func_nobracket(text, valuelist);
		}else if(text.contains("){")) { // func(value, value){ // {param, param}
			node = toPreXMLNode_func_bracket(text, valuelist);
		}else if(text.contains("=")) { // varname=b;
			node = toPreXMLNode_setvalue(text);
		}else {
			System.out.println("toPreXMLNode name attribute not handled!");
		}
		
		// generate PreXMLNodes for parameters
		List<PreXMLNode> parameters = new LinkedList<PreXMLNode>();
		for(int i=0;i<paramlist.size();i++) {
			String param = paramlist.get(i);
			String val="";
			if(valuelist.size() > i)
				val = valuelist.get(i);
			
			if(param.contains("=")) { // {name=value}
				// full parameters including values
				String[] p = param.split("=");
				PreXMLNode n = new PreXMLNode(p[0]);
				n.value = p[1].replace("\"", "");
				parameters.add(n);
			}else { // {name}
				PreXMLNode n = new PreXMLNode(param);
				n.value = val;
				parameters.add(n);
			}
		}
		
		// put parameters in
		if(node != null) {
			for(PreXMLNode pn : parameters) {
				//System.out.println("add parameter");
				node.attributes.put(pn.name, pn);
			}
		}
		
		if(parameters.size() == 0 && valuelist.size() == 1) {
			node.putAttrNode("value", valuelist.get(0));
		}
		
		return node;
	}

	// aiscript name{ // {name=value}	=> <aiscript name=value></aiscript>	
	private static PreXMLNode toPreXMLNode_aiscript(String text) {
		PreXMLNode node;
		//System.out.println("toPreXMLNode aiscript");
		String name = text.substring(9,text.indexOf("{"));
		
		node = new PreXMLNode("aiscript");
		node.putAttrNode("name", name);
		
		//System.out.println("	toPreXMLNode aiscript "+name+"{");
		return node;
	}

	// create variable	  => <set_value name="variable" />
	private static PreXMLNode toPreXMLNode_create(String text) {
		PreXMLNode node;
		//System.out.println("toPreXMLNode setValue (create)");
		String name = text.substring(7, text.lastIndexOf(";")).replace("\"", "");
		
		node = new PreXMLNode("set_value");
		
		node.putAttrNode("name", name);
		return node;
	}

	// delete variable		=> <remove_value name="variable" />	
	private static PreXMLNode toPreXMLNode_delete(String text) {
		PreXMLNode node;
		//System.out.println("toPreXMLNode setValue (delete)");
		String name = text.substring(7, text.lastIndexOf(";")).replace("\"", "");
		
		node = new PreXMLNode("remove_value");
		
		node.putAttrNode("name", name);
		return node;
	}

	// func(values){ // {name}		=> <func name=value></func>
	private static PreXMLNode toPreXMLNode_func_bracket(String text, List<String> valuelist) {
		PreXMLNode node;
		System.out.println("toPreXMLNode ){");
		String name = text.substring(0,text.indexOf("("));
		node = new PreXMLNode(name);
		//System.out.println("	toPreXMLNode "+name+"(???){");
		String vals = text.substring(text.indexOf("(")+1, text.lastIndexOf(")"));
		//System.out.println("	toPreXMLNode FUNC("+vals+"){");
		
		String svals = vals.replace(", ", ",");
		if(svals.contains("\",\"")) {
			for(String s : svals.split("\",\""))
				valuelist.add(s.replace("\"", ""));
		}else {
			if(vals.length() > 0)
				valuelist.add(vals.replace("\"", ""));
		}
		
		System.out.println("	toPreXMLNode FUNC("+svals+"){     --- "+valuelist.size());
		return node;
	}

	// func(values); // {name}		=> <func name=value />
	private static PreXMLNode toPreXMLNode_func_nobracket(String text, List<String> valuelist) {
		PreXMLNode node;
		//System.out.println("toPreXMLNode );");
		String name = text.substring(0,text.indexOf("("));
		node = new PreXMLNode(name);
		//System.out.println("	toPreXMLNode "+name+"(???);");
		String vals = text.substring(text.indexOf("(")+1, text.lastIndexOf(")"));
		
		String svals = vals.replace(", ", ",");
		if(svals.contains("\",\"")) {
			for(String s : svals.split("\",\"")) 
				valuelist.add(s.replace("\"", ""));
		}else {
			if(vals.length() > 0)
				valuelist.add(vals.replace("\"", ""));
		}
		
		
		//System.out.println("	toPreXMLNode FUNC("+vals+");");
		return node;
	}
	
	// function name(valuelist){ ...
	private static PreXMLNode toPreXMLNode_function(String text, List<String> valuelist) {
		PreXMLNode node;
		//System.out.println("toPreXMLNode function");
		String name = text.substring(9,text.indexOf("("));
		node = new PreXMLNode(name);
		String vals = text.substring(text.indexOf("(")+1, text.lastIndexOf(")"));
		vals = vals.replace(", ", ",");
		
		if(name.equals("attention")) {
			// add action PreXMLNode
			PreXMLNode actions = new PreXMLNode("actions");
			node.addChild(actions);
		}
		
		if(vals.contains("\",\"")) {
			for(String s : vals.split("\",\"")) 
				valuelist.add(s.replace("\"", ""));
		}else {
			if(vals.length() > 0) {
				if(name.equals("attention")) {
					node.putAttrNode("min", vals);
				}else valuelist.add(vals.replace("\"", ""));
			}
		}
		
		
		
		//System.out.println("	toPreXMLNode ! function "+name+"("+vals+"){");
		return node;
	}

	// if(value){		=> <do_if name=value></do_if>
	private static PreXMLNode toPreXMLNode_IfWhile(String text) {
		PreXMLNode node;
		
		if(text.startsWith("do_any") || text.startsWith("any")) {
			node = new PreXMLNode("any");
		}else {		
			String name = text.substring(0,text.indexOf("("));
			node = new PreXMLNode(name);
			String val = text.substring(text.indexOf("(")+1, text.lastIndexOf(")")).replace("\"", "");;
			node.putAttrNode("value", val);
		}
		
		return node;
	}
	
	
	// run_script NAME{ // {n=v}	=> <run_script name='NAME' n=v> .... </aiscript>	
	private static PreXMLNode toPreXMLNode_run_script(String text) {
		PreXMLNode node;
		//System.out.println("toPreXMLNode aiscript");
		String name = text.substring(11,text.indexOf("{"));
		
		node = new PreXMLNode("run_script");
		node.putAttrNode("name", name);
		
		//System.out.println("	toPreXMLNode aiscript "+name+"{");
		return node;
	}
	
	// input_param NAME = VALUE;	=> <input_param name="NAME" value="VALUE" />
	private static PreXMLNode toPreXMLNode_inputparam(String text) {
		PreXMLNode node;
		//System.out.println("toPreXMLNode inputparam name =    "+text);
		String[] vv = text.split("=");
		String name = vv[0].trim();
		String exact = vv[1].trim().replace(";", "").replace("\"", "");
		name = name.substring(12,name.length());
		
		node = new PreXMLNode("input_param");
		node.putAttrNode("name", name);
		node.putAttrNode("value", exact);
		
		System.out.println("toPreXMLNode inputparam name =    "+name+"="+exact);
		return node;
	}

	// #interrupt_handler_if(conditions){... actions }		=> <handler><conditions></conditions><actions></actions></handler>
	private static PreXMLNode toPreXMLNode_interrupt_handler_if(String text, List<String> valuelist) {
		//System.out.println("toPreXMLNode #interrupt if");
		
		// #interrupt_handler_if(check_any(event_object_attacked(group="$localtargetgroup") && event_object_signalled(check="false", object="this.sector", param="'police'")) && set_value(exact="if (event.name == 'event_object_attacked') then event.param else event.param2", name="$attacker") && $police && this.sector.exists && $attacker.isoperational && $attacker.zone.policefaction && not this.hasrelation.enemy.{$attacker.zone.policefaction} && $attacker.owner != this.owner && $attacker.owner != event.param3.owner){
		
		PreXMLNode handler = new PreXMLNode("interrupt_handler_if");
		
		// conditions
		PreXMLNode conn = new PreXMLNode("conditions");
		handleConds(conn, text.substring(text.indexOf("(")+1, text.lastIndexOf(")")));
		handler.addChild(conn);
		
		return handler;
	}
	
	// #interrupt_if(conditions){... actions }		=> <handler><conditions></conditions><actions></actions></handler>
	private static PreXMLNode toPreXMLNode_interrupt_if(String text, List<String> valuelist) {
		//System.out.println("toPreXMLNode #interrupt if");
		
		// #interrupt_if(check_any(event_object_attacked(group="$localtargetgroup") && event_object_signalled(check="false", object="this.sector", param="'police'")) && set_value(exact="if (event.name == 'event_object_attacked') then event.param else event.param2", name="$attacker") && $police && this.sector.exists && $attacker.isoperational && $attacker.zone.policefaction && not this.hasrelation.enemy.{$attacker.zone.policefaction} && $attacker.owner != this.owner && $attacker.owner != event.param3.owner){
		
		PreXMLNode handler = new PreXMLNode("interrupt_if");
		PreXMLNode cond = new PreXMLNode("conditions");
		
		// conditions
		
		handleConds(cond, text.substring(text.indexOf("(")+1, text.lastIndexOf(")")));
		handler.addChild(cond);
		
		return handler;
	}
	
	// a && b(c && d)	=> <conditions><a /><b><c /><d /></b></conditions> 
	private static void handleConds_defunct(PreXMLNode parent, String text) {
		String conds = text.substring(text.indexOf("(")+1, text.lastIndexOf(")"));
		
		
		System.out.println("CONDS_TEXT: "+text);
		System.out.println("CONDS: "+conds);
		
		//PreXMLNode conn = new PreXMLNode("conditions");
		
		if(conds.contains("&")) {
			conds = conds.replaceAll("&&", "&");
			List<String> condl = new LinkedList<String>();
			
			int len = conds.length();
			for(int i=0;i<len;i++) {
				if(Utils.countbrackets(conds.substring(0,i), '(', ')', 0) <= 0) {
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
							
							PreXMLNode condx = new PreXMLNode(cname);
							String act = x.substring(x.indexOf('(')+1, x.lastIndexOf(')'));
							
							for(String a : act.split("&")) {
								PreXMLNode n = condToNode_defunct(a);
								if(n != null)
									condx.addChild(n);
							}
							
							parent.addChild(condx);
						}else {
							PreXMLNode n = condToNode_defunct(x);
							if(n != null)
								parent.addChild(n);
						}
					}
				}
			}
		}else {
			PreXMLNode n = condToNode_defunct(conds);
			if(n != null)
				parent.addChild(n);
		}
	}
	
	// identify PreXMLNodes from conditions string  a && b && c(d && d)
	private static PreXMLNode condToNode_defunct(String text) {
		
		PreXMLNode condx2 = null;
		String comment = "";
		
		int bckt = Utils.countbrackets(text, '(', ')', 0);
		if(bckt >= 1) text += ")";
		
		System.out.println("CONDTONODE["+bckt+"]: "+text);
		
		String[] c = Utils.stripMLComment(text);
		text = c[0];
		comment = c[1];
		
		if(text.contains("(")) {
			String cname2 = text.substring(0,text.indexOf('('));
			String cval2 = text.substring(text.indexOf('(')+1, text.lastIndexOf(')')-1);
			
			
			
			System.out.println("CONDTONODE VALS: "+cname2+"  ---   "+cval2);
			condx2 = new PreXMLNode(cname2.trim());
			/// TODO FIX, this is a function(blah="blah", blah="blah")
			
			
			if(cval2.contains(",")) {
				String[] p = cval2.split(",");
				
				// Attributes
				for(String pa : p) {
					if(pa.contains("=")) {
						String[] nvpair = pa.split("=",2); // ,2
						String name = nvpair[0].trim();
						
						condx2.putAttrNode(name, nvpair[1].replaceAll("\"", "").trim());
					}
				}
			}else { // child node
				if(cval2.contains("=") && !cval2.contains("(")) {
					String[] nvpair = cval2.split("=",2);
					String name = nvpair[0].trim();
					condx2.putAttrNode(name, nvpair[1].replaceAll("\"", "").trim());
				}else if(cval2.contains("(")){
					//String cname3 = cval2.substring(0,cval2.indexOf('('));
					
					condToNode_defunct(cval2);
					//condx2.getChildren().add(condToNode(cval2));
				}else {
					PreXMLNode valn = new PreXMLNode("check_value");
					valn.putAttrNode("value", cval2.replaceAll("\"", "").trim());
					if(!comment.equals("")) valn.putAttrNode("comment", comment);
					condx2.addChild(valn);
				}
			}
		}else {
			PreXMLNode valn = new PreXMLNode("check_value");
			valn.putAttrNode("value", text.replaceAll("\"", "").trim());
			if(!comment.equals("")) valn.putAttrNode("comment", comment);
			condx2 = valn; //.addChild(valn);
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
	
	private static void handleConds(PreXMLNode parent, String text) {
		String conds = text;
		
		text = text.replace("&&", "&");
		System.out.println("CONDS2_TEXT: "+text);
		
		/*if(text.contains("(")){
			conds = text.substring(text.indexOf("(")+1, text.lastIndexOf(")"));
		}else conds = text;
		
		System.out.println("CONDS2: "+conds);
		*/
		
		
		
		// loop through the characters and split up the conditions by parentheses
		PreXMLNode condx2 = new PreXMLNode("");
		
		LinkedList<PreXMLNode> nodes = new LinkedList<PreXMLNode>(); 
		
		String pvalue="";
		int bracket=0, commentn=-2;
		for(char c : conds.toCharArray()) {
			if(c == '(') bracket++;
			else if(c== ')') bracket--;
			
			if(c== ')' && bracket <= 0) {
				if(pvalue.length() > 0) {
					System.out.println("handleConds -- '"+condx2.name+"' subvalue '"+pvalue+"'");
					
					
					
					if(condx2.name.trim().equals("set_value")) {
						condx2.addAttrsFromParams(pvalue, ",");
					}else if(pvalue.contains("&") || pvalue.contains("(")) {
						handleConds(condx2,pvalue+")");
					}else {
						condx2.addAttrsFromParams(pvalue, ",");
					}
					
					pvalue="";
				}
			}else if(c == '&' && bracket <= 0) {
				condx2.name = condx2.name.trim();
				System.out.println();
				if(condx2.name.length() > 0) {
					
					System.out.println("handleConds -- '"+condx2.name+"'");
					nodes.add(condx2);
					condx2 = new PreXMLNode("");
				}
			}else if(bracket >= 1 && !(bracket == 1 && c=='(')) {
				pvalue += c;
			}else if(bracket == 1 && c=='(') {
				// not here!
			}else {
				condx2.name += c; 
				//System.out.print(c);
			}
		}
		
		condx2.name = condx2.name.trim();
		if(condx2.name.length() > 0) {
			System.out.println("handleConds -- '"+condx2.name+"'");
			nodes.add(condx2);
			condx2 = new PreXMLNode("");
		}
		
		for(PreXMLNode node : nodes) {
			String comment = "";
			if(node.name.contains("/*")) {
				String[] t = Utils.stripMLComment(node.name);
				node.name = t[0];
				comment = t[1];
			}
			
			
			
			if(node.getChildren().size() == 0 && node.attributes.size() == 0) {
				// either a function or a value
				node.putAttrNode("value", node.name);
				node.name="check_value";
			}
			
			if(!comment.equals(""))
				node.putAttrNode("comment", comment);
			
			//node.printnode(1);
			parent.addChild(node);
		}
	}
	

	// #interrupt_handler_ref(REF);	=> <handler ref=REF />
	private static PreXMLNode toPreXMLNode_interrupt_handler_ref(String text) {
		PreXMLNode node;
		//System.out.println("toPreXMLNode #interrupt ref");
		String val = text.substring(text.indexOf("(")+1, text.lastIndexOf(")")).replace("\"", "");
		
		node = new PreXMLNode("handler");
		node.putAttrNode("ref", val);
		
		//System.out.println("	toPreXMLNode ref="+val);
		return node;
	}
	
	// #interrupt_handler_ref(REF);	=> <handler ref=REF />
	private static PreXMLNode toPreXMLNode_interrupt_ref(String text) {
		PreXMLNode node;
		//System.out.println("toPreXMLNode #interrupt ref");
		String val = text.substring(text.indexOf("(")+1, text.lastIndexOf(")")).replace("\"", "");
		
		node = new PreXMLNode("interrupt");
		node.putAttrNode("ref", val);
		
		//System.out.println("	toPreXMLNode ref="+val);
		return node;
	}

	// param {TYPE} NAME(VALUES); // {NAMES}		=> <param name=NAME NAMES=VALUES />
	//param {TYPE} NAME(VALUES){ // {NAMES}   \n .... } 		=> <param name=NAME NAMES=VALUES> ... </param>
	private static PreXMLNode toPreXMLNode_param(String text, List<String> valuelist) {
		PreXMLNode node;
		//System.out.println("toPreXMLNode param {type} name(def?)");
		String name = text.substring(6,text.indexOf("("));
		String val = text.substring(text.indexOf("(")+1, text.lastIndexOf(")")); //.replace("\"", "");
		String type="";
		
		if(name.contains(" ")) {
			String[] t = name.split(" ");
			name = t[1];
			type = t[0];
		}
		
		//System.out.println("toPreXMLNode param "+type+" "+name+"("+val+")");
		
		node = new PreXMLNode("param");
		node.putAttrNode("name", name);
		
		if(!type.equals("")) {
			node.putAttrNode("type", type);
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

	// NAME=VALUE;	=> <set_value name=NAME exact=VALUE />
	private static PreXMLNode toPreXMLNode_setvalue(String text) {
		PreXMLNode node;
		//System.out.println("toPreXMLNode setValue=    "+text);
		String[] vv = text.split("=",2);
		String name = vv[0].trim();
		String exact = vv[1].trim().replace(";", "");
		
		node = new PreXMLNode("set_value");
		node.putAttrNode("name", name);
		node.putAttrNode("exact", exact);
		return node;
	}
}
