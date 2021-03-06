# Quadcopter V1

#### This repository contains a modified version of the YMFC-AL project from [Joop Brokking](http://www.brokking.net/ymfc-al_main.html). Since I was a little boy that I develop a strong passion for electronics, and this guy inspired me to create my first quadcopter project. When I first started this project, my background in this area was too small, so I decided to use the code from this guy instead of starting from scratch. My idea was to create a quadcopter, able to be flight from a smartphone using an internet connection so that it would be possible to control it from everywhere. However, I came to discover that an internet connection is a little overkill since there is a lot of lag and jitter introduced. So instead of using an internet connection to control the quadcopter, I decided to Bluetooth instead.  

#### Briefly, in this repository it's possible to find the code that I have developed in C/C++/Android/Java/Python to make this possible.

### Main controller board hardware:
  - 1 x [Raspberry Pi Zero W](https://www.kubii.fr/les-cartes-raspberry-pi/1851-raspberry-pi-zero-w-kubii-3272496006997.html?src=raspberrypi)
  - 1 x [Arduino Uno](https://www.aliexpress.com/item/32665372585.html)
  - 1 x [Arduino Nano](https://www.aliexpress.com/item/32989224656.htm)

### Quadcopter housing hardware:
  - 1 x [450 size frame with integrated power distribution board](https://www.aliexpress.com/item/4000129400366.html)
  - 4 x [1000kV motor / 10x4.5 props / ESC combo](https://www.aliexpress.com/item/2035093137.html)
  - 1 x [3S / 2200mAh / 30C lipo](https://www.dx.com/p/11-1v-2200mah-30c-li-polymer-battery-pack-for-450-helicopter-dji-phantom-1-450-quadcopter-2048977.html#.Xlpl7C2tEWo)
  - 1 x [Battery XT60 connector](https://www.aliexpress.com/item/33061763696.html)
  - 1 x [2S/3S lipo battery charger](https://www.aliexpress.com/item/4000106254839.html)
  
#### Folder description:
  - YMFC-AL_setup -> [Step 4 - Run the setup software](http://www.brokking.net/ymfc-al_main.html)
  - YMFC-AL_esc_calibrate -> [Step 6 - Calibrate the ESC's & Step 7 - balance the motors and props](http://www.brokking.net/ymfc-al_main.html)
  - YMFC-AL_Flight_controller -> [Step 8 - Upload the flight controller software](http://www.brokking.net/ymfc-al_main.html)
  - ESC_CALIBRATE_RPi -> Software needed to be running in Raspberry Pi for Steps 6 and 7.
  - REMOTE_RPi -> Software needed to be running in Raspberry Pi for flight controller.
  - REMOTE_QUADCOPTER -> Software needed to be flashed in arduino nano for making a bridge between the raspberry pi and the arduino uno (that is running the main flight controller alghorithm)
  - REMOTE_POT -> Used during devoloping for test purposes.
  - APP_ANDROID -> Android application responsible to emulate the RF controller.
  - schematic.pdf -> Schematic used for the quadcopter.
  
#### Media:
[Youtube](https://www.youtube.com/watch?v=6dJKzOPGX_o)

![Quadcopter Image](/images/quadcopter.png)


##### Some commands required to fire up the bluetooth in the raspberrry pi:
  * sudo bluetoothctl
  * [bluetooth]# power on
  * [bluetooth]# agent on
  * [bluetooth]# discoverable on
  * [bluetooth]# pairable on
  * [bluetooth]# scan on
