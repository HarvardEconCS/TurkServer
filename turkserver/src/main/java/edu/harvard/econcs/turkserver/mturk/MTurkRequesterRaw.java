package edu.harvard.econcs.turkserver.mturk;

import edu.harvard.econcs.turkserver.mturk.response.ApproveAssignmentResponse;
import edu.harvard.econcs.turkserver.mturk.response.AssignQualificationResponse;
import edu.harvard.econcs.turkserver.mturk.response.BlockWorkerResponse;
import edu.harvard.econcs.turkserver.mturk.response.CreateHITResponse;
import edu.harvard.econcs.turkserver.mturk.response.DisableHITResponse;
import edu.harvard.econcs.turkserver.mturk.response.DisposeHITResponse;
import edu.harvard.econcs.turkserver.mturk.response.ForceExpireHITResponse;
import edu.harvard.econcs.turkserver.mturk.response.GetAccountBalanceResponse;
import edu.harvard.econcs.turkserver.mturk.response.GetAssignmentsForHITResponse;
import edu.harvard.econcs.turkserver.mturk.response.GetHITResponse;
import edu.harvard.econcs.turkserver.mturk.response.GetReviewableHITsResponse;
import edu.harvard.econcs.turkserver.mturk.response.GrantBonusResponse;
import edu.harvard.econcs.turkserver.mturk.response.HelpResponse;
import edu.harvard.econcs.turkserver.mturk.response.NotifyWorkersResponse;
import edu.harvard.econcs.turkserver.mturk.response.RegisterHITTypeResponse;
import edu.harvard.econcs.turkserver.mturk.response.RejectAssignmentResponse;
import edu.harvard.econcs.turkserver.mturk.response.RevokeQualificationResponse;
import edu.harvard.econcs.turkserver.mturk.response.SearchHITsResponse;
import edu.harvard.econcs.turkserver.mturk.response.UpdateQualificationScoreResponse;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SignatureException;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * This class just implements low-level functions.
 * @author mao
 *
 */
public abstract class MTurkRequesterRaw {

	private static final String SERVICE = "AWSMechanicalTurkRequester";
	private static final String VERSION = "2008-08-02";

	private static final String SERVER = "http://mechanicalturk.amazonaws.com/";
	private static final String SERVER_SANDBOX = "http://mechanicalturk.sandbox.amazonaws.com/";

	protected final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
	protected final String awsAccessKeyID;
	protected final String awsSecretAccessKey;
	protected final String serverURL;

	public MTurkRequesterRaw(String awsAccessKeyID, String awsSecretAccessKey, boolean sandbox) {
		this.awsAccessKeyID = awsAccessKeyID;
		this.awsSecretAccessKey = awsSecretAccessKey;
		this.serverURL = sandbox ? SERVER_SANDBOX : SERVER;
	}

	/**
	 * Approves an assignment after HIT was answered. worker Gets Paid
	 * @param assignmentId 
	 * @param requesterFeedBack
	 */
	public ApproveAssignmentResponse approveAssignment(String assignmentId, String requesterFeedback)
	throws TurkException {
		/*
				 &Operation=ApproveAssignment
				 &AssignmentId=123RVWYBAZW00EXAMPLE456RVWYBAZW00EXAMPLE
		 */

		StringBuffer sb = new StringBuffer();
		sb.append("&AssignmentId=").append(AWSUtils.urlencode(assignmentId));
		sb.append("&RequesterFeedback=").append(AWSUtils.urlencode(requesterFeedback));

		return new ApproveAssignmentResponse(makeRequestString("ApproveAssignment", sb.toString()));
	}

	public AssignQualificationResponse assignQualification(String qualification, String worker, 
			int value, boolean sendNotification) 
	throws TurkException {
		
		StringBuffer sb = new StringBuffer();		
		sb.append("&QualificationTypeId=").append(AWSUtils.urlencode(qualification));
		sb.append("&WorkerId=").append(AWSUtils.urlencode(worker));
		sb.append("&IntegerValue=").append(Integer.valueOf(value));
		sb.append("&SendNotification=").append( sendNotification ? "true" : "false" );
		
		return new AssignQualificationResponse(makeRequestString("AssignQualification", sb.toString()));
	}
	
	public BlockWorkerResponse blockWorker(String workerID, String reason) 
	throws TurkException {
		StringBuffer sb = new StringBuffer();
		sb.append("&WorkerId=").append(AWSUtils.urlencode(workerID));
		sb.append("&Reason=").append(AWSUtils.urlencode(reason));		

		return new BlockWorkerResponse(makeRequestString("BlockWorker", sb.toString()));
	}
	
