#!/usr/bin/python

import random
import threading
import time
import socket
from math import atan

from Adafruit_PWM_Servo_Driver import PWM


class Servo:
    """A class for controlling a Servo"""

    def _updateServo(self):
        print "update servo loop started..."
        self.count = 0
        while True:
            updatePeriod = 0.001
            k1 = 0.02
            targetRadians = self.targetPos * 3.14 / 100.0
            currentRadians = self.currentPos * 3.14 / 100.0
            deltaRadians = targetRadians - currentRadians

            maxAdjustment = min(abs(self.currentSpeed) + self.acceleration, self.maxSpeed)
            rawAdjustment = -atan(-k1 * deltaRadians)
            if rawAdjustment < 0:
                adjustment = max(rawAdjustment, -maxAdjustment)
            else:
                adjustment = min(rawAdjustment, maxAdjustment)

            self.currentSpeed = adjustment

            newPositionRadians = currentRadians + adjustment
            newPos = newPositionRadians * 100.0 / 3.14
            self.count += 1
            if self.count % 100 == 0:
                self.count = 0
                print "current=%f, target=%f, delta=%f, rawAdjustment=%f, max=%f, adjustment=%f" % (currentRadians, targetRadians, deltaRadians, rawAdjustment, maxAdjustment, adjustment)
                print "oldPos=%f, newPos=%f" % (self.currentPos, newPos)

            self.setServoPosition(newPos)
            time.sleep(updatePeriod)

    def __init__(self, name, pwm, channel, minPulseLength, maxPulseLength, maxSpeed, acceleration):
        self.name = name
        self.pwm = pwm
        self.channel = channel
        self.minPulseLength = minPulseLength
        self.maxPulseLength = maxPulseLength
        self.targetPos = 50.0
        self.currentPos = 50.0
        self.currentSpeed = 0
        self.maxRecordedSpeed = 0
        self.acceleration = acceleration
        self.maxSpeed = maxSpeed
        t = threading.Thread(target=self._updateServo)
        t.daemon = True
        t.start()

    def setServoPosition(self, pos):
        self.currentPos = pos
        pulseWidth = int((self.maxPulseLength - self.minPulseLength) * (pos / 100.0)) + self.minPulseLength
        self.pwm.setPWM(self.channel, 0, pulseWidth)

    def setTargetPosition(self, pos):
        print "new target position: %f" % pos
        self.targetPos = pos


class PanTiltDevice:
    """A class for controlling the PanTilt Device"""
    pwm = PWM(0x40)
    PAN_SERVO_CHANNEL = 0
    TILT_SERVO_CHANNEL = 1

    def __init__(self):
        self.pwm = PWM(0x40)
        self.pwm.setPWMFreq(10)  # Set frequency to 60 Hz
        # self.panServo = Servo("pan", self.pwm, self.PAN_SERVO_CHANNEL, 280, 660, 0.2, 0.003)
        self.panServo = Servo("pan", self.pwm, self.PAN_SERVO_CHANNEL, 150, 700, 0.002, 0.00005)
        self.tiltServo = Servo("tilt", self.pwm, self.TILT_SERVO_CHANNEL, 500, 680, 0.002, 0.00005)

    def pan(self, percent):
        # print "panning to %d percent" % self.panTarget
        self.panServo.setTargetPosition(percent)

    def tilt(self, percent):
        # print "tilting to %d percent" % percent
        self.tiltServo.setTargetPosition(percent)

    def panAndTilt(self, panPercent, tiltPercent):
        self.pan(panPercent)
        self.tilt(tiltPercent)


class RemotePanTiltController:
    def __init__(self, panTilt):
        self.HOST = 'skytrade.io'
        self.PORT = 5000
        self.panTilt = panTilt
        self.client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

        self.client_socket.settimeout(5.0)

        # check and turn on TCP Keepalive
        x = self.client_socket.getsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE)
        if x == 0:
            print 'Socket Keepalive off, turning on'
            x = self.client_socket.setsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE, 1)
            print 'setsockopt=', x
        else:
            print 'Socket Keepalive already on'

    def connect(self):
        self.client_socket.connect((self.HOST, self.PORT))
        print "connected to remote pan and tilt controller %s:%d" % (self.HOST, self.PORT)

    def streamCommands(self):
        msgLen = 2

        while True:
            bytes_recvd = 0
            chunks = []
            while bytes_recvd < msgLen:
                chunk = self.client_socket.recv(min(msgLen - bytes_recvd, 2048))
                if chunk == '':
                    raise RuntimeError("socket connection broken")
                chunks.append(chunk)
                bytes_recvd += len(chunk)
            msg = ''.join(chunks)
            pan = ord(msg[0])
            tilt = ord(msg[1])
            panTilt.panAndTilt(pan, tilt)

booted = False
panTilt = PanTiltDevice()

while True:
    print "Attempting to connect to the mothership..."
    remote = RemotePanTiltController(panTilt)
    try:
        remote.connect()
    except socket.error:
        print "Socket connection failed! Waiting for a bit then retrying..."
        booted = False
        # traceback.print_exc()
        time.sleep(5)
        continue
    print "Connected!"

    if not booted:
        booted = True
        panTilt.panAndTilt(45, 50)
        time.sleep(0.2)
        panTilt.panAndTilt(55, 50)
        time.sleep(0.2)
        panTilt.panAndTilt(50, 50)

    while True:
        try:
            print "streaming commands"
            remote.streamCommands()
        except socket.timeout:
            print "Socket timeout, trying to recv again..."
            break
        except:
            print "Socket error"
            booted = False
            break

    try:
        print "Closing..."
        self.client_socket.close
    except:
        pass
