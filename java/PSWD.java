// Version 3.0.2
// Author: David Waring <dave@davidwaring.net>
// Information: http://pswd.davidwaring.net/info.html

// NOTE: These functions have been pulled from the PSWD Class of the PSWD Android App
// See the full Git repo for more details

package com.waring.pswd;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;

import org.spongycastle.util.encoders.Hex;


public class PSWD {




	// GENERATE PASSWORD

	/**
	 * Generate the site-specific password
	 * @param username User Name
	 * @param master_password Master Password
	 * @param token User Token
	 * @param domain site Domain
	 * @param length length of final password
	 * @param caps include uppercase letters
	 * @param symbols include symbol characters
	 * @param symchars the symbol characters to choose from
	 * @param hashes the number of times the password is hashed
	 * @return the final formatted site-specific password
	 */
	public static String generate(String username, String master_password, String token, String domain, int length, boolean caps, boolean symbols, String symchars, int hashes) {
		String key = domain + master_password + token;
		String password = key;

		// Hash the password 'hashes' number of times
		password = hash(password, hashes);

		// Trim to final length
		password = password.substring(0, length);

		// generate nums string from key
		// these digits are used to determine the location of symbols and uppercase letters
		// hash the key (and a different salt) 4 times to ensure more than enough digits are generated
		String nums = hash(key + "numbers1") + hash(key + "numbers2") + hash(key + "numbers3") + hash(key + "numbers4");
		nums = nums.replaceAll("[^\\d]", "");


		// an index of the number being used from nums
		int num_index = 0;


		// ADD SYMBOLS, if requested
		if ( symbols ) {

			// create a list of characters to work with
			char[] s = password.toCharArray();

			// get the number of symbols to add to the password
			// use the length of the password / the first digit
			int div = Integer.parseInt(Character.toString(nums.charAt(num_index)));
			num_index = num_index + 1;	// increase num index
			if ( div <= 3 ) {						// <= 3 creates too many symbols
				div = 4;									// divide by at least 4
			}
			int num_of_symbols = length / div;

			// loop to add each symbol
			for ( int i = 0; i < num_of_symbols; i++ ) {

				// get the location to add the symbols (two digits)
				int location = Integer.parseInt(Character.toString(nums.charAt(num_index))+Character.toString(nums.charAt(num_index+1)));
				location = location % length;
				num_index = num_index + 2;

				// get the symbol character location
				int symbol_location = Integer.parseInt(Character.toString(nums.charAt(num_index))+Character.toString(nums.charAt(num_index+1)));
				symbol_location = symbol_location % symchars.length();
				num_index = num_index + 2;

				// get the symbol character
				Character symbol = symchars.charAt(symbol_location);

				// Add the symbol to the password
				s[location] = symbol;
			}

			password = new String(s);
		}



		// ADD CAPS, if requested
		if ( caps ) {

			// create a list of characters to work with
			char[] c = password.toCharArray();

			// get the number of characters to capitalize in the password
			// use the length of the password / the next digit
			int div = Integer.parseInt(Character.toString(nums.charAt(num_index)));
			num_index = num_index + 1;
			if ( div <= 2 ) {
				div = 3;
			}
			int num_of_caps = length / div;

			// loop to add each cap
			for ( int i = 0; i < num_of_caps; i++ ) {

				// get the location to add the cap (two digits)
				int location = Integer.parseInt(Character.toString(nums.charAt(num_index))+Character.toString(nums.charAt(num_index+1)));
				location = location % length;
				num_index = num_index + 2;

				// make sure to capitalize at least the first location
				if ( i == 0 ) {
					while ( c[location] == Character.toUpperCase(c[location]) ) {
						location = (location+1) % length;
					}
				}

				// Capitalize the character at the location
				c[location] = Character.toUpperCase(c[location]);
			}

			password = new String(c);
		}


		return password;
	}






	// SHA-256 HASH //

	/**
	 * Hash the specified message using SHA-256
	 * @param msg the message to hash
	 * @param passes the number of times the hash is repeated
	 * @param listener the HashListener interface to monitor
	 *  the progress of long-running hashes
	 * @return the hashed String
	 */
	private static String hash(String msg) {
		return hash(msg, 1, null);
	}

	private static String hash(String msg, int passes) {
		return hash(msg, passes, null);
	}

	private static String hash(String msg, int passes, HashListener listener) {
		try {
			try {
				MessageDigest sha = MessageDigest.getInstance("SHA-256");
				byte[] bytes;

				// Monitor the progress of the loop
				int percent = 0;
				int prev_percent = 0;

				// Keep track of the amount of time each 1-percent interval
				// takes to complete - use the average to estimate the remaining time
				ArrayList<Long> deltas = new ArrayList<Long>();
				long time = new Date().getTime();	// the initial time

				// Loop 'passes' number of times
				for ( int i = 0; i < passes; i++ ) {

					// the current progress
					percent = (int) Math.floor(((double) i / passes)*100);

					// Update the progress using the listener, if supplied
					// Only update the progress in 1-percent intervals
					if ( null != listener && percent > prev_percent ) {

						// ESTIMATE TIME REMAINING

						// Get amount of time for the previous percent chunk
						long currentTime = new Date().getTime();
						long timeDelta = currentTime - time;
						time = currentTime;
						deltas.add(timeDelta);

						// Get the average time of the percent chunks
						long sum = 0;
						for (Long delta : deltas) {
							sum += delta;
						}
						long avgDelta =  sum / deltas.size();

						// Get the estimated remaining time
						int percentRemaining = 100 - percent;
						int timeRemaining = (int) avgDelta*percentRemaining;
						int sec = (timeRemaining/1000) % 60;
						int min = (timeRemaining/(1000*60)) % 60;

						// Format time remaining
						String format = "";
						if (sec != 0) {
							format = sec + " seconds";
						}
						if (min != 0) {
							format = min + " minutes " + format;
						}
						format = format + " remaining";

						// Update the listener
						listener.updateProgress(percent, format);

						prev_percent = percent;
					}

					// Perform the Hash
					sha.update(msg.getBytes("iso-8859-1"), 0, msg.length());
					bytes = sha.digest();

					// Convert the digest to a Hex-encoded String
					msg = new String(Hex.encode(bytes));
				}

			}
			catch (NoSuchAlgorithmException e) {}
		}
		catch (UnsupportedEncodingException e) {}

		return msg;
	}



	// Hashing Progress Listener

  /**
    * Interface for monitoring long-running hashes
    * @param progress the percent complete
    * @param remaining a formatted String of the estimated time remaining
    */
	public interface HashListener {
		public void updateProgress(int progress, String remaining);
	}