	/**
	 * Creates a new HIT using HittypeID
	 * @param hitTypeID 
	 * @param question 
	 * @param lifetimeInSeconds 
	 */
	public CreateHITResponse createHITFromID(String hitTypeID, String question, String lifetimeInSeconds)
	throws TurkException {
		/*
				&HITTypeId=T100CN9P324W00EXAMPLE
				&Question=[URL-encoded question data]
			    &LifetimeInSeconds=604800
		 */

		StringBuffer sb = new StringBuffer();
		sb.append("&HITTypeId=").append(AWSUtils.urlencode(hitTypeID));
		sb.append("&Question=").append(AWSUtils.urlencode(question));
		sb.append("&LifetimeInSeconds=").append(AWSUtils.urlencode(lifetimeInSeconds));

		return new CreateHITResponse(makeRequestString("CreateHIT", sb.toString()));
	}

	/**
	 * Creates a new HIT without HITTypeID
	 * @param title
	 * @param description 
	 * @param keywords 
	 * @param rewardAmountInUSD 
	 * @param maxAssignments 
	 * @param assignmentDurationInSeconds
	 * @param autoApprovalDelayInSeconds
	 * @param question
	 * @param lifetimeInSeconds   
	 */
	public CreateHITResponse createHIT(String title, String description, String keywords,
			String rewardAmountInUSD, long maxAssignments, int assignmentDurationInSeconds, int autoApprovalDelayInSeconds, String question,
			String lifetimeInSeconds) throws TurkException {
		/*
				&Title=Location%20and%20Photograph%20Identification
				&Description=Select%20the%20image%20that%20best%20represents...
				&Reward.1.Amount=5
				&Reward.1.CurrencyCode=USD
				&Question=[URL-encoded question data]
				&AssignmentDurationInSeconds=30
				&LifetimeInSeconds=604800
				&Keywords=location,%20photograph,%20image,%20identification,%20opinion
		 */

		StringBuffer sb = new StringBuffer();

		sb.append("&Title=").append(AWSUtils.urlencode(title));
		sb.append("&Description=").append(AWSUtils.urlencode(description));
		sb.append("&Reward.1.Amount=").append(AWSUtils.urlencode(rewardAmountInUSD));
		sb.append("&Reward.1.CurrencyCode=").append("USD");
		sb.append("&AssignmentDurationInSeconds=").append(String.valueOf(assignmentDurationInSeconds));
		sb.append("&MaxAssignments=").append(String.valueOf(maxAssignments));
		sb.append("&Keywords=").append(AWSUtils.urlencode(keywords));
		sb.append("&AutoApprovalDelayInSeconds=").append(String.valueOf(autoApprovalDelayInSeconds));
		sb.append("&Question=").append(AWSUtils.urlencode(question));
		sb.append("&LifetimeInSeconds=").append(AWSUtils.urlencode(lifetimeInSeconds));

		return new CreateHITResponse(makeRequestString("CreateHIT", sb.toString()));
	}

	/**
	 * The disableHIT operation
	 * @param hitId
	 * @return
	 * @throws IOException
	 */
	public DisableHITResponse disableHIT(String hitId) throws TurkException {		
		return new DisableHITResponse(makeRequestString("DisableHIT", "&HITId=" + hitId ));
	}
	
	/**
	 * The disposeHIT operation
	 * @param hitId
	 * @return
	 * @throws IOException
	 */
	public DisposeHITResponse disposeHIT(String hitId) throws TurkException {		
		return new DisposeHITResponse(makeRequestString("DisposeHIT", "&HITId=" + hitId ));
	}
	
	/**
	 * The ForceExpireHIT operation
	 * @param hitId
	 * @return
	 * @throws TurkException
	 */
	public ForceExpireHITResponse forceExpireHIT(String hitId) throws TurkException {
		return new ForceExpireHITResponse(makeRequestString("ForceExpireHIT", "&HITId=" + hitId));
	}

	/**
	 * Gets amount of money remaining in account
	 * @return
	 * @throws IOException
	 */
	public GetAccountBalanceResponse getAccountBalance() throws TurkException {
		return new GetAccountBalanceResponse(makeRequestString("GetAccountBalance", ""));
	}

	/**
	 * Get Results of a HIT.
	 * @param hitID Hit ID
	 */
	public GetAssignmentsForHITResponse getAssignmentsForHIT(String hitID) throws TurkException {
		/*
			&Operation=GetAssignmentsForHIT
			&HITId=123RVWYBAZW00EXAMPLE
			&PageSize=5
			&PageNumber=1
		 */

		StringBuffer sb = new StringBuffer();
		sb.append("&HITId=").append(AWSUtils.urlencode(hitID));

		return new GetAssignmentsForHITResponse(makeRequestString("GetAssignmentsForHIT", sb.toString()));
	}

