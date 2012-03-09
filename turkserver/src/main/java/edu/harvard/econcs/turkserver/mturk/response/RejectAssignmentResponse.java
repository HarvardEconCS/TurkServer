package edu.harvard.econcs.turkserver.mturk.response;

import java.net.URL;

import edu.harvard.econcs.turkserver.mturk.TurkException;


public class RejectAssignmentResponse extends RESTResponse {

	public RejectAssignmentResponse(URL url) throws TurkException {
		super(url);
		
		if( !isValid() ) throw new TurkException(super.getErrorMsgs().toString());
	}

}
