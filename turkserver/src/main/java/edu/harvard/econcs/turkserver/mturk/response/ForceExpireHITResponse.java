package edu.harvard.econcs.turkserver.mturk.response;

import edu.harvard.econcs.turkserver.mturk.TurkException;

import java.net.URL;

public class ForceExpireHITResponse extends RESTResponse {

	public ForceExpireHITResponse(URL url) throws TurkException {
		super(url);		
	}

}
