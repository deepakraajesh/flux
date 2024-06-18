import csv
import uuid
from pymongo import MongoClient
import sys

if len(sys.argv) <= 1:
    print("Expected mongo_host should be provided as command line argument")
    exit()

mongo_host = sys.argv[1]
conn = MongoClient(mongo_host)

SITE_FILE = "sites.csv"

with open(SITE_FILE) as csvfile:
    site_reader = csv.reader(csvfile)
    is_header = True
    for row in site_reader:
        if is_header:
            is_header = False
            continue
        site_id = row[0].strip()
        site_key = row[1].strip()
        site_name = row[2].strip()
        owner = row[4].strip()
        tag = row[5].strip()
        language = row[6].strip()
        id = str(uuid.uuid1())
        print(site_key)
        state_data = {
            "siteId": site_id,
            "siteName": site_name,
            "language": language,
            "code": 200,
            "siteKey": site_key,
            "serveState": {"stateType": "INDEXING_STATE"},
            "_id": id
        }
        print(state_data)
        stateColl = conn.get_database(
            "skipper").get_collection("stateCollection")
        stateColl.insert(state_data)
