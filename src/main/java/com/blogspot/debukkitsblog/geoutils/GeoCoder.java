package com.blogspot.debukkitsblog.geoutils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Searches for a read world location or address specified by a name and tries
 * to find the corresponding coordinates (latitude, longitude) on earth using
 * external APIs.
 * 
 * @author DeBukkIt
 *
 */
public class GeoCoder {

	private static boolean silentMode = true;
	
	/**
	 * Tries to find <i>address</i> on earth. <b>You should use the variant of this
	 * method providing a GeoCache as the seconds argument to spare the external
	 * APIs.</b>
	 * 
	 * @param address
	 *            The address to find
	 * @return The GeoLocation (latitude, longitude) - or null if not found
	 */
	public static GeoLocation find(String address) {
		return find(address, null);
	}

	/**
	 * Tries to find <i>address</i> on earth
	 * 
	 * @param address
	 *            The address to find
	 * @param cache
	 *            The GeoCache to use. This spares the external APIs.
	 * @return The GeoLocation (latitude, longitude) - or null if not found
	 */
	public static GeoLocation find(String address, GeoCache cache) {
		// check parameters
		if (address == null || address.isEmpty()) {
			throw new IllegalArgumentException("Cannot find '' on earth, address must not be empty");
		}

		// read from cache if possible
		if (cache != null) {
			GeoLocation fromCache = cache.cacheReadPosition(address);
			if (fromCache != null) {
				if(!silentMode) System.out.println("Using cache to find " + address);
				return fromCache;
			}
		} else {
			System.err.println("Warning: GeoCoder is not using any cache!");
		}

		GeoLocation result = null;
		// Try using MapQuest
		try {
			if(!silentMode) System.out.println("Using MapQuest to find " + address);
			result = requestUsingMapQuest(address);
			// is the result good enough?
			if (!isResultPlausible(result)) {
				result = null;
				throw new Exception("Result outside plausible area");
			}
		} catch (Exception e) {
			System.err.println("Could not find " + address + " using MapQuest: " + e.getMessage());

			// else try using LocationIQ
			try {
				if(!silentMode) System.out.println("Using LocationIQ to find " + address);
				result = requestUsingLocationIQ(address);
				// is the result good enough?
				if (!isResultPlausible(result)) {
					result = null;
					throw new Exception("Result outside plausible area");
				}
				// else give up, return null
			} catch (Exception e2) {
				System.err.println("Could not find " + address + " using LocationIQ: " + e2.getMessage());
			}
		}

		// save to cache
		if (cache != null && result != null) {
			cache.cacheStorePosition(address, result);
		}

		return result;
	}

	/**
	 * Indicates whether a GeoLocation is plausible, i.e. if lies inside an area of
	 * the world that is not completely unlikely. Internally, a bounding box has
	 * been set up to check whether the GeoLocation is located in north-western
	 * Europe. The method can be overridden to specify a different bounding box.
	 * 
	 * @param loc
	 *            The location to check for plausibility
	 * @return true if it lies inside the hard-coded bounding box
	 */
	protected static boolean isResultPlausible(GeoLocation loc) {
		// check if result is plausible; example plausible bounding box for north west
		// Europe is
		// lower left upper right
		// 35.960223,-8.085938, 59.130863,28.652344
		return loc != null && !(loc.getLatitude() < 35.960223 || loc.getLatitude() > 59.130863
				|| loc.getLongitude() < -8.085938 || loc.getLongitude() > 28.652344);
	}

