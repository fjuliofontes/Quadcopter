int pin13=0,pin12=0,pin11=0,pin10=0,pin9=0;
int x1,x2,y1,y2;
// the setup function runs once when you press reset or power the board
void setup() {
  // initialize digital pin 13 as an output.
  pinMode(13, OUTPUT);
  pinMode(12, OUTPUT);
  pinMode(11, OUTPUT);
  pinMode(10, OUTPUT);

  pinMode(9, OUTPUT);
  pinMode(8, OUTPUT);
  pinMode(7, OUTPUT);
  pinMode(6, OUTPUT);

}


void sendCM(int val, int channel){
  switch (channel - 1) {
    case 0:
      //CHANNEL 1
      digitalWrite(7,LOW);
      digitalWrite(6,LOW);
      break;
    case 1:
      //CHANNEL 2
      digitalWrite(6,LOW);
      digitalWrite(7,HIGH);
      break;
    case 2:
      //CHANNEL 3
      digitalWrite(7,LOW);
      digitalWrite(6,HIGH);
      break;
    case 3:
      //CHANNEL 4
      digitalWrite(7,HIGH);
      digitalWrite(6,HIGH);
      break;
  }


  pin13 = (val>>4) & (0x1);
  pin12 = (val>>3) & (0x1);
  pin11 = (val>>2) & (0x1);
  pin10 = (val>>1) & (0x1);
  pin9 = val & (0x1);
  PORTB |= ( pin9 << 5) | ( pin10 << 4) | ( pin11 << 3) | ( pin12 << 2) | ( pin13 << 1) | 0 ; // SET PIN'S ON
  PORTB &= ( pin9 << 5) | ( pin10 << 4) | ( pin11 << 3) | ( pin12 << 2) | ( pin13 << 1) | 1 ; // SET PIN'S OFF

  digitalWrite(8, HIGH); //Enable
  delay(10);
  digitalWrite(8, LOW); //Disable

}

// the loop function runs over and over again forever
void loop() {


/*
  for(i=0;i<32;i++){
    pin13 = (i>>4) & (0x1);
    pin12 = (i>>3) & (0x1);
    pin11 = (i>>2) & (0x1);
    pin10 = (i>>1) & (0x1);
    pin9 = i & (0x1);

    PORTB |= ( pin9 << 5) | ( pin10 << 4) | ( pin11 << 3) | ( pin12 << 2) | ( pin13 << 1) | 0 ; // SET PIN'S ON
    PORTB &= ( pin9 << 5) | ( pin10 << 4) | ( pin11 << 3) | ( pin12 << 2) | ( pin13 << 1) | 1 ; // SET PIN'S OFF

    delay(200);
  }

  for(i=16;i>32;i--){
    pin13 = (i>>4) & (0x1);
    pin12 = (i>>3) & (0x1);
    pin11 = (i>>2) & (0x1);
    pin10 = (i>>1) & (0x1);
    pin9 = i & (0x1);

    PORTB |= ( pin9 << 5) | ( pin10 << 4) | ( pin11 << 3) | ( pin12 << 2) | ( pin13 << 1) | 0 ; // SET PIN'S ON
    PORTB &= ( pin9 << 5) | ( pin10 << 4) | ( pin11 << 3) | ( pin12 << 2) | ( pin13 << 1) | 1 ; // SET PIN'S OFF


    delay(200);
  }
  */

  y1 = analogRead(A0);            // reads the value of the potentiometer (value between 0 and 1023)
  y1 = map(y1, 0, 1023, 0, 31) ;

  x1 = analogRead(A1);            // reads the value of the potentiometer (value between 0 and 1023)
  x1 = map(x1, 0, 1023, 31, 0);

  y2 = analogRead(A2);            // reads the value of the potentiometer (value between 0 and 1023)
  y2 = map(y2, 0, 1023, 0, 31) ;

  x2 = analogRead(A3);            // reads the value of the potentiometer (value between 0 and 1023)
  x2 = map(x2, 0, 1023, 0, 31) ;

  sendCM(x2,1); // channel 1
  sendCM(y2,2); // channel 2
  sendCM(x1,3); // channel 3
  sendCM(y1,4); // channel 4

}
