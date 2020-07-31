from pyspark import SparkContext
from pyspark.streaming import StreamingContext
from pyspark.sql import SparkSession
from pyspark.sql.types import StructType
from pyspark import SparkContext
from pyspark.streaming import StreamingContext
from pyspark.sql import SparkSession
from pyspark.sql.functions import from_json,to_json,struct,col, mean as _mean, lit, first, avg
from pyspark.sql.types import StructType, StructField, StringType, DoubleType, DateType, ArrayType, StringType, TimestampType, IntegerType, LongType
from pyspark.sql.types import StructType
from pyspark.sql.functions import col, avg
from pyspark.sql import Window
from pyspark.sql import functions as F
from pyspark.sql.functions import expr
from pyspark.sql.functions import udf
from pyspark.sql import SQLContext
from pyspark.sql import SQLContext
from pyspark.ml import PipelineModel
from pyspark.sql.functions import lit
from pyspark.sql.functions import *
from pyspark.sql import Window
from pyspark.sql.types import StructType, StructField, StringType, IntegerType, LongType
from pyspark.ml.clustering import KMeans
from pyspark.ml.evaluation import ClusteringEvaluator
from pyspark.ml.feature import VectorAssembler
from pyspark.ml.feature import StandardScaler
from pyspark.ml.tuning import ParamGridBuilder   
from pyspark.ml import Pipeline 
from pyspark.ml.tuning import CrossValidator   
from pyspark.ml.evaluation import ClusteringEvaluator

import pandas as pd
import numpy as np
import os, uuid, json
import pymongo
import time
import datetime

import AzureStorage as AS

os.environ['PYSPARK_SUBMIT_ARGS'] = '--packages org.apache.spark:spark-streaming-kafka-0-8_2.11:2.4.5,org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.5 pyspark-shell'


sc = SparkContext("local[2]", "Sparktest")
spark= SparkSession(sc)\
        .builder.appName("Sparktest")\
        .master("local[4]")\
        .getOrCreate()

# --------------------------------------------------------------------------- #
# --------------------------------------------------------------------------- #

import ast
from numpy import array
from numpy import hstack

import tensorflow as tf
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM
from tensorflow.keras.layers import Dense

from tensorflow.keras.preprocessing.sequence import pad_sequences

from sklearn.preprocessing import StandardScaler
from sklearn.metrics import mean_absolute_error
from pyspark.sql import SQLContext

df = pd.read_csv("")


'''
LABEL
'''

sqlCtx = SQLContext(sc)
sql_label_agg = sqlCtx.createDataFrame(df)   

sql_label_agg= sql_label_agg \
            .groupBy('sd_routeId') \
            .agg(F.round(F.avg("LABEL_final_agg_acc_sec"), 2).alias("avg_agg_acc_sec"))
            
pandas_label_agg= sql_label_agg.toPandas()




df= df.loc[df.tripId.isin(['Y022WFVBR1', 'GEP5JZUZRY'])]

df= df.sample(frac=1)

ss_features= ["sd_routeId", \
           "sd_mass", \
            "agg_avg_speed", \
           "ts_agg_acc_sec", \
           "ts_agg_acc_meters", \
            "ts_incident", \
           "LABEL_final_agg_acc_sec"]



    
ss_features_ts= [
           "ts_agg_acc_sec", \
           "ts_agg_acc_meters", \
            "ts_incident"]    


def parse_boolean(temp):
    temp= [int(x) for x in temp]
    return temp
 
df_ss= df[ss_features]

samples= df_ss.shape[0]

len_ts= 100

for feature in ss_features_ts:
    df_ss[feature]=df_ss[feature].apply(ast.literal_eval)

  
df_ss["ts_incident"]= df_ss["ts_incident"].apply(parse_boolean)


for feature in ss_features_ts:
    df_ss[feature]= list(pad_sequences(df_ss[feature], padding='post', maxlen=len_ts))
    
df_ss["ts_features"]= df_ss.apply(lambda x: [[a, b, c] for a, b, c \
                                             in zip(x["ts_incident"], \
                                                    x["ts_agg_acc_sec"], \
                                                    x["ts_agg_acc_meters"])], \
                                                    axis=1)
    
df_ss= df_ss.merge(pandas_label_agg, on= "sd_routeId", how= "left")
    
df_ss["LABEL_DIFF"]= df_ss["avg_agg_acc_sec"]-df_ss["LABEL_final_agg_acc_sec"]
    
    

#SAVE
standardize_mean= df_ss["LABEL_DIFF"].mean()
standardize_std= df_ss["LABEL_DIFF"].std()

df_ss["LABEL_DIFF"]= (df_ss["LABEL_DIFF"]-standardize_mean)/standardize_std


input=df_ss["ts_features"].tolist()
output= df_ss["LABEL_DIFF"].tolist()

input= np.reshape(input, (samples,len_ts, 3)).astype(np.float32)
output= np.reshape(output, (samples, 1)).astype(np.float32)


###############
df= df[["tripId", "sd_routeId", "sd_mass", "agg_avg_speed"]]



# define model
model = Sequential()
#model.add(LSTM(50, activation='relu'))
model.add(LSTM(50, activation='relu'))
model.add(Dense(1))
model.compile(optimizer='adam', loss='mse')
# fit model
model.fit(input, output, epochs=20, verbose=0)

pred= model.predict(input)
