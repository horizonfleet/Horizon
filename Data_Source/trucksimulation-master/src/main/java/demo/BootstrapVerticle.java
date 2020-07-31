package demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.gson.Gson;

import helpers.*;
import enums.*;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;
import trucksimulation.Bus;
import trucksimulation.Serializer;
import trucksimulation.SimulationControllerVerticle;
import trucksimulation.routing.Position;
import trucksimulation.routing.RouteCalculationVerticle;

/**
 * Verticle for performing bootstrapping tasks such as
 * setting up the database and loading initial data.
 *
 */
public class BootstrapVerticle extends AbstractVerticle {
	
	private static final int MIN_GAP_BETWEEN_INCIDENTS = 15000;
	private static final int MAX_TRAFFIC_LENGTH = 5000;
	private static final int MIN_TRAFFIC_LENGTH = 1000;
	private static final int CITY_SAMPLE_SIZE = 100;
	private MongoClient mongo;
	private static final Logger LOGGER = LoggerFactory.getLogger(SimulationControllerVerticle.class);

	List<Integer> randomPlateNumber = IntStream.range(1, 9999).boxed().collect(Collectors.toCollection(ArrayList::new));

	@Override
	public void start() throws Exception {
		int cores = 2; //Runtime.getRuntime().availableProcessors();
		LOGGER.info("Deploying {0} RouteCalculationVerticles", cores - 1);
		DeploymentOptions routeMgrOptions = new DeploymentOptions().setWorker(true).setInstances(cores - 1).setConfig(config());
		
		mongo = MongoClient.createShared(vertx, config().getJsonObject("mongodb", new JsonObject())); //Mongoclient auf DB, der benötigt Server auf den er zugreifen kann 
		
		vertx.deployVerticle(RouteCalculationVerticle.class.getName(), routeMgrOptions, w -> { 
			if (w.failed()) {
				LOGGER.error("Deployment of RouteManager failed." + w.cause());
			} else {
				LOGGER.error("Graphhopper finished");
				//createDemoSimulation(); //Parsen der Openstreet Map über Graphhopper --> ZEIT/SPEICHER --> müsste bereits auf mongodb laden da client erstellt wird
				//createTestSimulation();
				createLargeSimulation();
				//createLargeDemoSimulation();
				Collections.shuffle(randomPlateNumber);
				createDemoSimulation(); //Parsen der Openstreet Map über Graphhopper --> ZEIT/SPEICHER --> müsste bereits auf mongodb laden da client erstellt wird
				indexRoutes();
				indexTraffic();
				indexTrucks();
			}
		});
			
	}

	private void indexRoutes() {
		JsonObject key = new JsonObject().put("segments", "2dsphere").put("simulation", 1);
		JsonObject indexCmd = new JsonObject() //
				.put("createIndexes", "routes") //
				.put("indexes", new JsonArray().add(new JsonObject().put("key", key).put("name", "segments-simulation")));
		
		mongo.runCommand("createIndexes", indexCmd, res-> {
			if(res.succeeded()) {
				LOGGER.info("created index for routes: " + res.result());
			} else {
				LOGGER.error(res.cause());
			}
		});
	}
	
	private void indexTraffic() {
		JsonObject key = new JsonObject().put("start", "2dsphere").put("end", "2dsphere").put("simulation", 1);
		JsonObject indexCmd = new JsonObject() //
				.put("createIndexes", "traffic") //
				.put("indexes", new JsonArray().add(new JsonObject().put("key", key).put("name", "start-end-simulation")));
		mongo.runCommand("createIndexes", indexCmd, res-> {
			if(res.succeeded()) {
				LOGGER.info("created index for traffic: " + res.result());
			} else {
				LOGGER.error(res.cause());
			}
		});
	}
	
	private void indexTrucks() {
		JsonObject key = new JsonObject().put("simulation", 1);
		JsonObject indexCmd = new JsonObject() //
				.put("createIndexes", "trucks") //
				.put("indexes", new JsonArray().add(new JsonObject().put("key", key).put("name", "simulation")));
		mongo.runCommand("createIndexes", indexCmd, res-> {
			if(res.succeeded()) {
				LOGGER.info("created index for trucks: " + res.result());
			} else {
				LOGGER.error(res.cause());
			}
		});
	}


	private void createDemoSimulation() {
		// create a few routes
		Position factoryStuttgart = new Position(48.772510, 9.165465);
		
		Position iphofen = new Position(49.701562, 10.259233);
		Position fischbach = new Position(49.088219, 7.711705);
		Position fridingen = new Position(48.019559, 8.921862);
		Position hamburg = new Position(53.57532, 10.01534);

		mongo.insert("simulations", new JsonObject().put("_id", "thesis").put("description", "small demo simulation with traffic incidents"), sim -> {
			createSimulationData(iphofen, factoryStuttgart, "thesis", true, null);
			createSimulationData(fischbach, factoryStuttgart, "thesis", true, null);
			createSimulationData(fridingen, factoryStuttgart, "thesis", true, null);
		});
	}

