/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogr.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Parameters defining an output format generated using an external tool ogr2ogr either a GML or a shapefile
 * or a GeoTIFF dump.
 *
 * @author Andrea Aime - OpenGeo
 *
 */
public class Format {
    /**
     * The tool output format name
     */
    private String toolFormat;

    /**
     * The GeoServer output format name
     */
    private String geoserverFormat;

    /**
     * The extension of the generated file, if any (shall include a dot, example, ".tab")
     */
    private String fileExtension;

    /**
     * The options that will be added to the command line
     */
    private List<String> options;

    /**
     * The type of format, used to instantiate the correct converter
     */
    private OutputType type;

    /**
     * If the output is a single file that can be streamed back. In that case we also need to know the mime type
     */
    private boolean singleFile;

    /**
     * The mime type of the single file output
     */
    private String mimeType;

    public Format() {
        this.options = Collections.emptyList();
    }

    public Format(String toolFormat, String formatName, String fileExtension, boolean singleFile,
            String mimeType, OutputType type, String... options) {
        this.toolFormat = toolFormat;
        this.geoserverFormat = formatName;
        this.fileExtension = fileExtension;
        this.singleFile = singleFile;
        this.mimeType = mimeType;
        this.type = type;
        if (options != null) {
            this.options = new ArrayList<String>(Arrays.asList(options));
        }
        if (type == null) {
            this.type = OutputType.BINARY;
        }
    }

    public Format(String toolFormat, String formatName, String fileExtension, boolean singleFile,
            String mimeType, String... options) {
        this(toolFormat, formatName, fileExtension, singleFile, mimeType, OutputType.BINARY, options);
    }

    public String getToolFormat() {
        return toolFormat;
    }

    public void setToolFormat(String toolFormat) {
        this.toolFormat = toolFormat;
    }

    public String getGeoserverFormat() {
        return geoserverFormat;
    }

    public void setGeoserverFormat(String geoserverFormat) {
        this.geoserverFormat = geoserverFormat;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public OutputType getType() {
        return type;
    }

    public void setType(OutputType type) {
        this.type = type;
    }

    public boolean isSingleFile() {
        return singleFile;
    }

    public void setSingleFile(boolean singleFile) {
        this.singleFile = singleFile;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

}
