
README
------

docker-compose is executed from remote by Jenkins, but if you want to run "docker-compose up" manually for testing, then please make sure to set DOCKER_IMAGE_TAG, for example

export DOCKER_IMAGE_TAG=michaelwechner/wyona:katie-1.349.0

(run 'docker images' to see available docker images)
