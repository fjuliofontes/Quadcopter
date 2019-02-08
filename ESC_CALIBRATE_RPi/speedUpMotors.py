import RPi.GPIO as io
import sys, tty, termios, time, os, glob
from array import *
from bluetooth import *
import time
TERMIOS = termios

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

def _assert(condition):
    if not condition:
        _quit()        # in case of error we need firs to ensure a normal exit()
    assert condition   # after that we now can give the assertion error

def stopEngines():
    sendValue(31,1)         # CH2 = 31
    time.sleep(0.005)       # user delay 5ms
    for i in range(15,32):
        sendValue(i,0)      # CH1 = 15 - > 31
        time.sleep(0.005)   # user delay 5ms
    for i in range(31,14,-1):
        sendValue(i,0)      # CH1 = 31 - > 15
        time.sleep(0.005)   # user delay 5ms

def startEngines():
    sendValue(31,1)         # CH2 = 31
    time.sleep(0.005)       # user delay 5ms
    for i in range(15,-1,-1):
        sendValue(i,0)      # CH1 = 15 - > 0
        time.sleep(0.005)   # user delay 5ms
    for i in range(0,16):
        sendValue(i,0)      # CH1 = 0 - > 15
        time.sleep(0.005)   # user delay 5ms
    pass

def getkey():
    fd = sys.stdin.fileno()
    old_settings = termios.tcgetattr(fd)
    tty.setraw(sys.stdin.fileno())
    ch = sys.stdin.read(1)
    termios.tcsetattr(fd, termios.TCSADRAIN, old_settings)
    return ch

def _exit():
    midPosition()
    io.cleanup()
    exit()

def midPosition():
    sendValue(15 , 0)           # Put the stick in midle position
    sendValue(31 , 1)           # To all the channel's
    sendValue(15 , 2)           # Except channel 1 whose corresponds to the
    sendValue(15 , 3)           # Throttle --> stopEngine

def allZero():
    sendValue(0 , 0)           # Put the stick in low position
    sendValue(0 , 1)           # To all the channel's
    sendValue(0 , 2)           # Avoiding problems
    sendValue(0 , 3)


#######################MAIN#############################
throttle = 31;                  # Start Speed (stoped)
oldthrottle = 31;               # Used to compare new values
midPosition()                   # Stop props
start = False                   # Start flag
while 1:                        # Main loop run's forever until a KeyboardInterrupt
    k = ord(getkey())
    if(k == 3):
        _exit()
    elif(k == 115):             # 's'-> start calibration
        start = not start          # switch mode
        midPosition()           # 0->ch1 15->ch2 15->ch3 15->ch4
    elif(k == 66):              # Arrow up
        if(throttle < 31):      # If already reach the max it's no needed to increment
            throttle = throttle + 1
    elif(k == 65):              # Arrow down
        if(throttle > 0):       # If already reach the zero it's no needed to decrement
            throttle = throttle - 1
    elif(k == 111):             # key = 'o'
        throttle = 0            # Maximum throttle
    elif(k == 112):             # key = 'p'
        throttle = 31           # Minimum position

    if((oldthrottle != throttle) and start):
        _assert((throttle >= 0) and (throttle <= 31))
        sendValue(throttle,1)   # Send the new value of throttle
        oldthrottle = throttle  # Actualize the old value
        print ("Actual Throttle", throttle)

########################################################
