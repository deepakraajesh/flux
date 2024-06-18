#!/bin/bash

if [ -z $mongo_host ];
then
   echo "mongo host should be provided as command line argument"
   exit 1
else
   echo "mongo host url is ${mongo_host}"
fi

if [ -z $s3_location_for_sites ];
then
   echo "s3 location should be provided as 2nd command line argument"
   exit 1
else
   echo "s3 location is ${s3_location_for_sites}"
fi

aws s3 cp $s3_location_for_sites .
if [ -f "sites.csv" ]; then
   echo "sites.csv file got downloaded correctly"
else
   echo "sites.csv file not downloaded"
   exit 1
fi
python populate_mongo_with_site_data.py $mongo_host 
