package com.botts.impl.client.sensorthings;

import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.model.*;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import net.opengis.OgcProperty;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.AbstractSWEIdentifiable;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.impl.system.SystemDatabaseTransactionHandler;
import org.sensorhub.impl.system.SystemTransactionHandler;
import org.sensorhub.impl.system.SystemUtils;
import org.sensorhub.impl.system.wrapper.SystemWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class SensorThingsIngestor {
    Logger logger = LoggerFactory.getLogger(SensorThingsIngestor.class);

    private final SensorThingsService sts;
    private final SystemDatabaseTransactionHandler transactionHandler;
    private final boolean usingStateDb;
    private final ExecutorService workerExecutor;

    static class SensorData {
        AbstractProcess smlDescription;
        Map<String, Datastream> datastreams;
        public SensorData(AbstractProcess smlDescription) {
            this.smlDescription = smlDescription;
            this.datastreams = new HashMap<>();
        }
    }

    public SensorThingsIngestor(URL apiUrl, boolean usingStateDb, SystemDatabaseTransactionHandler transactionHandler, ExecutorService workerExecutor) throws MalformedURLException {
        this.sts = new SensorThingsService(apiUrl);
        this.transactionHandler = transactionHandler;
        this.usingStateDb = usingStateDb;
        this.workerExecutor = workerExecutor;
    }

    public void start() {
        try {
            // Get all things
            var things = sts.things().query().list();
            // Register all things
            things.fullIterator().forEachRemaining(this::registerThing);
        }
        catch (ServiceFailureException e) {
            throw new RuntimeException(e);
        }
    }

    private void registerThing(Thing thing) {
        workerExecutor.submit(() -> {
            try {
                // SensorML-ify the Thing
                var smlThing = SensorThingsUtils.toSmlProcess(thing);
                var parentSystem = new SystemWrapper(smlThing);
                // Add system to database
                var handler = transactionHandler.addOrUpdateSystem(parentSystem);

                try {
                    // Add Thing's locations as FOIs
                    var thingLocations = thing.locations().query().list();
                    for(Location location : thingLocations.toList())
                        handler.addFoi(SensorThingsUtils.toGmlFeature(location, SensorThingsUtils.toUid(location.getName(), location.getId())));
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }

                var datastreams = thing.datastreams().query().top(100).list();
                Iterator<Datastream> datastreamIterator = datastreams.fullIterator();

                Map<Id<?>, SensorData> smlSensorMap = new HashMap<>();

                while(datastreamIterator.hasNext()) {
                    var datastream = datastreamIterator.next();
                    var sensor = datastream.getSensor();

                    // Keep track of Sensors
                    var sensorData = smlSensorMap.get(sensor.getId());
                    if(sensorData == null) {
                        sensorData = new SensorData(SensorThingsUtils.toSmlProcess(sensor));
                        smlSensorMap.put(sensor.getId(), sensorData);
                    }

                    // Add datastream output to its sensor description
                    sensorData.smlDescription.addOutput(datastream.getName(), SensorThingsUtils.toSweCommon(datastream));

                    // Keep track of the association between current datastream and sensor
                    sensorData.datastreams.put(datastream.getName(), datastream);
                }

                // Register Sensors to parent Thing
                for(SensorData sensorData : smlSensorMap.values())
                    registerSensor(sensorData, handler);
            } catch (Exception e) {
                throw new RuntimeException("Failed to register Thing: {}" + thing.toString(), e);
            }
        });
    }

    private void registerSensor(SensorData sensorData, SystemTransactionHandler parentHandler) throws DataStoreException, ServiceFailureException {
        var smlSensor = sensorData.smlDescription;

        // Add or update member handler
        var system = new SystemWrapper(smlSensor);
        var memberHandler = parentHandler.addOrUpdateMember(system);

        // Create datastreams if we have outputs
        if (smlSensor.getNumOutputs() > 0)
            SystemUtils.addDatastreamsFromOutputs(memberHandler, smlSensor.getOutputList());

        // Add historical observations
        for (OgcProperty<AbstractSWEIdentifiable> output : system.getFullDescription().getOutputList().getProperties()) {
            workerExecutor.submit(() -> {
                try {
                    // Get datastream handler for output
                    var dsHandler = this.transactionHandler.getDataStreamHandler(memberHandler.getSystemUID(), output.getName());
                    var datastream = sensorData.datastreams.get(output.getName());

                    // Add historical observations
                    if (!this.usingStateDb) {
                        var observations = datastream.observations().query().list();
                        Iterator<Observation> i = observations.fullIterator();

                        while (i.hasNext()) {
                            var observation = i.next();
                            // Add FOI if available
                            var feature = observation.getFeatureOfInterest();
                            BigId featureId = null;
                            if (feature != null)
                                featureId = parentHandler.addOrUpdateFoi(SensorThingsUtils.toGmlFeature(feature, SensorThingsUtils.toUid(feature.getName(), feature.getId()))).getInternalID();
                            var obs = SensorThingsUtils.toObsData(observation, dsHandler.getDataStreamKey().getInternalID(), featureId);
                            // Add observation
                            dsHandler.addObs(obs);
                        }
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            });
        }
    }

}
