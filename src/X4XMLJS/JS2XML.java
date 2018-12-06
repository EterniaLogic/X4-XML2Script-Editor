package X4XMLJS;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.XMLConstants;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;

public class JS2XML {
	
	public static String getXML(String js) {
		return "";
	}
	
	public static boolean verifyXML(String xml) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			File ftmp = File.createTempFile("js2xmltmp-"+md5(xml), ".xml");
			Document doc = dBuilder.parse(ftmp);
			
			
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
