FROM adoptopenjdk/openjdk11

ADD src/main/resources/keys/unbxd-marketplace-public-4529b885377c.json /skipper/keys/secret-key.json

ADD target/skipper.jar /skipper/skipper.jar

ADD scripts/start.sh ./

EXPOSE 8338

CMD ["./start.sh"]
