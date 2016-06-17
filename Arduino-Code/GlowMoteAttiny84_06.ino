/*
RGB LED strip controller meant to run on an Attiny 84.
Contains features such as multiple color selection,
sync to music, flash patterns, and a cycle function.

This program cannot exceed 8KB.

Written by: Marco White
Last Edited: 7/8/14
*/

#include <SoftwareSerial.h>
#include <EEPROM.h>;  // Include EEPROM so we can write to internal system memory
#define REDPIN 7      // Define Attiny84 pins
#define GREENPIN 6
#define BLUEPIN 8
#define RXDPIN 5
#define TXDPIN 4
#define ADDRESS_RED 0   // Define EEPROM addresses
#define ADDRESS_GREEN 1
#define ADDRESS_BLUE 2
#define ADDRESS_IS_FLASH 3
#define ADDRESS_IS_CYCLE 4
#define ADDRESS_CYCLE_STYLE 5
#define ADDRESS_CYCLE_SPEED 6
#define ADDRESS_IS_MUSIC 7
char incomingByte;    // Incoming data from android
char message[10];     // Assembled message
byte index = 0;
long previousMillis = 0;  // This is for timing flash patterns
byte wasFlash = 0;
byte wasCycle = 0;
byte r, g, b;

SoftwareSerial mySerial(5, 4);

void setup() {
  pinMode(RXDPIN, INPUT);
  pinMode(TXDPIN, OUTPUT);
  mySerial.begin(9600); // Initialize the serial communication
  pinMode(REDPIN, OUTPUT);
  pinMode(GREENPIN, OUTPUT);
  pinMode(BLUEPIN, OUTPUT);
  r = EEPROM.read(ADDRESS_RED);
  g = EEPROM.read(ADDRESS_GREEN);
  b = EEPROM.read(ADDRESS_BLUE);
  writeColor(r, g, b);   // Write the last set color (default is white)
  wasFlash = EEPROM.read(ADDRESS_IS_FLASH);
  wasCycle = EEPROM.read(ADDRESS_IS_CYCLE);
}

void loop() {
  if (mySerial.available() == 0) {  // No data, presumably arduino is not connected to android
    if (wasFlash != 0 && wasFlash != 255) {   // If there was a flash pattern set during last operation, turn it back on
      startFlash(wasFlash);
      wasFlash = 0;
    }
    else if (wasCycle != 0 && wasCycle != 255) {   // If the controller was cycling through colors during last operation, resume the cycle
      resumeCycle();
      wasCycle = 0;
    }
  }
  if (mySerial.available() > 0) {    // Data has been recieved by the arduino
    incomingByte = mySerial.read();  // Read the incoming byte
    message[index] = incomingByte;   // Assemble the message
    index++;
    if (incomingByte == '/') {  // The "/" is a notifier for the arduino that the message is complete
      if (message[0] == '0' || message[0] == '1' || message[0] == '2') {  // Message is a color code, analog write to the led strip
        EEPROM.write(ADDRESS_IS_CYCLE, 0);
        r = (message[0] - '0') * 100 + (message[1] - '0') * 10 + (message[2] - '0');
        g = (message[3] - '0') * 100 + (message[4] - '0') * 10 + (message[5] - '0');
        b = (message[6] - '0') * 100 + (message[7] - '0') * 10 + (message[8] - '0');
        saveColor(r, g, b);
        if (EEPROM.read(ADDRESS_IS_FLASH) == 0 || EEPROM.read(ADDRESS_IS_FLASH) == 255) {   // Check to see if the RGB strip is flashing
          writeColor(r, g, b);
        }
        else {
          startFlash(EEPROM.read(ADDRESS_IS_FLASH));
        }
      }
      else if (message[0] == 'f') {  // Start flash pattern
        startFlash(message[5] - '0');
      }
      else if (message[0] == 'c') {   // Cycle through colors
        char cycleStyle = message[5];   // Get cycle style, jumping or smooth
        int cycleSpeed = (message[6] - '0') * 10 + (message[7] - '0');  // Get the cycle speed
        if (message[8] == 'm') {   // Check if the "sync to music" adujst is on
          EEPROM.write(ADDRESS_IS_MUSIC, 1);
        }
        else {
          EEPROM.write(ADDRESS_IS_MUSIC, 0);
        }
        EEPROM.write(ADDRESS_IS_CYCLE, 1);
        if (EEPROM.read(ADDRESS_IS_FLASH) == 0 || EEPROM.read(ADDRESS_IS_FLASH) == 255) {  // Check to see if it's flashing
          cycle(cycleSpeed, cycleStyle); 
        }
        else {
          startFlash(EEPROM.read(3));
        }
      }
      index = 0;  // Reset the message index
    }
  }
}

