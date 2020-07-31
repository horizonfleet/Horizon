package trucksimulation.trucks;

import trucksimulation.routing.Position;

import java.lang.Math.*;
import java.util.LinkedList;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
/**
 * Represents the telemetry box which is mounted to a truck.
 * By default, boxes do not deteriorate data, unless explicitly set with {@link #setDeteriorate(boolean)}.
 *
 */
public class TelemetryBox {
	
	private String id;
	private double speed;
	private TelemetryData prevData;
	private TelemetryData curData;
	private boolean deteriorate;
	private int secondsSinceLastSend;
	// average Speed Array can only be implemented here without large code changes
	private transient LinkedList<Double> averageSpeedArray;
	private transient int publishInterval = 120;

	private static final transient Logger LOGGER = LoggerFactory.getLogger(TelemetryBox.class);


	public TelemetryBox(String id) {
		this.id = id;
		this.averageSpeedArray = new LinkedList<Double>();
	}
		

	public TelemetryData getTelemetryData() {
		return curData;		
	}
	
	/**
	 * Update position and timestamp
	 * @param pos
	 * @param timestamp time in ms
	 */
	public TelemetryData update(Position pos, long timestamp, int truckcond) {
		this.secondsSinceLastSend += 1;
		prevData = curData;
		curData = new TelemetryData(id, deteriorate);
		curData.setTimeStamp(timestamp);
		curData.setPosition(pos);
		double speed = getSpeed();
		this.speed = speed;

		if (averageSpeedArray.size() < publishInterval){
			averageSpeedArray.addFirst(speed);
		} else if (averageSpeedArray.size() == publishInterval) {
			averageSpeedArray.removeLast();
			averageSpeedArray.addFirst(speed);
		} else {
			LOGGER.warn("Average Speed Array is larger than publish Interval with length `{0}`", averageSpeedArray.size());
		}

		curData.setSpeed(speed);
		curData.setAverageIntervalSpeed(calculateAvgSpeed(), this.secondsSinceLastSend);
		curData.setAcceleration(getAcceleration());
		curData.setTruckCondition(truckcond);
		if(prevData != null) {
			curData.setBearing(prevData.getPosition().getBearing(pos));
		}
		return curData;
	}


	/**
	 * Calculates the current speed using the distance between last and current position.
	 * Due to inexact position data, speeds can differ from the actual speed.
	 * 
	 * @return speed in m/s
	 */
	private double getSpeed() {
		if(prevData != null && prevData.getPosition() != null) {
			Position prevPos = prevData.getPosition();
			Position curPos = curData.getPosition();
			double dist = prevPos.getDistance(curPos);
			double speed = dist / (curData.getTimeStamp() - prevData.getTimeStamp()) * 1000;
			if (Double.isInfinite(speed)) speed = 0;
			return speed;
		} else {
			return 0;
		}
	}

	/**
	 * Calculates the current acceleration using the difference between last and current speed.
	 * Due to inexact position data, speeds can differ from the actual speed and thus acceleration can differ too.
	 * 
	 * @return acceleration in m/s^2
	 */
	private double getAcceleration() {
		if(prevData != null && prevData.getPosition() != null) {
			double prevSpeed = prevData.getSpeed();
			double curSpeed = curData.getSpeed();
			double diff = curSpeed-prevSpeed;
			double acc = diff / (curData.getTimeStamp() - prevData.getTimeStamp()) * 1000;
			if (Double.isInfinite(acc)) acc = 0.0;
			return acc;
		} else {
			return 0;
		}
	}

	/**
	 * Calculates the current average speed sind the last time information was sent.
	 * 
	 * @return avg Speed over the last interval
	 */
	private double calculateAvgSpeed() {
		double sum = 0;
		int n = 0;
		if(averageSpeedArray.size() <= publishInterval){
	    	for (double value : averageSpeedArray) {
	    		n += 1;
	        	sum += value;
	        	if (n >= publishInterval) break;
	   		}
	    	return Math.max(((double) sum / publishInterval), 0.0);
	    } else {
	    	LOGGER.warn("AverageSpeedArray is too large, returning 0");
	    	return 0.0;
	    }
	}

	public void setId(String id) {
		this.id = id;
	}
	public void setPublishInterval(int interval) {
		this.publishInterval = interval;
	}

	public boolean isDeteriorating() {
		return deteriorate;
	}

	public void setDeteriorate(boolean deteriorate) {
		this.deteriorate = deteriorate;
	}

	public void resetInterval(){
		//LOGGER.warn("Average Speed Array reset! and length `{0}`", averageSpeedArray.size());
		//LOGGER.warn("Current Speed `{0}`", speed);
		//LOGGER.warn("Average Speed Array mean `{0}`", calculateAvgSpeed());
		this.secondsSinceLastSend = 0;
		this.averageSpeedArray = new LinkedList<Double>();
	}
}