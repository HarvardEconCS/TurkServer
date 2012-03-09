package edu.harvard.econcs.turkserver.mturk.response;

import java.net.URL;

import edu.harvard.econcs.turkserver.mturk.TurkException;


public class NotifyWorkersResponse extends RESTResponse {

	public NotifyWorkersResponse(URL url) throws TurkException {
		super(url);
		
		if( !isValid() ) {
			super.printXMLResponse();
			throw new TurkException(super.getErrorMsgs().toString());
		}
	}

}
