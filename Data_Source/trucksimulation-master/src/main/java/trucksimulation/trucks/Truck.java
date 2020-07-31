package trucksimulation.trucks;

import java.lang.Math.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import helpers.*;
import enums.*;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import trucksimulation.routing.Position;
import trucksimulation.routing.Route;
import trucksimulation.routing.RouteSegment;
import trucksimulation.routing.TargetExceededException;
import trucksimulation.traffic.TrafficIncident;

public class Truck {
	
	private String id;
	private Route route;
	private String routeId;
	private String tripId;
	private TelemetryBox telemetryBox;
	private TelemetryBox telemetryBoxInexact;
	private List<TrafficIncident> incidents = new ArrayList<>();
	/** points to the current traffic incident if the truck is affected by one. null otherwise. */
	private TrafficIncident curIncident = null;
	private TruckEventListener trafficEventListener;
	private int idleCountdown = 0;
	private boolean needsNewRoute = false;

	private TruckType truckType;
	private String licensePlate;
	private int year;
	private int massPayload; // kg
	private int massEmpty; // kg
	private double surface; // m^2
	private double cw;
	private double specificConsumption; // g/kWh
	private double fuelDensity; // kg/l
	private double fuelHeatingValue; // MJ/kg

	private double truckCondition; // 0=service needed, 1=service just done
	private double engineCondition; // 0=engine defect, 1=engine good
	private double tiresEfficiency; // 0=efficiency bad 1=efficiency good

	private Driver driver = new Driver();
	private double speed = 5.0;
	private double currentTargetSpeed = 5.0;
	private double nextTargetSpeed = 5.0;
	private double distanceToNextTargetSpeed = 1;
	private double acceleration = 0.0;
	private Position pos;
	private Position targetPos;
	private int interval = 1;
	private long ts = 1;
	
	private int curRouteSegment = 0;
	private int curSegmentPoint = 0;
	
	private static List<Truck> trucks = new ArrayList<>();
	private static long nextTruckId = 100;
	private static final Logger LOGGER = LoggerFactory.getLogger(Truck.class);


