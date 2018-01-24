import RPi.GPIO as io
import sys, tty, termios, time, os
from array import *

#######INITS############################################
io.setmode(io.BCM)
io.setwarnings(False)
io.cleanup()
#######################################################

#############################DEFINES####################
set_more_s = 11 # this GPIO pin controls each direction
set_less_s = 5 # combinations like 00 01 10 11
enable_pin = 9 # this enables the arduino reading value
set5 = 13 # more significant bit
set4 = 19 # this will set the value proply
set3 = 6 # 1000 + n*32.258 is the function
set2 = 26 # that decode the sent value
set1 = 10 # less significant bit
#######################################################

###################PINNOUT############################
io.setup(set_more_s,io.OUT) # All pins is outputs
io.setup(set_less_s,io.OUT) # Input will come from
io.setup(enable_pin,io.OUT) # an android app
io.setup(set5,io.OUT)
io.setup(set4,io.OUT)
io.setup(set3,io.OUT)
io.setup(set2,io.OUT)
io.setup(set1,io.OUT)
########################################################

def sendValue(intValue,intCh):
    #set the channel
    io.output(set_more_s, (intCh & 0x02) >> 1) # select channel
    io.output(set_less_s, (intCh & 0x01) >> 0) # select chennel

    #set the value
    io.output(set5, (intValue & 0x10) >> 4) #set the value dec2bin
    io.output(set4, (intValue & 0x08) >> 3)
    io.output(set3, (intValue & 0x04) >> 2)
    io.output(set2, (intValue & 0x02) >> 1)
    io.output(set1, (intValue & 0x01) >> 0)

    #enable
    io.output(enable_pin, 1) # enable arduino for 10ms

    #pause
    time.sleep(0.01) # wait 10ms

    #disable
    io.output(enable_pin, 0) # then disable arduino

#######################MAIN#############################
try:
    while 1:
        for x in range(0, 31):
            for y in range(0,3):
                sendValue(x,y)
        for x in range(31 , 0 ,-1):
            for y in range(3,0,-1):
                sendValue(x,y)
    time.sleep(5)
except KeyboardInterrupt:
    io.cleanup()
#########################################################
