package trucksimulation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.io.IOException;

import com.google.gson.Gson;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;

import trucksimulation.trucks.Truck;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Kafka class for publishing (and possibly receiving) messages to Kafka.
 */
public class Kafka {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Kafka.class);
	private static final Boolean logData = false;

	private Vertx vertx;

	private KafkaProducer testProducer;
	private Map<String, KafkaProducer> producers;

	private String adress = "localhost:9092";

	public Kafka(Vertx vertx, String kafkaURI) {
		this.vertx = vertx;
		this.adress = kafkaURI;
		producers = new HashMap<String, KafkaProducer>();
	}

	/**
	 * Creates a new "unassigned" KafkaProducer for testing purposes.
	 */
	public KafkaProducer createJsonProducer(){
		LOGGER.info("createJsonProducer");
		
		Map<String, String> config = new HashMap<>();
		config.put("bootstrap.servers", this.adress);
		
		config.put("key.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer");
		config.put("value.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer");

		// TODO: also create string producer?
		//config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		//config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		
		// This will mean the leader will write the record to its local log but will respond without awaiting full acknowledgement from all followers.
		// Other options: 0 (prod doesn't wait at all), all (waits for all followers to acknowledge the record)
		config.put("acks", "1"); 

		// Create producer for interacting with Apache Kafka
		KafkaProducer<String, String> producer = KafkaProducer.create(vertx, config);

		return producer;
	}

	/**
	 * Create a new KafkaProducer that is assigned to a Truck.
	 * @param adress String of the kafka destination (e.g. localhost:9092)
	 * @param truck
	 */
	public KafkaProducer createProducer(Truck truck){
		
		if (!producers.containsKey(truck.getId())) {
			KafkaProducer producer = createJsonProducer();
			producers.put(truck.getId(), producer);
			return producer;
		
		} else {
			LOGGER.info("Tried to create producer for truck that did not yet exist, returning already existing one");
			return this.producers.get(truck.getId());
		}
		
	}

	/**
	 * Returns the "single" producer that is created for testing.
	 */
	public KafkaProducer getProducer(){
		return this.testProducer;
	}

	/**
	 * Get the KafkaProducer that is assigned to a specific truck.
	 * @param truck
	 */
	 public KafkaProducer getProducer(Truck truck){
		
		if (producers.containsKey(truck.getId())) {
			return this.producers.get(truck.getId());
		} else{
			LOGGER.info("Tried to access producer for truck that did not yet exist");
			createProducer(truck);
			return this.producers.get(truck.getId());
		}
	}

	/**
	 * Sets the "single" producer that is created for testing.
	 */
	public void setProducer(KafkaProducer producer){
		this.testProducer = producer;
	}

	/**
	 * Sets a producer for a specified truck
	 */
	public void addProducer(KafkaProducer producer, Truck truck){
		if (producers.containsKey(truck.getId())) {
			LOGGER.info("Attempted to add producer for truck that already has one");

		} else {
			this.producers.put(truck.getId(), producer);
		}
	}

	/**
	 * Publish a message for a truck
	 * @param topic the kafka topic to publish to
	 * @param msg the message to publish (Json)
	 * @param truck the truck that publishes the message
	 */
	public void publishMessage(String topic, JsonObject msg, Truck truck){
		if (producers.containsKey(truck.getId())) {
			KafkaProducer producer = producers.get(truck.getId());
			KafkaProducerRecord<String, JsonObject> record = KafkaProducerRecord.create(topic, msg);
			producer.write(record);

			if (this.logData == true) {
				LOGGER.info(msg.toString());
				LOGGER.info(record.toString());
			}
		}
	}

	/**
	 * Publish a message by a producer
	 * @param topic the kafka topic to publish to
	 * @param msg the message to publish in json fromat
	 * @param producer the kafka producer
	 */
	public void publishMessage(String topic, JsonObject msg, KafkaProducer producer){
		
		KafkaProducerRecord<String, JsonObject> record = KafkaProducerRecord.create(topic, msg);
		
		if (this.logData == true) {
			LOGGER.info(msg.toString());
			LOGGER.info(record.toString());
		}

		producer.write(record);
	}

	/*
	* A simple method that only closes the producer without removing it from the producers map. Call closeProducer(Truck) instead.
	*/
	public void closeProducer(KafkaProducer producer){
		producer.close();
	}

	/*
	* Closes a producer that is linked to a truck and removes it from the list
	*/
	public void closeProducer(Truck truck){
		if (producers.containsKey(truck.getId())) {
			KafkaProducer producer = producers.get(truck.getId());
			producers.remove(truck.getId());
			producer.close();
		} else {
			LOGGER.info("Attempted to close producer that was not in list");
		}
	}

	/*
	* Closes all producers and empty list
	*/
	public void closeAllProducers(){
		if (!(producers.size() > 0)) {
			
			producers.forEach((key,producer) -> producer.close());
			producers.clear();



			LOGGER.info("Closed all kafka producers");
		}
	}

}