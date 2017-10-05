package com.amfam.billing.acquirer.util;

import java.util.ArrayList;

import com.amfam.billing.acquirer.SaratogaAuthResponse;
import com.amfam.billing.acquirer.SaratogaMessageParser;

/**
 * Scrubs any potential credit card number in a given string
 * 
 */
public class CleanseCreditCardNumber {

	private static String ccRegex = "^((4\\d{3})|(5[1-5]\\d{2})|(6011)|(7\\d{3}))-?\\d{4}-?\\d{4}-?\\d{4}|3[4,7]\\d{13}$";

	/**
	 * @param val
	 * @param replacements
	 * @return
	 */
	public static String replaceCcNumber(String val, String replacement) {
		if (replacement != null) {
			if (replacement.length() == 15) {
				val = val.replaceAll(replacement, "XXXXXXXXXXXXXXX");
			}
			if (replacement.length() == 16) {
				val = val.replaceAll(replacement, "XXXXXXXXXXXXXXXX");
			}
		}
		return val;
	}

	/**
	 * finds all potential credit card number in a given string
	 * 
	 * @param val
	 * @return
	 */
	public static ArrayList<String> findCCNumbers(String val) {
		ArrayList<String> matches = new ArrayList<String>();
		try {
			for (int i = 0; i < val.length(); i++) {
				String tt = val.substring(i, i + 16);
				if (tt.matches(ccRegex)) {
					if (!hasCCLeadingNumber(tt)) {
						continue;
					}

					String test = "";
					if (has16(tt)) {
						test = tt;
					} else {
						test = tt.substring(0, tt.length() - 1);
					}
					if (Luhn.Check(test)) {
						matches.add(test);
					}

				}
			}
		} catch (StringIndexOutOfBoundsException e) {
			// System.out.println("done");
		}
		return matches;
	}

	private static boolean hasCCLeadingNumber(String tt) {
		char c = tt.charAt(0);
		if (c == '3' || c == '4' || c == '5' || c == '6') {
			return true;
		} else {
			return false;
		}
	}

	private static boolean has16(String tt) {
		char c = tt.charAt(0);
		if (c == '3') {
			return false;
		} else {
			return true;
		}
	}

	public static String findCCNumber(String message) {
		SaratogaMessageParser parser = new SaratogaMessageParser();
		SaratogaAuthResponse res = parser.parse(message);
		/*
		 * CC number is in datafield #2 
		 */
		return (String) res.getDatafields().get(new Integer(2));
	}
}