	/**
	 * Barebones constructor (for use in test cases)
	 * @param id
	 */
    public Truck(String id) {
		this.id = id;
		telemetryBox = new TelemetryBox(id);
		telemetryBoxInexact = new TelemetryBox(id);
		telemetryBoxInexact.setDeteriorate(true);
		ts = LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC) * 1000;
	}

	/**
	 * Automatically assigns the initial timestamp to the current time in UTC.
	 * With each move, the time will be incremented by the interval (real time cannot be used, as simulation may be sped up)
	 * 
	 * @param id
	 */
	public Truck(String id, String licensePlate, int truckType, int year, int massEmpty, double surface, double cw) {
		this.id = id;
		this.licensePlate = licensePlate;
		this.truckType = TruckType.from(truckType);
		this.year = year;
		this.massEmpty = massEmpty;
		this.massPayload = getRandomPayload(this.truckType, this.massEmpty);
		this.surface = surface;
		this.cw = cw;
		this.specificConsumption = getRandomSpecificConsumption(this.year);
		this.fuelDensity = getRandomFuelDensity();
		this.fuelHeatingValue = getRandomFuelHeatingValue();

		this.truckCondition = GenerationHelper.getRandomValue(0.0, 1.0);
		this.engineCondition = GenerationHelper.getRandomValue(0.8, 1.0); // should not start with a damaged engine
		this.tiresEfficiency = GenerationHelper.getRandomValue(0.0, 1.0);

		telemetryBox = new TelemetryBox(id);
		telemetryBoxInexact = new TelemetryBox(id);
		telemetryBoxInexact.setDeteriorate(true);
		ts = LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC) * 1000;
	}
	
	public static Truck buildTruck() {
		Truck t = new Truck("truck " + nextTruckId++, "", 2010, 7000, 15000, 8.5, 0.5);
		trucks.add(t);
		return t;
	}

	/** Returns specific fuel consumption in g/kWh based on model year */
	private double getRandomSpecificConsumption(int year) {
		double efficiency1990 = GenerationHelper.getRandomValue(191.0, 195.0);
		double efficiency2000 = GenerationHelper.getRandomValue(187.0, 189.0);
		double efficiency2010 = GenerationHelper.getRandomValue(184.5, 185.5);
		double efficiency2020 = GenerationHelper.getRandomValue(182.5, 183.5);

		if (year >= 1990 && year < 2000 ) {
			return efficiency1990 + (year-1990) * (efficiency2000 - efficiency1990) / 10;
		} else if (year >= 2000 && year < 2010 ) {
			return efficiency2000 + (year-2000) * (efficiency2010 - efficiency2000) / 10;
		} else if (year >= 2010 && year < 2020 ) {
			return efficiency2010 + (year-2010) * (efficiency2020 - efficiency2010) / 10;
		} else {
			return 0;
		}
	}

	private String generateTripId(){
	    return randomString(10);
	}

    private String randomString(int length) {
        final String characters = "ABCDEFGHIJLMNOPQRSTUVWXYZ1234567890";
        StringBuilder result = new StringBuilder();
        while(length > 0) {
           Random rand = new Random();
           result.append(characters.charAt(rand.nextInt(characters.length())));
           length--;
        }
        return result.toString();
    }

	private int getRandomPayload(TruckType truckType, int massEmpty) {
		int massPayload = 0;
		if (truckType == TruckType.LOCAL) {
			massPayload = GenerationHelper.getRandomValue(0, 18000-massEmpty);
		} else if (truckType == TruckType.LONG_DISTANCE) {
			massPayload = GenerationHelper.getRandomValue(0, 26000-massEmpty);
		} else if (truckType == TruckType.LONG_DISTANCE_TRAILER) {
			massPayload = GenerationHelper.getRandomValue(0, 40000-massEmpty);
		}
		return massPayload;
	}

	/** Returns fuel density in kg/l */
	private double getRandomFuelDensity() {
		return GenerationHelper.getRandomValue(0.82, 0.845);
	}

	/** Returns fuel heating value in MJ/kg */
	private double getRandomFuelHeatingValue() {
		return GenerationHelper.getRandomValue(39.0, 43.2);
	}
	
	/**
	 * Instructs the truck to idel for the specified amount of minutes.<br>
	 * Consequent calls to {@link #move()} will not change the trucks position until the pause time is over.<br>
	 * 
	 * When called while a truck is already in pause mode, the current remaining pause time is replaced with the new value.
	 * 
	 * @param pauseTimeMinutes pause time in minutes
	 */
	public void pause(int pauseTimeMinutes) {
		LOGGER.info("truck `{0}` is having a break for {1} minute/s.", id, pauseTimeMinutes);
		this.idleCountdown = pauseTimeMinutes * 60;
	}
	
	public boolean isInPauseMode() {
		return this.idleCountdown > 0;
	}
	
	/**
	 * Moves the truck forward on its assigned route (unless it is in pause mode).
	 * 
	 * Throws a DestinationArrivedException if the truck is already at the last point of the route.
	 * 
	 * @see #pause(int)
	 */
	public void move() {
		if(isInPauseMode()) {
			idleCountdown--;
			if(!isInPauseMode()) {
				LOGGER.info("truck `{0}` completed its break.", id);
			}
		} else {
			move(speed);
		}
	}

	private void updateSpeed() {
		double lastAcceleration = acceleration;

		double speedFactor = driver.getParameters().getSpeedFactor();
		double maxSpeed = driver.getParameters().getMaxSpeed();
		double a_max = driver.getParameters().getAMax();
		double a_min = driver.getParameters().getAMin();
		double adt_max = driver.getParameters().getADtMax();
		double adt_min = driver.getParameters().getADtMin();
		double driverGasPedalReleaseExponent = driver.getParameters().getGasPedalReleaseGradient();
		double driverBrakePedalReleaseExponent = driver.getParameters().getBrakePedalReleaseGradient();

		double currentTargetSpeed = this.currentTargetSpeed;
		double nextTargetSpeed = this.nextTargetSpeed;

		// apply driver attentiveness
		double varition = 0.025; // higher values lead to faster speed variation (maximum is PI/2)
		double maxSpeedChange = 5.0 / 3.6; // higher values lead to higher speed variation [m/s]
		double speedChange = Math.sin(varition * ts/1000.0) * (1.0 - driver.getParameters().getEnergyLevel()) * maxSpeedChange;
		currentTargetSpeed += speedChange;
		nextTargetSpeed += speedChange;
		
		// apply driver speed
		currentTargetSpeed = Math.max(Math.min(currentTargetSpeed * speedFactor, maxSpeed), 0.0);
		nextTargetSpeed = Math.max(Math.min(nextTargetSpeed * speedFactor, maxSpeed), 0.0);
		
		// current speed controller
		double currentSpeedAcceleration = 0;
		if (currentTargetSpeed > speed) { // acceleration necessary
			currentSpeedAcceleration = a_max * (1-Math.pow(speed/currentTargetSpeed, driverGasPedalReleaseExponent));
		} else { // deceleration necessary
			currentSpeedAcceleration = a_min * (Math.pow(speed/currentTargetSpeed, driverBrakePedalReleaseExponent)-1);
		}

		// next speed controller
		double nextSpeedAcceleration = 0;
		double minTimeToReachTargetSpeed = 0;
		double minDistanceToReachTargetSpeed = 0;
		if (nextTargetSpeed > speed) { // acceleration necessary
			minTimeToReachTargetSpeed = (nextTargetSpeed-speed) / a_max;
			minDistanceToReachTargetSpeed = speed*minTimeToReachTargetSpeed + 0.5 * a_max * Math.pow(minTimeToReachTargetSpeed, 2);
			nextSpeedAcceleration = a_max * (1-Math.pow(speed/nextTargetSpeed, driverGasPedalReleaseExponent));
		} else { // deceleration necessary
			minTimeToReachTargetSpeed = (nextTargetSpeed-speed) / a_min;
			minDistanceToReachTargetSpeed = speed*minTimeToReachTargetSpeed + 0.5 * a_min * Math.pow(minTimeToReachTargetSpeed, 2);
			nextSpeedAcceleration = a_min * (Math.pow(speed/nextTargetSpeed, driverBrakePedalReleaseExponent)-1);
		}

		// switch between controllers
		if (minDistanceToReachTargetSpeed >= (distanceToNextTargetSpeed + speed*interval)) {
			acceleration = nextSpeedAcceleration;
		} else {
			acceleration = currentSpeedAcceleration;
		}

		// apply limitation
		acceleration = Math.max(Math.min(acceleration, a_max), a_min);
		if (acceleration - lastAcceleration > adt_max) {
			acceleration = lastAcceleration + adt_max;
		} else if (acceleration - lastAcceleration < adt_min) {
			acceleration = lastAcceleration + adt_min;
		}
		// calc new speed
		speed = speed + acceleration * interval;
		speed = Math.max(Math.min(speed, maxSpeed), 0.0);
	}

	private void updateDriver() {
		// maybe update driver parameters or warnings in the future
	}

	private void updateTruckCondition() {
		// Update truck condition
		// normally 1/4320000 per second for once a year (100000km for service, 200km driving per day, 6h driving per day)
		// here 1/100000 per second for maybe every 10 routes (10000s per route)
		double truckWearPerSecond = 0.00001;
		truckCondition = Math.max(truckCondition-truckWearPerSecond, 0.0);

		// Damage the engine
		// here with a chance of 0.001% per second (every 100000s) the engine changes its condition, but never gets better
		if( new Random().nextDouble() <= 0.00001 ) {
			engineCondition = Math.min(GenerationHelper.getRandomValue(0.0, 1.0), engineCondition);
		}
	}
	
	private void move(double moveSpeed) {
		if (this.pos == null){
			LOGGER.warn("NO TRUCK POSITION for truck `{0}`",  this.id);
		}
		if(hasArrived() && !needsNewRoute) {
			// use needsNewRoute flag to ensure exception is only called once per destination
			needsNewRoute = true;
			throw new DestinationArrivedException("Already arrived at destination. Set a new destination / route.");
		} else if (needsNewRoute) {
			// don't move until a new route has been set
			return;
		}
		updateSpeed();
		updateDriver();
		updateTruckCondition();
		try {
			pos = pos.moveTowards(targetPos, moveSpeed * interval);
		} catch (TargetExceededException e) {
			pos = targetPos;
			proceedToNextPoint();
			try {
				pos = pos.moveTowards(targetPos, e.getExceededBy());
			} catch (TargetExceededException e1) {
				// segment was very short or time is too high. take next segment, but don't try to move again
				pos = targetPos;
				proceedToNextPoint();
			}
		}

		ts += interval * 1000;
		telemetryBox.update(pos, ts, getCondition().getValue());
		telemetryBoxInexact.update(pos, ts, getCondition().getValue());
		updateTrafficMode();
	}

	private void proceedToNextPoint() {	
		RouteSegment currentSegment = route.getSegment(curRouteSegment);
		if(currentSegment.getSize() > curSegmentPoint + 1) {
			curSegmentPoint++;
			targetPos = currentSegment.getPoint(curSegmentPoint);
		} else {
			curRouteSegment++;
			curSegmentPoint = 0;
			if(curRouteSegment == route.getSegmentCount() -1) {
				currentSegment = route.getSegment(curRouteSegment);
				targetPos = currentSegment.getPoint(curSegmentPoint);
			} else if(curRouteSegment < route.getSegmentCount()) {
				currentSegment = route.getSegment(curRouteSegment);
				targetPos = currentSegment.getPoint(curSegmentPoint);
				currentTargetSpeed = currentSegment.getSpeed();
			} else {
				needsNewRoute = true;
				throw new DestinationArrivedException("Truck has reached its target. Please assign a new route before proceeding.");
			}
		}
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Route getRoute() {
		return route;
	}
	
	/**
	 * Assigns a route to the truck and positions the truck at the start of the route.
	 * 
	 * @param route
	 */
	public void setRoute(Route route) {
		this.massPayload = getRandomPayload(this.truckType, this.massEmpty);
		this.route = route;
		this.pos = route.getStart();
		curRouteSegment = 0;
		curSegmentPoint = 0;
		needsNewRoute = false;

		// update data in TelemetryBox as well
		telemetryBox.update(pos, ts, getCondition().getValue());
		telemetryBoxInexact.update(pos, ts, getCondition().getValue());

		this.proceedToNextPoint();
	}

	public double getSpeed() {
		return speed;
	}
	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public String getLicensePlate() {
		return licensePlate;
	}
	public double getSurface() {
		return surface;
	}
	public double getCw() {
		return cw;
	}
	public int getYear() {
		return year;
	}
	public double getMassTotal() {
		return massEmpty+massPayload;
	}
	public double getAcceleration() {
		return acceleration;
	}
	public double getDebug() {
		return nextTargetSpeed;
	}
	public boolean isInIncident() {
		return (curIncident != null);
	}
	public TruckType getTruckType() {
		return truckType;
	}
	public RoadType getCurrentRoadType() {
		return route.getSegment(curRouteSegment).getRoadType();
	}
	public WarningType getCondition() {
		if (truckCondition < 0.3) {
			return WarningType.BAD;
		} else if (truckCondition > 0.6) {
			return WarningType.NORMAL;
		} else {
			return WarningType.CONSPICIOUS;
		}
	}
	public WarningType getTireEfficiency() {
		if (tiresEfficiency < 0.3) {
			return WarningType.BAD;
		} else if (tiresEfficiency > 0.6) {
			return WarningType.NORMAL;
		} else {
			return WarningType.CONSPICIOUS;
		}
	}
	public WarningType getEngineEfficiency() {
		if (engineCondition < 0.5) {
			return WarningType.BAD;
		} else if (engineCondition > 0.8) {
			return WarningType.NORMAL;
		} else {
			return WarningType.CONSPICIOUS;
		}
	}
	public Driver getDriver() {
		return driver;
	}
	public boolean needsNewRoute() {
		return needsNewRoute;
	}

	public Position getPos() {
		return pos;
	}

	public void setPos(Position pos) {
		this.pos = pos;
	}

	public int getInterval() {
		return interval;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	public static List<Truck> getTrucks() {
		return trucks;
	}

	public static void setTrucks(List<Truck> trucks) {
		Truck.trucks = trucks;
	}

	public static long getNextTruckId() {
		return nextTruckId;
	}

	public static void setNextTruckId(long nextTruckId) {
		Truck.nextTruckId = nextTruckId;
	}	

	public TelemetryBox getTelemetryBoxInexact() {
		return telemetryBoxInexact;
	}

	public TelemetryBox getTelemetryBox() {
		return telemetryBox;
	}

	public double getConsumption() {
		double g = 9.81; // m/s^2
		double rho = 1.293; // kg/m^3
		double croll_min = 0.0045; // %
		double croll_max = 0.0065; // %
		double croll = croll_min + (croll_max - croll_min) * (1.0 - tiresEfficiency); // %
		double slopeAngle = 0; // rad
		double frot = 1.1 + 0.4 * (1.0 - truckCondition); // %
		double br = 0; // N
		double a = acceleration; // m/s^2
		double v = speed; // m/s
		double dt = interval; // s
		int massTotal = massEmpty+massPayload;

		double maxEngineDefectFactor = 0.2; // %
		double specificConsumption = this.specificConsumption;
		specificConsumption = specificConsumption * (1.0 + (maxEngineDefectFactor * (1.0 - engineCondition)));

		double FAir = 0.5 * rho * v * v * surface * cw; // kg*m/s^2
		double FRoll = massTotal * g * croll * Math.cos(slopeAngle);
		double FMountain = massTotal * g * Math.sin(slopeAngle);
		double FAccel = massTotal * a * frot;
		double FTotal = FAir + FRoll + FMountain + FAccel + br;
		double pEngine = FTotal * v * dt; // Ws
		pEngine = pEngine / 3600000.; // kWh

		double consumption = (pEngine * specificConsumption) / (v * dt); // g/m
		consumption = (consumption * 100.) / fuelDensity ; // l/100km
		
		consumption = Math.max(consumption, 0.0);
		return consumption;
	}

	/**
	 * Detects if the truck is entering or leaving a traffic incident.
	 * Uses a simple check for the current distance to the traffic incidents start and end point.
	 * It is assumed that only traffic incidents have been assigned to the truck that are actually
	 * on the trucks route. 
	  */
	private void updateTrafficMode() {
		Iterator<TrafficIncident> iter = incidents.iterator();
		while(iter.hasNext()) {
			TrafficIncident incident = iter.next();
			double distToStart = pos.getDistance(incident.getStart());
			double distToend = pos.getDistance(incident.getEnd());
			
			if(distToStart < speed * interval) {
				enterTraffic(incident);
			}
			if(distToend < speed * interval) {
				leaveTraffic(incident);
				iter.remove();
			}
		}

		if (curIncident == null) {
			if(curRouteSegment < route.getSegmentCount()-1) {
				// check distance to next segment's first point
				RouteSegment nextSegment = route.getSegment(curRouteSegment+1);
				this.currentTargetSpeed = route.getSegment(curRouteSegment).getSpeed();
				this.nextTargetSpeed = nextSegment.getSpeed();
				this.distanceToNextTargetSpeed = pos.getDistance(nextSegment.getPoint(0));
			} else {
				// last segment is reached, so distance until last point is segment is used
				RouteSegment currentSegment = route.getSegment(curRouteSegment);
				this.currentTargetSpeed = route.getSegment(curRouteSegment).getSpeed();
				this.nextTargetSpeed = 0; // end of route
				this.distanceToNextTargetSpeed = pos.getDistance(currentSegment.getLastPoint());
			}
		} else {
			// Incident has started -> reduce speed as fast as possible
			this.currentTargetSpeed = curIncident.getSpeed();
			this.nextTargetSpeed = route.getSegment(curRouteSegment).getSpeed();
			this.distanceToNextTargetSpeed = pos.getDistance(curIncident.getEnd());
		}
	}
	
	private void enterTraffic(TrafficIncident incident) {
		if(curIncident != null && !curIncident.equals(incident)) {
			throw new IllegalStateException("truck is already in a traffic incident. This is likely a bug in the updateTrafficMode method.");
		}
		curIncident = incident;
		LOGGER.info("truck `{0}` has entered traffic `{1}`", id, curIncident);
		if(trafficEventListener != null) {
			trafficEventListener.handleTrafficEvent(this, TruckEventListener.EventType.ENTER_TRAFFIC);
		}
	}
	
	private void leaveTraffic(TrafficIncident incident) {
		LOGGER.info("truck `{0}` has left traffic `{1}`", id, curIncident);
		curIncident = null;
		if(trafficEventListener != null) {
			trafficEventListener.handleTrafficEvent(this, TruckEventListener.EventType.LEAVE_TRAFFIC);
		}
		
	}


	public TrafficIncident getCurIncident() {
		return curIncident;
	}

	public void setCurIncident(TrafficIncident curIncident) {
		this.curIncident = curIncident;
	}

	public List<TrafficIncident> getTrafficIncidents() {
		return incidents;
	}
	
	public void addTrafficIncident(TrafficIncident incident) {
		if(incident == null) {
			throw new IllegalArgumentException("must not be null");
		}
		this.incidents.add(incident);
	}
	
	public String getRouteId() {
		return routeId;
	}

	public void setRouteId(String routeId) {
		this.routeId = routeId;
	}

    public String getTripId() {
		return tripId;
	}

	public String setNewTripId() {
		this.tripId = generateTripId();
		return tripId;
	}

	public boolean hasArrived() {
		if(this.pos != null){
			if(this.route != null){
				if(this.route.getGoal() != null){
					return this.pos.equals(route.getGoal());
				}
				LOGGER.warn("truck route goal is null");
			}
			LOGGER.warn("truck route is null");
		}
		LOGGER.warn("truck pos is null");
		return false;
	}
	
	public TruckEventListener getTrafficEventListener() {
		return trafficEventListener;
	}

	public void setTrafficEventListener(TruckEventListener trafficEventListener) {
		this.trafficEventListener = trafficEventListener;
	}	

}
