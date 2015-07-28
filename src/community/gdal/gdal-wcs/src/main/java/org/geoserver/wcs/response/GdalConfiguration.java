/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import com.thoughtworks.xstream.XStream;

/**
 * Represents the gdal_translate output format configuration as a whole.
 * Only used for XStream driven de-serialization.
 * 
 * @author Stefano Costa, GeoSolutions
 */
public class GdalConfiguration {
    public static final GdalConfiguration DEFAULT;
    static {
        DEFAULT = new GdalConfiguration();
        // assume it's in the classpath and GDAL_DATA is properly set in the enviroment
        DEFAULT.gdalTranslateLocation = "gdal_translate";
        // add some default formats
        DEFAULT.formats = new GdalFormat[] {
                new GdalFormat("JPEG2000", "GDAL-JPEG2000", ".jp2", true, "image/jp2"),
                new GdalFormat("PDF", "GDAL-PDF", ".pdf", true, "application/pdf"),
                new GdalFormat("AAIGrid", "GDAL-ArcInfoGrid", ".asc", false, null),
                new GdalFormat("XYZ", "GDAL-XYZ", ".txt", true, "text/plain", GdalType.TEXT)
        };
    }

    /**
     * The full path to gdal_translate
     */
    public String gdalTranslateLocation;
    /**
     * The full path of the GDAL_DATA folder
     */
    public String gdalData;
    /**
     * The configured output formats 
     */
    public GdalFormat[] formats;

    public static void main(String[] args) {
        // generates the default configuration xml and prints it to the output
        XStream xstream = GdalConfigurator.buildXStream();
        System.out.println(xstream.toXML(GdalConfiguration.DEFAULT));
    }
}
