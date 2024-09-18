#include <WiFi.h>
#include <WebSocketsServer.h>
#include <FastLED.h>
#include <vector>

#define LED_PIN    2
#define NUM_LEDS  800
#define LED_TYPE  WS2813

const char *ssid = "ESP32_Tetris";
const char *password = "tetris123";

CRGB leds[NUM_LEDS];
bool sendData = true;
bool startLightUpSequence = false;
WebSocketsServer webSocket = WebSocketsServer(81);

void setup() {
  Serial.begin(115200);
  FastLED.addLeds<LED_TYPE, LED_PIN, GRB>(leds, NUM_LEDS);
  FastLED.setBrightness(20);
  FastLED.show(); // Initialize all pixels to 'off'

  WiFi.softAP(ssid, password);

  webSocket.begin();
  webSocket.onEvent(webSocketEvent);

  Serial.println("WebSocket server started.");
}

void loop() {
  webSocket.loop();
  if (startLightUpSequence) {
    lightUpGroupsWithDelay();
  }
}

void webSocketEvent(uint8_t num, WStype_t type, uint8_t * payload, size_t length) {
    if (type == WStype_TEXT) {
        String data = String((char *)payload);
        Serial.println("Data received: " + data);

        if (data.startsWith("GAMEOVER")) {
            // Extract the score from the message
            int commaIdx = data.indexOf(',');
            String scoreStr = data.substring(commaIdx + 1);

            // Turn off all LEDs and display the score
            turnOffLEDs();
            displayTextOnLEDs();
            displayScoreOnLEDs(scoreStr);
            startLightUpSequence = true;

        } else if (data == "TURN_OFF") {
            sendData = false;
            turnOffLEDs();
        } else {
            sendData = true;
            updateLEDs(data);
        }
    }
}

void displayScoreOnLEDs(String scoreStr) {
    // Initialize the left and right digits
    int leftDigit = 0;
    int rightDigit;
    std::vector<int> left_indices;
    std::vector<int> right_indices;

    // Check if the score is one or two digits
    if (scoreStr.length() == 1) {
        rightDigit = scoreStr.toInt();
    } else {
        leftDigit = scoreStr.substring(0, 1).toInt();
        rightDigit = scoreStr.substring(1).toInt();
    }
    Serial.print("Left Digit: ");
    Serial.println(leftDigit);
    Serial.print("Right Digit: ");
    Serial.println(rightDigit);

    switch (leftDigit) {
        case 0:
            left_indices  = {547, 548, 549, 570, 572, 587, 589, 610, 612, 627, 629, 650, 651, 652};
            break;
        case 1:
            left_indices  = {548, 549, 570, 589, 610, 629, 650};
            break;
        case 2:
            left_indices  = {547, 548, 549, 570, 587, 588, 589, 612, 627, 650, 651, 652};
            break;
        case 3:
            left_indices  = {547, 548, 549, 570, 587, 588, 589, 610, 629, 650, 651, 652};
            break;
        case 4:
            left_indices  = {547, 549, 570, 572, 587, 588, 589, 610, 629, 650};
            break;
        case 5:
            left_indices  = {547, 548, 549, 572, 587, 588, 589, 610, 629, 650, 651, 652};
            break;
        case 6:
            left_indices  = {547, 548, 549, 572, 587, 588, 589, 610, 612, 627, 629, 650, 651, 652};
            break;
        case 7:
            left_indices  = {547, 548, 549, 570, 589, 610, 629, 650};
            break;
        case 8:
            left_indices  = {547, 548, 549, 570, 572, 587, 588, 589, 610, 611, 612, 627, 629, 650, 651, 652};
            break;
        case 9:
            left_indices  = {547, 548, 549, 570, 572, 587, 588, 589, 610, 627, 629, 650, 651, 652};
            break;
        default:
            break;
    }

    switch (rightDigit) {
        case 0:
            right_indices  = {551, 552, 553, 566, 568, 591, 593, 606, 608, 631, 633, 646, 647, 648};
            break;
        case 1:
            right_indices  = {551, 552, 567, 592, 607, 632, 647};
            break;
        case 2:
            right_indices  = {551, 552, 553, 566, 591, 592, 593, 608, 631, 646, 647, 648};
            break;
        case 3:
            right_indices  = {551, 552, 553, 566, 591, 592, 593, 606, 633, 646, 647, 648};
            break;
        case 4:
            right_indices  = {551, 553, 566, 568, 591, 592, 593, 606, 633, 646};
            break;
        case 5:
            right_indices  = {551, 552, 553, 568, 591, 592, 593, 606, 633, 646, 647, 648};
            break;
        case 6:
            right_indices  = {551, 552, 553, 568, 591, 592, 593, 606, 608, 631, 633, 646, 647, 648};
            break;
        case 7:
            right_indices  = {551, 552, 553, 566, 593, 606, 633, 646};
            break;
        case 8:
            right_indices  = {551, 552, 553, 566, 568, 591, 592, 593, 606, 607, 608, 631, 633, 646, 647, 648};
            break;
        case 9:
            right_indices  = {551, 552, 553, 566, 568, 591, 592, 593, 606, 631, 633, 646, 647, 648};
            break;
        default:
            break;
    }

    // Use the size() method to get the number of elements in the vector
    for (int i = 0; i < left_indices.size(); i++) {
        leds[left_indices[i]] = CRGB::Blue;  
    }

    for (int i = 0; i < right_indices.size(); i++) {
        leds[right_indices[i]] = CRGB::Blue;  
    }

    FastLED.show();  // Update the LED strip with the new colors
}


