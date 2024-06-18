import requests
import sys
from pymongo import MongoClient

""" script to remove currupted entries in ai synonyms set"""


def clean_up(site_key, mongo_conn):
    # synonyms
    synonyms = mongo_conn.get_database(site_key).get_collection("synonyms-ai.txt")
    corrupt_syn_entries = ["turtle,\\|turtleneck\\|",
                           "1\"tape\\|1\" tape\\|",
                           "3.5mm\\|1/8 in cable,1/8\" cable,3.5,3.5 cable,3.5 mm cable,3.5m,3.5m cable\\|",
                           "2\"kinesiotape\\|2\" kinesio\\|"]
    deleted_rows = 0
    for entry in corrupt_syn_entries:
        delete_result = synonyms.delete_many({"data": entry})
        deleted_rows = deleted_rows + delete_result.deleted_count
    print(f'{deleted_rows} synonym rows deleted for site_key: {site_key}')

    # mandatory
    mandatory = mongo_conn.get_database(site_key).get_collection("mandatory-ai.txt")
    update_result = mandatory.update_many({"data": "heatsinks)"}, {"$set": {"data": "heatsinks"}})
    print(f'{update_result.modified_count} mandatory rows updated for site_key: {site_key}')
    deleted_rows = 0
    for entry in ["suguna daily fresh (inside nilgiris)", "mr meetha ram ( jp nagar )"]:
        delete_result = mandatory.delete_many({"data": entry})
        deleted_rows = deleted_rows + delete_result.deleted_count
    print(f'{deleted_rows} mandatory rows deleted for site_key: {site_key}')


def get_site_keys(gimli_url):
    response = requests.get("%s/api/sites" % gimli_url)
    if response.status_code != 200:
        message = f'Error while fetching siteKeys from gimli reason: {response.text} , status code: {response.status_code}'
        print(message)
        raise ValueError(message)
    return response.json()


"""
Agruments that need to  passed:
- 1st Argument: Url of gimli.
- 2nd Argument: Mongo connection url.
"""

if (len(sys.argv) != 3):
    print(f'Error: Required arguments have not been passed')
    exit()

gimli_url = sys.argv[1]
mongo_host = sys.argv[2]
conn = MongoClient(mongo_host)
for site_key in get_site_keys(gimli_url):
    try:
        clean_up(site_key=site_key, mongo_conn=conn)
    except Exception as e:
        print(e)

print("The script is done executing.")
