FROM postman/newman:5-alpine

COPY skipper.postman-collection.json skipper.postman-collection.json
COPY local.postman_environment.json local.postman_environment.json

CMD ["local.postman_environment.json -e local.postman_environment.json"]
