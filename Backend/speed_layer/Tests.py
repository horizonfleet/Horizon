import os, json, pymongo, time
import pandas as pd
import requests

import Azure_Storage as AS
#import Weather
#from pyspark.ml import PipelineModel


########### ARRIVAL
'''
Test Azure Storage

aggregates_file_name = "arrival_"
container_name = "horizon"

newest_filename, success = AS.get_newest_file(container_name, aggregates_file_name)
print(newest_filename)
print(success)

path_to_downloaded, succeeded = AS.download_file(".", newest_filename, container_name)
print(path_to_downloaded, succeeded)
if success:
    AS.unzip(".", newest_filename)
arrival_path = os.path.join("./CURRENT_ARRIVAL_MODEL", newest_filename)
print(arrival_path)

#model_arrival = PipelineModel.read().load(arrival_path)
'''


######### CLUSTERING:
'''
save_path = "."
cloud_file_name = "cluster"
container_name = "horizon"

newest_filename, newest_filename_success = AS.get_newest_file(container_name, "cluster")
print(newest_filename, newest_filename_success)

AS.download_file(save_path, newest_filename, container_name)
cluster_path = os.path.join("./CURRENT_CLUSTER_MODEL", newest_filename)

filepath, cluster_download_success = AS.download_file(save_path, newest_filename, container_name)
if cluster_download_success:
    cluster_path = AS.unzip(".", filepath)
cluster_path = os.path.join("./CURRENT_CLUSTER_MODEL", newest_filename)

print(cluster_path)
#model_clustering = PipelineModel.read().load(cluster_path)
'''

##### AGGREGATES
'''
aggregates_file_name = "aggregates"
container_name = "horizon"

newest_file, success = AS.get_newest_file(container_name, aggregates_file_name)
print(newest_file)
path_to_downloaded, succeeded = AS.download_file(".", newest_file, container_name)
print(success)
frame = pd.read_csv(path_to_downloaded, sep = ",")
print(frame)
'''



###################################################
'''
Test Weather API
'''
'''
# stresstest
from multiprocessing.dummy import Pool as ThreadPool


pool = ThreadPool(64)
lats = []
lons = []
times = []
for i in range(1000):
    lat = 48.701520997405616
    lon = 10.259312007650113
    lats.append(lat + i/100)
    lons.append(lon + i/100)
    times.append(time.time())
pool.starmap()
'''

'''
# normal test

import time
import Weather
for i in range(5):
    Weather.get_weather_for_location(48.701520997405616, 10.259312007650113, time.time())
    print(Weather.amount_accessed)
    time.sleep(0.5)
'''
