'''
This file is a toned down and faster version of the speed layer, a "light" speed (or lightspeed - pun intended) version.
It is responsible for creating a first batch database when no models are yet available.
Therefore, some variables that are required for the machine learning in the batch later are dummys.
The models will continously get better as soon as the normal speed-layer can be employed with the online models.
(once the batch layer has completed a first run on the first batch database)
'''

from pyspark import SparkContext
from pyspark.streaming import StreamingContext
from pyspark.sql import SparkSession
from pyspark.sql.functions import from_json, to_json, struct, col, mean as _mean, lit, first, avg, concat, when, rand
from pyspark.sql.types import StructType, StructField, StringType, DoubleType, DateType, ArrayType, StringType, \
    TimestampType, IntegerType, LongType, BooleanType
from pyspark.sql.types import StructType
from pyspark.sql import Window
from pyspark.sql import functions as F
from pyspark.sql.functions import expr, udf, from_utc_timestamp
from pyspark.sql import SQLContext
from pyspark.ml import PipelineModel

import os, json, pymongo, time
import pandas as pd

import Azure_Storage as AS


os.environ[
    'PYSPARK_SUBMIT_ARGS'] = '--packages org.apache.spark:spark-streaming-kafka-0-8_2.11:2.4.5,org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.5 pyspark-shell'
# os.environ['JAVA_HOME'] = "C:\Program Files\Java\jdk-11.0.2"

mongoConnectionString =  open("mongoConnectionString", 'r').read().split('\n')[0]
mongodb_name = "horizon"
mongodb_collection = "firstbatch"


sc = SparkContext("local[2]", "Sparktest")
spark = SparkSession(sc) \
    .builder.appName("Sparktest") \
    .master("local[4]") \
    .getOrCreate()

# --------------------------------------------------------------------------- #

'''
Input: Initial loading
'''

schema = StructType(
    [
        StructField('truckId', StringType(), True),
        StructField('routeId', StringType(), True),
        StructField('tripId', StringType(), True),
        StructField('truckLP', StringType(), True),
        StructField('consumption', DoubleType(), True),
        StructField('mass', DoubleType(), True),
        StructField('timeStamp', LongType(), True),
        StructField('lat', DoubleType(), True),
        StructField('lon', DoubleType(), True),
        StructField('acceleration', DoubleType(), True),
        StructField('speed', DoubleType(), True),
        StructField('secSinceLast', DoubleType(), True),
        StructField('avgIntervSpeed', DoubleType(), True),
        StructField('speedWarn', IntegerType(), True),
        StructField('brakeWarn', IntegerType(), True),
        StructField('accWarn', IntegerType(), True),
        StructField('engineEff', IntegerType(), True),
        StructField('tireEff', IntegerType(), True),
        StructField('truckCond', IntegerType(), True),
        StructField('incident', BooleanType(), True),
        StructField('roadType', IntegerType(), True),
        StructField('arrivedStr', StringType(), True),
        StructField('arrived', IntegerType(), True),
        StructField('truckYear', IntegerType(), True),
        StructField('truckType', IntegerType(), True),
    ]
)

'''Prod Environment'''
df = spark.readStream.format("kafka") \
    .option("kafka.bootstrap.servers", "kafka:9092") \
    .option("subscribe", "simulation") \
    .load()

df = df.selectExpr("CAST(value AS STRING)")

df = df.withColumn("value", from_json("value", schema)) \
    .select(col('value.*')) \
 \

# --------------------------------------------------------------------------- #

'''
Transformation
'''

df_serve = df \
    .withColumn("speed", F.round(df["speed"], 2)) \
    .withColumn("consumption", F.round(df["consumption"], 2)) \
    .withColumn("acceleration", F.round(df["acceleration"], 2)) \
    .withColumn("meters_intervall", F.round((df["avgIntervSpeed"]) * df["secSinceLast"], 2)) \
    .withColumn("timestamp_s", F.round((df["timeStamp"] / 1000), 0))
    #.fillna({'arrived':0})
    # Get the current weather data (is returned as [code, desc, temp]) and split into three columns

