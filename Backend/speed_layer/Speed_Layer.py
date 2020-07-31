'''
Speed_Layer.py

@author: davidrundel, janders

The speed layer is responsible for (near) real-time data aggregation, enrichment and inference,
as well as ensuring ground-truth data is saved into the batch-database.
'''

import os, json

from pyspark import SparkContext
from pyspark.streaming import StreamingContext
from pyspark.sql import SparkSession
from pyspark.sql.functions import from_json, to_json, struct, col, mean as _mean, lit, first, avg, concat, when, rand
from pyspark.sql.types import StructType, StructField, StringType, DoubleType, DateType, ArrayType, StringType, \
    TimestampType, IntegerType, LongType, BooleanType
from pyspark.sql.types import StructType
from pyspark.sql import functions as F
from pyspark.sql.functions import expr, udf, from_utc_timestamp
from pyspark.sql import SQLContext
from pyspark.ml import PipelineModel

import pandas as pd
import pymongo

import Azure_Storage as AS
import Weather

os.environ[
    'PYSPARK_SUBMIT_ARGS'] = '--packages org.apache.spark:spark-streaming-kafka-0-8_2.11:2.4.5,org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.5 pyspark-shell'
# os.environ['JAVA_HOME'] = "C:\Program Files\Java\jdk-11.0.2"

mongoConnectionString = open("mongoConnectionString", 'r').read().split('\n')[0]
mongodb_name = "horizon"
mongodb_collection = "batch"
enrich_weather_data = True


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

'''Dev Environment'''
# df= spark \
#         .readStream \
#         .option("sep", ",") \
#         .option("header", "true") \
#         .schema(schema) \
#         .csv("path_to_a_csv")

'''Prod Environment'''
df = spark.readStream.format("kafka") \
    .option("kafka.bootstrap.servers", "kafka:9092") \
    .option("subscribe", "simulation") \
    .load()

df = df.selectExpr("CAST(value AS STRING)")

df = df.withColumn("value", from_json("value", schema)) \
    .select(col('value.*')) \

# --------------------------------------------------------------------------- #
'''
Data Enrichment section
Features
 
Master data:
sd_ + tripId truckId truckLP routeId
first_timestamp (for route time calculation)

Time series data:
ts_ + timeStamp, lat, lon, speed, acceleration, consumption, tiresWarning, brakeWarning, speedWarning

Aggregations:
acc_time (sum) # acc_time (sum) #label: last_acc_sum_time with last value
acc_meters (sum)
acc_distance_percent !!! (requires data [length of track] from db)

Most recent values for speedlayer
speed, acceleration, consumption, brakeWarn, speedWarn, tiresWarn,
last_timestamp, first_timestamp, last_lat, last_lon

Additional values for frontend:
avg_speed
avg_consumption
latest_PRED_Clustering
latest_PRED_Estimated_Arrival
'''

df_serve = df \
    .withColumn("speed", F.round(df["speed"], 2)) \
    .withColumn("consumption", F.round(df["consumption"], 2)) \
    .withColumn("acceleration", F.round(df["acceleration"], 2)) \
    .withColumn("meters_intervall", F.round((df["avgIntervSpeed"]) * df["secSinceLast"], 2)) \
    .withColumn("timestamp_s", F.round((df["timeStamp"] / 1000), 0))

# Get the current weather data (is returned as [code, desc, temp]) and split into three columns
@udf("integer")
def get_weather_code_for_location(lat, lon, unix_time):
    result = Weather.get_weather_for_location(lat, lon, unix_time)
    return result[0]

# Weather enrichment
if (enrich_weather_data):
    try:
        df_serve = df_serve \
            .withColumn("weatherCode", get_weather_code_for_location("lat","lon","timeStamp"))
    except:
        df_serve = df_serve.withColumn("weatherCode", lit(0))
else:
    # Turn off weather data enrichment during development to save on api calls
    df_serve = df_serve.withColumn("weatherCode", (F.round(rand()*7, 0)).cast(IntegerType()))

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
         F.last('weatherCode').alias("agg_last_weatherCode"),
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

# TODO: MovingAverage
# df_dashboard= df.withColumn("speed_movAvg", avg("speed") \
#                             .over(Window.orderBy("timeStamp").partitionBy("tripId").rowsBetween(-3,-1)))

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

