package X4XMLJS;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

// SubNode class used when converting Script to XML
class PreXMLNode{
	public String name="", value="";
	public HashMap<String,PreXMLNode> attributes = new HashMap<String,PreXMLNode>();
	private List<PreXMLNode> children = new LinkedList<PreXMLNode>();
	public PreXMLNode parent = null;
	
	public PreXMLNode(String name) {
		this.name=name;
	}
	
	public void printnode(int tab) {
		// this node
		System.out.print(Utils.toTab(tab+1)+"<"+name);
		for(PreXMLNode n : attributes.values()) {
			System.out.print(" "+n.name+"=\""+n.value+"\"");
		}
		
		value = value.replace("\"", "\\\"");
		
		if(!value.equals(""))
			System.out.print(" \""+value+"\"");
		
		if(children.size() == 0) System.out.println(" />"); 
		else System.out.println(">");
		
		// subnodes
		for(PreXMLNode n : children) {
			if(n != null)
				n.printnode(tab+1);
			else System.out.println("null node");
		}
	}
	
	// recursive down by elements
	public String toXML(int tab) {
		String t = "";
		
		if(name.equals("#comment")) {
			t += Utils.toTab(tab)+"<!-- ";
			t += value;
			t += "-->\n";
		}else {
		
			t += Utils.toTab(tab)+"<"+toXMLName();
			
			boolean multicloseoff = children.size() > 0 
					|| toXMLName().equals("do_if") || toXMLName().equals("do_else") // programmer error with a loop that has not content
					|| toXMLName().equals("do_elseif") || toXMLName().equals("do_any") || toXMLName().equals("do_while");
			
			
			List<PreXMLNode> elist = new LinkedList<PreXMLNode>();
			for(PreXMLNode n : attributes.values()) elist.add(n);
			
			List<PreXMLNode> attrlist = new LinkedList<PreXMLNode>();
			
			// listing methods to sort attribute tags by type
			String[] preflist = {"id","name", "exact", "order", "param", "value", "object", "type", "dock", "text", "chance", "class", "space", "commandaction"};
			
			// run attributes first and sort the 'preferred' attributes to be first
			for(String nam : preflist) {
				for(PreXMLNode n : attributes.values()) {
					if(n.name.equals(nam)) {
						attrlist.add(n);
						for(int i=0;i<elist.size();i++) {
							if(elist.get(i).name.equals(nam)) {
								elist.remove(i);
								break;
							}
						}
					}
				}
			}
			
			for(PreXMLNode n : attrlist) {
				//System.out.print(" "+n.name+"=\""+n.value+"\"");
				t+=" "+n.name+"=\""+n.value+"\"";
			}
			
			for(PreXMLNode n : elist) {
				//System.out.print(" "+n.name+"=\""+n.value+"\"");
				t+=" "+n.name+"=\""+n.value+"\"";
			}
			
			if(!multicloseoff)
				t += " />\n";
			else t+= ">\n";
			
			// loop through children
			for(PreXMLNode n : children) {
				t += n.toXML(tab+1);
			}
			
			
			if(multicloseoff)
				t += Utils.toTab(tab)+"</"+toXMLName()+">\n";
		}
		
		return t;
	}
	
	private String toXMLName() {
		if(name.equals("if")) return "do_if";
		else if(name.equals("while")) return "do_while";
		else if(name.equals("any")) return "do_any";
		else if(name.equals("else")) return "do_else";
		else if(name.equals("elseif")) return "do_elseif";
		else if(name.equals("interrupt_handler_if")) return "handler";
		else if(name.equals("interrupt_if")) return "interrupt";
		else if(name.equals("interrupt_ref")) return "interrupt";
		
		return name;
	}
	
	// look up parents and find if a parent is an interrupt
	public boolean isInterruptAction(PreXMLNode parentnode) {
		PreXMLNode par = parentnode;
		//System.out.println("isiaction: "+par);
		while(par != null) {
			//System.out.println("isiaction: "+par.name);
			if(par.name.startsWith("interrupt")) {
				return true;
			}
			
			par = par.parent;
		}
		
		return false;
	}
	
	public void addChild(PreXMLNode child) {
		PreXMLNode actns = null;
		for(PreXMLNode n : this.children)
			if(n.name.equals("actions")) 
				actns = n;
		
		
		if(this.children.contains(child)) return; // prevent duplicates
		else if(actns != null && actns.getChildren().contains(child)) return;
		
		if((name.equals("attention") || name.equals("interrupt_handler_if") || name.equals("interrupt_if"))
				&& !child.name.equals("actions")) {
			
			if(name.equals("interrupt_handler_if") || name.equals("interrupt_if"))
				System.out.println("add interrupt child: "+child.name);
			
			// conditions is a base node, otherwise add to actions
			if(!child.name.equals("conditions")) {
				
				
				if(actns == null) {
					actns = new PreXMLNode("actions");
					this.children.add(actns);
				}
				
				actns.parent = this; // make sure that actions node has a parent...
				child.parent = actns;
				actns.children.add(child); // add to <actions>
			}else{
				child.parent = this;
				children.add(child);
			}
		}else {
			child.parent=this;
			children.add(child);
		}
	}
	
	public void addChildren(List<PreXMLNode> children) {
		for(PreXMLNode c : children)
			addChild(c);
	}
	
	public List<PreXMLNode> getChildren(){
		return children;
	}
	
	public void putAttrNode(String name, String value) {
		PreXMLNode namen = new PreXMLNode(name);
		namen.value = value;
		this.attributes.put(name, namen);
	}
	
	public void addAttrsFromParams(String params, String splitter) {
		if(params.contains(splitter)) {
			for(String s : params.split(splitter)) {
				addAttrFromParam(s);
			}
		}else {
			addAttrFromParam(params);
		}
	}
	
	public void addAttrFromParam(String param) {
		if(param.contains("=")) {
			String[] t = param.split("=",2);
			putAttrNode(t[0].trim(), t[1].replace("\"", ""));
		}
	}
}