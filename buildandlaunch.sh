#!/bin/bash

cd ~/builds/osh-node-dev-template && ./gradlew build -x test && rm -rf build/distributions/osh-node-0.0.0 && cd build/distributions && unzip osh-node-0.0.0.zip && cd osh-node-0.0.0 && ./launch.sh
