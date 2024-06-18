import requests
import sys
from pymongo import MongoClient

""" 
Agruments pass:
- 1st Argument: Url of gimli.
- 2nd Argument: Url of the brewer.
- 3rd Argument: Mongo host.
"""

def migrateVariantInfo(siteKey,  brewerUrl, mongoHost):

    # Fetching Variants enable status from brewer
    brewerUrlForVariant = brewerUrl + "/api/" + siteKey + "/config"
    response = requests.get(brewerUrlForVariant)

    if (response.status_code != 200) :
        print(f'Error while fetching variants info from brewer for site key: {siteKey} reason: {response.text} Status code: {response.status_code}')
        return False


    # Setting skipper to that status.
    brewer_response = response.json()
    if("rulesets" in brewer_response \
        and "default" in brewer_response["rulesets"] \
        and "configSet" in brewer_response["rulesets"]["default"] \
        and "variants" in brewer_response["rulesets"]["default"]["configSet"]\
        and "value" in brewer_response["rulesets"]["default"]["configSet"]["variants"]):
        value = brewer_response["rulesets"]["default"]["configSet"]["variants"]["value"]
        variantValue = value[0] if type(value) is list and len(value) == 1 else "false"
    else:
        print(f'Variant value is not present in brewer config for site Key: {siteKey}')
        variantValue = "false"

    variantValue = True if variantValue == "true" else False
    # Update variant configuration in mongo
    conn = MongoClient(mongoHost)
    stateColl = conn.get_database("skipper").get_collection("stateCollection")
    stateColl.update({"siteKey":siteKey}, {"$set":{"variantsEnabled":variantValue}})   


    return True


def getSiteKeys(gimli_url):
    response = requests.get("%s/api/sites" % gimli_url)
    if (response.status_code != 200):
        print("Error while fetching siteKeys from gimli reason:" + response.text )
        return 
    return response.json()

    

if (len(sys.argv) != 4):
    print(f'Error: Required arguments have not been passed')
    exit()

for siteKey in getSiteKeys(sys.argv[1]):
    if migrateVariantInfo(siteKey, sys.argv[2], sys.argv[3]):
        print(f'Migration is successful for site key: {siteKey}')

print("The script is done executing.")


