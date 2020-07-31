import pymongo
import pandas
import time

myclient = pymongo.MongoClient("mongodb://mongo:27017/")
mydb = myclient["trucksimulation"]
routes = mydb["routes"]

ids = []
dists = []
starts = []
dests = []
startCoords = []
destCoords = []


for x in routes.find({}, {"volume": "true"}):
    ids.append(x["_id"])
    y = routes.find_one({"_id": x["_id"]})
    dists.append(y["distanceMeters"])
    starts.append(y["startName"])
    dests.append(y["destName"])
    startCoords.append(y["start"])
    destCoords.append(y["goal"])


data = {"routeId": ids, "dist": dists, "start":starts, "dest":dests}
#,"start_x": [item[0] for item in startCoords],
#"start_y": [item[0] for item in startCoords],
#"dest_x": [item[0] for item in startCoords],
#"dest_y": [item[0] for item in startCoords]
# "startCoords":startCoords, "destCoords":destCoords}
df = pandas.DataFrame(data, columns= ["routeId", "dist", "start", "dest"]) #, "startCoords", "destCords"])
df.to_csv("route_dist3.csv", index=False)


trucks = mydb["trucks"]

truckids = []
years = []
masses = []
surfaces = []
truckTypes = []
sims = []
routes = []
LPs = []

for x in trucks.find({}, {"volume": "true"}):
    truckids.append(x["_id"])
    y = trucks.find_one({"_id": x["_id"]})
    years.append(y["year"])
    masses.append(y["massEmpty"])
    surfaces.append(y["surface"])
    truckTypes.append(y["truckType"])
    routes.append(y["route"])
    LPs.append(y["licensePlate"])

data = {"sd_truckId": ids, "year": years, "mass":masses, "surface":surfaces, "truck_type":truckTypes, "initRoute":routes, "sd_truckLP":LPs} #, "startCoords":startCoords, "destCoords":destCoords}
df = pandas.DataFrame(data, columns= ["sd_truckId", "year", "mass", "surface", "truck_type", "initRoute", "sd_truckLP"])
df.to_csv("truckinfo_new.csv", index=False)

# Keep pod up and running to be able to bash and save files
while(True):
    time.sleep(5)
    print("Still running")
