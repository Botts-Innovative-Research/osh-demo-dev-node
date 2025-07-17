
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

package org.sensorhub.impl.comm.mavlink2.util;

import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;

import org.locationtech.proj4j.*;

/**
 * Handle coordinate transformations using
 * Azimuthal Equidistant projection
 *
 * @author Michael Stinson
 * @since 2.0
 */
public class ProjectionAzEq {

    private final CRSFactory crsFactory = new CRSFactory();
    //aeqd = Azimuthal Equidistant
    private String projString = "";
    private CoordinateReferenceSystem azEqProjection = null;

    private CoordinateTransform tfm = null;

    private CoordinateTransform inv = null;

    public ProjectionAzEq ( Double pLatitude, Double pLongitude ) {
        projString = "+proj=aeqd +lon_0="+ pLongitude + " +lat_0=" + pLatitude + " +ellps=WGS84";
        azEqProjection = crsFactory.createFromParameters("Custom_AE", projString);
        tfm = new CoordinateTransformFactory().createTransform(
                crsFactory.createFromName("EPSG:4326"),
                azEqProjection
        );
        inv = new CoordinateTransformFactory().createTransform(
                azEqProjection,
                crsFactory.createFromName("EPSG:4326")
        );
    }

    public ProjCoordinate transform( ProjCoordinate pSourceCoord )  {

        var resultCoord = new ProjCoordinate();
        tfm.transform( pSourceCoord, resultCoord );
        return resultCoord;
    }

    public ProjCoordinate inverse( ProjCoordinate pSourceCoord)  {

        var resultCoord = new ProjCoordinate();
        inv.transform( pSourceCoord, resultCoord );
        return resultCoord;
    }
}
