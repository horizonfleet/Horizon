# Pull base image
# maven image of openjdk8 - from buster_curl -> DEBIAN based
FROM maven:3.6.3-jdk-8
#Alternate base img (would require install of maven): mcr.microsoft.com/java/jdk:8-zulu-alpine

# Copy files
RUN mkdir -p /usr/src/app

# Replace this line with git clone for use outside project
# Copy all files for trucksimulation here
COPY . /usr/src/app

#COPY .graphhopper /

WORKDIR /usr/src/app

# Alternative: Download germany_osm_latest to right position instead of copy!

EXPOSE 8080 80 9090
#RUN mvn compile exec:java@bootstrap # does not terminate

#CMD ["mvn", "compile", "exec:java@bootstrap"]
CMD ["mvn", "compile", "exec:java@run"]