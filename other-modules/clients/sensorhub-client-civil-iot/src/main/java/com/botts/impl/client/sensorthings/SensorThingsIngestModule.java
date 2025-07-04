/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2024 Botts Innovative Research, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package com.botts.impl.client.sensorthings;

import com.google.common.base.Strings;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.event.IEventBus;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.system.SystemDatabaseTransactionHandler;
import org.vast.util.Asserts;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.*;


/**
 * <p>
 *     Service to ingest SensorThings API resources as OSH-readable resources.
 * </p>
 *
 * @author Alex Almanza
 * @since Dec 9, 2024
 */
public class SensorThingsIngestModule extends AbstractModule<SensorThingsIngestConfig>
{
    IEventBus eventBus;
    SystemDatabaseTransactionHandler transactionHandler;
    IObsSystemDatabase writeDb;
    ExecutorService executor;
    ExecutorService workerExecutor;

    @Override
    public void setConfiguration(SensorThingsIngestConfig config)
    {
        super.setConfiguration(config);
    }

    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        // Get obs database to store ingested data
        if (!Strings.isNullOrEmpty(config.databaseID)) {
            writeDb = (IObsSystemDatabase) getParentHub().getModuleRegistry().getModuleById(config.databaseID);
            if (writeDb != null && !writeDb.isOpen())
                writeDb = null;
        }
        else
            writeDb = getParentHub().getSystemDriverRegistry().getSystemStateDatabase();

        eventBus = getParentHub().getEventBus();
        transactionHandler = new SystemDatabaseTransactionHandler(eventBus, writeDb);

        Asserts.checkNotNull(config.staBaseResourcePathList, "Must specify SensorThings endpoints for ingestion");
        if (config.staBaseResourcePathList.isEmpty())
            reportError("Must specify SensorThings endpoints for ingestion", new IllegalArgumentException());

        // Create executor services for ingesting data
        executor = Executors.newFixedThreadPool(config.staBaseResourcePathList.size());
        int cores = Runtime.getRuntime().availableProcessors();
        int numThreads = Math.max(16, cores*2);
        workerExecutor = new ThreadPoolExecutor(
                numThreads,
                numThreads,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Override
    protected void doStart() throws SensorHubException {
        // TODO: Ingest Datastreams, Things, Sensors, Observations, ObservedProperties. Store in writeDb
        var urls = config.staBaseResourcePathList;
        Asserts.checkNotNull(urls);

        for (var url : urls) {
            try {
                URL sensorThingsUrl = new URL(url);
                SensorThingsIngestor ingestor = new SensorThingsIngestor(sensorThingsUrl,
                        Objects.equals(writeDb, getParentHub().getSystemDriverRegistry().getSystemStateDatabase()),
                        transactionHandler,
                        executor);
                executor.submit(ingestor::start);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        reportStatus("Queued up ingestion of " + config.staBaseResourcePathList.size() + " URL(s).");
    }

}
