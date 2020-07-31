package trucksimulation.routing;

import java.io.File;

import com.google.gson.Gson;
import com.graphhopper.GraphHopper;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;
import trucksimulation.Bus;
import trucksimulation.Serializer;

public class RouteCalculationVerticle extends AbstractVerticle {
	
	private String osmFile;
	private static final Logger LOGGER = LoggerFactory.getLogger(RouteCalculationVerticle.class);
	private MongoClient mongo;
	private volatile static GraphHopper hopper;

	private static String dockerPathToGraphhopper = "/usr/src/app/.graphhopper";
	private boolean useDocker = false;
	
	@Override
	public void start() throws Exception {
		mongo = MongoClient.createShared(vertx, config().getJsonObject("mongodb", new JsonObject()));
		//JsonObject simConf = config().getJsonObject("simulation", new JsonObject());
		//osmFile = simConf.getString("osmFile", new File("osm", "denmark-latest.osm.pbf").getAbsolutePath());
		//useDocker = config().getJsonObject("simulation", new JsonObject()).getBoolean("usingDocker", false);
		//LOGGER.info("Using osm file " + osmFile + " for route calculations.");

		//loadGraphHopper(osmFile, useDocker);
		//LOGGER.info("graph hopper loaded");
		//vertx.eventBus().consumer(Bus.CALC_ROUTE.address(), this::calcRoute);
		//vertx.eventBus().consumer(Bus.CITY_SAMPLE.address(), this::getCitySample);
	}
	
	private synchronized static void loadGraphHopper(String osmFile, boolean useDocker) {
		if(hopper == null) {
			String userHome = System.getProperty("user.home");
			String ghCacheLocation = new File(userHome, ".graphhopper").getAbsolutePath();
			// manual override in case docker is used
			if (useDocker) ghCacheLocation = dockerPathToGraphhopper;
			LOGGER.info("ghCacheLocation is " + ghCacheLocation + ".");
			hopper = GraphHopperBuilder.get(osmFile, ghCacheLocation);
		} else {
		    LOGGER.info("graph hopper already there");
		}
	}
	
	private void calcRoute(Message<JsonObject> msg) {
		JsonObject from = msg.body().getJsonObject("from");
		JsonObject to = msg.body().getJsonObject("to");
		Gson gson = Serializer.get();
		Position fromPos = gson.fromJson(from.toString(), Position.class);
		Position toPos = gson.fromJson(to.toString(), Position.class);
		
		try {
			Route route = Route.getRoute(hopper, fromPos, toPos);
			msg.reply(gson.toJson(route));
		} catch(Exception ex) {
			LOGGER.error("Route could not be calculated. From " + from + " to " + to, ex);
			msg.fail(500, ex.getMessage());
		}
	}
	
	/**
	 * @param msg can contain a size field to specify sample size
	 */
	private void getCitySample(Message<JsonObject> msg) {
		JsonObject message = msg.body();
		JsonObject sample = new JsonObject().put("$sample", new JsonObject().put("size", message.getInteger("size", 50)));
		JsonObject aggregate = new JsonObject();
		aggregate.put("aggregate", "cities").put("pipeline", new JsonArray().add(sample));
		mongo.runCommand("aggregate", aggregate, res -> {
			if(res.failed()) {
				msg.fail(500, res.cause().getMessage());
			} else {
				JsonArray cities = res.result().getJsonArray("result");
				msg.reply(cities);
			}
		});
	}
}