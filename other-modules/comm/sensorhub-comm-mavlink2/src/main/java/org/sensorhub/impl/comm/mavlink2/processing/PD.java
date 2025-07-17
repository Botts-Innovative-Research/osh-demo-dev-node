package org.sensorhub.impl.comm.mavlink2.processing;

import org.sensorhub.impl.processing.AbstractProcessProvider;

public class PD extends AbstractProcessProvider {

    public PD() {
        addImpl(ConstAltitudeLLA.INFO);
    }

}
