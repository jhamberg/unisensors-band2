# Band 2 Sensor Monitor

## Sensor information

| Sensor                       | Output                         | Sampling rate | Additional information                                                                                                               |
|------------------------------|--------------------------------|---------------|--------------------------------------------------------------------------------------------------------------------------------------|
| Galvanic Skin Response (GSR) | kΩ                             | 200ms         | Defaults to 340330 kΩ                                                                                                                |
| Heart rate (HR)              | bpm, quality                   | Event based   | Defaults to somewhere around 75. Quality (0 or 1) signifies reading reliability. Logging format: hr;quality                          |
| Inter-beat interval (RR)     | seconds                        | Event based   | Time between QRS complexes, can be used to calculate heart rate.<br>https://courses.kcumb.edu/physio/ecg%20primer/normecgcalcs.htm   |
| Gyroscope (GYRO)             | acceleration, angular velocity | 16ms          | Logging format: ax;ay;az;vx;vy;vz                                                                                                    |
| Accelerometer (ACC)          | acceleration                   | 16ms          | Logging format: ax;ay;az                                                                                                             |
| Barometer (BARO)             | mBar, ºC                       | ~1s           | Pressure readings seem to jump quite a bit.<br>Logging format: pressure;temperature                                                  |

## Logging and format

Logfiles are stored under the folder *Band2Monitor* in device's main storage. For every session, you will be prompted a session/subfolder name. All logfiles can then be found under this subfolder (*/Band2Monitor/&lt;Subfolder&gt;/*). Please refer to shorthand names listed above *(GSR, HR...)* when looking for a specific logfile.

**Format**: Every row in a logfile begins with a timestamp (Epoch time) and is delimited by semicolons. 

## Build instructions

1. Download Android Studio: https://developer.android.com/studio/index.html
2. Use SDK Manager to download tools and platforms: https://developer.android.com/studio/intro/update.html
3. Import Band 2 project into the IDE
4. Let Gradle synchronize project dependencies
5. Build and run on an emulator or your device
