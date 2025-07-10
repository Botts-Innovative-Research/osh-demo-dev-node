#!/bin/bash

#update/upgrade the system
sudo apt update && sudo apt upgrade -y &
wait
echo "Sys update/upgrade done"

# install build tools
sudo apt-get install build-essential cmake git zip unzip -y &
wait
echo "pre-req tools installed"

# install sdkman!
curl -s "https://get.sdkman.io" | bash &
wait
echo "sdkman! installed"
# source
source ~/.sdkman/bin/sdkman-init.sh &
wait
echo "skdman sourced"

#install java
sdk install java 21.0.6-tem &
wait
echo "java installed"

# Clone OSH Demo Dev Node w/ new Mavlink Driver
git clone --recursive https://github.com/Botts-Innovative-Research/osh-demo-dev-node.git &
wait
echo "osh demo node cloned"

# Build OSH
cd osh-demo-dev-node
./gradlew build -x test -x osgi &
wait
echo "osh built"

# Clone MAVSDK
cd ..
git clone https://github.com/mavlink/MAVSDK.git &
wait 
echo "MAVSDK cloned"

cd MAVSDK
git submodule update --init --recursive &
wait
echo "MAVSDK submodules updated"

# Build MAVSDK, including Server
cmake -DCMAKE_BUILD_TYPE=Release -DBUILD_MAVSDK_SERVER=ON -Bbuild -S. &
wait
echo "MAVSDK build config"

cmake --build build -j8 &
wait
echo "MAVSDK built"

# Install MAVSDK, including Server
sudo cmake --build build --target install &
wait
echo "MAVSDK installed"

# Update LDCONFIG
sudo ldconfig &
wait
echo "ldconfig updated"

# Clone Ardupilot
cd ..
git clone https://github.com/ArduPilot/ardupilot.git &
wait
echo "ardupilot cloned"

# Install Ardupilot prereqs
cd ardupilot
Tools/environment_install/install-prereqs-ubuntu.sh -y &
wait
echo "ardupilot pre-reqs installed"

#reload path
. ~/.profile &
wait
echo "path reloaded"

# Add Taiwan as pre-defined location in Ardupilot
echo "Taiwan=24.178772,120.649633,0,0" >> Tools/autotest/locations.txt &
wait
echo "Taiwan added as location to Ardupilot"

# Add additional mav out
mkdir ~/.mavproxy &
wait
echo ".mavproxy directory created"

touch ~/.mavproxy/mavinit.scr &
wait
echo "mavinit.scr created"

echo "output add 127.0.0.1:14540" >> ~/.mavproxy/mavinit.scr
echo "output add 127.0.0.1:14560" >> ~/.mavproxy/mavinit.scr
echo "output add 127.0.0.1:14550" >> ~/.mavproxy/mavinit.scr
echo "outputs added to mavinit.scr"
