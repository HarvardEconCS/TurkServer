package edu.harvard.econcs.turkserver.mturk;

import com.amazonaws.mturk.requester.Comparator;
import com.amazonaws.mturk.requester.Locale;
import com.amazonaws.mturk.requester.QualificationRequirement;

public abstract class QualMaker {
			
	public static QualificationRequirement getMinApprovalRateQual(int approvalRate) {
									
		String qualificationTypeId = "000000000000000000L0";
		Comparator comparator = Comparator.GreaterThan;
		
		
		return new QualificationRequirement(qualificationTypeId , comparator, 
				approvalRate, null, null);
													
	}

	public static QualificationRequirement getLocaleQual(String locale) {
		String qualificationTypeId = "00000000000000000071";
		Comparator comparator = Comparator.EqualTo;
				
		return new QualificationRequirement(qualificationTypeId, comparator, 
				null, new Locale(locale), null);
									
	}

	public static QualificationRequirement getCustomQual(String qualId, Comparator comp, int val) {				
		return new QualificationRequirement(qualId, comp, val, null, null);		
	}	
		
}
