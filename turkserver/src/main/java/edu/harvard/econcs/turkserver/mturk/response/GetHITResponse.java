package edu.harvard.econcs.turkserver.mturk.response;

import edu.harvard.econcs.turkserver.mturk.AWSUtils;
import edu.harvard.econcs.turkserver.mturk.HIT;
import edu.harvard.econcs.turkserver.mturk.TurkException;

import java.net.URL;
import java.text.ParseException;

import javax.xml.transform.TransformerException;

import org.apache.xpath.CachedXPathAPI;
import org.w3c.dom.Node;

public class GetHITResponse extends RESTResponse {

	public GetHITResponse(URL url) throws TurkException {
		super(url);
		
		if( !isValid() ) throw new TurkException(super.getErrorMsgs().toString());
	}

	public String getHITStatus() {
		return super.getXPathValue("//HIT/HITStatus");
	}
	
	/**
	 * This is different than the one from SearchHITs
	 * @return
	 */
	public HIT getHIT() {
		CachedXPathAPI xpath = new CachedXPathAPI();
		Node hitNode = super.getAllNodes("//HIT").nextNode();		
		
		HIT hit = new HIT();
		
		try {
			hit.setHitId(xpath.eval(hitNode, "HITId").toString());
			hit.setHitTypeId(xpath.eval(hitNode, "HITTypeId").toString());				
			hit.setCreationTime(
					AWSUtils.parseDateFromString(
							xpath.eval(hitNode, "CreationTime").toString()));
			hit.setTitle(xpath.eval(hitNode, "Title").toString());
			hit.setDescription(xpath.eval(hitNode, "Description").toString());
			
			// TODO xml-unescaped question field
			
			hit.setKeywords(xpath.eval(hitNode, "Keywords").toString());
			hit.setHitStatus(xpath.eval(hitNode, "HITStatus").toString());
			hit.setMaxAssignments(
					Integer.parseInt(
							xpath.eval(hitNode, "MaxAssignments").toString()));
			hit.setRewardInUSD(xpath.eval(hitNode, "Reward/Amount").toString());
			hit.setAutoApprovalDelayInSeconds(
					Integer.parseInt(
							xpath.eval(hitNode, "AutoApprovalDelayInSeconds").toString()));
			hit.setExpiration(
					AWSUtils.parseDateFromString(
							xpath.eval(hitNode, "Expiration").toString()));
			hit.setAssignmentDurationInSeconds(
					Integer.parseInt(
							xpath.eval(hitNode, "AssignmentDurationInSeconds").toString()));
			
			// TODO NumberOfSimilarHITs
			
			// TODO QualificationRequirement
			
			// TODO HITReviewStatus
			
			return hit;			
		} catch (TransformerException e) {				
			e.printStackTrace();
		} catch (ParseException e) {				
			e.printStackTrace();
		}	
		
		return null;
	}
	
}
