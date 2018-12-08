package X4XMLJS;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

public class Utils {
	
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
}