	/**
	 * Get HIT given the HIT ID
	 * @param hitID Hit ID
	 */
	public GetHITResponse getHIT(String hitID) throws TurkException {
		/*
		 	&Operation=GetHIT
		 	&HITId=123RVWYBAZW00EXAMPLE
		 */

		StringBuffer sb = new StringBuffer();
		sb.append("&HITId=").append(AWSUtils.urlencode(hitID));

		return new GetHITResponse(makeRequestString("GetHIT", sb.toString()));
	}

	/**
	 * Gets Reviewable HITs.
	 */
	public GetReviewableHITsResponse getReviewableHITs() throws TurkException {
		/*
			&Operation=GetReviewableHITs
			&PageSize=5
			&PageNumber=1
		 */

		StringBuffer sb = new StringBuffer();
		//sb.append("&PageSize=").append(String.valueOf(pageSize));
		//sb.append("&PageNumber=").append(String.valueOf(pageNumber));

		return new GetReviewableHITsResponse(makeRequestString("GetReviewableHITs", sb.toString()));
	}

	/**
	 * Grants a bonus
	 * @param workerId
	 * @param assignmentId
	 * @param bonusAmount
	 * @param reason
	 * @return
	 * @throws TurkException
	 */
	public GrantBonusResponse grantBonus(String workerId, String assignmentId, String bonusAmount, String reason)
	throws TurkException
	{
		StringBuffer sb = new StringBuffer();
		
		sb.append("&WorkerId=").append(AWSUtils.urlencode(workerId));
		sb.append("&AssignmentId=").append(AWSUtils.urlencode(assignmentId));
		sb.append("&BonusAmount.1.Amount=").append(AWSUtils.urlencode(bonusAmount));
		sb.append("&BonusAmount.1.CurrencyCode=USD");
		sb.append("&Reason=").append(AWSUtils.urlencode(reason));
		
		return new GrantBonusResponse(makeRequestString("GrantBonus", sb.toString()));
	}
	
	/**
	 * Requests help about an operation
	 * @param operation
	 * @return
	 * @throws IOException
	 */
	public HelpResponse help(String operation) throws TurkException {
		StringBuffer sb = new StringBuffer();

		sb.append("&HelpType=Operation");
		sb.append("&About=").append(operation);

		return new HelpResponse(makeRequestString("Help", sb.toString()));
	}

	/**
	 * Sends a message to workers. Takes up to 100 from the iterator.
	 * @param subject
	 * @param message
	 * @param workers
	 * @return
	 * @throws TurkException 
	 */
	public NotifyWorkersResponse notifyWorkers(String subject, String message, Iterator<String> workers) 
	throws TurkException {	
		StringBuffer sb = new StringBuffer();
		
		sb.append("&Subject=").append(AWSUtils.urlencode(subject));
		sb.append("&MessageText=").append(AWSUtils.urlencode(message));
		
		int i = 1;

		// TODO find out why this is crashing
		while( workers.hasNext() && i <= 10 ) {
			sb.append("&WorkerId.").append(i).append("=").append(AWSUtils.urlencode(workers.next()));
			i++;
		}
		
		return new NotifyWorkersResponse(makeRequestString("NotifyWorkers", sb.toString()));
	}
	
	/**
	 * Searches HITs
	 * @param sortProperty
	 * @param sortAscending
	 * @param pageSize
	 * @param pageNumber
	 * @return
	 * @throws TurkException
	 */
	public SearchHITsResponse searchHITs(String sortProperty, boolean sortAscending, int pageSize, int pageNumber)
	throws TurkException {		
		StringBuffer sb = new StringBuffer();

		if (sortProperty != null) sb.append("&SortProperty=").append(sortProperty);
		sb.append("&SortDirection=").append( sortAscending ? "Ascending" : "Descending" );
		sb.append("&PageSize=").append(String.valueOf(pageSize));
		sb.append("&PageNumber=").append(String.valueOf(pageNumber));

		return new SearchHITsResponse(makeRequestString("SearchHITs", sb.toString()));
	}

