package edu.harvard.econcs.turkserver.mturk.response;

import edu.harvard.econcs.turkserver.mturk.TurkException;

import java.net.URL;

public class DisableHITResponse extends RESTResponse {

	public DisableHITResponse(URL url) throws TurkException {
		super(url);
		
		if( !isValid() ) throw new TurkException(super.getErrorMsgs().toString());
	}

}
