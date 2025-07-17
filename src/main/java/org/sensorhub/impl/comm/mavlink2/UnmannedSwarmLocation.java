/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/


package org.sensorhub.impl.comm.mavlink2;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.locationtech.proj4j.ProjCoordinate;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.impl.comm.mavlink2.util.ProjectionAzEq;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.swe.helper.GeoPosHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * This class provides control stream capabilities
 * </p>
 *
 * @author Michael Stinson
 * @since Jul 2025
 */
public class UnmannedSwarmLocation extends AbstractSensorControl<UnmannedSwarm>
{
    private DataRecord commandDataStruct;

    /**
     * Name of the control
     */
    private static final String SENSOR_CONTROL_NAME = "UnmannedSwarmLocation";

    /**
     * Label for the control
     */
    private static final String SENSOR_CONTROL_LABEL = "Swarm Location Control";

    /**
     * Control description
     */
    private static final String SENSOR_CONTROL_DESCRIPTION =
            "Interfaces with MAVLINK and OSH to effectuate location control over the swarm";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/swarm_location_control";

    private io.mavsdk.System system = null;

    static double deltaSuccess =   0.000003; //distance from lat/lon to determine success

    public UnmannedSwarmLocation( UnmannedSwarm parentSensor) {
        super("mavSwarmLocationControl", parentSensor);
    }


    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }

    public void setSystem( io.mavsdk.System systemParam ) {
        system = systemParam;
    }

    public void init() {

        GeoPosHelper factory = new GeoPosHelper();

        commandDataStruct = factory.createRecord()
                .name(SENSOR_CONTROL_NAME)
                .label(SENSOR_CONTROL_LABEL)
                .description(SENSOR_CONTROL_DESCRIPTION)
                .addField( "locationVectorLLA", factory.createVector()
                        .addCoordinate("Latitude", factory.createQuantity()
                                .uom("deg"))
                        .addCoordinate("Longitude", factory.createQuantity()
                                .uom("deg"))
                        .addCoordinate("AltitudeAGL", factory.createQuantity()
                                .uom("m")
                                .value(30)))
                .addField( "returnToStart", factory.createBoolean().value(false))
                .addField( "hoverSeconds", factory.createCount().value(0))
                .build();
    }


    /**
     * Calculate parallel paths for these subsystems and send them on those paths. Avoid
     * collisions. For now lets keep them on a similar altitude. In the future we can
     * add avoidance via altitude differences.
     *
     * @param command
     * @return
     * @throws CommandException
     */
    @Override
    protected boolean execCommand(DataBlock command) throws CommandException {

        double destLatitude = command.getDoubleValue(0);
        double destLongitude = command.getDoubleValue(1);
        double altitude = command.getDoubleValue(2);
        boolean returnToStart = command.getBooleanValue(3);
        long hoverSeconds = command.getLongValue(4);


        try {
            for (IDataProducerModule<?> m: parent.getMembers().values()) {
                if ( !m.isInitialized() ) {
                    m.init();
                }

                if ( !m.isStarted() ) {
                    m.start();
                }
            }
        } catch (SensorHubException e) {
            throw new RuntimeException(e);
        }



        //Find the centroid of all of the drones. Then calculate the parallel
        //path from the centroid to the destination location.
        ProjectionAzEq projection = new ProjectionAzEq(destLatitude, destLongitude);
        ProjCoordinate destCoordGeographic = new ProjCoordinate(destLongitude, destLatitude);
        var destCoordProjected = projection.transform(destCoordGeographic);

        List<ProjCoordinate> droneProjectedCoords = new ArrayList<>();

        for (var subsystem : parent.getMembers().values()) {

            if (subsystem instanceof UnmannedSystem) {
                var input = (UnmannedControlLocation)((UnmannedSystem) subsystem).
                        getCommandInputs().get("mavLocationControl");
                if ( null != input ) {
                    io.mavsdk.System mavSystem = ((UnmannedSystem) subsystem).getMavSystem();

                    var currentLatitude = mavSystem.getTelemetry().getPosition().blockingFirst().getLatitudeDeg();
                    var currentLongitude = mavSystem.getTelemetry().getPosition().blockingFirst().getLongitudeDeg();

                    ProjCoordinate currentGeoCoord = new ProjCoordinate(currentLongitude,currentLatitude);
                    var cProjected = projection.transform(currentGeoCoord);

                    droneProjectedCoords.add(cProjected);
                }
            }
        }

        //calc centroid
        double sumX = 0, sumY = 0;
        for (ProjCoordinate p : droneProjectedCoords) {
            sumX += p.x;
            sumY += p.y;
        }
        int n = droneProjectedCoords.size();
        ProjCoordinate droneCentroidProjected = new ProjCoordinate(sumX / n, sumY / n);

        //translation vector
        double dx = destCoordProjected.x - droneCentroidProjected.x;
        double dy = destCoordProjected.y - droneCentroidProjected.y;

        for (var subsystem : parent.getMembers().values()) {

            if (subsystem instanceof UnmannedSystem) {
                var input = (UnmannedControlLocation)((UnmannedSystem) subsystem).
                        getCommandInputs().get("mavLocationControl");
                if ( null != input ) {
                    io.mavsdk.System mavSystem = ((UnmannedSystem) subsystem).getMavSystem();

                    var currentLatitude = mavSystem.getTelemetry().getPosition().blockingFirst().getLatitudeDeg();
                    var currentLongitude = mavSystem.getTelemetry().getPosition().blockingFirst().getLongitudeDeg();

                    //transform into 2D projected space
                    ProjCoordinate currentGeo = new ProjCoordinate(currentLongitude,currentLatitude);
                    var currentProj = projection.transform(currentGeo);
                    var translatedProj = new ProjCoordinate(currentProj.x + dx, currentProj.y + dy);
                    ProjCoordinate destGeo = projection.inverse(translatedProj);

                    //override command with new calculated values parallel to existing path
                    command.setDoubleValue(0, destGeo.y); //latitude
                    command.setDoubleValue( 1, destGeo.x ); //longitude

                    input.execCommand(command);
                }
            }
        }

        return true;
    }


    public void stop() {
        // TODO Auto-generated method stub
    }


    public static ProjCoordinate averageXY(List<ProjCoordinate> points) {
        double sumX = 0, sumY = 0;
        for (ProjCoordinate p : points) {
            sumX += p.x;
            sumY += p.y;
        }
        int n = points.size();
        return new ProjCoordinate(sumX / n, sumY / n);
    }
}

