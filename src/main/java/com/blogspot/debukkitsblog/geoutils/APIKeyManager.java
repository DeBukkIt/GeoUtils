package com.blogspot.debukkitsblog.geoutils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Loads API keys from a text file
 * 
 * @author DeBukkIt
 *
 */
public class APIKeyManager {

	private static Map<String, String> apiKeys;

	/**
	 * Returns the API key belonging to a given serviceID or null if the serviceID
	 * is not in the API-Keys.txt file, or the key is not in there or something went
	 * wrong loading the file.
	 * 
	 * @param serviceID
	 *            The serviceID
	 * @return the API key belonging to the serviceID
	 */
	public static String getAPIKey(String serviceID) {
		if (apiKeys == null) {
			init();
		}
		return apiKeys.get(serviceID);
	}

	/**
	 * Checks whether the API-Keys.txt file contains an API key for the given
	 * serviceID
	 * 
	 * @param serviceID
	 *            The serviceID
	 * @return true if the API-Keys.txt file contains a line starting with
	 *         <i>serviceID</i> followed by a space character and at least one more
	 *         non-space character (probably an API key) and the line does not
	 *         contain a 'PASTE' character sequence (which would probably be the
	 *         spacer); false otherwise
	 */
	public static boolean hasAPIKey(String serviceID) {
		return getAPIKey(serviceID) != null && !getAPIKey(serviceID).startsWith("PASTE_");
	}

	/**
	 * Loads a local text file into a HashMap, splitting each line into two parts
	 * separated by a space character. The first half is the 'serviceID', the second
	 * half is the API key.<br>
	 * This method is automatically called by
	 * <code>getAPIKey(String serviceID)</code> if the HashMap is still null.
	 */
	private static void init() {
		apiKeys = new HashMap<>();

		try {
			InputStream stream = APIKeyManager.class.getClassLoader().getResourceAsStream("API-Keys.txt");
			Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(stream, "UTF-8")));
			while (scanner.hasNextLine()) {
				String[] lineParts = scanner.nextLine().split(" ");
				apiKeys.put(lineParts[0], lineParts[1]);
			}
			scanner.close();
		} catch (Exception e) {
			System.err.println("Could not load API keys, services unavailable: " + e.getMessage());
		}
	}

}
