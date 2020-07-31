from pykafka import KafkaClient
from pykafka.common import OffsetType

# Process incoming Data
def consume_message():
    client = KafkaClient(hosts="kafka:9092")
    topic = client.topics["frontend"]

    consumer = topic.get_simple_consumer(auto_offset_reset=OffsetType.LATEST, reset_offset_on_start=True)
    for message in consumer:
        if message is not None:
            print(message.value.decode())

consume_message()