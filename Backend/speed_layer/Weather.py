# author janders

import requests
import time

#api.openweathermap.org/data/2.5/weather?lat={lat}&lon={lon}&appid={your api key}

#0 = Sunny
#1 = Cloudy
#2 = Windy/Storm
#3 = normal Rain
#4 = Thunderstorm
#5 = Nebula
#6 = Snow
#7 = Freezing/Ice
#8 = Night (And clear skies)

api_keys = open("weatherAPIKeys", 'r').read().split('\n')
amount_accessed = 0
amount_keys = len(api_keys)

def get_weather(api_key, lat, lon):
    url = "https://api.openweathermap.org/data/2.5/weather?lat=" + str(lat) + "&lon=" + \
          str(lon) + "&appid=" + str(api_key) + "&units=metric"
    try:
        weather_json = requests.get(url).json()
    except Exception as ex:
        print(ex)

    try:
        weather_id = weather_json["weather"][0]["id"]
        weather_desc = weather_json["weather"][0]["description"]
        temperature = float(weather_json["main"]["temp"])
        sunrise = int(weather_json["sys"]["sunrise"])
        sunset = int(weather_json["sys"]["sunset"])
    except Exception as ex:
        print(ex)
        return [-1, "Invalid response from weather api", -273.15, 0, 0]

    return [weather_id, weather_desc, temperature, sunrise, sunset]


def get_historical_weather(api_key, lat, lon, unix_time):
    url = "https://api.openweathermap.org/data/2.5/onecall/timemachine?lat=" + str(lat) + "&lon=" + \
          str(lon) + "&dt=" + str(unix_time) + "&appid=" + str(api_key) + "&units=metric"
    try:
        weather_json = requests.get(url).json()
    except Exception as ex:
        print(ex)

    try:
        weather_id = weather_json["current"]["weather"][0]["id"]
        weather_desc = weather_json["current"]["weather"][0]["description"]
        temperature = float(weather_json["current"]["temp"])
        sunrise = int(weather_json["current"]["sunrise"])
        sunset = int(weather_json["current"]["sunset"])
    except Exception as ex:
        print(ex)
        return [-1, "Invalid response from weather api, historical limit of 1000/day could have been reached", -273.15, 0, 0]

    return [weather_id, weather_desc, temperature, sunrise, sunset]


def get_weather_for_location(lat, lon, unix_time):

    # By using multiple keys at random it is ensured that the api is never accessed more than 60 times/minute
    global amount_accessed
    api_key = api_keys[amount_accessed % amount_keys]
    amount_accessed += 1
    # If timestamp is older than one hour, get historical data
    if (int(unix_time + 3600) < int(time.time())) :
        # Public api only allows to get historical data for the past 5 days, return error if older timestamp
        if (int(unix_time) < int(time.time()) - 432000):
            return [-1, "timestamp older than 5 days", -273.15]
        else:
            res = get_historical_weather(api_keys[randInt], lat, lon, unix_time)
    else:
        res = get_weather(api_key, lat, lon)

    weather_id =  res[0]
    weather_desc = res[1]
    temperature = res[2]
    sunrise = res[3]
    sunset = res[4]

    if (weather_id >= 199 and weather_id < 300):
        # Thunderstorm
        weather_code = 4
    elif (weather_id >= 299 and weather_id < 600):
        # Drizzle and Rain
        weather_code = 3
    elif (weather_id >= 600 and weather_id < 700):
        # Snow
        weather_code = 6
    elif (weather_id >= 700 and weather_id <= 770):
        # Fog or other visual obstructions
        weather_code = 5
    elif (weather_id >= 770 and weather_id < 800):
        # Storm or tornado
        weather_code = 2
    elif (weather_id == 800):
        # Clear weather
        if (unix_time > sunrise and unix_time < sunset):
            # During daytime
            weather_code = 0
        else:
            # During nighttime
            weather_code = 8
    elif (weather_id >= 800 and weather_id < 900):
        # Cloudy
        weather_code = 1
    else:
        weather_code = -1

    res = " got weather " + str(weather_code) + weather_desc + str(temperature)
    print(res)
    return [weather_code, weather_desc, temperature]
