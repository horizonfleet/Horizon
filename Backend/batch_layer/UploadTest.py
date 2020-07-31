import Azure_Storage as AS


local_zip_path = AS.zip(".", "test", "rzip")
AS.upload_file(local_zip_path, "horizon")