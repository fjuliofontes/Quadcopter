//Declaring Variables
byte last_channel_1, last_channel_2, last_channel_3, last_channel_4,last_channel_5;
int receiver_input_channel_1, receiver_input_channel_2, receiver_input_channel_3, receiver_input_channel_4,receiver_input_channel_5;
int receiver_input_channel_1_f, receiver_input_channel_2_f, receiver_input_channel_3_f, receiver_input_channel_4_f;
int receiver_decision_1,receiver_decision_2;
int last_decision_1,last_decision_2,last_decision_0;
int counter_channel_1, counter_channel_2, counter_channel_3, counter_channel_4, start;
unsigned long timer_channel_1, timer_channel_2, timer_channel_3, timer_channel_4, esc_timer, esc_loop_timer;
unsigned long zero_timer, timer_1, timer_2, timer_3, timer_4, current_time;
int first_time=0, change_ch_enable=0;

void pciSetup(byte pin)
{
    *digitalPinToPCMSK(pin) |= bit (digitalPinToPCMSKbit(pin));  // enable pin
    PCIFR  |= bit (digitalPinToPCICRbit(pin)); // clear any outstanding interrupt
    PCICR  |= bit (digitalPinToPCICRbit(pin)); // enable interrupt for the group
}


//Setup routine
void setup(){
  //Serial.begin(9600);
  DDRD |= B11110000;                                 //Configure digital poort 4, 5, 6 and 7 as output
  //Arduino Uno pins default to inputs, so they don't need to be explicitly declared as inputs
  pinMode(13,INPUT);

  PCICR |= (1 << PCIE0);                             // set PCIE0 to enable PCMSK0 scan
  PCMSK0 |= (1 << PCINT0);                           // set PCINT0 (digital input 8) to trigger an interrupt on state change
  PCMSK0 |= (1 << PCINT1);                           // set PCINT1 (digital input 9)to trigger an interrupt on state change
  PCMSK0 |= (1 << PCINT2);                           // set PCINT2 (digital input 10)to trigger an interrupt on state change
  PCMSK0 |= (1 << PCINT3);                           // set PCINT3 (digital input 11)to trigger an interrupt on state change
  PCMSK0 |= (1 << PCINT4);                           // set PCINT3 (digital input 12)to trigger an interrupt on state change
  PCMSK0 |= (1 << PCINT5);                           // set PCINT3 (digital input 13)to trigger an interrupt on state change
  pciSetup(3); pciSetup(2);                          // set PCINT18 (digital input 3) to trigger an interrupt on state change
  zero_timer = micros();                             //Set the zero_timer for the first loop.
}

//Main program loop
void loop(){
  while(zero_timer + 4000 > micros()){
    if(first_time && change_ch_enable){
      switch ((receiver_decision_2<<1) | receiver_decision_1) {
        case 0:
          receiver_input_channel_1_f=(int)((( receiver_input_channel_5 << 4) | ( receiver_input_channel_4 << 3) | ( receiver_input_channel_3 << 2) | ( receiver_input_channel_2 << 1) | receiver_input_channel_1)*(500/15)) + 1000;
          break;
        case 1:
          receiver_input_channel_2_f=(int)((( receiver_input_channel_5 << 4) | ( receiver_input_channel_4 << 3) | ( receiver_input_channel_3 << 2) | ( receiver_input_channel_2 << 1) | receiver_input_channel_1)*(500/15)) + 1000;
          break;
        case 2:
          receiver_input_channel_3_f=(int)((( receiver_input_channel_5 << 4) | ( receiver_input_channel_4 << 3) | ( receiver_input_channel_3 << 2) | ( receiver_input_channel_2 << 1) | receiver_input_channel_1)*(500/15)) + 1000;
          break;
        case 3:
          receiver_input_channel_4_f=(int)((( receiver_input_channel_5 << 4) | ( receiver_input_channel_4 << 3) | ( receiver_input_channel_3 << 2) | ( receiver_input_channel_2 << 1) | receiver_input_channel_1)*(500/15)) + 1000;
          break;
      }
      first_time=0;
    }
  };                       //Start the pulse after 4000 micro seconds.
  first_time=1;
  zero_timer = micros();                                     //Reset the zero timer.
  PORTD |= B11110000;
  timer_channel_1 = receiver_input_channel_1_f + zero_timer;   //Calculate the time when digital port 8 is set low.
  timer_channel_2 = receiver_input_channel_2_f + zero_timer;   //Calculate the time when digital port 9 is set low.
  timer_channel_3 = receiver_input_channel_3_f + zero_timer;   //Calculate the time when digital port 10 is set low.
  timer_channel_4 = receiver_input_channel_4_f + zero_timer;   //Calculate the time when digital port 11 is set low.

  while(PORTD >= 16){                                        //Execute the loop until digital port 8 til 11 is low.
    esc_loop_timer = micros();                               //Check the current time.
    if(timer_channel_1 <= esc_loop_timer)PORTD &= B11101111; //When the delay time is expired, digital port 8 is set low.
    if(timer_channel_2 <= esc_loop_timer)PORTD &= B11011111; //When the delay time is expired, digital port 9 is set low.
    if(timer_channel_3 <= esc_loop_timer)PORTD &= B10111111; //When the delay time is expired, digital port 10 is set low.
    if(timer_channel_4 <= esc_loop_timer)PORTD &= B01111111; //When the delay time is expired, digital port 11 is set low.
  }
}

