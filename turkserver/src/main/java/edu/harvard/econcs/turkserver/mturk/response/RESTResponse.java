//  This software code is made available "AS IS" without warranties of any
//  kind.  You may copy, display, modify and redistribute the software
//  code either by itself or as incorporated into your code; provided that
//  you do not remove any proprietary notices.  Your use of this software
//  code is at your own risk and you waive any claim against Amazon
//  Digital Services, Inc. or its affiliates with respect to your use of
//  this software code. (c) 2006 Amazon Digital Services, Inc. or its
//  affiliates.

package edu.harvard.econcs.turkserver.mturk.response;

import edu.harvard.econcs.turkserver.mturk.TurkException;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownServiceException;

import java.io.IOException;
import java.io.InputStream;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;

import org.apache.xpath.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import java.util.LinkedList;
import java.util.List;

/**
 * A Response object returned from AWSAuthConnection.get(). Exposes the
 * attribute object, which represents the retrieved object.
 */
public abstract class RESTResponse {
	// XML things that need to be instantiated
	private static DocumentBuilderFactory docBuilderFactory; 
	protected static DocumentBuilder docBuilder; 
	
	private static TransformerFactory transformerFactory;
	protected static Transformer transformer;
	
	static {
		docBuilderFactory = DocumentBuilderFactory.newInstance();
		try {
			docBuilder = docBuilderFactory.newDocumentBuilder();
			
		} catch (ParserConfigurationException e) {			
			e.printStackTrace();
		}
		
		transformerFactory = TransformerFactory.newInstance();
		try {
			transformer = transformerFactory.newTransformer();
		} catch (TransformerConfigurationException e) { 
			e.printStackTrace();
		}
	}
		
	private Document doc;
	private int responseCode = -1;
	
	private boolean isError = false;	

	/**
	 * Pulls a representation of an S3Object out of the HttpURLConnection
	 * response.
	 * @throws IOException 
	 */
	public RESTResponse(URL url) throws TurkException {										 
		try {
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			
			responseCode = connection.getResponseCode();			
			isError = (responseCode >= 400);
			
			InputStream xmlStream;
			
			if( !isError ) xmlStream = connection.getInputStream();
			else xmlStream = connection.getErrorStream();
			
			/***************************************************************
			 * How to use turn an XML file into a document object in Java
			 **************************************************************/
			// Parse the XML file and build the Document object in RAM
			doc = docBuilder.parse(xmlStream);

			// Normalize text representation.
			// Collapses adjacent text nodes into one node.
			doc.getDocumentElement().normalize();
			/** ************************************************************* */
				
		} catch (UnknownServiceException e) {			
			throw new TurkException(e);
		} catch (SAXException e) {
			// Parse error
			throw new TurkException(e);
		} catch (IOException e) {
			throw new TurkException(e);
		}
		
	}

	public Document getDocument() {
		return doc;
	}

	public int getResponseCode() {
		return responseCode;		
	}
	
	public boolean isError() {
		return isError;
	}
	
	public boolean isValid() {
		return getXPathValue("//Request/IsValid").equals("True");
		
	}

	public List<String> getErrorMsgs() {
		return getXPathValues("//Errors/Error/Message");
	}
	
	/**
	 * Print out the raw XML (for debugging)
	 * @throws TransformerException
	 */
	public void printXMLResponse() {
		
		transformer.setOutputProperty("indent", "yes");
		try {
			transformer.transform(new DOMSource(doc), new StreamResult(System.out));
		} catch (TransformerException e) {			
			e.printStackTrace();
		}		
	}

	/**
	 * Returns the first node that matches the xpath.
	 * @param xpathString
	 * @return
	 */
	protected String getXPathValue(String xpathString) {
		String str = null;
		
		try {
			// Catches the first node that meets the criteria of xpath string
			str = XPathAPI.eval(doc, xpathString).toString();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return str;
	}

	/**
	 * Returns all nodes that match the xpath.
	 * @param xpathString
	 * @return
	 */
	protected List<String> getXPathValues(String xpathString) {

		LinkedList<String> strarray = new LinkedList<String>();
		
		try {
			// Catches all the nodes that meets the criteria of xpath string
			NodeList nl = XPathAPI.selectNodeList(doc, xpathString);

			for (int i = 0; i < nl.getLength(); i++) {
				Node n = nl.item(i);
				strarray.add(n.getTextContent());
				
				// Serialize the found nodes to System.out
				// serializer.transform(new DOMSource(n),new StreamResult(System.out));
			}
		} catch (TransformerException e) {
			e.printStackTrace();
		}

		return strarray;
	}

	/**
	 * Gets an iterator over the nodes for some xpath string
	 * @param xpathString
	 * @return
	 */
	protected NodeIterator getAllNodes(String xpathString) {
		try {
			return XPathAPI.selectNodeIterator(doc, xpathString);
			
		} catch (TransformerException e) {			
			e.printStackTrace();
		}
		
		return null;
	}
}