df_serve = df_serve \
    .groupBy('tripId') \
    .agg(F.first("truckId").alias("sd_truckId"),
         F.first("truckLP").alias("sd_truckLP"),
         F.first("routeId").alias("sd_routeId"),
         F.first("mass").alias("sd_mass"),
         F.collect_list('timestamp_s').alias("ts_timeStamp"),
         F.collect_list('lat').alias("ts_lat"),
         F.collect_list('lon').alias("ts_lon"),
         F.collect_list('meters_intervall').alias("ts_meters_intervall"),
         F.collect_list('speed').alias("ts_speed"),
         F.collect_list('acceleration').alias("ts_acceleration"),
         F.collect_list('consumption').alias("ts_consumption"),
         F.collect_list('accWarn').alias("ts_accWarn"),
         F.collect_list('brakeWarn').alias("ts_brakeWarn"),
         F.collect_list('speedWarn').alias("ts_speedWarn"),
         F.collect_list('incident').alias("ts_incident"),
         F.collect_list('tireEff').alias("ts_tireEff"),
         F.collect_list('engineEff').alias("ts_engineEff"),
         F.collect_list('arrived').alias("ts_arrived"),
         F.first('timestamp_s').alias("agg_first_timeStamp"),
         F.last('timestamp_s').alias("agg_last_timeStamp"),
         F.last('lat').alias("agg_last_lat"),
         F.last('lon').alias("agg_last_lon"),
         F.last('consumption').alias("agg_last_consumption"),
         F.last('speed').alias("agg_last_speed"),
         F.last('accWarn').alias("agg_last_accWarn"),
         F.last('brakeWarn').alias("agg_last_brakeWarn"),
         F.last('speedWarn').alias("agg_last_speedWarn"),
         F.last('incident').alias("agg_last_incident"),
         F.last('tireEff').alias("agg_last_tireEff"),
         F.last('engineEff').alias("agg_last_engineEff"),
         F.last('truckCond').alias("agg_last_truckCond"),
         F.last('roadType').alias("agg_last_roadType"),
         F.round(F.avg('speed'), 2).alias("agg_avg_speed"),
         F.last('acceleration').alias("agg_last_acceleration"),
         F.round(F.avg('consumption'), 2).alias("agg_avg_consumption"),
         F.round(F.avg('acceleration'), 2).alias("agg_avg_acceleration"),
         F.last('arrived').alias("last_arrived"),
         F.last('arrivedStr').alias("last_arrivedStr"),
         F.last('truckYear').alias("sd_truck_year"),
         F.last('truckType').alias("sd_truck_type")
         )

@udf("array<integer>")
def PRODUCE_ts_agg_acc_sec(xs, ys):
    if xs and ys:
        temp = [int((x - ys)) for x in xs]
        return temp


@udf("integer")
def PRODUCE_LABEL_agg_acc_sec(xs):
    if xs:
        temp = xs[-1]
        return temp


@udf("array<integer>")
def PRODUCE_ts_agg_acc_meters(xs):
    if xs:
        temp = [int(round(sum(xs[0:i]), 0)) for i in (range(1, len(xs) + 1))]
        return temp


@udf("integer")
def PRODUCE_agg_acc_meters(xs):
    if xs:
        temp = xs[-1]
        return temp


df_serve = df_serve \
    .withColumn("ts_agg_acc_sec", PRODUCE_ts_agg_acc_sec("ts_timeStamp", "agg_first_timeStamp")) \
    .withColumn("agg_acc_sec", PRODUCE_LABEL_agg_acc_sec("ts_agg_acc_sec")) \
    .withColumn("ts_agg_acc_meters", PRODUCE_ts_agg_acc_meters("ts_meters_intervall")) \
    .withColumn("agg_acc_meters", PRODUCE_agg_acc_meters("ts_agg_acc_meters"))


if not os.path.exists("./route_dist.csv"):
    save_path = "."
    cloud_file_name = "route_dist.csv"
    container_name = "horizon"
    AS.download_file(save_path, cloud_file_name, container_name)
route_dist_pd = pd.read_csv("./route_dist.csv")


# Join route dists and truckinfo
sqlCtx = SQLContext(sc)
route_dist = sqlCtx.createDataFrame(route_dist_pd)

route_dist = route_dist.select(route_dist['routeId'].alias('sd_routeId'),
                               F.round(route_dist['dist'], 0).alias("sd_len_route"),
                               route_dist['start'].alias('sd_start'),
                               route_dist['dest'].alias('sd_dest'),
                               )
try:
    df_serve = df_serve.join(route_dist, 'sd_routeId', how='left_outer')
    # Fill nas in case csv does not provide information for a route id
    df_serve = df_serve.fillna({'sd_len_route': 100000.0, 'sd_start': "Start", 'sd_dest': "Destination"})
