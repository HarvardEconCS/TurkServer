package edu.harvard.econcs.turkserver.mturk.response;

import edu.harvard.econcs.turkserver.mturk.AWSUtils;
import edu.harvard.econcs.turkserver.mturk.HIT;
import edu.harvard.econcs.turkserver.mturk.TurkException;

import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.xpath.CachedXPathAPI;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;

public class SearchHITsResponse extends RESTResponse {		

	public SearchHITsResponse(URL url) throws TurkException {
		super(url);
		
		if( !isValid() ) throw new TurkException(super.getErrorMsgs().toString());
	}

	public int getNumResults() {		
		return Integer.parseInt(super.getXPathValue("//NumResults"));
	}

	public int getTotalNumResults() {
		return Integer.parseInt(super.getXPathValue("//TotalNumResults"));
	}
	
	public List<String> getHITIds() {
		// TODO fix this to get other things from the list of HITs
		return super.getXPathValues("//HIT/HITId");
	}

	public List<HIT> getHITList(List<HIT> hitList) {
		// Add to existing list, or create a new one if null
		if( hitList == null) hitList = new ArrayList<HIT>(getNumResults());
		
		NodeIterator hitIt = super.getAllNodes("//HIT");
		CachedXPathAPI xpath = new CachedXPathAPI();
		Node hitNode = null;
		while( (hitNode = hitIt.nextNode()) != null) {
			HIT hit = new HIT();
						
			try {
				hit.setHitId(xpath.eval(hitNode, "HITId").toString());
				hit.setHitTypeId(xpath.eval(hitNode, "HITTypeId").toString());				
				hit.setCreationTime(
						AWSUtils.parseDateFromString(
								xpath.eval(hitNode, "CreationTime").toString()));
				hit.setTitle(xpath.eval(hitNode, "Title").toString());
				hit.setDescription(xpath.eval(hitNode, "Description").toString());
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
				hit.setNumAssignmentsPending(
						Integer.parseInt(
								xpath.eval(hitNode, "NumberOfAssignmentsPending").toString()));
				hit.setNumAssignmentsAvailable(
						Integer.parseInt(
								xpath.eval(hitNode, "NumberOfAssignmentsAvailable").toString()));
				hit.setNumAssignmentsCompleted(
						Integer.parseInt(
								xpath.eval(hitNode, "NumberOfAssignmentsCompleted").toString()));
				
				hitList.add(hit);
				
			} catch (TransformerException e) {				
				e.printStackTrace();
			} catch (ParseException e) {				
				e.printStackTrace();
			}						
		}
				
		return hitList;
	}

}
