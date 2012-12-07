package edu.harvard.econcs.turkserver.server;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.andrewmao.math.RandomSelection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import edu.harvard.econcs.turkserver.schema.Experiment;

/**
 * Simple assignment maker which equalizes the number of treatments assigned
 * and also re-assigns the same assignment to an existing experiment
 * 
 * @author mao
 *
 */
@Singleton
public class Assigner {

	Set<String> assignments;
		
	@Inject
	Assigner(
			@Named(TSConfig.INPUT_LIST)
			Set<String> assignments,
			@Nullable
			List<Experiment> existing) {
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
		return RandomSelection.selectRandom(assignments);		
	}
	
	/**
	 * Increment the completed assignment record 
	 * @param assignment
	 */
	void completeAssignment(String assignment) {
		
	}
	
}
