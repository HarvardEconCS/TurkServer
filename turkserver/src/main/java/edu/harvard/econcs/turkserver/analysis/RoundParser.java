package edu.harvard.econcs.turkserver.analysis;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.regex.Pattern;

import edu.harvard.econcs.turkserver.schema.Round;

public class RoundParser {
	
	static DateFormat timeFormat;	
	{
		timeFormat = new SimpleDateFormat("mm:ss.SSS");
		timeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	static final Pattern startPat = Pattern.compile("Round (\\d+) started");	
	static final Pattern finishPat = Pattern.compile("Round (\\d+) finished");
	
	Round round;
	
	public RoundParser(Round round) {
		this.round = round;
	}
	
	public void parse(RoundParseTarget target) throws ParseException {
		try(Scanner sc = new Scanner(round.getResults())) {

			String endPat = null;
			long currentTime = 0;
			int currentLine = 1;
			int round = 0;
			
			// Consume start token
			currentTime = parseTime(sc.next());							
			
			try {
				sc.findInLine(startPat);
			}
			catch( Exception e ) {
				throw new ParseException("Round start expected but got: " + sc.nextLine(), currentLine);					
			}									

			round = Integer.parseInt(sc.match().group(1));
			target.roundStart(round);
			
			while( sc.hasNextLine() ) {				
				currentTime = parseTime(sc.next());
				currentLine++;

				// Try Consume end token
				endPat = sc.findInLine(finishPat);

				if( endPat == null ) {
					target.roundData(currentTime, sc.nextLine().substring(1));
				}
				else break;
			}

			if( endPat == null )
				throw new ParseException("Round end expected", currentLine);

			if (round != Integer.parseInt(sc.match().group(1)))
				throw new ParseException("Round end number didn't match start", currentLine);

			target.roundEnd(currentTime);
		}
	}

	long parseTime(String next) throws ParseException {
		return timeFormat.parse(next).getTime(); 
	}
}
