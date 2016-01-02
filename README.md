# ScalaPanTilt

ScalaPanTilt is a web based application for controlling a remote pan and tilt device. The purpose is to give
GTalk, Skype, or FaceTime users enhanced telepresence by allowing them to control a remote camera or iPhone
attached to a pan and tilt device.

## The Device

### What is a pan and tilt device?

### Where can I get one?

## The Architecture

## Building and Deploying

## Install Tools

### gcloud

### SBT

### Docker

### Fleetctl

#### How do I install docker?

#### How do I build the scala-pantilt docker image?

```>sbt docker:publishLocal

#### How do I deploy to Google Compute Engine?

First, compile and publish the docker image locally.
```>sbt docker:publishLocal

Second, run dockerize.sh to tag the image and push it to google's docker repository.
```PROJECT_HOME/gcloud >./dockerize.sh



