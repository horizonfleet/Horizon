package trucksimulation.trucks;

import java.util.Random;

import trucksimulation.routing.Position;

public class TelemetryData {
	
	private transient String id;
	private int truckCond;

	private long timeStamp;
	private transient Position pos;
	private transient double altitude;
	private transient int verticalAccuracy = 20;
	private transient int horizontalAccuracy = 5;
	private double speed;
	private double avgIntervSpeed;
	private int secSinceLast;
	private double acceleration;
	private double bearing;
	private transient double temperature = 20.0;
	private transient Random random = new Random();
	private transient boolean deteriorate;
	
	public TelemetryData(String id, boolean deteriorate) {
		this.id = id;
		this.deteriorate = deteriorate;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getTruckCondition() {
		return this.truckCond;
	}

	public void setTruckCondition(int cond) {
		this.truckCond = cond;
	}

	public double getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	} 
	public double getAltitude() {
		return altitude;
	}
	public void setAltitude(double altitude) {
		this.altitude = altitude;
	}
	public double getSpeed() {
		return speed;
	}
	public double getAverageIntervalSpeed() {
		return avgIntervSpeed;
	}
	public void setAverageIntervalSpeed(double avgIntervSpeed, int secSinceLast) {
		this.avgIntervSpeed = avgIntervSpeed;
		this.secSinceLast = secSinceLast;
	}
	public int getSecondsSinceLastSend() {
		return this.secSinceLast;
	}
	public void setSpeed(double speed) {
		this.speed = speed;
	}
	public double getAcceleration() {
		return acceleration;
	}
	public void setAcceleration(double acceleration) {
		this.acceleration = acceleration;
	}
	public Position getPosition() {
		return pos;
	}
	public void setPosition(Position position) {
		if(deteriorate) {
			this.pos = deteriorate(position);
		} else {
			this.pos = position;
		}
		
	}
	
	private Position deteriorate(Position pos) {
		double lat = pos.getLat() + getDeterioration();
		double lon = pos.getLon() + getDeterioration();
		Position newPos = new Position(lat, lon);
		horizontalAccuracy = (int) Math.round(newPos.getDistance(pos));
		return newPos;
	}
	
	/**
	 * Returns a uniformly distributed value which can be added to the gps coordinates.
	 * @return
	 */
	private double getDeterioration() {
		return (random.nextDouble() - 0.5)/10000;
	}
	
	public int getVerticalAccuracy() {
		return verticalAccuracy;
	}
	public void setVerticalAccuracy(int verticalAccuracy) {
		this.verticalAccuracy = verticalAccuracy;
	}
	public int getHorizontalAccuracy() {
		return horizontalAccuracy;
	}
	public void setHorizontalAccuracy(int horizontalAccuracy) {
		this.horizontalAccuracy = horizontalAccuracy;
	}
	public double getBearing() {
		return bearing;
	}
	public void setBearing(double bearing) {
		this.bearing = bearing;
	}
	public double getTemperature() {
		return temperature;
	}
	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}
}