/**
 * 
 */
package edu.harvard.econcs.turkserver.mturk.response;

import edu.harvard.econcs.turkserver.mturk.TurkException;

import java.net.URL;

public class CreateHITResponse extends RESTResponse {
	
	public CreateHITResponse(URL url) throws TurkException {
		super(url);
		
		if( !isValid() ) throw new TurkException(super.getErrorMsgs().toString());
	}

	public String getHITId() {		
		return super.getXPathValue("//HIT/HITId");
	}

}
