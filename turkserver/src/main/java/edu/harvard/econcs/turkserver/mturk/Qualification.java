package edu.harvard.econcs.turkserver.mturk;

public abstract class Qualification {
	
	public enum QualComparator {
		LessThan ("LessThan"),
		LessThanOrEqualTo ("LessThanOrEqualTo"),
		GreaterThan ("GreaterThan"),
		GreaterThanOrEqualTo ("GreaterThanOrEqualTo"),
		EqualTo ("EqualTo"),
		NotEqualTo ("NotEqualTo"),
		Exists ("Exists");
		
		final String str; 
		
		private QualComparator(String str) {
			this.str = str;
		}
	};
	
	public abstract void appendQualString(StringBuffer qualsb, int i);
	
	public static Qualification getMinApprovalRateQual(final int approvalRate) {
		return new Qualification() {
			@Override
			public void appendQualString(StringBuffer qualsb, int i) {
				// Approval greater than some number
				qualsb.append("&QualificationRequirement." + i + ".QualificationTypeId=000000000000000000L0");				
				qualsb.append("&QualificationRequirement." + i + ".Comparator=GreaterThan");
				qualsb.append("&QualificationRequirement." + i + ".IntegerValue=" + approvalRate);				
			}			
		};				
	}

	public static Qualification getLocaleQual(final String locale) {
		return new Qualification() {
			@Override
			public void appendQualString(StringBuffer qualsb, int i) {
				
				qualsb.append("&QualificationRequirement." + i + ".QualificationTypeId=00000000000000000071");
				qualsb.append("&QualificationRequirement." + i + ".Comparator=EqualTo");
				
				qualsb.append("&QualificationRequirement." + i + ".LocaleValue.Country=")
				.append(locale);
			}			
		};					
	}

	public static Qualification getCustomQual(final String qualId, final QualComparator comp, final int val) {
		return new Qualification() {
			@Override
			public void appendQualString(StringBuffer qualsb, int i) {
				qualsb
				.append("&QualificationRequirement." + i + ".QualificationTypeId=")
				.append(AWSUtils.urlencode(qualId));
				
				qualsb.append("&QualificationRequirement." + i + ".Comparator=").append(comp.str);
				
				if( !comp.equals(QualComparator.Exists) ) // Exists doesn't use an integer value
					qualsb.append("&QualificationRequirement." + i + ".IntegerValue=").append(val);
			}			
		};
	}	
		
}