#---------------------------------------------------------------#
'''
Inference section
'''

#Clustering

save_path = "."
cluster_substring = "cluster"
container_name = "horizon"

newest_filename, newest_filename_success = AS.get_newest_file(container_name, cluster_substring)
# Enter valid fallback cluster-file name here:
backup_cluster = ""

if newest_filename_success:
    if not os.path.exists("./" + newest_filename):
        filepath, cluster_download_success = AS.download_file(save_path, newest_filename, container_name)
        if cluster_download_success:
            cluster_path = AS.unzip(".", filepath)

    cluster_path = os.path.join("./CURRENT_CLUSTER_MODEL", newest_filename)
else:
    if not os.path.exists("./" + backup_cluster):
        filepath, cluster_download_success = AS.download_file(save_path, backup_cluster, container_name)
        if cluster_download_success:
            cluster_path = AS.unzip(".", filepath)
    cluster_path = os.path.join("./CURRENT_CLUSTER_MODEL", backup_cluster)

df_serve = df_serve.fillna({'agg_avg_speed': 0.0, 'agg_avg_acceleration': 0.0, 'agg_avg_consumption': 0.0})
model_clustering = PipelineModel.read().load(cluster_path)

try:
    df_serve = model_clustering.transform(df_serve)
    df_serve = df_serve.withColumnRenamed("prediction", "agg_latest_PRED_Clustering")
except Exception as ex:
    print(ex)
    df_serve = df_serve.withColumn("agg_latest_PRED_Clustering", lit(0).cast(DoubleType()))


# Aggregations on routes

aggregates_file_name = "aggregates"
container_name = "horizon"

aggregates_newest_filename, newest_filename_success = AS.get_newest_file(container_name, aggregates_file_name)
# Enter valid fallback aggregations-file name here:
backup_aggregates = ""

if newest_filename_success:
    if not os.path.exists("./" + aggregates_newest_filename):
        AS.download_file(save_path, aggregates_newest_filename, container_name)
else:
    if not os.path.exists("./" + backup_aggregates):
        AS.download_file(save_path, backup_aggregates, container_name)

aggregates_path = os.path.join(".", aggregates_newest_filename)
aggregates = pd.read_csv(aggregates_path, sep = ",")

sqlCtx = SQLContext(sc)

aggregates = sqlCtx.createDataFrame(aggregates)
aggregates = aggregates.select(aggregates['sd_routeId'],
                               F.round(aggregates['RP_avg_agg_acc_sec'], 2).alias("avg_agg_route_time"),
                               F.round(aggregates['RP_agg_avg_speed'], 2).alias('avg_agg_truck_speed'),
                               F.round(aggregates['RP_agg_avg_acceleration'], 2).alias('avg_agg_truck_acceleration'),
                               F.round(aggregates['RP_agg_avg_consumption'], 2).alias('agg_normal_consumption')
                               )
try:
    df_serve = df_serve.join(aggregates, 'sd_routeId', how='left_outer')
    # We cannot leave these fields empty. If the route does not exist in the batch database and therefore the aggregations file,
    # it gets filled up with dummy values.
    df_serve = df_serve.fillna({'avg_agg_route_time': 15000.0, 'avg_agg_truck_speed': 20.0,
                                'avg_agg_truck_acceleration': 1.0, 'agg_normal_consumption': 10.0})

except Exception as ex:
    df_serve = df_serve.withColumn('avg_agg_route_time', lit(15000.0)) \
            .withColumn('avg_agg_truck_speed', lit(20.0)) \
            .withColumn('avg_agg_truck_acceleration', lit(3.0)) \
            .withColumn('agg_normal_consumption', lit(10.0))
    print(ex)



# Inference of estimated arrival delay
save_path = "."
arrival_file_name = "arrival"
container_name = "horizon"

arrival_newest_filename, newest_filename_success = AS.get_newest_file(container_name, arrival_file_name)
# Enter valid fallback arrival-file name here:
backup_arrival_name = ""

