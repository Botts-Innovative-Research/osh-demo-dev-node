#!/bin/bash

set -e

#update/upgrade the system
sudo apt update && sudo apt upgrade -y
wait
echo "Sys update/upgrade done"

# set up adoptium
sudo apt install -y wget apt-transport-https
wait
echo "apt-transport-https installed"

sudo wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor | sudo tee /etc/apt/trusted.gpg.d/adoptium.gpg > /dev/null

sudo echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
wait
echo "Added adoptium to sources list"

#update again to get adoptium repo
sudo apt update
wait
echo "Updated to get adoptium"

echo "setting JAVA_HOME"
echo "export JAVA_HOME=$(realpath $(dirname $(readlink -f $(which java)))/../)" >> ~/.bashrc

# install build tools
sudo apt-get install build-essential cmake git zip unzip python3-pip temurin-17-jdk temurin-21-jdk -y
wait
echo "pre-req tools installed"

# Clone MAVSDK
cd ..
git clone https://github.com/mavlink/MAVSDK.git
wait
echo "MAVSDK cloned"

cd MAVSDK
git submodule update --init --recursive
wait
echo "MAVSDK submodules updated"

# Build MAVSDK, including Server
cmake -DCMAKE_BUILD_TYPE=Release -DBUILD_MAVSDK_SERVER=ON -Bbuild -S.
wait
echo "MAVSDK build config"

cmake --build build -j8
wait
echo "MAVSDK built"

# Install MAVSDK, including Server
sudo cmake --build build --target install
wait
echo "MAVSDK installed"

# Update LDCONFIG
sudo ldconfig
wait
echo "ldconfig updated"

# Clone Ardupilot
cd ..
git clone https://github.com/ArduPilot/ardupilot.git
wait
echo "ardupilot cloned"

# Install Ardupilot prereqs
cd ardupilot
Tools/environment_install/install-prereqs-ubuntu.sh -y
wait
echo "ardupilot pre-reqs installed"

#reload path
. ~/.profile
wait
echo "path reloaded"

# Add Taiwan as pre-defined location in Ardupilot
echo "Taiwan=24.178772,120.649633,0,0" >> Tools/autotest/locations.txt
wait
echo "Taiwan added as location to Ardupilot"

# Add additional mav out
mkdir ~/.mavproxy
wait
echo ".mavproxy directory created"

touch ~/.mavproxy/mavinit.scr
wait
echo "mavinit.scr created"

echo "output add 127.0.0.1:14540" >> ~/.mavproxy/mavinit.scr
echo "output add 127.0.0.1:14560" >> ~/.mavproxy/mavinit.scr
echo "output add 127.0.0.1:14550" >> ~/.mavproxy/mavinit.scr
echo "outputs added to mavinit.scr"
