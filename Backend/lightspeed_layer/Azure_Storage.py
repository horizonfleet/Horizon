# @author: janders, drundel

import os, uuid, datetime
# External library needed for file access read/write
from azure.storage.blob import BlobServiceClient, BlobClient, ContainerClient
import zipfile

storageConnectionString =  open("storageConnectionString", 'r').read().split('\n')[0]
container_name = "horizon"
local_path = "."
download_folder = "downloads"
print("Import storageConnection with connectionString ", storageConnectionString)

def zipdir(path, ziph):
    # ziph is zipfile handle
    for root, dirs, files in os.walk(path):
        for file in files:
            ziph.write(os.path.join(root, file))

# upload a file:
def upload_file(local_file_name, container_name):
    try:
        # Create the BlobServiceClient object which will be used to create a container client
        with BlobServiceClient.from_connection_string(storageConnectionString) as service_client:
            # Create a blob client using the local file name as the name for the blob
            with service_client.get_blob_client(container=container_name, blob=local_file_name) as blob_client:
                print("Uploading to Azure Storage as blob:\n\t" + local_file_name)
                # Upload the model
                with open(local_file_name, "rb") as data:
                    blob_client.upload_blob(data)

    except Exception as ex:
        print('Azure Blob Storage Exception:')
        print(ex)


# list all files in directory and get the newest file conforming to
# the following schnema: "substringYY-MM-DD-HH-mm"
def get_newest_file(container_name, substring):
    newest_filename = ""
    i = 0
    try:
        with ContainerClient.from_connection_string(storageConnectionString, container_name) as container_client:
            blob_list = container_client.list_blobs()
            for filename in blob_list:
                if substring in filename.name:
                    i += 1
                    try:
                        timestamp = datetime.datetime.strptime(filename.name[len(substring):], '%Y-%m-%d_%H-%M')
                    except:
                        continue
                    if i == 1:
                        newest_timestamp = timestamp
                        newest_filename = filename.name
                    else:
                        if (newest_timestamp < timestamp):
                            newest_timestamp = timestamp
                            newest_filename = filename.name
    except Exception as ex:
        i = 0
        print(ex)

    return newest_filename, i > 0


def zip(local_path, local_file_name, zip_name):
    local_file_path = os.path.join(local_path, local_file_name)
    local_zip_path = os.path.join(local_path, zip_name)

    zipf = zipfile.ZipFile(local_zip_path, 'w', zipfile.ZIP_DEFLATED)
    zipdir(local_file_path, zipf)
    zipf.close()
    return local_zip_path

def unzip(save_path, filename):
    directory_to_extract_from = os.path.join(save_path, filename)
    with zipfile.ZipFile(directory_to_extract_from, "r") as cluster_model:
        cluster_model.extractall(save_path)
        print("Saving unzipped files at: " + str(save_path))
    return os.path.join(save_path, filename)

## Download a file:
def download_file(save_path, cloud_file_name, container_name):

    blobnames = []
    try:
        with ContainerClient.from_connection_string(storageConnectionString, container_name) as container_client:

            # First check if the file actually exists
            blob_list = container_client.list_blobs()
            for blob in blob_list:
                if (blob.name == cloud_file_name):
                    blobnames.append(blob.name)
            # Download files to specified download folder
            if len(blobnames) == 1:
                for filename in blobnames:
                    with container_client.get_blob_client(filename) as blob_client:
                        with open(os.path.join(save_path, filename), "wb") as file_path:
                            file_path.write(blob_client.download_blob().readall())
                print("Downloaded file: " + str(cloud_file_name))
                return os.path.join(save_path, filename), True
            else:
                print("Could not find requested blob ", str(cloud_file_name), " in the following list:")
                for blob in blob_list:
                    print(blob.name)
                return " ", False
    except Exception as ex:
        print('Azure Blob Storage Exception:')
        print(ex)
        return " ", False