	/**
	 * Sends a request to the MapQuest GeoCoding API
	 * 
	 * @param address
	 *            The postal address or the name of the physical location
	 * @return A GeoLocation contains latitude and longitude of the location - or
	 *         null if <i>address</i> could not be found
	 * @throws IOException
	 *             if something went wrong sending a request to the MapQuest API via
	 *             the Internet
	 */
	private static GeoLocation requestUsingMapQuest(String address) throws IOException {
		if (!APIKeyManager.hasAPIKey("mapquest")) {
			System.err.println("Missing API key for 'mapquest'");
			return null;
		}

		double lat, lng;

		// request from MapQuest
		StringBuilder responseBuilder = new StringBuilder();
		URL url = new URL("http://open.mapquestapi.com/geocoding/v1/address?key=" + APIKeyManager.getAPIKey("mapquest")
				+ "&maxResults=1&outFormat=json&boundingBox=40.880295,-6.372070,56.897004,18.698730&location="
				+ URLEncoder.encode(address, "UTF-8"));
		Scanner s = new Scanner(new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8")));
		while (s.hasNextLine()) {
			responseBuilder.append(s.nextLine()).append(System.getProperty("line.separator"));
		}
		s.close();
		String response = responseBuilder.toString();

		// process JSON
		JSONObject json = new JSONObject(response);
		JSONArray jsonResults = json.getJSONArray("results");
		JSONArray jsonLocations = ((JSONObject) jsonResults.get(0)).getJSONArray("locations");
		JSONObject jsonTargetLocation = ((JSONObject) jsonLocations.get(0));
		JSONObject jsonLatLng = jsonTargetLocation.getJSONObject("latLng");

		lat = jsonLatLng.getDouble("lat");
		lng = jsonLatLng.getDouble("lng");

		// also try to extract the actual address information of the given result
		try {
			String street = jsonTargetLocation.getString("street");
			String neighborhood = jsonTargetLocation.getString("adminArea6");
			String zipCode = jsonTargetLocation.getString("postalCode");
			String city = jsonTargetLocation.getString("adminArea5");
			String county = jsonTargetLocation.getString("adminArea4");
			String state = jsonTargetLocation.getString("adminArea3");
			return new GeoLocation(lat, lng, street, zipCode, neighborhood, city, county, state, null);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return new GeoLocation(lat, lng);
	}

	/**
	 * Sends a request to the LocationIQ GeoCoding API
	 * 
	 * @param address
	 *            The postal address or the name of the physical location
	 * @return A GeoLocation contains latitude and longitude of the location - or
	 *         null if <i>address</i> could not be found
	 * @throws IOException
	 *             if something went wrong sending a request to the LocationIQ API
	 *             via the Internet
	 */
	private static GeoLocation requestUsingLocationIQ(String address) throws IOException {
		if (!APIKeyManager.hasAPIKey("locationiq")) {
			System.err.println("Missing API key for 'locationiq'");
			return null;
		}

		double lat, lng;

		// request from LocationIQ
		StringBuilder responseBuilder = new StringBuilder();
		URL url = new URL("https://eu1.locationiq.org/v1/search.php?key=" + APIKeyManager.getAPIKey("locationiq")
				+ "&q=" + URLEncoder.encode(address, "UTF-8") + "&format=json&addressdetails=1&limit=1");
		Scanner s = new Scanner(new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8")));
		while (s.hasNextLine()) {
			responseBuilder.append(s.nextLine()).append(System.getProperty("line.separator"));
		}
		s.close();
		String response = responseBuilder.toString();

		// process JSON
		JSONArray json = new JSONArray(response);
		JSONObject jsonPlace = json.getJSONObject(0);

		lat = jsonPlace.getDouble("lat");
		lng = jsonPlace.getDouble("lon");

		// also try to extract the actual address information of the given result
		try {
			JSONObject jsonAddress = jsonPlace.getJSONObject("address");
			String street = null;
			if (jsonAddress.has("road")) {
				street = jsonAddress.getString("road");
			}
			if (jsonAddress.has("house_number")) {
				street += " " + jsonAddress.getString("house_number");
			}
			String neighborhood = null;
			if (jsonAddress.has("suburb")) {
				neighborhood = jsonAddress.getString("suburb");
			}
			String zipCode = null;
			if (jsonAddress.has("postcode")) {
				zipCode = jsonAddress.getString("postcode");
			}
			String city = null;
			if (jsonAddress.has("town")) {
				city = jsonAddress.getString("town");
			} else if (jsonAddress.has("city")) {
				city = jsonAddress.getString("city");
			}
			String county = null;
			if (jsonAddress.has("county")) {
				county = jsonAddress.getString("county");
			}
			String state = null;
			if (jsonAddress.has("state")) {
				state = jsonAddress.getString("state");
			}
			return new GeoLocation(lat, lng, street, zipCode, neighborhood, city, county, state, null);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return new GeoLocation(lat, lng);
	}
	
	/**
	 * Toggles the output mode (do or do no output)
	 * 
	 * @param silent true for output; false for silence
	 */
	public static void setSilentmode(boolean silent) {
		silentMode = silent;
	}

}
