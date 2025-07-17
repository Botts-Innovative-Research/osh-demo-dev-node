package org.sensorhub.impl.comm.mavlink2;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.api.module.IModule;
import org.sensorhub.impl.sensor.SensorSystem;
import org.sensorhub.impl.sensor.SensorSystemConfig;
import org.vast.util.Asserts;

/**
 * <p>
 * SensorSystem for Swarm Operations
 * </p>
 *
 * @author Michael Stinson
 * @since Jul 2025
 */
public class UnmannedSwarm extends SensorSystem {
    static final String UID_PREFIX = "urn:osh:driver:mavswarm:";
    static final String XML_PREFIX = "MAVSWARM_DRIVER_";

    UnmannedSwarmTakeoff swarmTakeoffControl;
    UnmannedSwarmLanding swarmLandingControl;
    UnmannedSwarmLocation swarmLocationControl;

    public UnmannedSwarm() {
        super();
    }

    @Override
    public void doInit() throws SensorHubException {
        super.doInit();

        generateUniqueID(UID_PREFIX, uniqueID);
        generateXmlID(XML_PREFIX, uniqueID);

        this.swarmTakeoffControl = new UnmannedSwarmTakeoff(this);
        addControlInput(this.swarmTakeoffControl);
        this.swarmTakeoffControl.init();

        this.swarmLandingControl = new UnmannedSwarmLanding(this);
        addControlInput(this.swarmLandingControl);
        this.swarmLandingControl.init();

        this.swarmLocationControl = new UnmannedSwarmLocation(this);
        addControlInput(this.swarmLocationControl);
        this.swarmLocationControl.init();
    }
}
