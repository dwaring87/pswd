package com.waring.pswd;

import java.io.UnsupportedEncodingException;
import java.security.*;

public class PSWD {

	// DEFAULT FUNCTION VARS
	static final String def_domain = null;
	static final String def_password = null;
	static final int def_length = 14;
	static final boolean def_caps = true;
	static final boolean def_symbols = false;
	static final String def_symbolChars = "!@#$%^&*()_-+=<,>.?/";
	static final int def_passes = 250;

	// FUNCTION VARS
	String domain;
	String password;
	int length;
	boolean caps;
	boolean symbols;
	String symbolChars;
	int passes;




	/** convert a byte[] representation of a hash into
	 *  a hex string
	 * @param data the byte representation of a hash
	 * @return a string hex representation of a hash
	 */
	private static String convToHex(byte[] data) {
	  StringBuilder buf = new StringBuilder();
	  for (int i = 0; i < data.length; i++) {
      int halfbyte = (data[i] >>> 4) & 0x0F;
      int two_halfs = 0;
      do {
        if ((0 <= halfbyte) && (halfbyte <= 9))
          buf.append((char) ('0' + halfbyte));
        else
          buf.append((char) ('a' + (halfbyte - 10)));
        halfbyte = data[i] & 0x0F;
      } while(two_halfs++ < 1);
	  }
	  return buf.toString();
  }



	/** Generate Password Hash.
	 * @throws UnsupportedEncodingException
	 * @throws NoSuchAlgorithmException */
	private String generate(String domain, String password, int length, boolean caps, boolean symbols,
			String symbol_chars, int passes) throws UnsupportedEncodingException, NoSuchAlgorithmException {


		if (domain.equals(null) || domain.equals("")) {
			Toast toast = Toast.makeText(getApplicationContext(), "Please enter a domain", Toast.LENGTH_LONG);
			toast.show();
			return "Error";
		}
		if (password.equals(null) || password.equals("")) {
			Toast toast = Toast.makeText(getApplicationContext(), "Please enter a master password", Toast.LENGTH_LONG);
			toast.show();
			return "Error";
		}



		// generate hash key, hashed 'passes' number of times
		String key = domain + password;
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA1");
			for ( int i = 0; i < passes; i++ ) {
				byte[] sha1hash = new byte[40];
				sha1.update(key.getBytes("iso-8859-1"), 0, key.length());
				sha1hash = sha1.digest();
				key = convToHex(sha1hash);
			}
		}
		catch (NoSuchAlgorithmException e) {}



		// trim the key to the specified length
		String trimmed = key.substring(0, length);



		// generate a string of numbers, based on the hash key
		String temp = "";
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA1");
			byte[] sha1hash = new byte[40];
			key = key + "numbers";
			sha1.update(key.getBytes("iso-8859-1"), 0, key.length());
			sha1hash = sha1.digest();
			temp = convToHex(sha1hash);
		}
		catch (NoSuchAlgorithmException e) {}

		// strip letters from the string
		String nums = "";
		for ( int i = 0; i < temp.length(); i++ ) {
			if ( Character.isDigit(temp.charAt(i)) ) {
				nums = nums + temp.charAt(i);
			}
		}

		// lengthen the number of numbers
		nums = nums + nums + nums + nums + nums;
		int num_count = 0;



		// add symbols, if requested
		String symboled = "";

		if ( symbols == true ) {

			// generate a list to work with
			char[] chararray = trimmed.toCharArray();

			// get the number of symbols to add to the password
			int div = Integer.parseInt(Character.toString(nums.charAt(num_count)));
			num_count ++;

			if ( div < 4 ) {
				div = 4;
			}

			int num_of_symbols = (int) Math.floor(length / div);


			// loop to add symbols
			for ( int i = 0; i < num_of_symbols; i++ ) {

				// get the location of the symbol (two digits)
				String loc =  Character.toString(nums.charAt(num_count)) + Character.toString(nums.charAt(num_count+1));
				int location = Integer.parseInt(loc) % length;
				num_count = num_count + 2;

				// get the symbol to add (two digits)
				String symbol_loc = Character.toString(nums.charAt(num_count)) + Character.toString(nums.charAt(num_count+1));
				int symbol_location = Integer.parseInt(symbol_loc) % symbol_chars.length();
				char symbol = symbol_chars.charAt(symbol_location);
				num_count = num_count + 2;

				// add the symbol at the location
				chararray[location] = symbol;

			}

			// generate final 'symbolized' password string
			symboled = new String(chararray);

		}
		else {
			symboled = trimmed;
		}



		// add caps, if requested
		String capsed = "";

		if ( caps == true ) {

			// generate a list to work with
			char[] chararray = symboled.toCharArray();

			// get the number of characters to capitalize
			int div = Integer.parseInt(Character.toString(nums.charAt(num_count)));
			num_count ++;

			if ( div < 3 ) {
				div = 3;
			}

			int num_of_caps = (int) Math.floor(length / div);

			// loop to add caps
			for ( int i = 0; i < num_of_caps; i++ ) {

				// get the location to add the cap (two digits)
				String loc =  Character.toString(nums.charAt(num_count)) + Character.toString(nums.charAt(num_count+1));
				int location = Integer.parseInt(loc) % length;
				num_count = num_count + 2;

				// make sure to capitalize at least the first location
				if ( i == 0 ) {
					while ( Character.isLetter(chararray[location]) == false ) {
						location = ( location + 1 ) % length;
					}
				}

				// try to capitalize the char at the location
				chararray[location] = Character.toUpperCase(chararray[location]);

			}

			// join the list to a string
			capsed = new String(chararray);

		}
		else {
			capsed = symboled;
		}


		// Final Password
		String FINAL_PASSWORD = capsed;

		return FINAL_PASSWORD;

	}

}
