/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import java.util.HashMap;
import java.util.Map;

import org.geoserver.ogr.core.AbstractToolConfigurator;
import org.geoserver.ogr.core.Format;
import org.geoserver.ogr.core.FormatConverter;
import org.geoserver.ogr.core.OutputType;
import org.geoserver.ogr.core.ToolConfiguration;
import org.geoserver.ogr.core.ToolWrapper;

/**
 * Loads the gdal_translate.xml configuration file and configures the output format accordingly.
 *
 * <p>Also keeps tabs on the configuration file, reloading the file as needed.
 * 
 * @author Stefano Costa, GeoSolutions
 *
 */
public class GdalConfigurator extends AbstractToolConfigurator {

    public static final ToolConfiguration DEFAULT;
    static {
        // assume it's in the classpath and GDAL_DATA is properly set in the enviroment
        // and add some default formats
        DEFAULT = new ToolConfiguration(
                "gdal_translate",
                new HashMap<String, String>(),
                new Format[] {
                    new Format("JPEG2000", "GDAL-JPEG2000", ".jp2", true, "image/jp2"),
                    new Format("PDF", "GDAL-PDF", ".pdf", true, "application/pdf"),
                    new Format("AAIGrid", "GDAL-ArcInfoGrid", ".asc", false, null),
                    new Format("XYZ", "GDAL-XYZ", ".txt", true, "text/plain", OutputType.TEXT)
                });
    }

    public GdalConfigurator(FormatConverter format) {
        super(format);
    }

    @Override
    protected String getConfigurationFile() {
        return "gdal_translate.xml";
    }

    @Override
    protected ToolConfiguration getDefaultConfiguration() {
        return DEFAULT;
    }

    @Override
    protected ToolWrapper createWrapper(String executable, Map<String, String> environment) {
        return new GdalWrapper(executable, environment);
    }

}
