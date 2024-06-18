echo "Mongo host is $mongoHost"
./setup-pim-workflow.sh $mongoHost
./setup-dim-map.sh $mongoHost
./setup-site-meta.sh
