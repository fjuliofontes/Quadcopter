import RPi.GPIO as io
import sys, tty, termios, time, os, glob, threading, thread , Queue
from array import *
from bluetooth import *

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

def _quit():
    client_sock.close() # stop bluetooth server
    server_sock.close() # stop bluetooth server
    stopEngines()       # kill the motors if they are on
    io.cleanup()        # cleanup io pins

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

def getData(var1,var2):
    var2.put(client_sock.recv(1024)) # Start decoding data (polling method)

#######################MAIN#############################
server_sock=BluetoothSocket( RFCOMM )                   # Start bluetooth server
server_sock.bind(("",PORT_ANY))                         # config bluetooth port
server_sock.listen(1)                                   # Start listening
port = server_sock.getsockname()[1]                     # Get pi name
uuid = "94f39d29-7d6d-437d-973b-fba39e49d4ee"
advertise_service( server_sock, "QuadPiServer",
                    service_id = uuid,
                    service_classes = [ uuid, SERIAL_PORT_CLASS ],
                    profiles = [ SERIAL_PORT_PROFILE ],
                    #protocols = [ OBEX_UUID ]
                    )
while 1:                                                # Main loop run's forever until a KeyboardInterrupt
    try:
        print "Waiting for connection on RFCOMM channel %d" % port
        client_sock, client_info = server_sock.accept()
        print "Accepted connection from ", client_info # At this point he stablish contact with android app
        run = True                                     # ok let's enter in loop
        threads = []                                   #
        q = Queue.Queue()                              # start a new Queue
    except KeyboardInterrupt:
        print "Aborted"
        run = False                                    # something happened :(
        _quit()                                        # ensure a normal exit
        exit()                                         # exit()

    while run:                                         # Secondary loop, runs just if there is a connected phone
        try:
            t = threading.Thread(target=getData,args=([],q))  # create the objects for the keepalive thread
            threads.append(t)                          #
            t.daemon = True                            # When the main thread stop every thread else also stop
            t.start()                                  # Start a new thread only if there is no other running
            t.join(2)                                  # Try to join the thread with a interval of 2 seconds
            if (t.isAlive()):                          # If no response stop the engines
                stopEngines()
                print "The engines were stopped!"
            while(t.isAlive()):                        # Keep trying getting data
                t.join(1)                              # Wait until get again range allowing ctrl-c
            data = q.get()                             # pick data from queue
            _assert(len(data) != 0)                    # check for bugs, because it's almost impossible fail
            if (data.find("done()") != -1):            # check for a termination signal from app()
                run = False                            # stop the secundary loop
                stopEngines()                          # Stop engines in case motors on, and back again to primary loop
                break
            if(data.find("on") == -1):                     # Ignore keepalive mensages
                #start_time = time.time()                  # if we want to count execution time
                alldata = (data.split("\n"))               # Split the 1024 stored bytes ("\n"-> represents a termination)
                pos = [0, int((len(alldata)-1)/2), len(alldata)-2] # Begin mid and End are the most important mensages
                for i in range(0,3):                       # So execute the most important mensages
                    channels = alldata[pos[i]].split("_")  # Split the channel info ("_"-> represent's channel terminatio)
                    #print alldata[pos[i]]
                    #_assert(len(channels) == 4)           # check if Received all the channels
                    if(len(channels) == 4):                # to provide a better performance we just ignore the channels with less info
                        for x in range(0,4):               # Send the values individualy to all the channels
                            ch_val = channels[x].split(":")# Get the proprely value (":"-> represent's value termination)
                            _assert(ch_val[0] == "CH"+str(x + 1))# check if we are sending to the correct channel
                            ch_val = int(float(ch_val[1])) # convert the value to int
                            _assert((ch_val >= 0) and (ch_val <= 31)) #check if is a valid value, inside the bounds
                            sendValue(ch_val,x)            # send the desired value
                #print("--- %s seconds ---" % (time.time() - start_time)) # print the time needed to the complete process
        except IOError:
            pass
        except KeyboardInterrupt:
            print "Aborted"                            # In case of KeyboardInterrupt
            _quit()                                    # Ensure a safe exit()
            run = False                                # stop the secondary loop ('isnt necessery but ... ')
            exit()                                     # close the program
#########################################################

#################THINGS_THAT_CAN_BE_USEFUL###############
#data = 'WTF!'
#client_sock.send(data)
#print "Sending [%s]" % data
#for x in range(0, 31):
#    for y in range(0,3):
#        sendValue(x,y)
#for x in range(31 , 0 ,-1):
#    for y in range(3,0,-1):
#        sendValue(x,y)
#########################################################
