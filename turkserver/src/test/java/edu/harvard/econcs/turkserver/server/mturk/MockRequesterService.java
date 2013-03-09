package edu.harvard.econcs.turkserver.server.mturk;

import java.util.List;

import com.amazonaws.mturk.requester.HIT;
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
	
	public MockRequesterService() {
		super(mockConfig);		
	}
	
	@Override
	public HIT createHITExternalFromID(String hitTypeID, String title,
			String url, int frameHeight, String lifetimeInSeconds)
			throws ServiceException {
		throw new UnsupportedOperationException();
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
	public void safeDisableHIT(String hitId) {
		throw new UnsupportedOperationException();
	}
	
	

}
