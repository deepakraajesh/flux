FROM python:3.7.6-slim-buster

RUN apt-get update

# install dependencies
RUN pip install --upgrade pip
RUN pip install requests
RUN pip install pymongo

WORKDIR /variants
COPY . .

CMD "./variant-migration.sh"