void startFlash(int switchStatement) {  // Function to start the flash pattern
  switch (switchStatement) {
  case 0:
    EEPROM.write(ADDRESS_IS_FLASH, 0);
    if (EEPROM.read(ADDRESS_IS_CYCLE) == 1) {
      resumeCycle();
    }
    else {
      writeColor(r, g, b);
    }
    break;
  case 1:
    setPattern(1, 1000, false);
    break;
  case 2:
    setPattern(2, 500, false);
    break;
  case 3:
    setPattern(3, 100, false);
    break;
  case 4:
    setPattern(4, 1000, true);
    break;
  case 5:
    setPattern(5, 500, true);
    break;
  case 6:
    setPattern(6, 150, true);
    break;
  }
}

void setPattern(int switchStatment, long interval, boolean isBurst) {  // Function to set the flash pattern
  EEPROM.write(ADDRESS_IS_FLASH, switchStatment);
  while (mySerial.available() == 0) {
    flash(interval, isBurst);
  }
}

void flash(long interval, boolean isBurst) {  // Function for the flash patterns
  unsigned long currentMillis = millis();
  if (currentMillis - previousMillis > interval) {
    if (isBurst) {  // Burst flash pattern
      flashCycleColor();
      for (int i = 0; i <= 3; i++) {
        int delayTime;
        if (interval == 150) {
          delayTime = 30;
        }
        else {
          delayTime = 40;
        }
        writeColor(r, g, b);
        delay(delayTime);
        writeColor(0, 0, 0);
        delay(delayTime);
      }
    }
    else {  // Normal flash pattern
      int delayTime;
      if (interval == 1000) {
        delayTime = 750;
      }
      else {
        delayTime = interval / 2;
      }
      flashCycleColor();
      writeColor(r, g, b);
      delay(delayTime);
      writeColor(0, 0, 0);
    }
    previousMillis = millis();
  }
}

void flashCycleColor() {  // Function to flash and cycle colors at the same time
  if (EEPROM.read(ADDRESS_IS_CYCLE) == 1) {
    if (r == 255 && g == 0 && b == 0) {
      g = 30;
    }
    else if (r == 255 && g == 30 && b == 0) {
      g = 150;
    }
    else if (r == 255 && g == 150 && b == 0) {
      r = 0;
      g = 255;
    }
    else if (r == 0 && g == 255 && b == 0) {
      g = 0;
      b = 255;
    }
    else if (r == 0 && g == 0 && b == 255) {
      r = 150;
    }
    else if (r == 150 && g == 0 && b == 255) {
      r = 255;
      b = 50;
    }
    else if (r == 255 && g == 0 && b == 50) {
      b = 0;
    }
    else {
      r = 255;
      g = 0;
      b = 0;
    }
  }
}

void cycle(int cycleSpeed, char cycleStyle) {  // Function to cycle through colors
  EEPROM.write(ADDRESS_CYCLE_SPEED, cycleSpeed);
  if (cycleStyle == 'j') {
    EEPROM.write(ADDRESS_CYCLE_STYLE, 0);
    while (mySerial.available() == 0) {
      jumpTo(255, 0, 0);
      if (mySerial.available() > 0) {
        break;
      }
      jumpTo(255, 30, 0);
      if (mySerial.available() > 0) {
        break;
      }
      jumpTo(255, 150, 0);
      if (mySerial.available() > 0) {
        break;
      }
      jumpTo(0, 255, 0);
      if (mySerial.available() > 0) {
        break;
      }
      jumpTo(0, 0, 255);
      if (mySerial.available() > 0) {
        break;
      }
      jumpTo(150, 0, 255);
      if (mySerial.available() > 0) {
        break;
      }
      jumpTo(255, 0, 50);
    }
  }
  else {
    EEPROM.write(ADDRESS_CYCLE_STYLE, 1);
    writeColor(255, 0, 0);
    while (mySerial.available() == 0) {
      fade();
      if (mySerial.available() > 0) {
        break;
      }
    }
  }
}

void jumpTo(byte r, byte g, byte b) {  // Function for the jumping cycle
  if (EEPROM.read(ADDRESS_IS_MUSIC) == 1) {
    writeColor((byte) (r * .95), g, (byte) (b * .25));
  }
  else {
    writeColor(r, g, b);
  }
  delay(EEPROM.read(ADDRESS_CYCLE_SPEED) * 100);
}