if newest_filename_success:
    if not os.path.exists("./" + arrival_newest_filename):
        filepath, download_success = AS.download_file(save_path, arrival_newest_filename, container_name)
        if download_success:
            AS.unzip(".", filepath)
        arrival_path = os.path.join("./CURRENT_ARRIVAL_MODEL", arrival_newest_filename)
    else:
        arrival_path = os.path.join("./CURRENT_ARRIVAL_MODEL", arrival_newest_filename)

else:
    if not os.path.exists("./" + backup_arrival_name):
        filepath, download_success = AS.download_file(save_path, backup_arrival_name, container_name)
        if download_success:
            AS.unzip(".", filepath)
        arrival_path = os.path.join("./CURRENT_ARRIVAL_MODEL", backup_arrival_name)

try:
    df_serve = df_serve.drop("features")
except:
    pass
try:
    df_serve = df_serve.drop("scaledFeatures")
except:
    pass

try:
    model_arrival = PipelineModel.read().load(arrival_path)
    df_serve = model_arrival.transform(df_serve)
    df_serve = df_serve.withColumnRenamed("prediction", "agg_latest_PRED_Arrival")
    # dummy delay for development
    #df_serve = df_serve.withColumn("agg_latest_PRED_Arrival", F.round(rand() * 60, 2).cast(DoubleType()))
except:
    df_serve = df_serve.withColumn("agg_latest_PRED_Arrival", lit(0).cast(DoubleType()))


# --------------------------------------------------------------------------- #
'''
Batch-db section:

# Save ground truth of values into the database
# The database is used by the batch layer for training on all tuples for which the LABEL for the entire trip duration
# is available, which is inserted as soon as the truck arrives

Contains Stammdaten    
Contains Time Series
Contains Labels for prediction
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
        global mongoConnectionString

        with pymongo.MongoClient(mongoConnectionString) as client:
            mydb = client[mongodb_name]
            sparkcollection = mydb[mongodb_collection]

            df_json = json.loads(tempdataframe.T.to_json()).values()

            try:
                sparkcollection.insert(df_json)
            except Exception as e:
                print(e)
                
          
        #EINFÜGEN DES LABELS
        filtered_df_to_batch= df_to_batch.filter(df_to_batch.last_arrivedStr == "arr_true") #[1, "true", True])
        pd_temp_df_old = filtered_df_to_batch.toPandas()
        pd_temp_df_old = pd_temp_df_old[["tripId", "agg_acc_sec"]]
    

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


# --------------------------------------------------------------------------- #
'''
Frontend section:

