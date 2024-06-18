import requests
import sys

""" migrate ai dictionary set from asterix to skipper """


def migrate_dictionary(site_key, skipper_url, cookie):
    dictionaries = ["stemdict", "stopwords", "mandatory", "synonyms", "multiwords", "excludeTerms"]
    cookies = {"_un_sso_uid": cookie}
    for dictionary in dictionaries:
        dictionary_sync_url = f'{skipper_url}/skipper/site/{site_key}/dictionary/{dictionary}/sync?type=ai'
        response = requests.post(dictionary_sync_url, cookies=cookies)
        if response.status_code != 200:
            print(
                f'Error while calling skipper dictionary sync api for site key: {site_key}, dictionary : {dictionary}, '
                f'reason: {response.text}, status code: {response.status_code}')
    return True


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
- 2nd Argument: Url of the skipper.
- 3rd Argument: Admin cookie for making requests to skipper.
"""

if (len(sys.argv) != 4):
    print(f'Error: Required arguments have not been passed')
    exit()


for site_key in get_site_keys(sys.argv[1]):
    if migrate_dictionary(site_key=site_key, skipper_url=sys.argv[2], cookie=sys.argv[3]):
        print(f'Migration is successful for site key: {site_key}')


print("The script is done executing.")
