/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.gdal;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.wcs.response.GdalCoverageResponseDelegate;
import org.geoserver.wcs.response.GdalFormat;
import org.geoserver.wps.ppio.PPIOFactory;
import org.geoserver.wps.ppio.ProcessParameterIO;

/**
 * Factory to create an output PPIO for each GDAL format managed by gdal_translate command.
 */
public class GdalPPIOFactory implements PPIOFactory {

    private GdalCoverageResponseDelegate delegate;

    public GdalPPIOFactory(GdalCoverageResponseDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<ProcessParameterIO> getProcessParameterIO() {
        List<ProcessParameterIO> gdalParams = new ArrayList<ProcessParameterIO>();
        for (GdalFormat of : this.delegate.getFormats()) {
            ProcessParameterIO ppio = null;
            String computedMimeType = delegate.getMimeType(of.formatName);
            if (of.formatName != null && !of.formatName.isEmpty()) {
                computedMimeType = computedMimeType + "; subtype=" + of.formatName;
            }
            if (of.type == null) {
                // binary is the default type
                ppio = new GdalBinaryPPIO(of.formatName, delegate, computedMimeType);
            } else {
                switch (of.type) {
                case BINARY:
                    ppio = new GdalBinaryPPIO(of.formatName, delegate, computedMimeType);
                    break;
                case TEXT:
                    ppio = new GdalCDataPPIO(of.formatName, delegate, computedMimeType);
                    break;
                case XML:
                    ppio = new GdalXMLPPIO(of.formatName, delegate);
                    break;
                default:
                    break;
                }
            }
            if (ppio != null) {
                gdalParams.add(ppio);
            }
        }
        return gdalParams;
    }

}
