package edu.harvard.econcs.turkserver.mturk;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SignatureException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class AWSUtils {

	private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

	private static String TIMESTAMP_SEC_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	private static String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	private static TimeZone TIME_ZONE = TimeZone.getTimeZone("UTC");
	
	private static SimpleDateFormat DATE_SEC_FORMAT;
	private static SimpleDateFormat DATE_FORMAT;
	static {
		DATE_SEC_FORMAT = new SimpleDateFormat(TIMESTAMP_SEC_FORMAT);
		DATE_SEC_FORMAT.setTimeZone(TIME_ZONE);
		
		DATE_FORMAT = new SimpleDateFormat(TIMESTAMP_FORMAT);
		DATE_FORMAT.setTimeZone(TIME_ZONE);
	}

	/**
	 * @param serviceName The service name
	 * @param operation   The Operation name
	 * @param timestamp   Calendar timestamp.
	 * Use AWSDateFormatter.getCurrentTimeStampAsCalendar()
	 * @param key         The signing key
	 * @return The base64-encoded RFC 2104-compliant HMAC signature
	 * @throws java.security.SignatureException
	 *
	 */
	public static String generateSignature
	(String serviceName,
			String operation,
			String timestamp,
			String key)
	throws java.security.SignatureException {
		return generateSignature(serviceName + operation + timestamp, key);
	}

	/**
	 * Computes RFC 2104-compliant HMAC signature.
	 *
	 * @param data The data to be signed
	 * @param key  The signing key
	 * @return The base64-encoded RFC 2104-compliant HMAC signature
	 * @throws java.security.SignatureException
	 *
	 */
	public static String generateSignature(String data, String key)
	throws SignatureException {
		try {            
			Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
			mac.init(new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM));            
			return new String(Base64.encodeBase64(mac.doFinal(data.getBytes())));            
		} catch (Exception e) {
			throw new SignatureException("Failed to generate Signature : " + e.getMessage());
		}        
	}

	/**
	 * Gets current time in the calendar format
	 * @return current timestamp
	 * @throws ParseException
	 */
	public static Calendar getCurrentTimeStampAsCalendar()
	throws ParseException {
		Calendar ts = new GregorianCalendar();
		// TODO This is a bit convoluted, isn't it?
		ts.setTime(DATE_SEC_FORMAT.parse(convertDateToString(Calendar.getInstance())));
		return ts;
	}

	/**
	 * Parses a timestamp that no milliseconds
	 * @param timestamp
	 * @return
	 * @throws ParseException
	 */
	public static Date parseDateFromString(String timestamp) throws ParseException {
		return DATE_FORMAT.parse(timestamp);
	}
	
	/**
	 * Gets current time in the timestamp format
	 * @return String of current time in the timestamp format
	 */
	public static String getCurrentTimeStampAsString() {
		return convertDateToString(Calendar.getInstance());
	}

	/**
	 * Converts Calendar timestamp to String
	 * @param time
	 * @return Calendar time stamp
	 */
	public static String convertDateToString(Calendar time) {		
		return DATE_SEC_FORMAT.format(time.getTime());
	}

	/**
	 * Converts String timestamp to Calendar
	 * @param timestamp formatted string timestamp
	 * @return Calndar time stamp
	 * @throws java.text.ParseException
	 */
	public static Calendar convertStringToCalendar(String timestamp)
	throws ParseException {				
		Calendar ts = new GregorianCalendar();		
		ts.setTime(DATE_SEC_FORMAT.parse(timestamp));
		return ts;
	}

	public static String urlencode(String unencoded) {
		try {
			return URLEncoder.encode(unencoded, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// should never happen
			throw new RuntimeException("Could not url encode to UTF-8", e);
		}
	}

	public static XMLReader createXMLReader() {
		try {
			return XMLReaderFactory.createXMLReader();
		} catch (SAXException e) {
			// oops, lets try doing this (needed in 1.4)
			System.setProperty("org.xml.sax.driver", "org.apache.crimson.parser.XMLReaderImpl");
		}
		try {
			// try once more
			return XMLReaderFactory.createXMLReader();
		} catch (SAXException e) {
			throw new RuntimeException("Couldn't initialize a sax driver for the XMLReader");
		}
	}	
	
}