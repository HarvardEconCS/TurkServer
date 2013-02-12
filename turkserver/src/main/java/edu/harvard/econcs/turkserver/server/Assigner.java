package edu.harvard.econcs.turkserver.server;

public interface Assigner {
	
	/**
	 * Get an item assigned without a specific user. Usually used for groups.
	 * @return
	 */
	String getAssignment();
	
	/**
	 * Get an item to be assigned to a specific user. 
	 * @param user
	 * @param prevItem
	 * @return
	 */
	String getAssignment(String user, String prevItem);

	void completeAssignment(String item);
	
	void completeAssignment(String user, String item);	
}
