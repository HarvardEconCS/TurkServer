package edu.harvard.econcs.turkserver.mturk.response;

import edu.harvard.econcs.turkserver.mturk.AWSUtils;
import edu.harvard.econcs.turkserver.mturk.Assignment;
import edu.harvard.econcs.turkserver.mturk.TurkException;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.xpath.CachedXPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class GetAssignmentsForHITResponse extends RESTResponse {

	public GetAssignmentsForHITResponse(URL url) throws TurkException {
		super(url);
		
		if( !isValid() ) throw new TurkException(super.getErrorMsgs().toString());
	}

	public int getNumResults() {
		return Integer.parseInt(super.getXPathValue("//NumResults"));
	}
	
	public List<Assignment> getAssignments() {
		NodeIterator ni = super.getAllNodes("//Assignment");
		List<Assignment> assignmentList = new ArrayList<Assignment>(getNumResults());
		
		CachedXPathAPI xpath = new CachedXPathAPI();
		Node n = null;
		while( (n = ni.nextNode()) != null ) {
			Assignment a = new Assignment();
			
			String unescaped = null;
			
			try {
				a.setAssignmentId(xpath.eval(n, "AssignmentId").toString());
				a.setWorkerId(xpath.eval(n, "WorkerId").toString());
				a.setHitId(xpath.eval(n, "HITId").toString());
				
				a.setAssignmentStatus(xpath.eval(n, "AssignmentStatus").toString());
				
				a.setAutoApprovalTime(
						AWSUtils.parseDateFromString(
								xpath.eval(n, "AutoApprovalTime").toString()));
				a.setAcceptTime(
						AWSUtils.parseDateFromString(
								xpath.eval(n, "AcceptTime").toString()));
				a.setSubmitTime(
						AWSUtils.parseDateFromString(
								xpath.eval(n, "SubmitTime").toString()));
				
				// Unescape the xml in the answer
				unescaped =
					StringEscapeUtils.unescapeXml(xpath.eval(n, "Answer").toString());
				
				// Hack if they pasted in the java console
				String fixed = unescaped.replace("<n>", "[n]").replace("&", "&amp;");
				
				Document doc = RESTResponse.docBuilder.parse(
						new InputSource(new StringReader(fixed)));
				doc.getDocumentElement().normalize();
				a.setAnswer(doc);
				
				assignmentList.add(a);
			} catch(TransformerException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
				System.out.println(unescaped);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return assignmentList;	
	}
}
