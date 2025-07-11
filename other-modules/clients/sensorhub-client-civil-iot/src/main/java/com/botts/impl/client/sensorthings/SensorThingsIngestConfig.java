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

import org.sensorhub.api.client.ClientConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.database.IObsSystemDatabase;

import java.util.ArrayList;
import java.util.List;


/**
 * <p>
 * Configuration class for the STA Ingest Module
 * </p>
 *
 * @author Alex Almanza
 * @since Dec 9, 2024
 */
public class SensorThingsIngestConfig extends ClientConfig {
    @DisplayInfo(label="SensorThings API Base URLs", desc="List of SensorThings API base URLs (i.e. https://www.example.com/v1.0/")
    public List<String> staBaseResourcePathList = new ArrayList<>();

    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.MODULE_ID)
    @DisplayInfo.ModuleType(IObsSystemDatabase.class)
    @DisplayInfo(label = "Database ID", desc = "Database to store ingested data from SensorThings API. If none available, only the most recent ")
    public String databaseID = null;
}
