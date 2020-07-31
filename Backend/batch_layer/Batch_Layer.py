#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Sun May 24 19:53:23 2020

@author: davidrundel

The batch layer runs as a cronjob in intervals and creates aggregations, a driver cluster model and
a truck delay model and uploads them to Azure Blob Stoage.
"""
import os, time, datetime, ast

from pyspark.sql import SparkSession
from pyspark.sql import functions as F
from pyspark.sql import SQLContext
from pyspark.sql.functions import *
from pyspark.ml.clustering import KMeans
from pyspark.ml.evaluation import ClusteringEvaluator
from pyspark.ml.feature import VectorAssembler
from pyspark.ml.feature import StandardScaler
from pyspark.ml.tuning import ParamGridBuilder   
from pyspark.ml import Pipeline 
from pyspark.ml.tuning import CrossValidator   
from pyspark.ml.evaluation import RegressionEvaluator
from pyspark.ml.regression import GBTRegressor
from pyspark.ml.feature import StringIndexer

import pandas as pd
import pymongo

import Azure_Storage as AS


# --------------------------------------------------------------------------- #
# --------------------------------------------------------------------------- #
'''
Initialize Spark-Project and Environment
'''

os.environ['PYSPARK_SUBMIT_ARGS'] = '--packages org.apache.spark:spark-streaming-kafka-0-8_2.11:2.4.5,org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.5 pyspark-shell'

try:
    os.mkdir("./CURRENT_CLUSTER_MODEL")
except:
    print("Already statisfied")
 
    
try:    
    os.mkdir("./CURRENT_AGGREGATES")
except:
    print("Already statisfied")
    
    
try:    
    os.mkdir("./CURRENT_ARRIVAL_MODEL")
except:
    print("Already statisfied")
    

sc = SparkContext("local[2]", "Sparktest")
spark = SparkSession(sc)\
        .builder.appName("Sparktest")\
        .master("local[4]")\
        .getOrCreate()
sqlCtx = SQLContext(sc)

Upload = True


# --------------------------------------------------------------------------- #
# --------------------------------------------------------------------------- #
'''
Fetch Data from Mongo-DB or local file
'''
mode = "prod"

database_name = "horizon"
collection_name = "firstbatch"

train_examples = 3000


def get_batch(train_examples, save_to_disk):
    mongoConnectionString = open("mongoConnectionString", 'r').read().split('\n')[0]

    with pymongo.MongoClient(mongoConnectionString) as client:
        mydb = client[database_name]
        sparkcollection = mydb[collection_name]
        
        trips= {}
        pulled_amount = 0
        for x in sparkcollection.find({}, {"volume": "true"}):
            if pulled_amount > train_examples:
                break
            pulled_amount += 1
            trips[x.get("_id")] = sparkcollection.find_one(x)

    pd_df_trips = pd.DataFrame.from_dict(trips, orient='index')

    # Ensure that only finished trips are used for learning
    pd_df_trips = pd_df_trips[pd_df_trips['LABEL_final_agg_acc_sec'].notnull()]

    pd_df_trips[["_id", "tripId", "sd_truckId", "sd_truckLP", "sd_routeId", "sd_start", "sd_dest"]] = \
        pd_df_trips[["_id", "tripId", "sd_truckId", "sd_truckLP", "sd_routeId", "sd_start", "sd_dest"]].astype(str)

    if save_to_disk:
        pd_df_trips.to_csv("pd_df_trips.csv")


if mode == "prod":
    get_batch(train_examples, False)
if mode == "dev":
    if os.path.exists("pd_df_trips.csv"):
        pd_df_trips = pd.read_csv("pd_df_trips.csv")
    else:
        get_batch(train_examples, True)
        pd_df_trips = pd.read_csv("pd_df_trips.csv")


# --------------------------------------------------------------------------- #
# --------------------------------------------------------------------------- #
'''
Aggregate Historical Data for Delta-Computations in Speed-Layer
Write to Blob-Storage
'''

sql_df_trips = sqlCtx.createDataFrame(pd_df_trips) 

sql_label_agg = sql_df_trips 

sql_label_agg= sql_label_agg \
            .groupBy('tripId') \
            .agg(F.round(F.last("LABEL_final_agg_acc_sec"), 2).alias("temp_avg_agg_acc_sec"), \
                 F.round(F.last("agg_avg_speed"), 2).alias("temp_agg_avg_speed"), \
                 F.round(F.last("agg_avg_acceleration"), 2).alias("temp_agg_avg_acceleration"), \
                 F.round(F.last("agg_avg_consumption"), 2).alias("temp_agg_avg_consumption"), \
                 F.last("sd_routeId").alias("sd_routeId"))
            
sql_label_agg= sql_label_agg \
            .groupBy('sd_routeId') \
            .agg(F.round(F.avg("temp_avg_agg_acc_sec"), 2).alias("RP_avg_agg_acc_sec"), \
                 F.round(F.avg("temp_agg_avg_speed"), 2).alias("RP_agg_avg_speed"), \
                 F.round(F.avg("temp_agg_avg_acceleration"), 2).alias("RP_agg_avg_acceleration"), \
                 F.round(F.avg("temp_agg_avg_consumption"), 2).alias("RP_agg_avg_consumption"))
                
pandas_label_agg= sql_label_agg.toPandas()        
   

#Write to Blob-Storage                    
timestamp = datetime.datetime.fromtimestamp(time.time()).strftime('%Y-%m-%d_%H-%M')
local_file_name = "aggregates" + timestamp
container_name= "horizon"

try:
    pandas_label_agg.to_csv(local_file_name)
except Exception as e:
    print('Azure Blob Storage Exception:')
    print(e)
  
if Upload is True:     
    AS.upload_file(local_file_name, container_name)


# --------------------------------------------------------------------------- #
# --------------------------------------------------------------------------- #
'''
Clustering Model
Standardization, Pipeline, Hyperparameter-Tuning, Cross-Validation
Write to Blob-Storage
'''

#Insert corresponding Label
sql_df_trips= sql_df_trips.join(sql_label_agg, how= "left", on= "sd_routeId")

sql_df_trips= sql_df_trips.withColumn("label", \
                                      F.round((sql_df_trips.RP_avg_agg_acc_sec - \
                                               sql_df_trips.LABEL_final_agg_acc_sec), 2))

    
#Produce input column
@udf("integer")
def PRODUCE_last_value(xs):
    if xs:
        temp= xs
        try:
            temp = ast.literal_eval(xs)
        except:
            temp = [0]
        res = temp[-1]
        return res


    
@udf("double")
def PRODUCE_last_value_double(xs):
    if xs:
        temp= xs
        try:
            temp = ast.literal_eval(xs)
        except:
            temp = [0.0]
        res = temp[-1]
        return res

sql_df_trips= sql_df_trips.withColumn("agg_acc_meters", \
                                      PRODUCE_last_value("ts_agg_acc_meters"))
    

sql_df_trips= sql_df_trips.withColumn("agg_acc_distance_percent", \
                                      PRODUCE_last_value_double("ts_acc_distance_percent"))

sql_df_trips= sql_df_trips.withColumn("agg_last_accWarn", \
                                      PRODUCE_last_value("ts_accWarn")) \
                            .withColumn("agg_last_brakeWarn", \
                                      PRODUCE_last_value("ts_brakeWarn")) \
                            .withColumn("agg_last_speedWarn", \
                                      PRODUCE_last_value("ts_speedWarn"))


#Build Pipeline
CL_vectorAssembler = VectorAssembler() \
      .setInputCols(["agg_avg_speed", \
                     "agg_avg_acceleration", \
                     "agg_avg_consumption"]) \
      .setOutputCol("features") \
      .setHandleInvalid("skip")
    

CL_scaler = StandardScaler(inputCol="features", outputCol="scaledFeatures",
                        withStd=True, withMean=False)


CL_kmeans = KMeans().setK(3).setSeed(1).setFeaturesCol("scaledFeatures")


CL_paramGrid = ParamGridBuilder() \
    .addGrid(CL_kmeans.k, [3, 4, 5]) \
    .build()


CL_pipeline= Pipeline().setStages([CL_vectorAssembler, CL_scaler, CL_kmeans])


CL_crossval = CrossValidator(estimator=CL_pipeline,
                          estimatorParamMaps=CL_paramGrid,
                          evaluator=ClusteringEvaluator(),
                          numFolds=5)


CL_crossval = CL_crossval.fit(sql_df_trips)
CL_best_model = CL_crossval.bestModel


 
#Write to Blob-Storage  
local_path = "./CURRENT_CLUSTER_MODEL"
timestamp = datetime.datetime.fromtimestamp(time.time()).strftime('%Y-%m-%d_%H-%M')
local_file_name = "cluster" + str(timestamp)
local_dir_path = os.path.join(local_path, local_file_name)
container_name= "horizon"

try:
    CL_best_model.write().overwrite().save(local_dir_path)
except Exception as e:
    print('Azure Blob Storage Exception:')
    print(e)
        
if Upload is True: 
    local_zip_path = AS.zip_here(local_dir_path, local_file_name)
    
    AS.upload_file(local_zip_path, container_name)


# --------------------------------------------------------------------------- #
# --------------------------------------------------------------------------- #
'''
Gradient Boosted Tree Regressor
Standardization, Pipeline, Hyperparameter-Tuning, Cross-Validation
Write to Blob-Storage
'''

EA_indexer1 = StringIndexer(inputCol="sd_truckId", outputCol="sd_truckId_ind", handleInvalid="skip")
#indexed= EA_indexer1.fit(sql_df_trips).transform(sql_df_trips)

EA_indexer2 = StringIndexer(inputCol="sd_routeId", outputCol="sd_routeId_ind", handleInvalid="skip")
#indexed= EA_indexer2.fit(indexed).transform(indexed)

EA_vectorAssembler = VectorAssembler()\
    .setInputCols(["sd_truckId_ind",
                     "sd_mass",
                    "sd_routeId_ind",
                    "agg_avg_speed",
                    "agg_avg_acceleration",
                    "agg_acc_meters",
                    "agg_acc_sec",
                    #"agg_acc_distance_percent", \
                    "agg_last_tireEff",
                    "agg_last_engineEff",
                    "agg_last_truckCond",
                    "agg_last_roadType",
                    "agg_last_accWarn",
                    "agg_last_brakeWarn",
                    "agg_last_speedWarn"])\
    .setOutputCol("features")\
    .setHandleInvalid("skip")

EA_gbt = GBTRegressor(labelCol="label", featuresCol="features")
#indexed= EA_gbt.fit(indexed)


#Best Parameters:
EA_paramGrid = ParamGridBuilder() \
    .addGrid(EA_gbt.maxDepth, [5]) \
    .addGrid(EA_gbt.maxIter, [100]) \
    .addGrid(EA_gbt.stepSize, [0.01]) \
    .build()

# Alternatively perform a grid search
# EA_paramGrid = ParamGridBuilder() \
#     .addGrid(EA_gbt.maxDepth, [3, 5, 7]) \
#     .addGrid(EA_gbt.maxIter, [10, 20, 50, 100]) \
#     .addGrid(EA_gbt.stepSize, [0.001, 0.01, 0.1, 1]) \
#     .build()

EA_pipeline= Pipeline().setStages([EA_indexer1, EA_indexer2, EA_vectorAssembler, \
                                   EA_gbt])


EA_crossval = CrossValidator(estimator=EA_pipeline,
                          estimatorParamMaps=EA_paramGrid,
                          evaluator=RegressionEvaluator(),
                          numFolds=5)


EA_crossval = EA_crossval.fit(sql_df_trips)
EA_best_model = EA_crossval.bestModel


#Write to Blob-Storage  
local_path = "./CURRENT_ARRIVAL_MODEL"
timestamp = datetime.datetime.fromtimestamp(time.time()).strftime('%Y-%m-%d_%H-%M')
local_file_name = "arrival" + str(timestamp)
local_dir_path = os.path.join(local_path, local_file_name)
container_name= "horizon"

try:
    EA_best_model.write().overwrite().save(local_dir_path)
except Exception as e:
    print('Azure Blob Storage Exception:')
    print(e)

if Upload is True:     
    local_zip_path = AS.zip_here(local_dir_path, local_file_name)
    
    AS.upload_file(local_zip_path, container_name)
