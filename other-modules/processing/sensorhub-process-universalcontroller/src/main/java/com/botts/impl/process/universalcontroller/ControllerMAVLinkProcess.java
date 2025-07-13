package com.botts.impl.process.universalcontroller;

import com.botts.impl.sensor.universalcontroller.helpers.UniversalControllerComponent;
import net.opengis.swe.v20.*;
import org.sensorhub.api.processing.OSHProcessInfo;
import com.botts.api.process.universalcontroller.AbstractControllerTaskingProcess;
import org.vast.process.ProcessException;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

public class ControllerMAVLinkProcess extends AbstractControllerTaskingProcess {

    DataRecord bodyVelocity;
    float curYaw = 0.0f;

    public static final OSHProcessInfo INFO = new OSHProcessInfo(
            "controllerMavlinkProcess",
            "Process to send MAVLink commands",
            null,
            ControllerMAVLinkProcess.class);

    public ControllerMAVLinkProcess() {
        super(INFO);

        GeoPosHelper geo = new GeoPosHelper();

//        outputData.add("takeoffAltitude", takeoffAltitude = fac.createQuantity()
//                        .label("Take-Off Altitude")
//                        .definition(SWEHelper.getPropertyUri("AltitudeAboveGround"))
//                        .uomCode("m")
//                        .dataType(DataType.FLOAT)
//                        .build());
//
//        outputData.add("landingLocation", landingLocation = geo.createLocationVectorLatLon()
//                .label("Landing Location")
//                .definition(SWEHelper.getPropertyUri("PlatformLocation"))
//                .description("Landing location or NaN to land at the current location")
//                .build());

        outputData.add("bodyVelocity", bodyVelocity = geo.createRecord()
                .addField("velocity", geo.newVelocityVectorNED(
                        SWEHelper.getPropertyUri("PlatformVelocity"),
                        "m/s"))
                .addField("yawRate", fac.createQuantity()
                        .label("Yaw Rate")
                        .definition(SWEHelper.getPropertyUri("YawRate"))
                        .uomCode("deg/s")
                        .dataType(DataType.FLOAT))
                .build());

        paramData.getComponent(0).getData().setIntValue(0);
    }

    @Override
    public void updateOutputs() throws ProcessException {
        // x-y velocity
        // rx-ry heading
        // dpad up and down
        // a takeoff
        // b land

        // Controller reference
        // x = left stick up (1.0), down (-1.0)
        // y = left stick right (1.0), down (-1.0)
        // rx = right stick ""
        // ry = right stick ""

        // Velocity reference
        // X +forward -backward
        // Y +right -left
        // Z +down -up
        // YawRate turns +right -left

        // TODO: Might have error of continuous landing command if 0,0,0 is sent repeatedly. May need to switch all outputs to just be a single DataChoice output

        float currentX = fac.getComponentValueInput(UniversalControllerComponent.X_AXIS);
        float currentY = fac.getComponentValueInput(UniversalControllerComponent.Y_AXIS);
        float currentRX = fac.getComponentValueInput(UniversalControllerComponent.RX_AXIS);
        float currentRY = fac.getComponentValueInput(UniversalControllerComponent.RY_AXIS);

        float sensitivity = 10.0f;

        // Velocity
        // TODO: Add sensitivity modifiers
        bodyVelocity.getData().setFloatValue(0, currentY  * sensitivity);
        bodyVelocity.getData().setFloatValue(1, currentX * sensitivity);
        bodyVelocity.getData().setFloatValue(2, currentRY * sensitivity);
        // Yaw rate
        bodyVelocity.getData().setFloatValue(3, currentRX * sensitivity);
    }
}
