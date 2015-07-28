/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parameters defining an output format generated using gdal_translate from a GeoTIFF dump
 *
 * @author Stefano Costa, GeoSolutions
 *
 */
public class GdalFormat {
    /**
     * The ??? parameter
     */
    public String gdalFormat;

    /**
     * The GeoServer output format name
     */
    public String formatName;

    /**
     * The extension of the generated file, if any (shall include a dot, example, ".tab")
     */
    public String fileExtension;

    /**
     * The options that will be added to the command line
     */
    public List<String> options;

    /**
     * The type of format, used to instantiate the correct converter
     */
    public GdalType type;

    /**
     * If the output is a single file that can be streamed back. In that case we also need to know the mime type
     */
    public boolean singleFile;

    /**
     * The mime type of the single file output
     */
    public String mimeType;

    public GdalFormat(String gdalFormat, String formatName, String fileExtension, boolean singleFile,
            String mimeType, GdalType type, String... options) {
        this.gdalFormat = gdalFormat;
        this.formatName = formatName;
        this.fileExtension = fileExtension;
        this.singleFile = singleFile;
        this.mimeType = mimeType;
        this.type = type;
        if (options != null) {
            this.options = new ArrayList<String>(Arrays.asList(options));
        }
        if (type == null) {
            this.type = GdalType.BINARY;
        }
    }

    public GdalFormat(String gdalFormat, String formatName, String fileExtension, boolean singleFile,
            String mimeType, String... options) {
        this(gdalFormat, formatName, fileExtension, singleFile, mimeType, GdalType.BINARY, options);
    }

}
