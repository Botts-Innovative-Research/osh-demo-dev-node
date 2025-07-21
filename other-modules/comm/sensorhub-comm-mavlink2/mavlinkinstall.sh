#!/bin/bash

set -e

#update/upgrade the system
sudo apt update && sudo apt upgrade -y
wait
echo "Sys update/upgrade done"

# install build tools
sudo apt-get install build-essential cmake git zip unzip python3-pip -y
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

