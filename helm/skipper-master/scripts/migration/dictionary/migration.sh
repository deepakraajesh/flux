#!/bin/bash

echo "start clean_up"
python clean_up.py $gimli_url $mongo_host
echo "clean_up done !! starting migration now"
python migration.py $gimli_url $skipper_url $admin_cookie
echo "migration done"

