package com.blogspot.debukkitsblog.geoutils;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A geographical route between to geographical locations
 * 
 * @author DeBukkIt
 *
 */
public class GeoRoute implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2074755458534480248L;
	
	private GeoLocation start;
	private GeoLocation destination;

	private List<GeoLocation> waypoints;

	private float duration;
	private float distance;

	/**
	 * Constructs an empty route
	 */
	public GeoRoute() {
		start = null;
		destination = null;
		waypoints = new ArrayList<>();
		duration = -1.0f;
		distance = -1.0f;
	}

	/**
	 * Constructs a route
	 * 
	 * @param start
	 *            The starting location
	 * @param destination
	 *            The destination location
	 * @param duration
	 *            The duration
	 * @param distance
	 *            The indirect distance (vis streets, not bee line) between start
	 *            and destination
	 */
	public GeoRoute(GeoLocation start, GeoLocation destination, float duration, float distance) {
		this.start = start;
		this.destination = destination;
		this.waypoints = new ArrayList<>();
		this.duration = duration;
		this.distance = distance;
	}

	public GeoLocation getStart() {
		return start;
	}

	public void setStart(GeoLocation start) {
		this.start = start;
	}

	public GeoLocation getDestination() {
		return destination;
	}

	public void setDestination(GeoLocation destination) {
		this.destination = destination;
	}

	public float getDuration() {
		return duration;
	}

	public void setDuration(float duration) {
		this.duration = duration;
	}

	public float getDistance() {
		return distance;
	}

	public void setDistance(float distance) {
		this.distance = distance;
	}

	public List<GeoLocation> getWaypoints() {
		return waypoints;
	}

	public void setWaypoints(List<GeoLocation> waypoints) {
		this.waypoints = waypoints;
	}

	/**
	 * @return A string representation of the route (start and destination) and its
	 *         distance and duration attributes
	 */
	@Override
	public String toString() {
		LocalTime time = LocalTime.ofSecondOfDay((long) duration);
		return "[" + start + " -> " + destination + "; " + new DecimalFormat("#.##").format(distance).replace('.', ',')
				+ " km, " + time.toString() + " h]";
	}

}
