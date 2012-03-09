package edu.harvard.econcs.turkserver.mturk.response;

import java.net.URL;

import edu.harvard.econcs.turkserver.mturk.TurkException;


public class RegisterHITTypeResponse extends RESTResponse {

	public RegisterHITTypeResponse(URL url) throws TurkException {
		super(url);
		
		if( !isValid() ) throw new TurkException(super.getErrorMsgs().toString());
	}

	public String getHITTypeId() {		
		return super.getXPathValue("//HITTypeId");
	}

}