//This routine is called every time input 8, 9, 10 or 11 changed state
ISR (PCINT2_vect){
  //D1 =========================================
  if(PIND & B00000100){                                        //Is input 2 high?
    if(last_decision_1 == 0){                                   //Input 2 changed from 0 to 1
      last_decision_1 = 1;                                      //Remember current input state
      receiver_decision_1=last_decision_1;                 //Set timer_1 to current_time
    }
  }
  else if(last_decision_1 == 1){                                //Input 2 is not high and changed from 1 to 0
    last_decision_1 = 0;                                        //Remember current input state
    receiver_decision_1 = last_decision_1;                 //Channel 1 is current_time - timer_1
  }
  //D2 =========================================
  if(PIND & B00001000 ){                                       //Is input 3 high?
    if(last_decision_2 == 0){                                   //Input 3 changed from 0 to 1
      last_decision_2 = 1;                                      //Remember current input state
      receiver_decision_2 = last_decision_2;                                  //Set timer_2 to current_time
    }
  }
  else if(last_decision_2 == 1){                                //Input 3 is not high and changed from 1 to 0
    last_decision_2 = 0;                                        //Remember current input state
    receiver_decision_2 = last_decision_2;         //Channel 2 is current_time - timer_2
  }

}


ISR(PCINT0_vect){
  //D0 =========================================
  if(PINB & B00100000 ){                                        //Is input 13 high?
    if(last_decision_0 == 0){                                   //Input 13 changed from 0 to 1
      last_decision_0 = 1;                                      //Remember current input state
      change_ch_enable = last_decision_0;                       //Set timer_2 to current_time
    }
  }
  else if(last_decision_0 == 1){                                //Input 13 is not high and changed from 1 to 0
    last_decision_0 = 0;                                        //Remember current input state
    change_ch_enable = last_decision_0;
  }
  //Channel 1=========================================
  if(PINB & B00000001){                                        //Is input 8 high?
    if(last_channel_1 == 0){                                   //Input 8 changed from 0 to 1
      last_channel_1 = 1;                                      //Remember current input state
      receiver_input_channel_1=last_channel_1;                 //Set timer_1 to current_time
    }
  }
  else if(last_channel_1 == 1){                                //Input 8 is not high and changed from 1 to 0
    last_channel_1 = 0;                                        //Remember current input state
    receiver_input_channel_1 = last_channel_1;                 //Channel 1 is current_time - timer_1
  }
  //Channel 2=========================================
  if(PINB & B00000010 ){                                       //Is input 9 high?
    if(last_channel_2 == 0){                                   //Input 9 changed from 0 to 1
      last_channel_2 = 1;                                      //Remember current input state
      receiver_input_channel_2 = last_channel_2;                                  //Set timer_2 to current_time
    }
  }
  else if(last_channel_2 == 1){                                //Input 9 is not high and changed from 1 to 0
    last_channel_2 = 0;                                        //Remember current input state
    receiver_input_channel_2 = last_channel_2;         //Channel 2 is current_time - timer_2
  }
  //Channel 3=========================================
  if(PINB & B00000100 ){                                       //Is input 10 high?
    if(last_channel_3 == 0){                                   //Input 10 changed from 0 to 1
      last_channel_3 = 1;                                      //Remember current input state
      receiver_input_channel_3 = last_channel_3;
    }
  }
  else if(last_channel_3 == 1){                                //Input 10 is not high and changed from 1 to 0
    last_channel_3 = 0;                                        //Remember current input state
    receiver_input_channel_3 = last_channel_3;
  }
  //Channel 4=========================================
  if(PINB & B00001000 ){                                       //Is input 11 high?
    if(last_channel_4 == 0){                                   //Input 11 changed from 0 to 1
      last_channel_4 = 1;                                      //Remember current input state
      receiver_input_channel_4 = last_channel_4;
    }
  }
  else if(last_channel_4 == 1){                                //Input 11 is not high and changed from 1 to 0
    last_channel_4 = 0;                                        //Remember current input state
    receiver_input_channel_4 = last_channel_4;
  }
  //Channel 5=========================================
  if(PINB & B00010000 ){                                       //Is input 12 high?
    if(last_channel_5 == 0){                                   //Input 12 changed from 0 to 1
      last_channel_5 = 1;                                      //Remember current input state
      receiver_input_channel_5 = last_channel_5;
    }
  }
  else if(last_channel_5 == 1){                                //Input 12 is not high and changed from 1 to 0
    last_channel_5 = 0;                                        //Remember current input state
    receiver_input_channel_5 = last_channel_5;
  }
}
