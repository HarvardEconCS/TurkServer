package edu.harvard.econcs.turkserver.mturk;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class MTurkUtils {

	private static DocumentBuilderFactory docBuilderFactory; 
	protected static DocumentBuilder docBuilder;
	       
	static {
		docBuilderFactory = DocumentBuilderFactory.newInstance();
		try {
			docBuilder = docBuilderFactory.newDocumentBuilder();

		} catch (ParserConfigurationException e) {                      
			e.printStackTrace();
		}
	}
	
	public static Document convert(String xml) {
		// TODO check if this is still necessary
//		xml = StringEscapeUtils.unescapeXml(xml);

		// Hack if they pasted in the java console
		String fixed = xml.replace("<n>", "[n]").replace("&", "&amp;");
		 
		try {
			Document doc = docBuilder.parse(new InputSource(new StringReader(fixed)));
			doc.getDocumentElement().normalize();
			return doc;
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		return null;				
	}
}
