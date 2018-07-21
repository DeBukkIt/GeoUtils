package com.blogspot.debukkitsblog.geoutils;

import java.io.Serializable;

/**
 * A GeoLocation represents a physical location on earth marked by its name and
 * postal address
 * 
 * @author DeBukkIt
 *
 */
public class GeoLocation implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2483555097115607589L;
	
	private String name = null;
	private String streetAndNumber;
	private String zipCode;
	private String neighborhood;
	private String city;
	private String county;
	private String state;
	private String country;

	private double latitude;
	private double longitude;

	/**
	 * Constructs a new GeoLocation based on its decimal coordinates
	 * 
	 * @param latitude
	 *            The latitude
	 * @param longitude
	 *            The longitude
	 */
	public GeoLocation(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	/**
	 * Constructs a new GeoLocation based on its decimal coordinates and a name
	 * 
	 * @param name
	 *            The name of the location
	 * @param latitude
	 *            The latitude
	 * @param longitude
	 *            The longitude
	 */
	public GeoLocation(String name, double latitude, double longitude) {
		this(latitude, longitude);
		this.name = name;
	}

	/**
	 * Constructs a new GeoLocation based on its decimal coordinates and its postal
	 * address
	 * 
	 * @param latitude
	 *            The latitude
	 * @param longitude
	 *            The longitude
	 * @param streetAndNumber
	 *            The street name and - if present - a house number
	 * @param zipCode
	 *            A postal code
	 * @param neighborhood
	 *            The name of the neighborhood or borough
	 * @param city
	 *            The name of the city
	 * @param county
	 *            The name of the county or similar administrative area
	 * @param state
	 *            The name of the state (not country)
	 * @param country
	 *            The name of the country
	 */
	public GeoLocation(double latitude, double longitude, String streetAndNumber, String zipCode, String neighborhood,
			String city, String county, String state, String country) {
		this(latitude, longitude);
		if (streetAndNumber != null) {
			this.streetAndNumber = streetAndNumber.trim();
		}
		if (zipCode != null) {
			this.zipCode = zipCode.trim();
		}
		if (neighborhood != null) {
			this.neighborhood = neighborhood.trim();
		}
		if (city != null) {
			this.city = city.trim();
		}
		if (county != null) {
			this.county = county.trim();
		}
		if (state != null) {
			this.state = state.trim();
		}
		if (country != null) {
			this.country = country.trim();
		}
	}

	/**
	 * Constructs a new GeoLocation based on its decimal coordinates, its name and
	 * postal address
	 * 
	 * @param name
	 * @param latitude
	 *            The latitude
	 * @param longitude
	 *            The longitude
	 * @param streetAndNumber
	 *            The street name and - if present - a house number
	 * @param zipCode
	 *            A postal code
	 * @param neighborhood
	 *            The name of the neighborhood or borough
	 * @param city
	 *            The name of the city
	 * @param county
	 *            The name of the county or similar administrative area
	 * @param state
	 *            The name of the state (not country)
	 * @param country
	 *            The name of the country
	 */
	public GeoLocation(String name, double latitude, double longitude, String streetAndNumber, String zipCode,
			String neighborhood, String city, String county, String state, String country) {
		this(latitude, longitude, streetAndNumber, zipCode, neighborhood, city, county, state, country);
		this.name = name;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public String getStreetAndNumber() {
		return streetAndNumber;
	}

	public void setStreetAndNumber(String streetAndNumber) {
		this.streetAndNumber = streetAndNumber;
	}

	public String getZipCode() {
		return zipCode;
	}

	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}

	public String getNeighborhood() {
		return neighborhood;
	}

	public void setNeighborhood(String neighborhood) {
		this.neighborhood = neighborhood;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCounty() {
		return county;
	}

	public void setCounty(String county) {
		this.county = county;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	/**
	 * Calculates the direct distance between this and another GeoLocation in meters
	 * 
	 * @param other
	 *            The other GeoLocation
	 * @return the direct distance (bee line) between this and another GeoLocation
	 *         in meters
	 */
	public float distanceTo(GeoLocation other) {
		double R = 6378.137;
		double dLat = other.getLatitude() * Math.PI / 180 - this.getLatitude() * Math.PI / 180;
		double dLon = other.getLongitude() * Math.PI / 180 - this.getLongitude() * Math.PI / 180;
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(this.getLatitude() * Math.PI / 180)
				* Math.cos(other.getLatitude() * Math.PI / 180) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double d = R * c;
		return (float) (d * 1000);
	}

	/**
	 * @return A String representation of the GeoLocation. If it has a name, if is
	 *         just the name. If not, it is the postal address (or at least as much
	 *         as there is of the postal address).
	 */
	@Override
	public String toString() {
		// if it has a name, return a name
		if (name != null && !name.isEmpty()) {
			return name;
		}

		// else return an address
		StringBuilder builder = new StringBuilder(160);
		if (streetAndNumber != null && !streetAndNumber.isEmpty()) {
			builder.append(streetAndNumber);
		}
		if (zipCode != null && !zipCode.isEmpty()) {
			builder.append(", " + zipCode);
		}
		if (city != null && !city.isEmpty()) {
			if (zipCode != null && !zipCode.isEmpty()) {
				builder.append(" " + city);
			} else {
				builder.append(", " + city);
			}
		}
		if (neighborhood != null && !neighborhood.isEmpty()) {
			if (city != null && !city.isEmpty()) {
				builder.append(" (" + neighborhood + ")");
			} else {
				builder.append(" " + neighborhood);
			}
		}
		if (county != null && !county.isEmpty()) {
			builder.append(", " + county);
		}
		if (state != null && !state.isEmpty()) {
			builder.append(", " + state);
		}
		if (country != null && !country.isEmpty()) {
			builder.append(", " + country);
		}
		return builder.toString().replaceFirst("^, ", "");
	}

}