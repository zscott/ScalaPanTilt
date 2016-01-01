#!/bin/sh

docker tag -f scala-pantilt:1.0 gcr.io/strong-pursuit-722/pan-web
gcloud docker push gcr.io/strong-pursuit-722/pan-web
