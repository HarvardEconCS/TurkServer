package edu.harvard.econcs.turkserver.server;

import java.util.List;
import java.util.Set;

import edu.harvard.econcs.turkserver.schema.Experiment;

/**
 * Simple assignment maker which equalizes the number of treatments assigned
 * and also re-assigns the same assignment to an existing experiment
 * 
 * @author mao
 *
 */
public class Assigner {

	Set<String> assignments;
		
	Assigner(Set<String> assignments, List<Experiment> existing) {
		this.assignments = assignments;
		
		// TODO initialize counts for each assignment, using a heap
		if( existing != null ) {
			
		}
	}
	
	Assigner(Set<String> assignments) {
		this(assignments, null);				
	}

	/**
	 * Get an assignment for an experiment
	 * 
	 * @param exp can be null, or contain prior data
	 * @return
	 */
	String getAssignment(Experiment exp) {
		if( exp != null  ) {
			String prevInput = exp.getInputdata();
			if( prevInput != null && assignments.contains(prevInput) ) return prevInput;
		}
		
		// TODO choose the lowest
		return null;		
	}
	
	/**
	 * Increment the completed assignment record 
	 * @param assignment
	 */
	void completeAssignment(String assignment) {
		
	}
	
}
