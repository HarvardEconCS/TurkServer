package edu.harvard.econcs.turkserver.mturk;

import java.util.List;
import java.util.Set;

import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.requester.QualificationRequirement;
import com.amazonaws.mturk.service.exception.ServiceException;
import com.amazonaws.mturk.util.ClientConfig;

import edu.harvard.econcs.turkserver.mturk.RequesterServiceExt;

public class MockRequesterService extends RequesterServiceExt {

	// Just so the superclass doesn't effin complain
	static ClientConfig mockConfig;
	static {
		mockConfig = new ClientConfig();
		
		mockConfig.setAccessKeyId("someAccessKeyId");
		mockConfig.setSecretAccessKey("someSecretAccessKey");				
		mockConfig.setServiceURL(ClientConfig.SANDBOX_SERVICE_URL);
	}
	
	int hitCount = 0;	
	Set<String> createdHitIds;
	Set<String> disabledHitIds;
	
	public MockRequesterService() {
		super(mockConfig);		
	}
	
	void setCreationSet(Set<String> set) {
		this.createdHitIds = set;
	}
	
	void setDisableSet(Set<String> set) {
		this.disabledHitIds = set;
	}
	
	@Override
	public String registerHITType(Long autoApprovalDelayInSeconds,
			Long assignmentDurationInSeconds, Double reward, String title,
			String keywords, String description,
			QualificationRequirement[] qualRequirements)
			throws ServiceException {
		throw new UnsupportedOperationException();
	}

	@Override
	public HIT createHITExternalFromID(String hitTypeID, String title,
			String url, int frameHeight, String lifetimeInSeconds)
			throws ServiceException {
		HIT hit = new HIT();
		
		String hitId = "HIT " + ++hitCount; 			
		hit.setHITId(hitId);
		if( createdHitIds != null ) createdHitIds.add(hitId);
		
		return hit;
	}

	@Override
	public boolean notifyAllWorkers(String subject, String message,
			List<String> workers, int skipAmount) throws ServiceException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int disableAllHITs() {		
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int disableUnassignedHITs() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean safeDisableHIT(String hitId) {
		if( disabledHitIds != null )
			disabledHitIds.add(hitId);
		return true;
	}		

}
