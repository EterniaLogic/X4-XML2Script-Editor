package X4XMLJS;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
import org.xml.sax.SAXException;

public class JS2XML {
	
	public static String getXML(String js) {
		// First, determine what we are doing.
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
		
		
		// Quite a few tags were stripped when it was converted to a script. Now, they must be put back in.
		
		return xml;
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
