package trucksimulation;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Random;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.google.gson.Gson;

import helpers.GenerationHelper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;

import trucksimulation.routing.Position;
import trucksimulation.routing.Route;
import trucksimulation.traffic.TrafficIncident;
import trucksimulation.trucks.DestinationArrivedException;
import trucksimulation.trucks.TelemetryData;
import trucksimulation.trucks.Truck;
import trucksimulation.trucks.TruckEventListener;
import trucksimulation.Kafka;

/**
 * Simulation representation.
 */
public class Simulation implements TruckEventListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(Simulation.class);

	private String id;
	private boolean endlessMode = true;
	private Vertx vertx;
	private Map<String, HashSet<Truck>> route2trucksMap = new HashMap<>();
	private List<Truck> trucks = new ArrayList<>();
	private List<Long> timerIds = new ArrayList<>();
	/**
	 * Mapping of truck ids to interval counts.
	 */
	private Map<String, Integer> intervalCount = new HashMap<>();
	private Map<TrafficIncident, List<String>> incident2RoutesMap = new HashMap<>();
	private int truckCount;
	private int incidentCount;
	private Future<Boolean> allRoutesLoaded = Future.future();
	private Future<Boolean> allIncidentsAssigned = Future.future();
	private LocalDateTime startTime;
	/**
	 * Interval in which the trucks' positions should be updated in the simulation.
	 * Is controlled by value in conf.json
	 */
	private long intervalMs = 1000;
	/**
	 * Interval in which messages should be published by the box. Box data will be
	 * sent every {@link #publishInterval} * {@link #intervalMs} ms.
	 * This means that only every {@link #publishInterval} a message "goes through".
	 * Is controlled by value in conf.json
	 */
	private int publishInterval = 120;

	private static boolean endlessOnExistingRoutes = true;

	private static final Boolean generateCSV = false;
	private BufferedWriter bw;
	private static final String pathToCSV = "largeSim_data.csv";
	//private long dataCount = 5000; // deprecated
	private static final String CSV_SEPARATOR = ",";

	private Boolean publishToKafka = false;
	private String kafkaURI;
	private String kafkaTopic;
	private String kafkaArrivalTopic;
	private String kafkaStartTopic;
	private static final Boolean publishToVertx = false;
	private Kafka kafkaManager;

	public Simulation(String simulationId) {
		this.id = simulationId;
	}

	public Simulation(String simulationId, Vertx vertx) {
		this(simulationId);
		this.vertx = vertx;
	}

	/**
	 * Starts the simulation as soon as all routes and incidents have been loaded.
	 * 
	 * Make sure to set the expected number of trucks and incidents and all
	 * incident, truck and route instances, otherwise the simulation won't start.
	 * Also creates kafka producers for every Truck once everything else has loaded.
	 *
	 * @see #setTruckCount(int)
	 * @see #addTruck(Truck)
	 * @see #setIncidentCount(int)
	 * @see #addRoute(String, Route)
	 * @see #addTrafficIncident(TrafficIncident, List)
	 */
	public void start() {
		LOGGER.info("simulation `{0}`: start requested", id);
		try {
			Files.deleteIfExists(Paths.get(pathToCSV));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (this.publishToKafka){
		    this.kafkaManager = new Kafka(this.vertx, this.kafkaURI);
		    LOGGER.info("kafkaManager initialized");
		}
		allIncidentsAssigned.setHandler(h -> {
			allRoutesLoaded.setHandler(j -> {
				LOGGER.info("simulation `{0}`: initialization completed, starting simulation with {1} trucks.", id,
						trucks.size());

				startTime = LocalDateTime.now(ZoneOffset.UTC);
				
				startAllTrucks();
			});
			LOGGER.info("simulation `{0}`: initialization completed, starting simulation with {1} trucks.", id, trucks.size());
			startTime = LocalDateTime.now(ZoneOffset.UTC);
		});
	}

	private void startAllTrucks(){
		int count = 0;

        for (Truck truck : trucks) {
            count += 1;
            LOGGER.debug("Info for truck `{0}` :", count);
            LOGGER.debug("Truck `{0}` has route id `{1}`", truck.getId(), truck.getRouteId());
            LOGGER.debug("Truck `{0}` has position `{1}` , `{2}`", truck.getId(), truck.getPos().getLat(), truck.getPos().getLon());
            LOGGER.debug("Truck `{0}` route has start  position `{1}` , `{2}`", truck.getId(), truck.getRoute().getStart().getLat(), truck.getRoute().getStart().getLon());
        }

		for (Truck truck : trucks) {
			// Creating publishers and setting timings right before they start moving
			truck.setTrafficEventListener(this);
			truck.getTelemetryBox().setPublishInterval(this.publishInterval);
			truck.getTelemetryBoxInexact().setPublishInterval(this.publishInterval);
            if (publishToKafka){
                this.kafkaManager.createProducer(truck);
            }
			long timerId = startMovingDynamic(truck);
			timerIds.add(timerId);
		}
	}

	private long startMovingDynamic(Truck truck){
	    truck.setNewTripId();

	    //KafkaProducer truckProducer = this.kafkaManager.getProducer(truck);
	    //JsonObject startMsg1 = new JsonObject().put("started", truck.getTripId());
	    //this.kafkaManager.publishMessage(kafkaStartTopic, startMsg1, truckProducer);

	    if (generateCSV) startMovingCSV();

	    intervalCount.put(truck.getId(), 0);
		long tId = vertx.setPeriodic(intervalMs, timerId -> {
			try {
				truck.move();

				// Decide to which "services" data is sent
				if (publishToKafka) {
			        //LOGGER.debug("streaming data to kafka");
			        streamBoxData(truck);
		        }
		        if (publishToVertx){
			        //LOGGER.debug("streaming data to vertx");
			        publishBoxData(truck);
		        }
				if (generateCSV){
					writeBoxData(truck);
				}
			} catch(DestinationArrivedException ex) {
				LOGGER.info("truck `{0}` has arrived at destination", truck.getId());
				if(endlessMode) {

                    JsonObject truckMsg = getBaseMessage(truck, Serializer.get());
                    truckMsg.put("roadType", 5);
                    truckMsg.put("arrivedStr", "arr_true");
                    truckMsg.put("arrived", 1);
                    this.kafkaManager.publishMessage("simulation", truckMsg, truck);

					int pauseDur = new Random().nextInt(120);
					truck.pause(pauseDur + 45);

					//KafkaProducer truckProducer = this.kafkaManager.getProducer(truck);
					//JsonObject arrivedMsg = new JsonObject().put("arrived", truck.getTripId());
					//his.kafkaManager.publishMessage(kafkaArrivalTopic, arrivedMsg, truckProducer);

					// TODO: Add probability that new route is a trip without cargo (Leerfahrt)
					if(endlessOnExistingRoutes){
					    assignExistingRoute(truck); // this also sets new trip id

					    //JsonObject startMsg = new JsonObject().put("started", truck.getTripId());
					    //this.kafkaManager.publishMessage(kafkaStartTopic, startMsg, truckProducer);

					} else {
					    LOGGER.info("Simulation is not in endless mode");
					    //assignNewRoute(truck); // this also sets new trip id
					}
				} else {
				    LOGGER.info("Simulation is not in endless mode");
					cancelTimer(timerId);
				}
			} catch (Exception ex) {
				LOGGER.error("truck `{0}`: Unexpected error, stopping", truck.getId(), ex);
				cancelTimer(timerId);
			}
		});
		return tId;
	}

	/**
	 * Deletes, creates and inserts new csv
	 */
	//private long startMovingCSV() {
	private void startMovingCSV() {
		try {
			//intervalCount.put(truck.getId(), 0);
			this.bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathToCSV, true), "UTF-8"));

			// Write header if file is empty yet
			BufferedReader br = new BufferedReader(new FileReader(pathToCSV));
			if (br.readLine() == null) {
				try {
					writeBoxDataHeader(bw);
				} catch (Exception e) {e.printStackTrace();}
			}
			br.close();
        }
        catch (UnsupportedEncodingException e) {e.printStackTrace();}
        catch (FileNotFoundException e){e.printStackTrace();}
        catch (IOException e){e.printStackTrace();}
		return;
	}

    /**
	 * Retrieves a route that starts from the current location and sets it as the new destination for the truck.
	 * If no route can be found, a new random route to a random destination is created (This does not yet work)
	 * @param truck
	 */
	private void assignExistingRoute(Truck truck) {
        List<Route> routes = Route.getAllRoutes();
        List<Route> possibleRoutes = new ArrayList<>();

        for (Route r : routes){
            // compare current position and add to possible future routes
            double r_lat = r.getStart().getLat();
            double r_lon = r.getStart().getLon();
            double t_lat = truck.getRoute().getGoal().getLat();
            double t_lon = truck.getRoute().getGoal().getLon();

            if (r_lat == t_lat && r_lon == t_lon) {
                possibleRoutes.add(r);
            }
        }
        if (possibleRoutes.size() > 0){
            //Select random route from candidates
            Route newRoute = possibleRoutes.get(new Random().nextInt(possibleRoutes.size()));
            truck.setRoute(newRoute);
            truck.setNewTripId();
            LOGGER.info("Truck `{0}` got new route to `{1}` , `{2}`", truck.getId(), newRoute.getGoal().getLat(), newRoute.getGoal().getLon());

        // If no route can be found that fits, generate new route
        // Does not work in the current version of fleetSim for Horizon, disabled for stability
        } else {
        	LOGGER.warn("No existing route could be found for truck `{0}`", truck.getId());
            //assignNewRoute(truck);
            //truck.setNewTripId();
        }
	}

	/**
	 * Retrieves a random city and sets it as the new destination for the truck.
	 * 
	 * @param truck
	 */
	private void assignNewRoute(Truck truck) {
		Gson gson = Serializer.get();
		vertx.eventBus().send(Bus.CITY_SAMPLE.address(), new JsonObject().put("size", 1), (AsyncResult<Message<JsonArray>> repl) -> {
			if(repl.succeeded()) {
				JsonObject city = repl.result().body().getJsonObject(0);
				JsonArray destPos = city.getJsonObject("pos").getJsonArray("coordinates");
				String to = gson.toJson(new Position(destPos.getDouble(1), destPos.getDouble(0)));
				String from = gson.toJson(truck.getPos());
				JsonObject msg = new JsonObject().put("from", new JsonObject(from)).put("to", new JsonObject(to));	
				
				vertx.eventBus().send(Bus.CALC_ROUTE.address(), msg, (AsyncResult<Message<String>> r) -> {
					if(r.succeeded()) {
						Route route = gson.fromJson(r.result().body(), Route.class);
						truck.setRoute(route);
						truck.setNewTripId();
						LOGGER.info("truck `{0}`: new destination is {1}", truck.getId(), city.getString("name"));
					} else {
						// destination not found on map, retry
						LOGGER.warn("truck `{0}`: failed to assign new destination", truck.getId(), r.cause());
						assignNewRoute(truck);
					}
				});	
			} else {
				LOGGER.error("truck `{0}`: failed to assign new destination", truck.getId(), repl.cause());
			}
		});
	}
	
	private void cancelTimer(long timerId) {
		vertx.cancelTimer(timerId);
		this.timerIds.remove(timerId);
		if(!isRunning()) {
			LOGGER.info("simulation `{0}` has ended, all trucks have arrived", id);
			vertx.eventBus().publish(Bus.SIMULATION_ENDED.address(), new JsonObject().put("id", this.id));
		}
	}
	
	private void writeBoxDataHeader(BufferedWriter bw) throws Exception {
		StringBuffer oneLine = new StringBuffer();
		oneLine.append("stream_info");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("truck_id");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("truck_license_plate");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("route_id");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("trip_id");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("truck_mass");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("truck_consumption");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("telemetry_timestamp");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("telemetry_lat");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("telemetry_lon");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("truck_speed");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("truck_acceleration");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("debug_info");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("truck_year");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("truck_surface");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("truck_cw");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("incident");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("road_type");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("truck_type");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("warning_info");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("driver_speed");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("driver_acceleration");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("driver_brake");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("driver_tired");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("truck_condition");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("engine_efficiency");
		oneLine.append(CSV_SEPARATOR);
		oneLine.append("tires_efficiency");
		try {
			bw.write(oneLine.toString());
			bw.newLine();
		} catch (Exception e) {throw e;}
	}

	/**
	 * Writes the correct simulation data into CSV when called.
	 * Writes the deteriorated data into csv at {@link #pathToCSV}
	 * every {@link #publishInterval} calls.
	 * @param truck
	 */
	//private void writeBoxData(Truck truck, BufferedWriter bw) throws Exception {
	private void writeBoxData(Truck truck) throws Exception {
		// TelemetryData correctData = truck.getTelemetryBox().getTelemetryData();

		// only write data if msgInterval is reached
		int ctr = intervalCount.get(truck.getId()) + 1;
		intervalCount.put(truck.getId(), ctr);
		if(ctr % publishInterval == 0) {

            TelemetryData inexactData = truck.getTelemetryBoxInexact().getTelemetryData();

            // Generate the new row in csv
            StringBuffer oneLine = new StringBuffer();
            oneLine.append("stream_info");
            oneLine.append(CSV_SEPARATOR);
            oneLine.append(truck.getId());
            oneLine.append(CSV_SEPARATOR);
		    oneLine.append(truck.getLicensePlate());
		    oneLine.append(CSV_SEPARATOR);
            oneLine.append(truck.getRouteId());
            oneLine.append(CSV_SEPARATOR);
            oneLine.append(truck.getTripId());
            oneLine.append(CSV_SEPARATOR);
            oneLine.append(truck.getMassTotal());
            oneLine.append(CSV_SEPARATOR);
            oneLine.append(GenerationHelper.round(truck.getConsumption(), 2));
            oneLine.append(CSV_SEPARATOR);
            oneLine.append(inexactData.getTimeStamp());
            oneLine.append(CSV_SEPARATOR);
            oneLine.append(inexactData.getPosition().getLat());
            oneLine.append(CSV_SEPARATOR);
            oneLine.append(inexactData.getPosition().getLon());
            oneLine.append(CSV_SEPARATOR);
            oneLine.append(GenerationHelper.round(truck.getSpeed(), 2));
            oneLine.append(CSV_SEPARATOR);
            oneLine.append(GenerationHelper.round(truck.getAcceleration(), 2));
            oneLine.append(CSV_SEPARATOR);
            oneLine.append("debug_info");
            oneLine.append(CSV_SEPARATOR);
            oneLine.append(truck.getYear());
            oneLine.append(CSV_SEPARATOR);
            oneLine.append(GenerationHelper.round(truck.getSurface(), 2));
            oneLine.append(CSV_SEPARATOR);
            oneLine.append(GenerationHelper.round(truck.getCw(), 2));
            oneLine.append(CSV_SEPARATOR);
            oneLine.append(truck.isInIncident());
            oneLine.append(CSV_SEPARATOR);
            oneLine.append(truck.getCurrentRoadType());
            oneLine.append(CSV_SEPARATOR);
            oneLine.append(truck.getTruckType());
            oneLine.append(CSV_SEPARATOR);
            oneLine.append("warning_info");
            oneLine.append(CSV_SEPARATOR);
            oneLine.append(truck.getDriver().getSpeedWarning().getValue());
            oneLine.append(CSV_SEPARATOR);
            oneLine.append(truck.getDriver().getAccelerationWarning().getValue());
            oneLine.append(CSV_SEPARATOR);
            oneLine.append(truck.getDriver().getBrakeWarning().getValue());
            oneLine.append(CSV_SEPARATOR);
            oneLine.append(truck.getDriver().getTiredWarning().getValue());
            oneLine.append(CSV_SEPARATOR);
            oneLine.append(truck.getCondition().getValue());
            oneLine.append(CSV_SEPARATOR);
            oneLine.append(truck.getEngineEfficiency().getValue());
            oneLine.append(CSV_SEPARATOR);
            oneLine.append(truck.getTireEfficiency().getValue());

            try {
                bw.write(oneLine.toString());
                bw.newLine();
            } catch (Exception e) {throw e;}
		}
	}

	/**
	 * Publishes the correct simulation data when called and deteriorated data
	 * every {@link #publishInterval} calls.
	 * @param truck
	 */
	private void publishBoxData(Truck truck) {
		TelemetryData correctData = truck.getTelemetryBox().getTelemetryData();
		Gson gson = Serializer.get();
		JsonObject correctDataJson = new JsonObject(gson.toJson(correctData)).put("truckId", truck.getId());
		vertx.eventBus().publish(Bus.BOX_MSG.address(), correctDataJson);
		
		int ctr = intervalCount.get(truck.getId()) + 1;
		intervalCount.put(truck.getId(), ctr);
		if(ctr % publishInterval == 0) {
			intervalCount.put(truck.getId(), 0);
			TelemetryData inexactData = truck.getTelemetryBoxInexact().getTelemetryData();
			JsonObject dataJson = new JsonObject(gson.toJson(inexactData)).put("truckId", truck.getId());
			vertx.eventBus().publish(Bus.BOX_MSG_DETER.address(), dataJson);
		}
	}

	/**
	 * Publishes the correct simulation data to kafka when called
	 * every {@link #publishInterval} calls.
	 * @param truck
	 */
	private void streamBoxData(Truck truck) {
		TelemetryData correctData = truck.getTelemetryBox().getTelemetryData();
		Gson gson = Serializer.get();
		JsonObject correctDataJson = new JsonObject(gson.toJson(correctData)).put("truckId", truck.getId());

		// Publish the current data to kafka topic
		// this.kafkaManager.publishMessage(this.topic, correctDataJson, truck);
		int ctr = intervalCount.get(truck.getId()) + 1;
		intervalCount.put(truck.getId(), ctr);
		if (ctr % publishInterval == 0) {
			intervalCount.put(truck.getId(), 0);

			JsonObject dataJson = getBaseMessage(truck, gson);

            dataJson.put("roadType", truck.getCurrentRoadType().getValue());
            dataJson.put("arrivedStr", "arr_false");
            dataJson.put("arrived", 0);

            this.kafkaManager.publishMessage("simulation", dataJson, truck);

			// reset the timer in Telemetry box that calculates average speed per interval
			truck.getTelemetryBox().resetInterval();
			truck.getTelemetryBoxInexact().resetInterval();
		}
	}

	@Override
	public void handleTrafficEvent(Truck truck, EventType type) {
		JsonObject truckStateMessage = new JsonObject() //
				.put("truckId", truck.getId()) //
				.put("ts", System.currentTimeMillis()) //
				.put("eventType", type.name());
		vertx.eventBus().publish(Bus.TRUCK_STATE.address(), truckStateMessage);
	}
	
	public void stop() {
		if(vertx == null) {
			throw new IllegalStateException("Simulation obj must be initialized with vertx instance.");
		}
		if (this.kafkaManager != null) {
			this.kafkaManager.closeAllProducers();
		}
		for(long timerId : timerIds) {
			vertx.cancelTimer(timerId);
		}
	}

	public JsonObject getBaseMessage(Truck truck, Gson gson){

        TelemetryData inexactData = truck.getTelemetryBoxInexact().getTelemetryData();
        JsonObject dataJson = new JsonObject(gson.toJson(inexactData));
        // Extend the Telemetry Data with additional information
        dataJson.put("mass", truck.getMassTotal()).put("consumption", truck.getConsumption());
        dataJson.put("lat", inexactData.getPosition().getLat()).put("lon", inexactData.getPosition().getLon());
        dataJson.put("engineEff", truck.getEngineEfficiency().getValue());
        dataJson.put("tireEff", truck.getTireEfficiency().getValue());
        dataJson.put("truckYear", truck.getYear());
        dataJson.put("truckType", truck.getTruckType().getValue());
        // Warnings
        dataJson.put("speedWarn", truck.getDriver().getSpeedWarning().getValue());
        dataJson.put("accWarn", truck.getDriver().getAccelerationWarning().getValue());
        dataJson.put("brakeWarn", truck.getDriver().getBrakeWarning().getValue());
        dataJson.put("incident", truck.isInIncident());
        // Ids
        dataJson.put("routeId", truck.getRouteId()).put("tripId", truck.getTripId());
        dataJson.put("truckLP", truck.getLicensePlate()).put("truckId", truck.getId());

        return dataJson;
	}
	
	
	public void addTruck(Truck truck) {
		this.trucks.add(truck);
		if(!route2trucksMap.containsKey(truck.getRouteId())) {
			route2trucksMap.put(truck.getRouteId(), new HashSet<>());
		}
		this.route2trucksMap.get(truck.getRouteId()).add(truck);
	}
	
	public void removeTruck(Truck truck) {
		trucks.remove(truck);
		route2trucksMap.remove(truck.getRouteId());

		if (this.kafkaManager != null){
			this.kafkaManager.closeProducer(truck);
		}
	}
	
	/**
	 * <p>Assigns a traffic incident object to all trucks which drive on one of the specified routes.
	 * The caller must make sure that the mapping from incident to route is correct as no
	 * additional checks are performed in the simulation object.</p>
	 * 
	 * <p>An assignment to all affected trucks cannot be performed immediately, hence we store 
	 * the incident to routes mapping in the {@link #incident2RoutesMap}.
	 * When all routes have been added, and all incidents have been stored in the map then we can perform
	 * the actual assignment for incidents to trucks.</p>
	 * 
	 * @see #getAllRoutesLoaded()
	 * @see #addRoute(String, Route)
	 * 
	 * @param incident
	 * @param routeIds ids of all routes which are affected by the traffic incident
	 */
	public void addTrafficIncident(TrafficIncident incident, List<String> routeIds) {
		this.incident2RoutesMap.put(incident, routeIds);
		incidentCount--;
		
		// perform the actual assingment when the last incident has been assigned (incidentCount)
		// and all routes are loaded (allRoutesLoaded)
		if(incidentCount == 0) {
			allRoutesLoaded.setHandler(h -> {
				for(Entry<TrafficIncident, List<String>> entry : incident2RoutesMap.entrySet()) {
					List<String> routeIDs = entry.getValue();
					TrafficIncident in = entry.getKey();
					
					for(String routeId : routeIDs) {
						Set<Truck> trucks = route2trucksMap.get(routeId);
						if(trucks != null) {
							trucks.forEach(t -> t.addTrafficIncident(in));
						} else {
							throw new IllegalArgumentException("No trucks with route id " + routeId + " could be found in simulation " + id);
						}
					}				
				}
				LOGGER.info("simulation `{0}`: assignment of traffic incidents completed");
				allIncidentsAssigned.complete();
			});
		}
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setKafkaInfo(String uri, String topic, String arrivalTopic, String startTopic, boolean useKafka){
	    this.kafkaURI = uri;
	    this.kafkaTopic = topic;
	    this.kafkaArrivalTopic = arrivalTopic;
	    this.kafkaStartTopic = startTopic;
	    this.publishToKafka = useKafka;
	}
	
	public boolean isRunning() {
		return timerIds.size() >= 1;
	}

	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}
	
	public void addRoute(String routeId, Route route) {
		Set<Truck> trucks = route2trucksMap.get(routeId);
		if(trucks != null) {
			trucks.forEach(t -> {
				t.setRoute(route);
				truckCount--;
				LOGGER.warn("Assigned truck `{0}` with route `{1}`. New truckCount `{2}`", t.getId(), route.getId(), truckCount);
				if(truckCount == 0) {
					LOGGER.info("simulation `{0}`: Assignment of truck routes completed", id);
					allRoutesLoaded.complete();
				}
			});
		} else {
			LOGGER.warn("simulation `{0}`: Attempted to add route, but simulation has no trucks.", id);
		}
	}

	public Future<Boolean> getAllRoutesLoaded() {
		return allRoutesLoaded;
	}

	public int getTruckCount() {
		return truckCount;
	}

	public void setTruckCount(int truckCount) {
		this.truckCount = truckCount;
	}

	public int getIncidentCount() {
		return incidentCount;
	}

	public void setIncidentCount(int incidentCount) {
		if(incidentCount == 0) {
			LOGGER.info("simulation `{0}`: No traffic incidents expected (incident count was set to 0)", id);
			allIncidentsAssigned.complete();
		}
		this.incidentCount = incidentCount;
	}

	public long getIntervalMs() {
		return intervalMs;
	}

	public void setIntervalMs(long intervalMs) {
		this.intervalMs = intervalMs;
	}

	public int getPublishInterval() {
		return publishInterval;
	}

	public void setPublishInterval(int publishInterval) {
		this.publishInterval = publishInterval;
	}

	public List<Truck> getTrucks() {
		return trucks;
	}

	/**
	 * Returns the time at which the simulation actually started 
	 * (which usually differs from the time when the start method has been called due to async loading of data).
	 * @return start time
	 */
	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}

	public boolean isEndlessMode() {
		return endlessMode;
	}

	public void setEndlessMode(boolean endlessMode) {
		this.endlessMode = endlessMode;
	}

}
