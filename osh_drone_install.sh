#!/bin/bash

#update/upgrade the system
sudo apt update && sudo apt upgrade -y

# install build tools
sudo apt-get install build-essential cmake git

# install sdkman!
curl -s "https://get.sdkman.io" | bash

# source
source "$HOME/.sdkman/bin/sdkman-init.sh"

#install java
sdk install java 21.0.6-tem


# Clone OSH Demo Dev Node w/ new Mavlink Driver
git clone --recursive https://github.com/Botts-Innovative-Research/osh-demo-dev-node.git

# Build OSH
cd osh-demo-dev-node
./gradlew build -x test -x osgi

# Clone MAVSDK
cd ..
git clone https://github.com/mavlink/MAVSDK.git
cd MAVSDK
git submodule update --init --recursive

# Build MAVSDK, including Server
cmake -DCMAKE_BUILD_TYPE=Release -DBUILD_MAVSDK_SERVER=ON -Bbuild -S.
cmake --build build -j8

# Install MAVSDK, including Server
sudo cmake --build build --target install

# Update LDCONFIG
sudo ldconfig

# Clone Ardupilot
cd ..
git clone https://github.com/ArduPilot/ardupilot.git

# Install Ardupilot prereqs
cd ardupilot
Tools/environment_install/install-prereqs-ubuntu.sh -y

#reload path
. ~/.profile

# Add Taiwan as pre-defined location in Ardupilot
echo "Taiwan=24.178772,120.649633,0,0" >> Tools/autotest/locations.txt

# Add additional mav out
mkdir ~/.mavproxy
touch ~/.mavproxy/mavinit.scr
echo "output add 127.0.0.1:14540" >> ~/.mavproxy/mavinit.scr
echo "output add 127.0.0.1:14560" >> ~/.mavproxy/mavinit.scr
echo "output add 127.0.0.1:14550" >> ~/.mavproxy/mavinit.scr
