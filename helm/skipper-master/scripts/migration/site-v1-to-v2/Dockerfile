FROM python:3.7.6-slim-buster

RUN apt-get update
RUN pip install --upgrade pip
RUN pip install pymongo
RUN pip --no-cache-dir install --upgrade awscli

WORKDIR /site-migration
COPY . .

CMD ["./migrate_site.sh"]
