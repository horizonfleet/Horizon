FROM mongo

# Copy files
RUN mkdir -p /usr/src/db
# Copy Database files from a local mongodb (only way to get them to persist in an image)
# TODO: Use an online tar-ed Data folder and unpack inside container for CI
COPY /Data/ /data/db/
COPY mongod.conf /etc/mongod.conf

# Alternatively copy database restore files and run "mongorestore -d trucksimulation /data/trucksimulation/ && exit" manually
# folder "trucksimulation" and "Data" that contain the db restore point will not be uploaded to github

CMD ["mongod","--config","/etc/mongod.conf"]

#RUN nohup mongod --config /etc/mongod.conf
#CMD ["mongorestore","-d","trucksimulation","/data/trucksimulation/"]