void displayTextOnLEDs() {
    // Turn off all LEDs first
    turnOffLEDs();

    // Specify the indices for each part of the text
    int indices[] = {
        122, 123, 124, 126, 128, 131, 132, 135, 137, 142, 144, 
                            146, 149, 151, 153, 155, 157, 163, 164, 166, 168, 170, 
                            173, 175, 177, 183, 186, 189, 191, 193, 195, 197, 202, 
                            204, 206, 207, 208, 211, 212, 216, 260, 261, 262, 265,
                            266, 269, 270, 273, 274, 275, 277, 278, 279, 282, 282, 
                            284, 286, 288, 291, 295, 299, 300, 301, 302, 304, 308, 
                            311, 313, 314, 317, 318, 319, 322, 324, 326, 328, 331, 
                            335, 337, 340, 341, 342, 345, 346, 349, 350, 353, 355, 
                            357, 358, 359, 406, 407, 408, 410, 411, 412, 428, 431, 
                            446, 447, 448, 451, 473, 468, 486, 487, 488, 490, 491, 
                            492
    };

    // Loop through the indices and light them up
    for (int i = 0; i < sizeof(indices) / sizeof(indices[0]); i++) {
        leds[indices[i]] = CRGB::Blue; 
    }

    FastLED.show();  // Update the LED strip
}

void lightUpGroupsWithDelay() {
    std::vector<std::vector<int>> ledGroups = {
        {105, 110, 114, 666, 670, 673},      
        {95, 89, 84, 685, 689, 694},    
        {63, 70, 76, 715, 710, 704}, 
        {57, 49, 42, 723, 729, 736}, 
        {21, 30, 38, 757, 750, 742}, 
        {19, 9, 0, 761, 769, 778}  
    };

    int delayTime = 200; // 0.1 second delay

    static unsigned long lastUpdateTime = 0;
    static int currentGroup = 0;

    unsigned long currentTime = millis();

    if (currentTime - lastUpdateTime >= delayTime) {
        lastUpdateTime = currentTime;

        // Light up all groups from the first to the current one
        for (int i = 0; i <= currentGroup; i++) {
            for (int j = 0; j < ledGroups[i].size(); j++) {
                leds[ledGroups[i][j]] = CRGB::Orange;  // Light up the LED with a color
            }
        }

        // Display the LEDs
        FastLED.show();

        // Move to the next group
        currentGroup++;

        // If all groups have been lit, reset by turning off all groups except the first one
        if (currentGroup >= ledGroups.size()) {
            // Turn off LEDs in all groups except the first group
            for (int i = 1; i < ledGroups.size(); i++) {
                for (int j = 0; j < ledGroups[i].size(); j++) {
                    leds[ledGroups[i][j]] = CRGB::Black;  // Turn off the LEDs in the group
                }
            }

            // Display the LEDs
            FastLED.show();

            // Reset to start over with the first group already lit
            currentGroup = 0;
        }
    }
}



void updateLEDs(String data) {
    int startIdx = 0;
    while (startIdx < data.length()) {
        int endIdx = data.indexOf(')', startIdx);
        if (endIdx == -1) break;

        String segment = data.substring(startIdx + 1, endIdx); // Skip '(' and ')'
        int commaIdx1 = segment.indexOf(',');
        int commaIdx2 = segment.indexOf(',', commaIdx1 + 1);
        int commaIdx3 = segment.indexOf(',', commaIdx2 + 1);

        int row = segment.substring(0, commaIdx1).toInt();
        int col = segment.substring(commaIdx1 + 1, commaIdx2).toInt();
        int deleteFlag = segment.substring(commaIdx2 + 1, commaIdx3).toInt();
        String colorStr = segment.substring(commaIdx3 + 1);

        uint8_t r = strtol(colorStr.substring(2, 4).c_str(), NULL, 16);
        uint8_t g = strtol(colorStr.substring(4, 6).c_str(), NULL, 16);
        uint8_t b = strtol(colorStr.substring(6, 8).c_str(), NULL, 16);
        CRGB color = CRGB(r, g, b);

        int ledIndices[4];
        calculateLEDIndex(row, col, ledIndices);

        if (deleteFlag == 1) {
            for (int i = 0; i < 4; i++) {
                leds[ledIndices[i]] = CRGB::Black;
            }
        } else {
            for (int i = 0; i < 4; i++) {
                leds[ledIndices[i]] = color;
            }
        }
        // Move past the closing ')' and the next comma (if any)
        startIdx = endIdx + 2; 
    }
    FastLED.show();
}

void calculateLEDIndex(int row, int col, int ledIndices[4]) {
    int actualRow1 = row * 2;
    int actualRow2 = actualRow1 + 1;
    int actualCol1 = col * 2;
    int actualCol2 = actualCol1 + 1;

    ledIndices[0] = actualRow1 * 20 + (19 - actualCol1);
    ledIndices[1] = ledIndices[0] - 1;
    ledIndices[2] = actualRow2 * 20 + actualCol2;
    ledIndices[3] = ledIndices[2] - 1;
}

void turnOffLEDs() {
    for (int i = 0; i < NUM_LEDS; i++) {
        leds[i] = CRGB::Black;
    }
    FastLED.show();
    startLightUpSequence = false;
    Serial.println("All LEDs turned off.");
}
