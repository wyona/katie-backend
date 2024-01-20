#!/bin/sh

VOLUME_KATIE=/home/wyona

#VOLUME_KATIE=/Users/michaelwechner/src/wyona/wyona/katie-backend/volume

NAME=askkatie-local-latest

echo "Update Katie ..."

git pull
sh build.sh

docker stop $NAME
docker rm $NAME
docker rmi askkatie-local:latest
docker build -t askkatie-local .
docker run -d --name $NAME -p 6060:8080 -v $VOLUME_KATIE:/ask-katie askkatie-local