void fade() { // Function for the smooth cycle
  int r, g, b;
  if (EEPROM.read(ADDRESS_IS_MUSIC) == 1) {
    for (g = 0; g <= 255; g++) {
      analogWrite(GREENPIN, g);
      delay(EEPROM.read(ADDRESS_CYCLE_SPEED) * 5);
      if (mySerial.available() > 0) {
        return;
      }
    }
    if (mySerial.available() > 0) {
      return;
    }
    for (r = 255; r >= 0; r--) {
      analogWrite(REDPIN, (byte) (r * .95));
      delay(EEPROM.read(ADDRESS_CYCLE_SPEED) * 5);
      if (mySerial.available() > 0) {
        return;
      }
    }
    if (mySerial.available() > 0) {
      return;
    }
    for (b = 0; b <= 255; b++) {
      analogWrite(BLUEPIN, (byte) (b * .25));
      delay(EEPROM.read(ADDRESS_CYCLE_SPEED) * 5);
      if (mySerial.available() > 0) {
        return;
      }
    }
    if (mySerial.available() > 0) {
      return;
    }
    for (g = 255; g >= 0; g--) {
      analogWrite(GREENPIN, g);
      delay(EEPROM.read(ADDRESS_CYCLE_SPEED) * 5);
      if (mySerial.available() > 0) {
        return;
      }
    }
    if (mySerial.available() > 0) {
      return;
    }
    for (r = 0; r <= 255; r++) {
      analogWrite(REDPIN, (byte) (r * .95));
      delay(EEPROM.read(ADDRESS_CYCLE_SPEED) * 5);
      if (mySerial.available() > 0) {
        return;
      }
    }
    if (mySerial.available() > 0) {
      return;
    }
    for (b = 255; b >= 0; b--) {
      analogWrite(BLUEPIN, (byte) (b * .25));
      delay(EEPROM.read(ADDRESS_CYCLE_SPEED) * 5);
      if (mySerial.available() > 0) {
        return;
      }
    }
    if (mySerial.available() > 0) {
      return;
    }
  }
  else {
    for (g = 0; g <= 255; g++) {
      analogWrite(GREENPIN, g);
      delay(EEPROM.read(ADDRESS_CYCLE_SPEED) * 5);
      if (mySerial.available() > 0) {
        return;
      }
    }
    if (mySerial.available() > 0) {
      return;
    }
    for (r = 255; r >= 0; r--) {
      analogWrite(REDPIN, r);
      delay(EEPROM.read(ADDRESS_CYCLE_SPEED) * 5);
      if (mySerial.available() > 0) {
        return;
      }
    }
    if (mySerial.available() > 0) {
      return;
    }
    for (b = 0; b <= 255; b++) {
      analogWrite(BLUEPIN, b);
      delay(EEPROM.read(ADDRESS_CYCLE_SPEED) * 5);
      if (mySerial.available() > 0) {
        return;
      }
    }
    if (mySerial.available() > 0) {
      return;
    }
    for (g = 255; g >= 0; g--) {
      analogWrite(GREENPIN, g);
      delay(EEPROM.read(ADDRESS_CYCLE_SPEED) * 5);
      if (mySerial.available() > 0) {
        return;
      }
    }
    if (mySerial.available() > 0) {
      return;
    }
    for (r = 0; r <= 255; r++) {
      analogWrite(REDPIN, r);
      delay(EEPROM.read(ADDRESS_CYCLE_SPEED) * 5);
      if (mySerial.available() > 0) {
        return;
      }
    }
    if (mySerial.available() > 0) {
      return;
    }
    for (b = 255; b >= 0; b--) {
      analogWrite(BLUEPIN, b);
      delay(EEPROM.read(ADDRESS_CYCLE_SPEED) * 5);
      if (mySerial.available() > 0) {
        return;
      }
    }
    if (mySerial.available() > 0) {
      return;
    }
  }
}

void resumeCycle() {   // Function to resume cycling
  switch (EEPROM.read(ADDRESS_CYCLE_STYLE)) {
    case 0:
      cycle(EEPROM.read(ADDRESS_CYCLE_SPEED), 'j');
      break;
    case 1:
      cycle(EEPROM.read(ADDRESS_CYCLE_SPEED), 'f');
      break;
  }
}

void writeColor(byte red, byte green, byte blue) {   // Function to write colors to rgb strip
  analogWrite(REDPIN, red);
  analogWrite(GREENPIN, green);
  analogWrite(BLUEPIN, blue);
}

void saveColor(byte red, byte green, byte blue) {  // Function to save previously set color
  EEPROM.write(ADDRESS_RED, red);
  EEPROM.write(ADDRESS_GREEN, green);
  EEPROM.write(ADDRESS_BLUE, blue);
}
