package edu.harvard.econcs.turkserver.mturk;

import edu.harvard.econcs.turkserver.schema.Session;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker.SessionSummary;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.mturk.requester.Assignment;
import com.amazonaws.mturk.requester.AssignmentStatus;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.requester.HITStatus;
import com.amazonaws.mturk.requester.Price;
import com.amazonaws.mturk.requester.QualificationRequirement;
import com.amazonaws.mturk.service.exception.InvalidStateException;
import com.amazonaws.mturk.service.exception.ServiceException;
import com.google.inject.Inject;

/**
 * A hacked together class to create HITs and expire leftovers when the end
 * has been reached.
 * 
 * posts gradually to keep us at the top of the list.
 * 
 * TODO needs a lot of work.
 * 
 * @author mao
 *
 */
public class TurkHITController implements HITController {

	private static final int HIT_SLEEP_MILLIS = 200;
	private static final int SERVICE_UNAVAILABLE_MILLIS = 5000;

	private static final CreateTask POISON_PILL = new CreateTask(0,0,0,0,0,0);
	
	protected final Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());
	
	private final RequesterServiceExt requester;
	private final ExperimentDataTracker tracker;
	
	private String hitTypeId;
	private String title;
	
	private String externalURL;
	private int frameHeight;
	private int lifeTime;
			
	private final BlockingQueue<CreateTask> jobs;
	private volatile boolean expireFlag;
	
	private long lastHitCreation = 0;
	
	/**
	 * 
	 * @param req
	 * @param tracker
	 * @param initialAmount the amount of hits to initially create
	 * @param additionalDelay the amount to wait before each additional hit
	 * @param totalAmount
	 */
	@Inject
	public TurkHITController(
			RequesterServiceExt req, 
			ExperimentDataTracker tracker
			) {
		this.requester = req;
		this.tracker = tracker;
				
		jobs = new LinkedBlockingQueue<CreateTask>();
		expireFlag = false;
	}

	/**
	 * Sets the HIT type that this manager will use
	 * @param title
	 * @param description
	 * @param keywords
	 * @param rewardAmountInUSD
	 * @param assignmentDurationInSeconds
	 * @param autoApprovalDelayInSeconds
	 */
	@Override
	public void setHITType(
			String title, 
			String description,
			String keywords,
			double reward,
			long assignmentDurationInSeconds,
			long autoApprovalDelayInSeconds,			
			QualificationRequirement[] qualRequirements) {
		try {					
			hitTypeId = requester.registerHITType(
					autoApprovalDelayInSeconds, assignmentDurationInSeconds, 
					reward, title, keywords, description, qualRequirements);
			this.title = title;
			
			logger.info("Got HIT Type: {}, title is {}", hitTypeId, title);
			
		} catch (ServiceException e) {			
			e.printStackTrace();
		}		
	}
	
	/**
	 * Sets the parameters for external question
	 * @param url
	 * @param frameHeight
	 * @param lifetime
	 */
	@Override
	public void setExternalParams(String url, int frameHeight, int lifetime) {
		this.externalURL = url;
		this.frameHeight = frameHeight;
		this.lifeTime = lifetime;
	}		
	
	@Override
	public void postBatchHITs(int target, int minOverhead, int maxOverhead, int minDelay, int maxDelay, double pctOverhead) {
		jobs.offer(new CreateTask(target, minOverhead, maxOverhead, minDelay, maxDelay, pctOverhead));
	}
	
	static class CreateTask {
		final int target, minOverhead, maxOverhead, minDelay, maxDelay;
		final double pctOverhead;
		public CreateTask(int target, int minOverhead, int maxOverhead, 
				int minDelay, int maxDelay, double pctOverhead) {
			this.target = target;
			this.minOverhead = minOverhead;
			this.maxOverhead = maxOverhead;
			this.minDelay = minDelay;
			this.maxDelay = maxDelay;
			this.pctOverhead = pctOverhead;
		}	
	}

	@Override
	public void disableAndShutdown() {
		expireFlag = true;
		jobs.offer(POISON_PILL);		
	}

	@Override
	public void run() {
		Thread.currentThread().setName(this.getClass().getSimpleName());			
		
		while( !expireFlag ) {
			CreateTask job = null;
			try { job = jobs.take(); }
			catch (InterruptedException e) { e.printStackTrace(); }			
			if( job == POISON_PILL ) continue;						
			
			logger.info("Starting HIT creation: max overhead {}, min delay {}", job.maxOverhead, job.minDelay);			
			
			int maxHITs = getAdaptiveTarget(job.target, job);
					
			SessionSummary summary;
			
			// Create HITs until our limit is reached, or expire			
			do {																		
				logger.debug("Sleeping for " + job.minDelay);
				
				try { Thread.sleep(job.minDelay); } 
				catch (InterruptedException e) { e.printStackTrace();	}
				
				// Quit if expiration was reached while sleeping
				if( expireFlag ) break;
				
//				if( System.currentTimeMillis() - lastHitCreation > job.maxDelay ) {
					/*
					 * Delete a HIT from DB and disable it
					 * TODO this causes payment problems with HITs that are accepted even after disabling
					 * (i.e. during quiz)
					 */
//					Session unused = tracker.deleteUnusedSession();
//					if( unused == null ) {
//						logger.warn("Could not find any unused HITs to delete for reposting");
//					}
//					else {
//						logger.info("Disabling {} to repost another HIT", unused.getHitId());
//						requester.safeDisableHIT(unused.getHitId());
//					}
//				}
				
				summary = tracker.getSetSessionSummary();				
				int target = getAdaptiveTarget(summary.assignedHITs, job);
				
				// Skip creating HIT if we are above the target and have created a hit recently
				if( summary.createdHITs >= target && 
						System.currentTimeMillis() <= lastHitCreation + job.maxDelay ) 
					continue;
							
				try {
					HIT resp = requester.createHITExternalFromID(
								hitTypeId, title, externalURL, frameHeight, String.valueOf(lifeTime));

					String hitId = resp.getHITId();
					tracker.saveHITId(hitId);
					lastHitCreation = System.currentTimeMillis();
					
					logger.info(String.format("Created %d HITs", summary.createdHITs + 1));
				} catch (ServiceException e) {					
					e.printStackTrace();					

					logger.info("Got error; Throttling HIT creating");
					// Throttle it a bit
					try { Thread.sleep(SERVICE_UNAVAILABLE_MILLIS); } 
					catch (InterruptedException e1) { e1.printStackTrace();	}				
				}							
			} while (summary.createdHITs < maxHITs);		
		}								
		
		// Wait around until server tells us to expire HITs
		while( !expireFlag ) {
			try { Thread.sleep(2000); } 
			catch (InterruptedException e) { e.printStackTrace(); }
		}
		
		logger.info("Disabling leftover hits");
		List<Session> unusedHITs = tracker.expireUnusedSessions();
		
		for( Session session : unusedHITs ) {
			String hitId = session.getHitId();									
			requester.safeDisableHIT(hitId);			
			
			try { Thread.sleep(HIT_SLEEP_MILLIS); } 
			catch (InterruptedException e1) { e1.printStackTrace();	}
		}
		
		logger.info("Turk HIT posting thread finished");
	}

	static int getAdaptiveTarget(int target, CreateTask nextJob) {
		int adjusted = (int) Math.round(target * (1 + nextJob.pctOverhead));
		adjusted = Math.max(adjusted, target + nextJob.minOverhead);
		adjusted = Math.min(adjusted, target + nextJob.maxOverhead);
		return adjusted;
	}

	@Override
	public void disableHIT(String hitId) {
		requester.safeDisableHIT(hitId);		
	}

	public Integer disableUnassignedAndRemoveFromDB() {
		HIT[] hits = requester.searchAllHITs();
		
		int deleted = 0;
		logger.info("{} total HITs found", hits.length);

		for (HIT hit : hits ) {
			
			System.out.printf("HIT %s has status %s (Pending:%d, Available:%d, Completed:%d)\n",
					hit.getHITId(), hit.getHITStatus().toString(),
					hit.getNumberOfAssignmentsPending(), hit.getNumberOfAssignmentsAvailable(), 
					hit.getNumberOfAssignmentsCompleted());

			HITStatus hitStatus = hit.getHITStatus();

			// TODO we're assuming here that each HIT just has one assignment
			if( hit.getNumberOfAssignmentsAvailable() == 1) {					
				if( !HITStatus.Assignable.equals(hitStatus) ) {
					logger.info("Hit has 1 assignment but not assignable: " + hitStatus);
					continue;
				}
				
				logger.info("HIT " + hit + " is available, will be disabled");					
							
				requester.safeDisableHIT(hit.getHITId());
				tracker.deleteSession(hit.getHITId());
				deleted++;								
			}
			else if (HITStatus.Unassignable.equals(hitStatus) && hit.getNumberOfAssignmentsPending() == 1) {
				logger.info("HIT " + hit + " is being held, skipping");
			}
		}
		
		logger.info("Disabled " + deleted + " hits");	
		return deleted;				
	}

	/**
	 * Disables all hits with no experiment or result in the current set
	 * Synchronous method.
	 * @return
	 */
	public int disableUnusedFromDB() {
		List<Session> unassigned = tracker.expireUnusedSessions();
						
		System.out.println(unassigned.size() + " unused HITs found in this set and deleted.");
		
		int deleted = 0;
		
		for( Session s : unassigned ) {
			String hitId = s.getHitId();
			if (requester.safeDisableHIT(hitId))
				deleted++;

			// Would delete from the DB if it wasn't already done above						
//			qr.update("DELETE FROM session where hitId=? AND results IS NULL", hitId);
		}
		
		return deleted;
	}

	/**
	 * Pays workers their base wage and bonus according to what is recorded in the DB.
	 * Synchronous method.
	 * @return
	 */
	public double reviewAndPayWorkers(PaymentPolicy policy) {
		int approved = 0;
		int rejected = 0;
		int skipped = 0;
		int bonused = 0;
		int alreadypaid = 0;
		int unreviewable = 0;
		
		double amountPaid = 0;
		
		/* Get a list of workers that have been assigned to experiments, 
		 * finished, but not yet paid
		 */
		
		List<Session> completedSessions = tracker.getCompletedSessions();
		
		logger.info(completedSessions.size() + " workers found in set");
		
		for( Session s : completedSessions ) {		
			if( s.getPaid() && (s.getBonus() == null || s.getBonusPaid()) ) continue;
			
			String hitId = s.getHitId();
			String assignmentId = s.getAssignmentId();			
			String workerId = s.getWorkerId();
						
			while(true) {
				try {						
					HIT hit = requester.getHIT(hitId);
					// There should only be one assignment
					Assignment[] assts = requester.getAllAssignmentsForHIT(hitId);	

					if( assts.length < 1 ) {
						logger.info("No assignments for HIT {} (probably did not complete submission)", hitId);
						unreviewable++;
						break;
					} else if (assts.length > 1) {
						// This indicates a serious problem
						throw new RuntimeException("Too many assignments for HIT: " +  hitId);
					}

					HITStatus status = hit.getHITStatus();

					if( HITStatus.Reviewable.equals(status) || HITStatus.Disposed.equals(status) ) {							
						// How much are we paying this hack?
						BigDecimal reward = hit.getReward().getAmount();

						// Save the stuff that the worker submitted
						logger.info("HIT {} is {}, checking the result", hitId, status);						

						Assignment a = assts[0];
						String submittedAsstId = a.getAssignmentId();
						String submittedWorkerId = a.getWorkerId();

						// Fill in worker Id if its null or wrong							
						if( workerId == null || !workerId.equals(submittedWorkerId) ) {
							logger.warn("Found incorrect worker {} when actual worker was {}", workerId, submittedWorkerId);
							s.setWorkerId(submittedWorkerId);						
							tracker.saveSession(s);
							workerId = submittedWorkerId;
						}

						// Correct assignment Id if it is wrong
						if( !assignmentId.equals(submittedAsstId) ) {
							logger.warn("AssignmentId for submitted did not match db, correcting HIT " + hitId);
							// Should stop here and figure out what is going on
							s.setAssignmentId(submittedAsstId);
							tracker.saveSession(s);
							assignmentId = submittedAsstId;
						}																				

						if( HITStatus.Disposed.equals(status) ) {
							// TODO if it's disposed, they should have been paid, right?
							logger.info("Skipping payment for previously disposed HIT " + hitId);
							skipped++;
							break;
						}					

						logger.info("Assignment status: " + a.getAssignmentStatus());
						
						if( AssignmentStatus.Submitted.equals(a.getAssignmentStatus()) ) {
												
							if ( policy.shouldPayBaseReward(s) ) {
								// Approve and pay assignment
								requester.approveAssignment(assignmentId, policy.getLastAssignmentFeedback());
								
								// Save in database that we paid them
								s.setPayment(reward);		
								s.setPaid(true);
								
								amountPaid += reward.doubleValue();								
								approved++;
							}
							else {
								requester.rejectAssignment(assignmentId, policy.getLastAssignmentFeedback());
								
								s.setPayment(BigDecimal.ZERO);
								s.setPaid(false);								
								
								rejected++;
							}
							
							tracker.saveSession(s);
						}
						else if( AssignmentStatus.Approved.equals(a.getAssignmentStatus()) ){
							logger.warn("HIT {} was already approved, saving base payment", hitId);
														
							s.setPayment(hit.getReward().getAmount());
							s.setPaid(true);
							tracker.saveSession(s);														
							
							alreadypaid++;
						}
						else {							
							logger.warn("HIT {} was previously rejected ({})", hitId, a.getAssignmentStatus());
							skipped++;
						}
						
						// Pay bonus even for auto-approved or disabled HITs if possible
										
						if( s.getBonus() != null && !s.getBonusPaid() ) {							
							policy.checkAndAdjustBonus(s, 
									tracker.getExperiment(s.getExperimentId()), 
									tracker.getExperimentRounds(s.getExperimentId()));
							
							BigDecimal bonus = s.getBonus();
							
							if( bonus.doubleValue() > 0d ) {
								requester.grantBonus(workerId, bonus.doubleValue(), assignmentId, 
										policy.getLastBonusFeedback());
								
								s.setBonusPaid(true);								
								
								amountPaid += reward.doubleValue();
								
								bonused++;	
							}							
							
							tracker.saveSession(s);
						}

					} else {							
						logger.info("HIT {} has status {}, skipping", hitId, hit.getHITStatus().toString());							
						unreviewable++;
					}

					// break out of loop
				}
				catch (InvalidStateException e) {
					// To be fixed if we observe any other of these
					e.printStackTrace();
				}
				catch (ServiceException e) {					
					e.printStackTrace();

					System.out.println("Throttling");
					// Throttle it a bit
					try { Thread.sleep(5000); } 
					catch (InterruptedException e1) { e1.printStackTrace();	}	
					continue;
				}
				
				break;
			}
		}
		
		System.out.println("Total approved: " + approved);
		System.out.println("Total already paid or expired: " + alreadypaid);
		System.out.println("Total rejected: " + rejected);
		System.out.println("Total bonuses paid: " + bonused);
		System.out.println("Total skipped: " + skipped);
		System.out.println("Total unreviewable:" + unreviewable);
		System.out.println("Total amount paid:" + amountPaid);
		
		// TODO return something more meaningful
		return amountPaid;
	}

	/**
	 * Double-checks and disposes of paid HITs.
	 * Synchronous method.
	 * @return
	 */
	public int checkAndDispose() {
		int disposed = 0;
		int skipped = 0;		
		
		/* Get a list of workers that have been assigned to experiments, 
		 * finished, but not yet paid
		 * 
		 * TODO right now not disposed <==> hitStatus is null
		 */
		List<Session> completed = tracker.getCompletedSessions();
		
		logger.info("Checking out {} completed sessions", completed.size());		

		for( Session s : completed ) {
//			"SELECT hitId, assignmentId, workerId FROM session " +
//			"WHERE paid IS NOT NULL AND hitStatus IS NULL"
			
			// Ignore unpaid or already disposed HITs
			if( s.getPaid() == false || s.getHitStatus() != null ) continue;
			
			String hitId = s.getHitId();
			String assignmentId = s.getAssignmentId();
			
			while(true) {			 										
				try {						
					HIT hit = requester.getHIT(hitId);						
					Price reward = hit.getReward();

					Assignment[] assts = requester.getAllAssignmentsForHIT(hitId);											

					// check for stupid shit
					if( assts.length == 0 ) {
						System.out.println("No assignments on " + hitId);
						break;
					} else if (assts.length > 1) {
						throw new RuntimeException("Multiple assignments on this " + hitId);
					}

					Assignment a = assts[0];
					if( !a.getAssignmentId().equals(assignmentId) )
						throw new RuntimeException("Assignment ID does not match on " + hitId);

					// Check that the pay amount is right						
					BigDecimal paidReward = s.getPayment();

					if( AssignmentStatus.Approved.equals(a.getAssignmentStatus())) {
						if ( !reward.getAmount().equals(paidReward) ) {
							logger.error("Paid reward: " + paidReward);
							logger.error("Recorded reward: " + reward.getAmount());
							throw new RuntimeException("amount we recorded as paid is different from actual amount!");
						}
					}
					else if ( AssignmentStatus.Rejected.equals(a.getAssignmentStatus()) ) {
						if ( paidReward.doubleValue() != 0d )
							throw new RuntimeException("recorded a nonzero amount for a rejected HIT!");
					}
					else {
						throw new RuntimeException("Unexpected status for assignment recorded as paid: " 
								+ a.getAssignmentStatus());
					}							
					
					if( HITStatus.Reviewable.equals(hit.getHITStatus()) ) {
						// We got the right pay and the right notify, safe to dispose							
						requester.disposeHIT(hitId);						

						logger.info("Disposed " + hitId);
						
						s.setHitStatus("Disposed");
						tracker.saveSession(s);
												
						disposed++;
					} else if( HITStatus.Disposed.equals(hit.getHITStatus()) ) {
						logger.info(hitId + "already disposed");						
						
						skipped++;
					} else {
						throw new RuntimeException("Unrecognized status " + hit.getHITStatus());
					}

				} catch (ServiceException e) {					
					e.printStackTrace();

					System.out.println("Throttling");
					// Throttle it a bit
					try { Thread.sleep(5000); } 
					catch (InterruptedException e1) { e1.printStackTrace();	}	
					continue;
				}

				break;
			}	
		}
		
		System.out.println("Total disposed: " + disposed);
		System.out.println("Total skipped:" + skipped);
		
		return disposed;
	}

}
