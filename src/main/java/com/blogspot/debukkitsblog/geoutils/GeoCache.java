package com.blogspot.debukkitsblog.geoutils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import com.blogspot.debukkitsblog.util.FileStorage;

/**
 * Provides a simple cache storage for use with the GeoCoder and GeoRouter
 * services
 * 
 * @author DeBukkIt
 *
 */
public class GeoCache implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 853274164210716569L;
	
	private FileStorage storage;
	private long durability;

	public static final long ONE_DAY = 1000 * 60 * 60 * 24;
	public static final long TWO_DAYS = ONE_DAY * 2;
	public static final long ONE_WEEK = ONE_DAY * 7;
	public static final long TWO_WEEKS = ONE_WEEK * 2;
	public static final long ONE_MONTH = ONE_DAY * 30;
	public static final long THREE_MONTHS = ONE_MONTH * 3;
	public static final long SIX_MONTHS = ONE_MONTH * 6;
	public static final long ONE_YEAR = ONE_DAY * 365;

	/**
	 * Creates a new cache object specified by a file where the cached information
	 * should be stored and a durability after which a particular cache element is
	 * renewed.
	 * 
	 * @param file
	 *            The file where the cached information should be stored
	 * @param durability
	 *            a durability after which a particular cache element is renewed
	 * @throws IOException
	 *             if something goes wrong loading or creating <i>file</i>
	 * @throws IllegalArgumentException
	 *             if <i>file</i> is a directory
	 */
	public GeoCache(File file, long durability) throws IOException, IllegalArgumentException {
		this.durability = durability;
		storage = new FileStorage(file, true);
	}

	/**
	 * Stores a GeoLocation object in the cache
	 * 
	 * @param address
	 *            The key to find the object with
	 * @param pos
	 *            The GeoLocation to be stored
	 */
	public void cacheStorePosition(String address, GeoLocation pos) {
		// save to storage wrapped in a GeoLocationCache object
		try {
			storage.store(address.toLowerCase(), new CacheElement(pos));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads a GeoLocation object from the cache
	 * 
	 * @param address
	 *            The key to find the object with
	 * @return The GeoLocation - or null if there is no object cached under
	 *         <i>address</i> key
	 */
	public GeoLocation cacheReadPosition(String address) {
		// load from storage
		Object obj = storage.get(address.toLowerCase());
		if (obj != null && obj instanceof CacheElement) {
			// convert to GeoLocationCache object
			CacheElement cachedPosition = (CacheElement) obj;
			// only return if not expired yet
			if (cachedPosition.time + durability >= System.currentTimeMillis()) {
				return (GeoLocation) cachedPosition.content;
			}
		}
		return null;
	}

	/**
	 * Stores a GeoRoute object in the cache. A key is generated using the String
	 * representations of <i>from</i> and </i>to</i>. So a slightly different
	 * GeoLocation coordinate or a rounded coordinate will cause a new key being
	 * generated for caching.
	 * 
	 * @param from
	 *            The start of the route
	 * @param to
	 *            The destination of the route
	 * @param route
	 *            The route to be cached
	 */
	public void cacheStoreRoute(GeoLocation from, GeoLocation to, GeoRoute route) {
		// save to storage wrapped in a GeoLocationCache object
		try {
			storage.store(from.toString() + "->" + to.toString(), new CacheElement(route));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads a GeoRoute object from the cache
	 * 
	 * @param from
	 *            The start of the route
	 * @param to
	 *            The destination of the route
	 * @return The GeoRoute - or null if there is no object cached under <i>from</i>
	 *         and </i>to</i>
	 */
	public GeoRoute cacheReadRoute(GeoLocation from, GeoLocation to) {
		// load from storage
		Object obj = storage.get(from.toString() + "->" + to.toString());
		if (obj != null && obj instanceof CacheElement) {
			// convert to GeoLocationCache object
			CacheElement cachedRoute = (CacheElement) obj;
			// only return if not expired yet
			if (cachedRoute.time + durability >= System.currentTimeMillis()) {
				return (GeoRoute) cachedRoute.content;
			}
		}
		return null;
	}

	/**
	 * A CacheElement wrapping any object with and the current system time of
	 * instantiation. The time stamp will be used later for calculating the
	 * durability of the cached object.
	 * 
	 * @author DeBukkIt
	 *
	 */
	private class CacheElement implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5916047588133440995L;

		public Object content;
		public long time;

		public CacheElement(Object content) {
			this.content = content;
			this.time = System.currentTimeMillis();
		}
	}

}