	/**
	 * Registers an HITType
	 * @param title
	 * @param description 
	 * @param keywords 
	 * @param rewardAmountInUSD 
	 * @param assignmentDurationInSeconds
	 * @param autoApprovalDelayInSeconds
	 */
	public RegisterHITTypeResponse registerHITType(String title, String description, String keywords,
			String rewardAmountInUSD, int assignmentDurationInSeconds, int autoApprovalDelayInSeconds,
			String qualificationRequirement) throws TurkException {
		
		/*&Title=Location%20and%20Photograph%20Identification
			    	&Description=Select%20the%20image%20that%20best%20represents...
			    	&Reward.1.Amount=5
			    	&Reward.1.CurrencyCode=USD
			    	&AssignmentDurationInSeconds=30
			    	&Keywords=location,%20photograph,%20image,%20identification,%20opinion
			    	&QualificationRequirement.1.QualificationTypeId=789RVWYBAZW00EXAMPLE
					&QualificationRequirement.1.Comparator=GreaterThan
					&QualificationRequirement.1.IntegerValue=18
					&QualificationRequirement.2.QualificationTypeId=237HSIANVCI00EXAMPLE
					&QualificationRequirement.2.Comparator=EqualTo
					&QualificationRequirement.2.IntegerValue=1

		 */
		StringBuffer sb = new StringBuffer();

		sb.append("&Title=").append(AWSUtils.urlencode(title));
		sb.append("&Description=").append(AWSUtils.urlencode(description));
		sb.append("&Reward.1.Amount=").append(AWSUtils.urlencode(rewardAmountInUSD));
		sb.append("&Reward.1.CurrencyCode=").append("USD");
		sb.append("&AssignmentDurationInSeconds=").append(String.valueOf(assignmentDurationInSeconds));
		sb.append("&Keywords=").append(AWSUtils.urlencode(keywords));
		sb.append("&AutoApprovalDelayInSeconds=").append(String.valueOf(autoApprovalDelayInSeconds));
		
		if( qualificationRequirement != null ) 
			sb.append(qualificationRequirement);

		return new RegisterHITTypeResponse(makeRequestString("RegisterHITType", sb.toString()));              	
	}

	/**
	 * Rejects an assignment after HIT was answered.
	 * @param assignmentId 
	 * @param requesterFeedBack
	 */
	public RejectAssignmentResponse rejectAssignment(String assignmentId, String requesterFeedback)
	throws TurkException {
		/*
				 	&Operation=RejectAssignment
				 	&AssignmentId=123RVWYBAZW00EXAMPLE456RVWYBAZW00EXAMPLE
		 */

		StringBuffer sb = new StringBuffer();
		sb.append("&AssignmentId=").append(AWSUtils.urlencode(assignmentId));
		sb.append("&RequesterFeedback=").append(AWSUtils.urlencode(requesterFeedback));

		return new RejectAssignmentResponse(makeRequestString("RejectAssignment", sb.toString()));
	}

	public RevokeQualificationResponse revokeQualification(String subjectId, String qual, String reason) 
	throws TurkException {
		
		StringBuffer sb = new StringBuffer();
		
		sb.append("&SubjectId=").append(AWSUtils.urlencode(subjectId));
		sb.append("&QualificationTypeId=").append(AWSUtils.urlencode(qual));
		sb.append("&Reason=").append(AWSUtils.urlencode(reason));
		
		return new RevokeQualificationResponse(makeRequestString("RevokeQualification", sb.toString()));
	}
	
	public UpdateQualificationScoreResponse updateQualificationScore(
			String qualification, String worker, int value) throws TurkException {
		
		StringBuffer sb = new StringBuffer();		
		sb.append("&QualificationTypeId=").append(AWSUtils.urlencode(qualification));
		sb.append("&SubjectId=").append(AWSUtils.urlencode(worker));
		sb.append("&IntegerValue=").append(Integer.valueOf(value));		
		
		return new UpdateQualificationScoreResponse(makeRequestString("UpdateQualificationScore", sb.toString()));
	}
	
	private URL makeRequestString(String operation, String operationParamsString)
	throws TurkException {
		StringBuffer urlSB = new StringBuffer();

		String timeStamp = AWSUtils.getCurrentTimeStampAsString();
		String signature;

		try {
			signature = AWSUtils.generateSignature(SERVICE, operation, timeStamp, awsSecretAccessKey);
		} catch (SignatureException e) {						
			throw new TurkException(e);
		}

		// Common parameters
		urlSB.append(serverURL);
		urlSB.append("?Service=").append(SERVICE);
		urlSB.append("&AWSAccessKeyId=").append(awsAccessKeyID);
		urlSB.append("&Version=").append(VERSION);
		urlSB.append("&Operation=").append(operation);
		urlSB.append("&Signature=").append(AWSUtils.urlencode(signature));
		urlSB.append("&Timestamp=").append(AWSUtils.urlencode(timeStamp));

		/*http://mechanicalturk.amazonaws.com/?Service=AWSMechanicalTurkRequester
					&AWSAccessKeyId=[the Requester's Access Key ID]
					&Version=2006-06-20
					&Operation=CreateHIT
					&Signature=[signature for this request]
					&Timestamp=[your system's local time]
		 */

		// Operation-specific parameters
		urlSB.append(operationParamsString);

		// logger.info(String.format("%s: %s", operation, urlSB.toString()));

		try {
			return new URL(urlSB.toString());		
		} catch (MalformedURLException e) {			
			throw new TurkException(e);			
		} 			
	}

}
