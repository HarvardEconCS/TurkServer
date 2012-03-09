package edu.harvard.econcs.turkserver.mturk.response;

import edu.harvard.econcs.turkserver.mturk.TurkException;

import java.net.URL;

public class GetAccountBalanceResponse extends RESTResponse {

	public GetAccountBalanceResponse(URL url) throws TurkException {
		super(url);
		
		if( !isValid() ) throw new TurkException(super.getErrorMsgs().toString());
	}

}
