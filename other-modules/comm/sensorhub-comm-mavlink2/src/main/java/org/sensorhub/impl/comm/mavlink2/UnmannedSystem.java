/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.comm.mavlink2;

import io.mavsdk.action.Action;
import io.mavsdk.core.Core;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.abs;
import static org.sensorhub.impl.comm.mavlink2.UnmannedOutput.*;

/**
 * Driver implementation for the sensor.
 * <p>
 * This class is responsible for providing sensor information, managing output registration,
 * and performing initialization and shutdown for the driver and its outputs.
 */
public class UnmannedSystem extends AbstractSensorModule<UnmannedConfig> {
    static final String UID_PREFIX = "urn:osh:driver:mavlink2:";
    static final String XML_PREFIX = "MAVLINK2_DRIVER_";

    private static final Logger logger = LoggerFactory.getLogger(UnmannedSystem.class);

    UnmannedOutput output;
    io.mavsdk.System system;
    boolean isConnected = false;
    volatile boolean doProcessing = true;

    UnmannedControlTakeoff unmannedControlTakeoff;
    UnmannedControlLocation unmannedControlLocation;
    UnmannedControlLanding unmannedControlLanding;
    UnmannedControlOffboard unmannedControlOffboard;

    @Override
    public void doInit() throws SensorHubException {
        super.doInit();

        reportStatus("Listening for system connection...");

        io.mavsdk.System drone = new io.mavsdk.System(config.SDKAddress, config.SDKPort);
        this.system = drone;
        drone.getCore().getConnectionState()
                .filter(Core.ConnectionState::getIsConnected)
                .firstElement()
                .subscribe(state -> {
                    isConnected = true;
                    reportStatus("Successfully connected to a system");
                }, e -> reportError("System not found", new IllegalStateException()));

        // Generate identifiers
        generateUniqueID(UID_PREFIX, config.serialNumber);
        generateXmlID(XML_PREFIX, config.serialNumber);

        // Create and initialize output
        output = new UnmannedOutput(this);
        addOutput(output, false);

        // add Lat/Lon/Alt control stream
        this.unmannedControlTakeoff = new UnmannedControlTakeoff(this);
        addControlInput(this.unmannedControlTakeoff);
        unmannedControlTakeoff.init();

        this.unmannedControlLocation = new UnmannedControlLocation(this);
        addControlInput(this.unmannedControlLocation);
        unmannedControlLocation.init();

        this.unmannedControlLanding = new UnmannedControlLanding(this);
        addControlInput(this.unmannedControlLanding);
        unmannedControlLanding.init();

        this.unmannedControlOffboard = new UnmannedControlOffboard(this);
        addControlInput(this.unmannedControlOffboard);
        unmannedControlOffboard.init();

        output.doInit();
    }

    @Override
    public void doStart() throws SensorHubException {
        super.doStart();
        if (this.system != null && isConnected) {
            output.subscribeTelemetry(system);
            //setUpScenario(system);
            //sendMission(system);
        }
    }

    @Override
    public void doStop() throws SensorHubException {
        super.doStop();
//        output.unsubscribe();
        // TODO: Stop connection to outputs/control interfaces
    }

    @Override
    public boolean isConnected() {
        return system != null && isConnected;
    }


    private static void setUpScenario( io.mavsdk.System drone ) {

        System.out.println("Setting up scenario...");

        CountDownLatch latch = new CountDownLatch(1);
        //downloadLog(system);

        //subscribeTelemetry(system);
        //printVideoStreamInfo(system);

        //printParams(system);
        //printHealth(system);

        //system.getOffboard().

        //printTransponderInfo(system);

        drone.getAction().arm()
                .doOnComplete(() -> {

                    System.out.println("Arming...");

                    printDroneInfo(drone);

                })
                .doOnError(throwable -> {

                    System.out.println("Failed to arm: " + ((Action.ActionException) throwable).getCode());

                })
                .andThen(drone.getAction().setTakeoffAltitude(homeAltitude))
                .andThen(drone.getAction().takeoff()
                        .doOnComplete(() -> {

                            System.out.println("Taking off...");

                        })
                        .doOnError(throwable -> {

                            System.out.println("Failed to take off: " + ((Action.ActionException) throwable).getCode());

                        }))
                .delay(5, TimeUnit.SECONDS)
                .andThen(drone.getTelemetry().getPosition()
                        .filter(pos -> pos.getRelativeAltitudeM() >= homeAltitude)
                        .firstElement()
                        .ignoreElement()
                )
                .delay(5, TimeUnit.SECONDS)
                .andThen(drone.getAction().gotoLocation(destLatitude, destLongitude,
                                destAltitude + drone.getTelemetry().getPosition().blockingFirst().getAbsoluteAltitudeM(),
                                45.0F)
                        .doOnComplete( () -> {

                            System.out.println("Moving to target location");

                        }))
                .doOnError( throwable -> {

                    System.out.println("Failed to go to target: " + ((Action.ActionException) throwable).getCode());

                })
                .andThen(drone.getTelemetry().getPosition()
                        .filter(pos -> (abs(pos.getLatitudeDeg() - destLatitude) <= deltaSuccess && abs(pos.getLongitudeDeg() - destLongitude) <= deltaSuccess))
                        .firstElement()
                        .ignoreElement()
                )
                .delay( 8, TimeUnit.SECONDS )
                .andThen(drone.getAction().gotoLocation(homeLatitude, homeLongitude,
                        homeAltitude + drone.getTelemetry().getPosition().blockingFirst().getAbsoluteAltitudeM()
                        , 0.0F))
                .doOnComplete( () -> {

                    System.out.println("Moving to landing location");

                })
                .doOnError( throwable -> {

                    System.out.println("Failed to go to landing location: " + ((Action.ActionException) throwable).getCode());

                })
                .andThen(drone.getTelemetry().getPosition()
                        .filter(pos -> (abs(pos.getLatitudeDeg() - homeLatitude) <= deltaSuccess && abs(pos.getLongitudeDeg() - homeLongitude) <= deltaSuccess))
                        .firstElement()
                        .ignoreElement()
                )
                .andThen(drone.getAction().land().doOnComplete(() -> {

                            System.out.println("Landing...");

                        })
                        .doOnError(throwable -> {

                            System.out.println("Failed to land: " + ((Action.ActionException) throwable).getCode());

                        }))
                .subscribe(latch::countDown, throwable -> {

                    latch.countDown();

                });

        try {
            latch.await();
        } catch (InterruptedException ignored) {
            // This is expected
        }
    }

}
