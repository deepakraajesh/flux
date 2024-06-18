FROM mongo:5.0.9
RUN apt-get update
RUN apt-get install -y awscli
ADD backup.sh .
CMD ["bash","backup.sh"]