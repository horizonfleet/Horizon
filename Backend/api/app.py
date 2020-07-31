from flask import Flask, render_template, Response, request
from kafka import KafkaConsumer, KafkaProducer
import json
import requests
import threading

app = Flask(__name__)

store = dict()

def update_store():

    consumer = KafkaConsumer("frontend", bootstrap_servers="kafka:9092",
                             value_deserializer=lambda x: json.loads(x.decode("ascii")), auto_offset_reset="earliest")
    for message in consumer:
        data = message.value
        truck_id = data["truck_id"]
        del data["truck_id"]
        store[truck_id] = data

update_store_thread = threading.Thread(target=update_store)
update_store_thread.start()


@app.route("/", methods=["GET"])
def index():
    return "<h1>Hello to Horizon API</h1>"


@app.route("/data", methods=["GET"])
def consumer():

    result = list()

    for truck_id in store.keys():
        data = store[truck_id]
        data["truck_id"] = truck_id
        result.append(data)

    return str(json.dumps(result))


@app.route("/reset",methods=["GET"])
def reset():

    num = len(store.keys())
    store.clear()

    return "<p>Successfully cleared data of "+str(num)+" trucks!</p>"
if __name__ == "__main__":
    app.run(host="0.0.0.0", debug=True, port=6969, use_reloader=False)

