package com.waring.pswd;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.spongycastle.util.encoders.Hex;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Base64;


public class PSWD {
	
	
	// A Base64 Encoded String for the AES Encryption Key (256 bits)
	private static final String ENCRYPT_KEY = "YOUR_PRIVATE_KEY";
	
	
	/**
	 * Check to see if the specified User Name has a cached User Token in SharedPreferences
	 * @param context the Context of the Activity making the call
	 * @param username the User Name of the User
	 * @return true if the User has a cached User Token
	 */
	public static boolean hasUserToken(Context context, String username) {
		String token = getUserToken(context, username);
		
		if ( token.equals("") ) {
			return false;
		}
		else {
			return true;
		}
	}
	
	
	/**
	 * Get the User Token of the specified User from SharedPreferences
	 * @param context the Context of the Activity making the call
	 * @param username the User Name of the User
	 * @return a String of the plain-text User Token
	 */
	public static String getUserToken(Context context, String username) {
		// Get the User ID
		String userid = hash(username);
		
		// Get a saved token, if present
		// Return an empty String if no saved User Token is present
		SharedPreferences sharedPref = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
		String token = sharedPref.getString(userid, "");
		
		// Return the plain-text value of the User Token
		return PSWD.decrypt(token);
	}
	
	
	/**
	 * Save the specified User Token for the given User
	 * The tokens are stored in the SharedPreferences of the app (PSWD_PREFS.xml)
	 * The key is the User ID (SHA256 hash of the User Name)
	 * The value is the AES encrypted & base 64 encoded value of the User Token 
	 * @param context the Context of the Activity making the call
	 * @param username the User Name of the User
	 * @param token the User Token of the User
	 */
	public static void saveUserToken(Context context, String username, String token) {
		SharedPreferences sharedPref = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
		Editor edit = sharedPref.edit();
		
		// Key = User ID
		// Value = Encrypted User Token
		edit.putString(PSWD.getUserId(username), PSWD.encrypt(token));
		
		edit.commit();
	}
	
	
	/**
	 * Remove the stored User Token from the SharedPreferences
	 * @param context the Context of the Activity making the call
	 * @param username the User Name of the User
	 */
	public static void eraseUserToken(Context context, String username) {
		SharedPreferences sharedPref = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
		Editor edit = sharedPref.edit();
		edit.remove(PSWD.getUserId(username));
    	edit.commit();
	}
	
	
	/**
	 * Generate the User Token for the specified User Name / Master Password pair
	 * The User Token is a SHA256 hash of the User Name and Master Password that is looped 10^7 times
	 * This is designed to take a significant amount of time.  Progress of the hashing is monitored
	 * using the HashListener interface (provides percent complete and estimated time remaining)
	 * @param username the User Name of the User
	 * @param password the Master Password of the User
	 * @param listener the HashListener used to monitor progress
	 * @return the generated User Token (plain-text String)
	 */
	public static String generateUserToken(String username, String password, HashListener listener) {
		String token = username + password;
		return hash(token, MainActivity.DEFAULT_K1, listener);
	}
	
	
	/**
	 * Get the User ID for the specified User Name
	 * This is a single SHA-256 hash of the User Name
	 * @param username the User Name of the User
	 * @return the User ID of the User
	 */
	public static String getUserId(String username) {
		return hash(username);
	}
	
	
	
	
	
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
			if ( div <= 3 ) {			// <= 3 creates too many symbols
				div = 4;				// divide by at least 4
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
	
	
	

	
	
	// AES ENCRYPTION / DECRYPTION //
	
	/**
	 * Encrypt the plain-text message using AES
	 * The output is Base64 encoded
	 * @param plainMessage the message to encrypt
	 * @return a Base64 encoded String of the encrypted message
	 */
	protected static String encrypt(final String plainMessage) {
		if ( plainMessage.equals("") ) {
			return plainMessage;
		}

        try {
        	final byte[] symKeyData = Arrays.copyOfRange(Base64.decode(PSWD.ENCRYPT_KEY, Base64.DEFAULT), 0, 32);	// Ensure 256 bits / 32 bytes
            final byte[] encodedMessage = plainMessage.getBytes(Charset.forName("UTF-8"));
        	
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            final int blockSize = cipher.getBlockSize();

            // create the key
            final SecretKeySpec symKey = new SecretKeySpec(symKeyData, "AES");

            // generate random IV using block size (possibly create a method for this)
            final byte[] ivData = new byte[blockSize];
            final SecureRandom rnd = SecureRandom.getInstance("SHA1PRNG");
            rnd.nextBytes(ivData);
            final IvParameterSpec iv = new IvParameterSpec(ivData);
            
            cipher.init(Cipher.ENCRYPT_MODE, symKey, iv);
            
            final byte[] encryptedMessage = cipher.doFinal(encodedMessage);

            // concatenate IV and encrypted message
            final byte[] ivAndEncryptedMessage = new byte[ivData.length + encryptedMessage.length];
            System.arraycopy(ivData, 0, ivAndEncryptedMessage, 0, blockSize);
            System.arraycopy(encryptedMessage, 0, ivAndEncryptedMessage, blockSize, encryptedMessage.length);
            
            final String ivAndEncryptedMessageBase64 = Base64.encodeToString(ivAndEncryptedMessage, Base64.DEFAULT);
            
            return ivAndEncryptedMessageBase64;
        } 
        catch (Exception e) {
        	e.printStackTrace();
        	return plainMessage;
        }
    }

	/**
	 * Decrypt the Base64 encoded String
	 * @param ivAndEncryptedMessageBase64 the Base64 encoded encrypted String.
	 * @return the plain-text message
	 */
    protected static String decrypt(final String ivAndEncryptedMessageBase64) {
		if ( ivAndEncryptedMessageBase64.equals("") ) {
			return ivAndEncryptedMessageBase64;
		}
    	
        try {
        	final byte[] symKeyData = Arrays.copyOfRange(Base64.decode(PSWD.ENCRYPT_KEY, Base64.DEFAULT), 0, 32);	// Ensure 256 bits / 32 bytes
            final byte[] ivAndEncryptedMessage = Base64.decode(ivAndEncryptedMessageBase64, Base64.DEFAULT);
        	
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            final int blockSize = cipher.getBlockSize();

            // create the key
            final SecretKeySpec symKey = new SecretKeySpec(symKeyData, "AES");

            // retrieve random IV from start of the received message
            final byte[] ivData = new byte[blockSize];
            System.arraycopy(ivAndEncryptedMessage, 0, ivData, 0, blockSize);
            final IvParameterSpec iv = new IvParameterSpec(ivData);

            // retrieve the encrypted message itself
            final byte[] encryptedMessage = new byte[ivAndEncryptedMessage.length - blockSize];
            System.arraycopy(ivAndEncryptedMessage, blockSize, encryptedMessage, 0, encryptedMessage.length);
            
            cipher.init(Cipher.DECRYPT_MODE, symKey, iv);
            final byte[] encodedMessage = cipher.doFinal(encryptedMessage);
            final String message = new String(encodedMessage, Charset.forName("UTF-8"));

            return message;
        } 
        catch (Exception e) {
        	e.printStackTrace();
        	return ivAndEncryptedMessageBase64;
        }
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
 }