	private void createTestSimulation() {
		// create a few routes
		Position stuttgart = new Position(48.772510, 9.165465);
		Position hamburg = new Position(53.57532, 10.01534);
		Position berlin = new Position(52.5233, 13.41377);

        JsonObject demoBig = new JsonObject().put("_id", "smalltest")
				.put("description", "Small test simulation with 3 cities in endless mode")
				.put("endless", true);
		mongo.insert("simulations", new JsonObject().put("_id", "smalltest").put("description", "small demo simulation with traffic incidents"), sim -> {
			createSimulationData(stuttgart, hamburg, "smalltest", true, null);
			createSimulationData(hamburg, berlin, "smalltest", true, null);
			createSimulationData(berlin, stuttgart, "smalltest", true, null);
			createSimulationData(hamburg, stuttgart, "smalltest", true, null);
			createSimulationData(berlin, hamburg, "smalltest", true, null);
			createSimulationData(stuttgart, berlin, "smalltest", true, null);
		});
	}

	private void createLargeDemoSimulation() {
		JsonObject demoBig = new JsonObject().put("_id", "demoBig")
				.put("description", "large demo simulation in endless mode without traffic incidents")
				.put("endless", true);
		mongo.insert("simulations", demoBig, h -> {
			createRandomSimulationData("demoBig");
		});
	}

	private void createLargeSimulation() {
		JsonObject largeSim = new JsonObject().put("_id", "largeSim")
				.put("description", "large demo simulation in endless mode without traffic incidents")
				.put("endless", true);
		mongo.insert("simulations", largeSim, h -> {
			convertLargeSimulation("largeSim");
		});
	}


	/**
	 * Retrieve a random sample of cities from mongodb and use city pairs to create routes, trucks and traffic.
	 * Method is not indempotent and will increase the total number of trucks/routes with each call.
	 *
	 * @param simId name of the simulation
	 */
	private void convertLargeSimulation(String simId) {
        HashMap<String, Position> allPos = new HashMap<String, Position>();
        // Firstly, get all positions a truck can arrive or start from
        try{
            BufferedReader reader = new BufferedReader(new FileReader("fixtures/DE/largeSimCities.json"));
            String currentLine;

            while ((currentLine = reader.readLine()) != null) {
            	JsonObject obj = new JsonObject(currentLine);
                String id = obj.getString("id");
                String name = obj.getString("name");
                JsonArray coords = obj.getJsonObject("pos").getJsonArray("coordinates");
                // create position
                Position city = new Position(coords.getDouble(1), coords.getDouble(0), name, id);
                allPos.put(id, city);
            }
            reader.close();
        } catch(IOException e) {
            e.printStackTrace();
        }

        // Now create all routes
        try{
            BufferedReader reader = new BufferedReader(new FileReader("fixtures/DE/largeSimRoutes.json"));
            String currentLine;
            int routeNum = 0;
            while ((currentLine = reader.readLine()) != null) {
            	routeNum += 1;
                JsonObject obj= new JsonObject(currentLine);
                // get start and end position
                Position start = allPos.get(obj.getString("start"));
                Position dest = allPos.get(obj.getString("dest"));
                // insert position into db
                createSimulationData(start, dest, simId, true, null);
            }
            reader.close();
        } catch(IOException e) {
            LOGGER.warn(e);
        }

	}


	/**
	 * Retrieve a random sample of cities from mongodb and use city pairs to create routes, trucks and traffic.
	 * Method is not indempotent and will increase the total number of trucks/routes with each call.
	 * 
	 * @param simId name of the simulation
	 */
	private void createRandomSimulationData(String simId) {
		
		vertx.eventBus().send(Bus.CITY_SAMPLE.address(), new JsonObject().put("size", CITY_SAMPLE_SIZE), (AsyncResult<Message<JsonArray>> res) -> {
			JsonArray cities = res.result().body();
			for(int i = 0; i + 1 < cities.size(); i += 2) {
				final int requestNo = i/2+1;
				JsonArray startPos = ((JsonObject) cities.getJsonObject(i)).getJsonObject("pos").getJsonArray("coordinates");
				JsonArray destPos = ((JsonObject) cities.getJsonObject(i+1)).getJsonObject("pos").getJsonArray("coordinates");
				Position start = new Position(startPos.getDouble(1), startPos.getDouble(0));
				Position dest = new Position(destPos.getDouble(1), destPos.getDouble(0));
				
				vertx.setTimer(1+i*200, h -> { // throttle to avoid timeouts due to large queue of pending requests
					LOGGER.info("Creating new simulation data {0} of {1}", requestNo, cities.size()/2);
					Future<Void> f = Future.future();
					createSimulationData(start, dest, simId, false, f);
					if(requestNo == cities.size()/2) {
						f.setHandler(c -> {
							LOGGER.info("STOPPING VERTX");
							vertx.close();
						});
					}
				});
			}
		});
	}
	
