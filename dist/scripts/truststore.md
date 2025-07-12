Add truststore to launch.sh or launch.bat with

`-Djavax.net.ssl.trustStore="./truststore.jks"`

`-Djavax.net.ssl.trustStorePassword="changeit"`

This is a requirement for the Civil-IoT video driver. 
Instructions to generate your own trust store are in the README of `sensorhub-driver-civil-iot`