except Exception as ex:
    df_serve = df_serve.join(route_dist, 'sd_routeId', how='left_outer')
    df_serve = df_serve.withColumn('sd_len_route', lit(100000.0)) \
        .withColumn('sd_start', lit("Start")) \
        .withColumn('sd_dest', lit("Dest"))
    print(ex)


@udf("array<double>")
def PRODUCE_ts_acc_distance_percent(xs, ys):
    if xs and ys:
        temp = [float(round((x / ys), 4)) for x in xs]
        return temp


@udf("double")
def PRODUCE_agg_acc_distance_percent(xs):
    if xs:
        temp = xs[-1]
        return temp


try:
    df_serve = df_serve.withColumn("ts_acc_distance_percent", PRODUCE_ts_acc_distance_percent("ts_agg_acc_meters", "sd_len_route"))
except:
    a = [0.0, 0.0]
    df_serve = df_serve.withColumn("ts_acc_distance_percent", F.array([F.lit(x) for x in a]))
try:
    df_serve = df_serve.withColumn("agg_acc_distance_percent", PRODUCE_agg_acc_distance_percent("ts_acc_distance_percent"))
except:
    df_serve = df_serve.withColumn("agg_acc_distance_percent", lit(0.0))



# ADD DUMMY CLUSTERING RESULT -> Is needed by batch layer
df_serve = df_serve.withColumn("agg_latest_PRED_Clustering", lit(0).cast(DoubleType()))


# --------------------------------------------------------------------------- #
'''
Batch:

# Save ground truth of values into the database
# The database is used by the batch layer for training on all tuples for which the LABEL for the entire trip duration
# is available, which is inserted as soon as the truck arrives

Contains master data    
Contains time series
Contains labels for arrival prediction
'''

df_to_batch = df_serve.select(
    "tripId",
    "sd_truckId",
    "sd_truckLP",
    "sd_routeId",
    "sd_start",
    "sd_dest",
    "sd_mass",
    "ts_timeStamp",
    "ts_lat",
    "ts_lon",
    "ts_accWarn",
    "ts_brakeWarn",
    "ts_speedWarn",
    "ts_incident",
    "agg_last_tireEff",
    "agg_last_engineEff",
    "agg_last_truckCond",
    "agg_last_roadType",
    "ts_meters_intervall",
    "ts_speed",
    "ts_acceleration",
    "ts_consumption",
    "agg_acc_sec",
    "ts_agg_acc_sec",
    "agg_avg_speed",
    "agg_avg_acceleration",
    "agg_avg_consumption",
    "agg_latest_PRED_Clustering",
    "ts_agg_acc_meters",
    "ts_acc_distance_percent",
    "last_arrived",
    "last_arrivedStr",
    "ts_arrived"
)

tempdataframe = pd.DataFrame()


def write_to_mongo(df_to_batch, epoch_id):
    tempdataframe = df_to_batch.toPandas()

    if tempdataframe.empty:
        pass

    else:
        with pymongo.MongoClient(mongoConnectionString) as client:
            mydb = client[mongodb_name]
            sparkcollection = mydb[mongodb_collection]

            df_json = json.loads(tempdataframe.T.to_json()).values()

            sparkcollection.insert_many(df_json)
          
        #EINFÜGEN DES LABELS
        filtered_df_to_batch= df_to_batch.filter(df_to_batch.last_arrivedStr == "arr_true") #[1, "true", True])
        pd_temp_df_old = filtered_df_to_batch.toPandas()
        pd_temp_df_old = pd_temp_df_old[["tripId", "agg_acc_sec"]]

        if pd_temp_df_old.empty:
            pass
        else:
            with pymongo.MongoClient(mongoConnectionString) as client:
                mydb = client[mongodb_name]
                sparkcollection = mydb[mongodb_collection]

                for tuple in pd_temp_df_old.iterrows():
                    temp = tuple[1]
                    temp_tripId = temp["tripId"]
                    temp_LABEL = temp["agg_acc_sec"]

                    sparkcollection.update_many({"tripId": temp_tripId}, \
                                                {"$set": {"LABEL_final_agg_acc_sec": temp_LABEL}})
    pass



df_to_batch_stream = df_to_batch.writeStream \
    .outputMode("Update") \
    .foreachBatch(write_to_mongo) \
    .start()

# Disable the following stream for production:
df_console_stream = df_to_batch \
    .writeStream \
    .outputMode("Update") \
    .format("console") \
    .option("truncate", "true") \
    .start()

# --------------------------------------------------------------------------- #
# Disable the following stream for production:
df_console_stream.awaitTermination()

df_to_batch_stream.awaitTermination()