	/**
	 */
	private void createSimulationData(Position start, Position dest, String simId, boolean createTrafficIncidents, Future<Void> f) {
		Gson gson = Serializer.get();		
		String to = gson.toJson(dest);
		String from = gson.toJson(start);
		JsonObject msg = new JsonObject().put("from", new JsonObject(from)).put("to", new JsonObject(to));
		
		// calculate routes
		vertx.eventBus().send(Bus.CALC_ROUTE.address(), msg, (AsyncResult<Message<String>> rpl) -> {
			if(rpl.succeeded()) {
				JsonObject route = new JsonObject(rpl.result().body());
				route.put("simulation", simId);
				route.put("startName", start.getName());
				route.put("destName", dest.getName());
				
				mongo.insert("routes", route, res -> {
					if(res.succeeded()) {
						LOGGER.info("Inserted new route " + res.result());
						mongo.insert("trucks", createTruck(res.result(), simId), t -> {
							LOGGER.info("created truck " + t.result());
							if(f != null) f.complete();
						});
						
						if(createTrafficIncidents) {
						    Random rand = new Random();
                            int maxIncidentNum = rand.nextInt(4);
							List<JsonObject> incidents = incidentsOnRoute(route, maxIncidentNum);
							for(JsonObject incident : incidents) {
								incident.put("simulation", simId);
								mongo.insert("traffic", incident, t -> {
									LOGGER.info("created traffic incident " + t.result());
								});
							}	
						}
					} else {
						LOGGER.error("Route insertion failed: ", res.cause());
					}
				});
			}
		});
	}
	
	private JsonObject createTruck(String routeId, String simId) {
		JsonObject truck = new JsonObject();
		truck.put("route", routeId);
		truck.put("simulation", simId);

		TruckType truckType = TruckType.from(GenerationHelper.getRandomValue(TruckType.getMinValue(), TruckType.getMaxValue()));
		truck.put("truckType", truckType.getValue());

		int massEmpty = 0;
		if (truckType == TruckType.LOCAL) {
			massEmpty = GenerationHelper.getRandomValue(7000, 9000);
		} else if (truckType == TruckType.LONG_DISTANCE) {
			massEmpty = GenerationHelper.getRandomValue(11000, 15000);
		} else if (truckType == TruckType.LONG_DISTANCE_TRAILER) {
			massEmpty = GenerationHelper.getRandomValue(14000, 20000);
		}
		truck.put("massEmpty", massEmpty);

		double surface = 0.0;
		if (truckType == TruckType.LOCAL) {
			surface = GenerationHelper.getRandomValue(8.2, 8.5);
		} else if (truckType == TruckType.LONG_DISTANCE || truckType == TruckType.LONG_DISTANCE_TRAILER) {
			surface = GenerationHelper.getRandomValue(9.8, 10.2);
		}
		truck.put("surface", surface);

		double cw = 0.0;
		if (truckType == TruckType.LOCAL) {
			cw = GenerationHelper.getRandomValue(0.45, 0.55);
		} else if (truckType == TruckType.LONG_DISTANCE) {
			cw = GenerationHelper.getRandomValue(0.49, 0.59);
		} else if (truckType == TruckType.LONG_DISTANCE_TRAILER) {
			cw = GenerationHelper.getRandomValue(0.55, 0.75);
		}
		truck.put("cw", cw);

		int year = GenerationHelper.getRandomValue(1990, 2020);
		truck.put("year", year);

		String licensePlate = "S" + " " + "HZ" + " " + randomPlateNumber.get(0);
		randomPlateNumber.remove(0);
		truck.put("licensePlate", licensePlate);

		return truck;
	}
	
	private List<JsonObject> incidentsOnRoute(JsonObject route, int max) {

		JsonArray geometries = route.getJsonObject("segments").getJsonArray("geometries");
		JsonArray startCoord, endCoord;
		List<JsonObject> incidents = new ArrayList<>();
		long gapBetweenIncidents = Long.MAX_VALUE;
		
		for(Object geo : geometries) {
			JsonObject geometry = (JsonObject) geo;
			
			if(geometry.getDouble("distance") > MIN_TRAFFIC_LENGTH && geometry.getDouble("distance") < MAX_TRAFFIC_LENGTH && incidents.size() < max && gapBetweenIncidents > MIN_GAP_BETWEEN_INCIDENTS) {
				JsonArray coordinates = geometry.getJsonArray("coordinates");
				startCoord = coordinates.getJsonArray(0);
				endCoord = coordinates.getJsonArray(coordinates.size() - 1);
				JsonObject incident = new JsonObject();
				JsonObject startPos = new JsonObject().put("type", "Point").put("coordinates", startCoord);
				JsonObject endPos = new JsonObject().put("type", "Point").put("coordinates", endCoord);
				
				incident.put("start", startPos);
				incident.put("end", endPos);
				incident.put("active", true);
				incident.put("reported", true);
				incident.put("speed", GenerationHelper.getRandomValue(2.77, 8.33));
				incidents.add(incident);
				
				gapBetweenIncidents = 0;
			} else {
				gapBetweenIncidents += geometry.getDouble("distance");
			}
		}
		return incidents;
	}	
	
}
