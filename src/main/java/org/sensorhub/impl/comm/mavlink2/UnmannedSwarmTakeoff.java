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
import org.sensorhub.api.command.CommandException;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.sensor.SensorSystem;
import org.vast.swe.helper.GeoPosHelper;

/**
 * <p>
 * This class provides control stream capabilities
 * </p>
 *
 * @author Michael Stinson
 * @since Jul 2025
 */
public class UnmannedSwarmTakeoff extends AbstractSensorControl<UnmannedSwarm>
{
    private DataRecord commandDataStruct;

    /**
     * Name of the control
     */
    private static final String SENSOR_CONTROL_NAME = "UnmannedSwarmTakeoff";

    /**
     * Label for the control
     */
    private static final String SENSOR_CONTROL_LABEL = "Swarm Takeoff Control";

    /**
     * Control description
     */
    private static final String SENSOR_CONTROL_DESCRIPTION =
            "Interfaces with MAVLINK and OSH to effectuate takeoff control over the swarm";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/swarm_takeoff_control";

    private io.mavsdk.System system = null;

    static double deltaSuccess =   0.000003; //distance from lat/lon to determine success

    public UnmannedSwarmTakeoff( UnmannedSwarm parentSensor) {
        super("mavSwarmTakeoffControl", parentSensor);
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
                .addField("TakeoffAltitudeAGL", factory.createQuantity().value(20).build())
                .build();
    }


    @Override
    protected boolean execCommand(DataBlock command) throws CommandException {

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

        for (var subsystem : parent.getMembers().values()) {

            if (subsystem instanceof UnmannedSystem) {
                var input = (UnmannedControlTakeoff)((UnmannedSystem) subsystem).getCommandInputs().get("mavTakeoffControl");
                if ( null != input ) {
                    input.execCommand(command);
                }
            }
        }

        return true;
    }


    public void stop() {
        // TODO Auto-generated method stub
    }
}

