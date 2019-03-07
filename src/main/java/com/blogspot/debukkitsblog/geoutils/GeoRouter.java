package com.blogspot.debukkitsblog.geoutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Calculates a route between to GeoLocations, calculates a duration matrix
 * between many starts and one destination, finds the shortest of many routes by
 * duration comparison
 * 
 * @author DeBukkIt
 *
 */
public class GeoRouter {

	private boolean silentMode;

	/**
	 * Constructs a new router. This will start a local OSRM route server
	 * (osrm-routed.exe) if present in the resources/osrm_sever directory.
	 */
	@Deprecated
	public GeoRouter() {
		this(true);
	}

	/**
	 * Constructs a new router. If the parameter is set to {@code true}, the program
	 * will try to start a local OSRM route server (osrm-routed.exe) if such an
	 * executable is present in the resources/osrm_sever directory.
	 */
	public GeoRouter(boolean useLocalOSRMServer) {
		silentMode = true;
		if (useLocalOSRMServer) {
			startOSRMRouteServer();
		}
	}

	/**
	 * Toggles the output mode (do or do no output)
	 * 
	 * @param silent true for output; false for silence
	 */
	public void setSilentmode(boolean silent) {
		this.silentMode = silent;
	}

	/**
	 * Starts a local OSRM route server (osrm-routed.exe) if present in the
	 * resources/osrm_sever directory. This feature might only work on Microsoft
	 * Windows operting systems.
	 */
	private void startOSRMRouteServer() {
		// don't start OSRM process twice
		if (isOSRMRouteServerRunning()) {
			System.err.println("OSRM Route Server already running, not starting it again.");
			return;
		}
		try {
			// Start OSRM process
			String pathToServerExe = new File(
					GeoRouter.class.getClassLoader().getResource("osrm_server/osrm-routed.exe").toURI())
							.getAbsolutePath();
			Process procOSRM = Runtime.getRuntime().exec(new String[] { pathToServerExe, "muenster-regbez-latest.osrm",
					"-i", "127.0.0.1", "-p", "7880", "-a", "MLD" }, null, new File(pathToServerExe).getParentFile());
			// Initialize OSRM-Output-Reader
			new Thread(() -> {
				BufferedReader reader = new BufferedReader(new InputStreamReader(procOSRM.getInputStream()));
				while (procOSRM.isAlive()) {
					String line = "";
					try {
						while ((line = reader.readLine()) != null) {
							if (!silentMode)
								System.out.println("[OSRM-Route-Server] " + line);
						}
					} catch (Exception e) {
						System.err.println("[OSRM-Route-Server] " + e.getMessage());
					}
				}
			}).start();
			// Initialize OSRM-Error-Reader
			new Thread(() -> {
				BufferedReader reader = new BufferedReader(new InputStreamReader(procOSRM.getErrorStream()));
				while (procOSRM.isAlive()) {
					String line = "";
					try {
						while ((line = reader.readLine()) != null) {
							if (!silentMode)
								System.out.println("[OSRM-Route-Server] " + line);
						}
					} catch (Exception e) {
						System.err.println("[OSRM-Route-Server] " + e.getMessage());
					}
				}
			}).start();
			// shutdown OSRM process on JVM exit
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				procOSRM.destroy();
			}));
		} catch (Exception e) {
			System.err.println("Could not start local OSRM route server:" + e.getMessage());
		}
	}

	/**
	 * Checks whether the service 'osrm-routed.exe' is running on the system. This
	 * method is absolutely platform dependent and will only work on Microsoft
	 * Windows operating systems.
	 * 
	 * @return true if there is a service with the name 'osrm-routed.exe' running.
	 */
	private boolean isOSRMRouteServerRunning() {
		if (!System.getProperty("os.name").contains("Windows")) {
			System.err.println(
					"GeoRouter.isOSRMRouteServerRunning() is only available under Microsoft Windows operating systems");
			return false;
		}
		try {
			String line;
			String pidInfo = "";

			Process p = Runtime.getRuntime().exec(System.getenv("windir") + "\\system32\\" + "tasklist.exe");

			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((line = input.readLine()) != null) {
				pidInfo += line;
			}
			input.close();

			if (pidInfo.contains("osrm-routed.exe")) {
				return true;
			}
		} catch (Exception e) {
			System.err.println("Could not check wether OSRM route server is running: " + e.getMessage());
		}
		return false;
	}

	/**
	 * Calculates the recommended route between <i>from</i> and <i>to</i>
	 * 
	 * @param from The start
	 * @param to   The destination
	 * @return The recommended route as GeoRoute object
	 */
	public GeoRoute calculateRoute(GeoLocation from, GeoLocation to) {
		return calculateRoute(from, to, null);
	}

	/**
	 * Calculates the recommended route between <i>from</i> and <i>to</i> using a
	 * GeoCache to speed up queries and spare external APIs.
	 * 
	 * @param from  The start
	 * @param to    The destination
	 * @param cache The GeoCache to be used
	 * @return The recommended route as GeoRoute object
	 */
	public GeoRoute calculateRoute(GeoLocation from, GeoLocation to, GeoCache cache) {
		// check parameters
		if (from == null || to == null) {
			throw new IllegalArgumentException("start and destination may not be null");
		}

		// check local server online
		if (!isOSRMRouteServerRunning()) {
			startOSRMRouteServer();
		}

		// read from cache if possible
		if (cache != null) {
			GeoRoute fromCache = cache.cacheReadRoute(from, to);
			if (fromCache != null) {
				if (!silentMode)
					System.out.println("Using cache to route from " + from + " to " + to);
				return fromCache;
			}
		} else {
			System.err.println("Warning: GeoRouter is not using any cache!");
		}

		// calculate route
		GeoRoute result = null;

		try {
			result = requestUsingOSRMLocal(from, to);
		} catch (Exception e) {
			System.err.println("Could not find route using OSRMLocal: " + e.getMessage());
			e.printStackTrace();
			try {
				result = requestUsingOSRMDemo(from, to);
			} catch (Exception e1) {
				System.err.println("Could not find route using OSRMDemo: " + e1.getMessage());
				try {
					result = requestUsingOpenRouteService(from, to);
				} catch (Exception e2) {
					System.err.println("Could not find route using OpenRouteService: " + e.getMessage());
				}
			}
		}

		// save to cache
		if (cache != null && result != null) {
			cache.cacheStoreRoute(from, to, result);
		}

		return result;
	}

	/**
	 * Sends a request to the local OSRM route server (osrm-routed.exe) running on
	 * port 7880, if any does so, and returns the route found by it.
	 * 
	 * @param from The start
	 * @param to   The destination
	 * @return The recommended route between start and destination found - or null
	 *         if no route was found
	 * @throws IOException if something went wrong sending the request to the local
	 *                     OSRM route server
	 */
	private GeoRoute requestUsingOSRMLocal(GeoLocation from, GeoLocation to) throws IOException {
		GeoRoute result = new GeoRoute();

		// prepare request URL
		String requestUrlString = "http://127.0.0.1:7880/route/v1/driving/_LNG1_,_LAT1_;_LNG2_,_LAT2_?geometries=geojson&steps=true&generate_hints=false";
		requestUrlString = requestUrlString.replace("_LAT1_", String.valueOf(from.getLatitude()))
				.replace("_LNG1_", String.valueOf(from.getLongitude()))
				.replace("_LAT2_", String.valueOf(to.getLatitude()))
				.replace("_LNG2_", String.valueOf(to.getLongitude()));

		// request from OpenRouteService
		StringBuilder responseBuilder = new StringBuilder();
		URL url = new URL(requestUrlString);
		Scanner s = new Scanner(new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8")));
		while (s.hasNextLine()) {
			responseBuilder.append(s.nextLine()).append(System.getProperty("line.separator"));
		}
		s.close();
		String response = responseBuilder.toString();

		// process JSON
		JSONObject json = new JSONObject(response);
		JSONArray jsonRoutes = json.getJSONArray("routes");
		JSONObject jsonTargetRoute = jsonRoutes.getJSONObject(0);
		JSONArray jsonLegs = jsonTargetRoute.getJSONArray("legs");
		JSONObject jsonTargetLeg = jsonLegs.getJSONObject(0);
		result.setDuration(jsonTargetLeg.getFloat("duration"));
		result.setDistance(jsonTargetLeg.getFloat("distance") / 1000);

		JSONArray jsonSteps = jsonTargetLeg.getJSONArray("steps");
		List<GeoLocation> waypoints = new ArrayList<>();
		for (int i = 0; i < jsonSteps.length(); i++) {
			JSONObject currentStep = jsonSteps.getJSONObject(i);
			JSONObject currentStepGeometry = currentStep.getJSONObject("geometry");
			JSONArray currentStepGeometryCoordinates = currentStepGeometry.getJSONArray("coordinates");
			for (int k = 0; k < currentStepGeometryCoordinates.length(); k++) {
				JSONArray currentCoord = currentStepGeometryCoordinates.getJSONArray(k);
				waypoints.add(new GeoLocation(currentCoord.getFloat(1), currentCoord.getFloat(0)));
			}
		}
		result.setWaypoints(waypoints);

		return result;
	}

	/**
	 * Sends a request to the public Demo OSRM route server via the Internet and
	 * returns the route found by it.
	 * 
	 * @param from The start
	 * @param to   The destination
	 * @return The recommended route between start and destination found - or null
	 *         if no route was found
	 * @throws IOException if something went wrong sending the request to the public
	 *                     Demo OSRM route server
	 */
	private GeoRoute requestUsingOSRMDemo(GeoLocation from, GeoLocation to) throws IOException {
		GeoRoute result = new GeoRoute();

		// prepare request URL
		String requestUrlString = "https://router.project-osrm.org/route/v1/driving/_LNG1_,_LAT1_;_LNG2_,_LAT2_?geometries=geojson&steps=true&generate_hints=false";
		requestUrlString = requestUrlString.replace("_LAT1_", String.valueOf(from.getLatitude()))
				.replace("_LNG1_", String.valueOf(from.getLongitude()))
				.replace("_LAT2_", String.valueOf(to.getLatitude()))
				.replace("_LNG2_", String.valueOf(to.getLongitude()));

		// request from OpenRouteService
		StringBuilder responseBuilder = new StringBuilder();
		URL url = new URL(requestUrlString);
		Scanner s = new Scanner(new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8")));
		while (s.hasNextLine()) {
			responseBuilder.append(s.nextLine()).append(System.getProperty("line.separator"));
		}
		s.close();
		String response = responseBuilder.toString();

		// process JSON
		JSONObject json = new JSONObject(response);
		JSONArray jsonRoutes = json.getJSONArray("routes");
		JSONObject jsonTargetRoute = jsonRoutes.getJSONObject(0);
		JSONArray jsonLegs = jsonTargetRoute.getJSONArray("legs");
		JSONObject jsonTargetLeg = jsonLegs.getJSONObject(0);
		result.setDuration(jsonTargetLeg.getFloat("duration"));
		result.setDistance(jsonTargetLeg.getFloat("distance") / 1000);

		JSONArray jsonSteps = jsonTargetLeg.getJSONArray("steps");
		List<GeoLocation> waypoints = new ArrayList<>();
		for (int i = 0; i < jsonSteps.length(); i++) {
			JSONObject currentStep = jsonSteps.getJSONObject(i);
			JSONObject currentStepGeometry = currentStep.getJSONObject("geometry");
			JSONArray currentStepGeometryCoordinates = currentStepGeometry.getJSONArray("coordinates");
			for (int k = 0; k < currentStepGeometryCoordinates.length(); k++) {
				JSONArray currentCoord = currentStepGeometryCoordinates.getJSONArray(k);
				waypoints.add(new GeoLocation(currentCoord.getFloat(1), currentCoord.getFloat(0)));
			}
		}
		result.setWaypoints(waypoints);

		return result;
	}

	/**
	 * Sends a request to the OpenRouteService API via the Internet and returns the
	 * route found by it.
	 * 
	 * @param from The start
	 * @param to   The destination
	 * @return The recommended route between start and destination found - or null
	 *         if no route was found
	 * @throws IOException if something went wrong sending the request to the
	 *                     OpenRouteService API
	 */
	private GeoRoute requestUsingOpenRouteService(GeoLocation from, GeoLocation to) throws IOException {
		if (!APIKeyManager.hasAPIKey("openrouteservice")) {
			System.err.println("Missing API key for 'openrouteservice'");
			return null;
		}

		GeoRoute result = new GeoRoute();

		// prepare request URL
		String requestUrlString = "https://api.openrouteservice.org/directions?api_key="
				+ APIKeyManager.getAPIKey("openrouteservice")
				+ "&coordinates=_LNG1_%2C_LAT1_%7C_LNG2_%2C_LAT2_&profile=driving-car&preference=recommended&format=json&units=km&language=de&instructions=true&instructions_format=text";
		requestUrlString = requestUrlString.replace("_LAT1_", String.valueOf(from.getLatitude()))
				.replace("_LNG1_", String.valueOf(from.getLongitude()))
				.replace("_LAT2_", String.valueOf(to.getLatitude()))
				.replace("_LNG2_", String.valueOf(to.getLongitude()));

		// request from OpenRouteService
		StringBuilder responseBuilder = new StringBuilder();
		URL url = new URL(requestUrlString);
		Scanner s = new Scanner(new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8")));
		while (s.hasNextLine()) {
			responseBuilder.append(s.nextLine()).append(System.getProperty("line.separator"));
		}
		s.close();
		String response = responseBuilder.toString();

		// process JSON
		JSONObject json = new JSONObject(response);
		JSONArray jsonRoutes = json.getJSONArray("routes");
		JSONObject jsonTargetRoute = jsonRoutes.getJSONObject(0);
		JSONObject jsonRouteSummary = jsonTargetRoute.getJSONObject("summary");
		result.setDistance(jsonRouteSummary.getFloat("distance"));
		result.setDuration(jsonRouteSummary.getFloat("duration"));

		return result;
	}

	/**
	 * Calculates a duration matrix between destination and every start location
	 * given using a local OSRM server (if any is running on port 7880)
	 * 
	 * @param destination The destination
	 * @param starts      An array of starts
	 * @return an array of GeoRoutes containing one element of <i>starts</i>, the
	 *         destination and the calculated duration. The distance of that route
	 *         is always -2.0f.
	 * @throws IOException if something went wrong sending the request to the local
	 *                     OSRM server
	 */
	public GeoRoute[] calculateMatrix(GeoLocation destination, GeoLocation... starts) throws IOException {
		if (starts.length < 1) {
			throw new IllegalArgumentException("there must be at least one start to calculate a matrix");
		}

		// collect coordinates
		StringBuilder coords = new StringBuilder();
		coords.append(destination.getLongitude() + "," + destination.getLatitude());
		for (GeoLocation pos : starts) {
			if (pos != null) {
				coords.append(";" + pos.getLongitude() + "," + pos.getLatitude());
			}
		}
		// collect destination indices
		StringBuilder indices = new StringBuilder(starts.length * 2);
		indices.append("1");
		for (int i = 1; i < starts.length; i++) {
			indices.append(";" + (i + 1));
		}

		// send request to local OSRM server
		StringBuilder responseBuilder = new StringBuilder();
		URL url = new URL("http://127.0.0.1:7880/table/v1/driving/" + coords.toString() + "?destinations=0&sources="
				+ indices.toString());
		Scanner s = new Scanner(new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8")));
		while (s.hasNextLine()) {
			responseBuilder.append(s.nextLine()).append(System.getProperty("line.separator"));
		}
		s.close();
		String response = responseBuilder.toString();

		// process json
		JSONObject json = new JSONObject(response);
		JSONArray jsonSources = json.getJSONArray("sources");
		JSONArray jsonDurations = json.getJSONArray("durations");

		// collect results and return them
		GeoRoute[] result = new GeoRoute[jsonSources.length()];
		for (int i = 0; i < result.length; i++) {
			result[i] = new GeoRoute(starts[i], destination, jsonDurations.getJSONArray(i).getFloat(0), -2.0f);
		}

		return result;
	}

	/**
	 * Finds the GeoRoute with the smallest duration in an array if GeoRoutes
	 * 
	 * @param routes The array of GeoRoutes
	 * @return The GeoRoute with the smallest duration
	 */
	public GeoRoute getShortestRoute(GeoRoute[] routes) {
		if (routes == null) {
			throw new IllegalArgumentException("routes may not be null");
		}
		if (routes.length == 1) {
			return routes[0];
		}

		List<GeoRoute> resultAsList = Arrays.asList(routes);
		Object[] sortedResult = resultAsList.stream().sorted(new Comparator<GeoRoute>() {
			@Override
			public int compare(GeoRoute o1, GeoRoute o2) {
				if (o1 != null && o2 != null && o1.getDuration() < o2.getDuration()) {
					return -1;
				} else {
					return 1;
				}
			}
		}).toArray();
		return (GeoRoute) sortedResult[0];
	}
}
