package trucksimulation.routing;

import enums.RoadType;

public class RouteSegment {
	private double[] lats;
	private double[] lons;
	private double[] eles;
	private double timeMs;
	private double distanceMeters;
	private int annotation; // com.graphhopper.util.Instruction enumeration
	
	public RouteSegment() {
		
	}
	
	public RouteSegment(double[] lats, double[] lons, double time, double distance) {
		this.lats = lats;
		this.lons = lons;
		this.timeMs = time;
		this.distanceMeters = distance;
	}

	public RouteSegment(double[] lats, double[] lons, double[] eles, double time, double distance, int annotation) {
		this.lats = lats;
		this.lons = lons;
		this.eles = eles;
		this.timeMs = time;
		this.distanceMeters = distance;
		this.annotation = annotation;
	}

	public double[] getLats() {
		return lats;
	}

	public void setLats(double... lats) {
		this.lats = lats;
	}

	public double[] getLons() {
		return lons;
	}

	public void setLons(double... lons) {
		this.lons = lons;
	}

	public double[] getEles() {
		return eles;
	}

	public void setEles(double... eles) {
		this.eles = eles;
	}
	
	public int getSize() {
		if(this.lats != null) {
			return this.lats.length;
		} else {
			return 0;
		}
	}
	
	public Position getPoint(int idx) {
		return new Position(lats[idx], lons[idx]);
	}

	public Position getLastPoint() {
		return getPoint(getSize()-1);
	}

	/**
	 * 
	 * @return time in milliseconds
	 */
	public double getTime() {
		return timeMs;
	}

	/**
	 * @param time in milliseconds
	 */
	public void setTime(double time) {
		this.timeMs = time;
	}

	/**
	 * 
	 * @return distance in meters
	 */
	public double getDistance() {
		return distanceMeters;
	}

	/**
	 * @param distance distance in meters
	 */
	public void setDistance(double distance) {
		this.distanceMeters = distance;
	}

	/**
	 * 
	 * @return annotation of instruction
	 */
	public int getAnnotation() {
		return annotation;
	}

	/**
	 * @param annotation annotation of instruction
	 */
	public void setAnnotation(int annotation) {
		this.annotation = annotation;
	}
	
	/**
	 * Returns the driving speed which can be expected on this segment.
	 * @return speed in m/s
	 */
	public double getSpeed() {
		return getDistance() / getTime() * 1000;
	}	

	public RoadType getRoadType() {
		if (getSpeed() > 100.0/3.6) {
			return RoadType.FREEWAY;
		} else if (getSpeed() > 65.0/3.6) {
			return RoadType.HIGHWAY;
		} else if (getSpeed() > 40.0/3.6) {
			return RoadType.INTERURBAN;
		} else {
			return RoadType.URBAN;
		}
	}
	

}
