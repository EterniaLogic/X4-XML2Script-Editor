package X2S_ScriptEditor;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class Utils {
	
	// use an md5 for temporary filenames based off of hash
	public static String md5(String ins) {
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

	// get an XML tag's attribute value by name from XML node
	public static String getAttrValue(Node child, String attrname) {
		NamedNodeMap m = child.getAttributes();
		String name="";
		String def="";
		
		// parameters for node
		for(Node n : Utils.getAttrs(child)) {
			if(n.getNodeName().equals(attrname)) {
				return n.getNodeValue();
			}
		}
		
		return "";
	}

	// Print attributes with a space between from XML node
	public static String handleComment(String com, int tabs, boolean forceml) {
		if(com.contains("\n") || forceml) {
			String[] lines = com.split("\n");
			String text="";
			if(!forceml) text += Utils.toTab(tabs)+"/* "+lines[0].trim()+"\n";
			else text += "/* "+lines[0].trim();
			
			for(int i=1;i<lines.length;i++) {
				text += Utils.toTab(tabs+1)+lines[i].trim()+"\n";
			}
			
			if(!forceml) return text+Utils.toTab(tabs)+" */";
			else  return text+" */";
		}else return Utils.toTab(tabs)+"// "+com+"";
	}

	// get {name=value, ...} for all attributes from XML node
	// returns {name=value,   comment}
	public static String[] strAttrs(Node node, boolean movecomment, String[] filter) {
		String t = "", comment="";
		
		// create a list for filters if used
		List<String> filterlist = new LinkedList<String>();
		if(filter != null && filter.length > 0)
			for(String s : filter) 
				filterlist.add(s);
		
		if(node == null) return new String[] {"",""};
		if(node.getNodeName().equals("#text")) return new String[] {"",""};
		
		
		List<Node> nlist = new LinkedList<Node>();
		NamedNodeMap m = node.getAttributes();
		for(int i=0;i<m.getLength();i++) {
			if(m.item(i).getNodeName().equals("comment") && movecomment) {
				comment = m.item(i).getNodeValue();
			}else if(filterlist.contains(m.item(i).getNodeName())) {
				// value filtered out
			}else nlist.add(m.item(i));
		}
		
		
		for(Node n : nlist) {
			t += n.getNodeName()+"=\""+n.getNodeValue()+"\"";
			if(n != nlist.get(nlist.size()-1)) t += ", ";
		}
		
		return new String[] {t, comment};
	}
	
	public static String strAttrs_tocomment(Node node, boolean movecomment, String[] filter) {
		String comment="";
		
		String[] s = Utils.strAttrs(node,movecomment,filter);
		if(s[0].length() > 0 || s[1].length() > 0) {
			comment = " // "+s[1];
			if(s[0].length() > 0) comment += "{"+s[0]+"}";
		}
		
		return comment;
	}
	
	public static String[] stripMLComment(String text) {
		String comment="";
		
		if(text.contains("/*") && text.contains("*/")) {
			comment =  text.substring(text.indexOf("/*")+2, text.lastIndexOf("*/")-1).trim();
			
			String a="",b="";
			if(text.indexOf("/*")-1 > 0) a=text.substring(0, text.indexOf("/*")-1);
			if((text.length()-(text.lastIndexOf("*/")+2)) > 0) b=text.substring(text.lastIndexOf("*/")+2, text.length());
			
			text = a + b;
		}
		
		return new String[] {text, comment};
	}

	// get {value, ...} for all XML attributes, where name=value from XML node
	public static String strAttrsValOnly(Node node) {
		String t = "";
		
		if(node.getNodeName().equals("#text")) return "";
		if(node == null) return "";
		
		List<Node> nlist = new LinkedList<Node>();
		NamedNodeMap m = node.getAttributes();
		for(int i=0;i<m.getLength();i++) {
			if(m.item(i).getNodeName().equals("comment")) {
				//comment = m.item(i).getNodeValue();
			}else nlist.add(m.item(i));
		}
		
		
		for(Node n : nlist) {
			t += "\""+n.getNodeValue()+"\"";
			if(n != nlist.get(nlist.size()-1)) t += ", ";
		}
		
		return t;
	}

	// get // {name,name, ...} for all XML attributes, name in name=value from XML node
	public static String strAttrsComment(Node node) {
		String t = "";
		
		if(node.getNodeName().equals("#text")) return "";
		if(node == null) return "";
		
		String comment=""; // comment="value" XML attribute
		
		
		List<Node> nlist = new LinkedList<Node>();
		NamedNodeMap m = node.getAttributes();
		for(int i=0;i<m.getLength();i++) {
			if(m.item(i).getNodeName().equals("comment")) {
				comment = m.item(i).getNodeValue()+" ";
			}else nlist.add(m.item(i));
		}
		
		
		for(Node n : nlist) {
			t += n.getNodeName();
			if(n != nlist.get(nlist.size()-1)) t += ", ";
		}
		
		if(t.length() > 0) t=" // "+comment+"{"+t+"}";
		else if(comment.length() > 0) t="// "+comment;
		
		return t;
	}

	// get the tabulation for a line
	public static String toTab(int tabulation) {
		String text="";
		for(int i=0;i<tabulation;i++) text += "    ";
		return text;
	}

	// get a list of attributes from the node. from XML node
	public static List<Node> getAttrs(Node node){
		LinkedList<Node> list = new LinkedList<Node>();
		if(node.getNodeName().equals("#text")) return list;
		if(node == null) return list;
		
		
		NamedNodeMap m = node.getAttributes();
		if(m == null) return list;
		for(int i=0;i<m.getLength();i++) {
			list.add(m.item(i));
		}
		
		return list;
	}

	// returns a list of children from an XML node. (remove #text nodes) from XML node
	public static List<Node> getChildren(Node n){
		LinkedList<Node> list = new LinkedList<Node>();
		
		for(int j=0;j<n.getChildNodes().getLength();j++) {
			Node child = n.getChildNodes().item(j);
			if(child.getNodeName().equals("#text")) continue;
			
			list.add(child);
		}
		
		return list;
	}

	// count {} brackets, used in code identification. for Script.
	public static int countbrackets(String line, char a, char b, int bcnt) {
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

	// returns text, params, comment
	public static String[] stripTextParamsComment(String text) {
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
			//System.out.println("SLComment "+comment);
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
			//System.out.println("mlcomment start");
		}else {
			//System.out.println("LINE: "+newtext);
		}
		
		
		return new String[] {newtext, params, comment};
	}
}
