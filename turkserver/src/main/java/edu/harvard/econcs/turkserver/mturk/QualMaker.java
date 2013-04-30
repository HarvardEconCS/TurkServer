package edu.harvard.econcs.turkserver.mturk;

import com.amazonaws.mturk.requester.Comparator;
import com.amazonaws.mturk.requester.Locale;
import com.amazonaws.mturk.requester.QualificationRequirement;

public abstract class QualMaker {
	
	static final String WORKER_APPROVAL_RATE = "000000000000000000L0";
	static final String WORKER_HITS_APPROVED = "00000000000000000040";
	static final String WORKER_LOCALE = "00000000000000000071";
	
	public static QualificationRequirement getMinApprovalRateQual(int approvalRate) {								
		String qualificationTypeId = WORKER_APPROVAL_RATE;
		Comparator comparator = Comparator.GreaterThan;
				
		return new QualificationRequirement(qualificationTypeId , comparator, 
				approvalRate, null, null);
													
	}

	public static QualificationRequirement getMinTasksApprovedQual(int numTasks) {
		String qualificationTypeId = WORKER_HITS_APPROVED;
		Comparator comparator = Comparator.GreaterThanOrEqualTo;
		
		return new QualificationRequirement(qualificationTypeId , comparator, 
				numTasks, null, null);
	}

	public static QualificationRequirement getLocaleQual(String locale) {
		String qualificationTypeId = WORKER_LOCALE;
		Comparator comparator = Comparator.EqualTo;		
				
		return new QualificationRequirement(qualificationTypeId, comparator, 
				null, new Locale(locale), null);
									
	}

	public static QualificationRequirement getCustomQual(String qualId, Comparator comp, int val) {				
		return new QualificationRequirement(qualId, comp, val, null, null);		
	}	
		
}
