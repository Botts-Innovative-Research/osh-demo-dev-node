package org.sensorhub.impl.comm.mavlink2.processing;

import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataType;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

public class ConstAltitudeLLA extends ExecutableProcessImpl {

    public static final OSHProcessInfo INFO = new OSHProcessInfo("constAltitudeLLA", "Constant Altitude LLA", null, ConstAltitudeLLA.class);

    DataComponent locationInput;
    DataComponent locationOutput;
    DataComponent altitudeParam;

    public ConstAltitudeLLA() {
        super(INFO);
        GeoPosHelper fac = new GeoPosHelper();

        paramData.add("altitude", altitudeParam = fac.createQuantity().dataType(DataType.DOUBLE).uom("m").definition(SWEHelper.getPropertyUri("AltitudeAGL")).build());
        inputData.add("locationInput", locationInput = fac.createLocationVectorLLA()
                .definition(SWEHelper.getPropertyUri("FeatureOfInterestLocation"))
                .label("Target Location")
                .build());
        outputData.add("locationOutput", locationOutput = fac.createLocationVectorLLA()
                .definition(SWEHelper.getPropertyUri("FeatureOfInterestLocation"))
                .label("Target Location")
                .build());
    }

    @Override
    public void execute() throws ProcessException {
        double lat = locationInput.getData().getDoubleValue(0);
        double lon = locationInput.getData().getDoubleValue(1);
        double alt = altitudeParam.getData().getDoubleValue(0);

        locationOutput.getData().setDoubleValue(0, lat);
        locationOutput.getData().setDoubleValue(1, lon);
        locationOutput.getData().setDoubleValue(2, alt);
    }
}