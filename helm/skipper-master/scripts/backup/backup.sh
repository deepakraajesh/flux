mongo_url="${mongo_url/\/?replicaSet=rs0//skipper?replicaSet=rs0}"

# dump and export skipper
mongodump --uri="$mongo_url"
aws s3 cp --recursive dump "$s3_location/$(date +%F)/"