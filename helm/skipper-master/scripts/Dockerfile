FROM mongo:3.4.22

RUN apt-get update && apt-get install -y curl

ADD *.sh ./

CMD ["./setup.sh"]