The frontend receives:
- ground truth values from serving dataframe / stream
- aggregates
- all predictions
'''

@udf("integer")
def sum_array(xs):
    if xs:
        temp = sum(xs)
        return temp

df_to_frontend = df_serve \
    .withColumn("delay", lit(5)) \
    .withColumn('sum_arrived', sum_array("ts_arrived")) \
    .withColumn("service_interval", (15000 - (500 * (2020 - col("sd_truck_year")))).cast(IntegerType())) \
    .withColumn("next_service", (F.round(rand()*400, 0)).cast(IntegerType()))
df_to_frontend = df_to_frontend \
    .withColumn("int_mass", df_to_frontend["sd_mass"].cast(IntegerType())) \
    .withColumn("agg_first_timeStamp", df_to_frontend["agg_first_timeStamp"].cast(IntegerType())) \
    .withColumn("agg_last_timeStamp", df_to_frontend["agg_last_timeStamp"].cast(IntegerType())) \
    .withColumn("agg_latest_PRED_Arrival", when(col("agg_latest_PRED_Arrival") >= 4400.0, 4399)
                                          .otherwise(df_serve["agg_latest_PRED_Arrival"].cast(IntegerType()))) \
    .withColumn("avg_route_time", df_to_frontend["avg_agg_route_time"].cast(IntegerType())) \
    .withColumn("driver_duration", df_to_frontend["agg_acc_sec"].cast(IntegerType())) \
    .withColumn("agg_acc_distance_percent", (col("agg_acc_distance_percent") * 100).cast(IntegerType()))
df_to_frontend = df_to_frontend.withColumn("calculated_arrival_time",
                                           ((col("agg_first_timeStamp").cast(IntegerType()))
                                            + (col("avg_route_time")).cast(IntegerType())))
df_to_frontend = df_to_frontend \
    .withColumn("road_type", when(col("agg_last_roadType") == 0, "URBAN")
                            .when(col("agg_last_roadType") == 1, "INTERURBAN")
                            .when(col("agg_last_roadType") == 2, "HIGHWAY")
                            .when(col("agg_last_roadType") == 3, "FREEWAY")
                            .when(col("agg_last_roadType") == 5, "TRUCKARRIVED")
                            .otherwise("NO ROAD")) \
    .withColumn("truck_type", when(col("sd_truck_type") == 0, "LOCAL")
                             .when(col("sd_truck_type") == 1, "LONG_DISTANCE")
                             .when(col("sd_truck_type") == 2, "LONG_DISTANCE_TRAILER")
                             .otherwise("NO TRUCK"))
    #.withColumn("driver_duration", str(df_to_frontend["driver_duration_h"]) + str(":") + str(df_to_frontend["driver_duration_m"]))
df_to_frontend = df_to_frontend.select(
    col('tripId').alias("trip_id"),
    col("sd_truckId").alias("truck_id"),
    col("sd_truckLP").alias("number_plate"),
    col("sd_routeId").alias("route_id"),
    col("int_mass").alias("truck_mass"),
    col("sd_start").alias("departure"),
    col("sd_dest").alias("arrival"),
    col("agg_first_timeStamp").alias("departure_time"),
    col("agg_last_timeStamp").alias("telemetry_timestamp"),
    col("calculated_arrival_time").alias("arrival_time"),
    col("agg_last_lat").alias("telemetry_lat"),
    col("agg_last_lon").alias("telemetry_lon"),
    col("agg_last_speed").alias("truck_speed"),
    col("agg_last_consumption").alias("truck_consumption"),
    col("agg_avg_speed").alias("avg_truck_speed"),
    col("agg_last_acceleration").alias("truck_acceleration"),
    col("agg_avg_acceleration").alias("avg_truck_acceleration"),
    col("agg_avg_consumption").alias("normal_consumption"),
    col("agg_acc_distance_percent").alias("route_progress"),
    col("agg_latest_PRED_Clustering").alias("driver_class"),
    col("agg_latest_PRED_Arrival").alias("delay"),
    col("agg_last_accWarn").alias("driver_acceleration"),
    col("agg_last_speedWarn").alias("driver_speed"),
    col("agg_last_brakeWarn").alias("driver_brake"),
    col("agg_last_incident").alias("incident"),
    col("agg_last_truckCond").alias("truck_condition"),
    col("agg_last_tireEff").alias("tires_efficiency"),
    col("agg_last_engineEff").alias("engine_efficiency"),
    col("agg_last_weatherCode").alias("weather"),
    col("sd_truck_year").alias("year"),
    col("last_arrived").alias("arrived"),
    "truck_type",
    "driver_duration",
    "road_type",
    "next_service",
    "service_interval"
    ).filter(df_to_frontend.sum_arrived < 2)


df_to_frontend_stream_console = df_to_frontend \
    .writeStream \
    .outputMode("Update") \
    .format("console") \
    .option("truncate", "true") \
    .start()


df_to_frontend = df_to_frontend.select(
    to_json(struct([col(c).alias(c) for c in df_to_frontend.columns])).alias("value"))

df_to_frontend = df_to_frontend.selectExpr("CAST(value AS STRING)")

df_to_frontend_stream = df_to_frontend \
    .writeStream \
    .format("kafka") \
    .option("kafka.bootstrap.servers", "kafka:9092") \
    .option("topic", "frontend") \
    .outputMode("Complete") \
    .option("checkpointLocation", False) \
    .start()


# --------------------------------------------------------------------------- #
'''
Output
'''

'''Dev Environment'''
df_stream = df_serve \
    .filter(df_serve.last_arrivedStr == "arr_true") \
    .writeStream \
    .outputMode("Update") \
    .format("console") \
    .option("truncate", "true") \
    .start()
   # .select("tripId", "agg_last_weatherCode", "sd_truckLP", "arrived")


'''
Start streams
'''
# Remove these in production environment
df_stream.awaitTermination()
df_to_frontend_stream_console.awaitTermination()

df_to_frontend_stream.awaitTermination()
df_to_batch_stream.awaitTermination()
