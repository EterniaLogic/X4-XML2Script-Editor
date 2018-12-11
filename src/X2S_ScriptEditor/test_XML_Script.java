package X2S_ScriptEditor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import X2S_ScriptEditor.Script.XML2JS;
import X2S_ScriptEditor.XML.JS2XML;

public class test_XML_Script {

	// no gui testing with /cat directory
	public static void main(String[] args) {
		//XML2JS x2js = new XML2JS(System.getProperty("user.dir")+"/cat/aiscripts/order.move.recon.xml");
		XML2JS x2js = new XML2JS(System.getProperty("user.dir")+"/cat/aiscripts/masstraffic.watchdog.xml");
		//XML2JS x2js = new XML2JS(System.getProperty("user.dir")+"/cat/aiscripts/boarding.pod.return.xml");
		//XML2JS x2js = new XML2JS(System.getProperty("user.dir")+"/testin.xml");
		String js = x2js.getJS();
		
		// save
		try {
		String saveloc = System.getProperty("user.dir")+"/test.xml.script";
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(saveloc)));
		    writer.write(js);
		    writer.close();
			}catch(Exception e) {}
			
			String xml = JS2XML.getXML(js);
		    
		// save XML
		try {
			String saveloc = System.getProperty("user.dir")+"/test.xml.test";
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(saveloc)));
		    writer.write(xml);
		    writer.close();
		}catch(Exception e) {}
	}
}
