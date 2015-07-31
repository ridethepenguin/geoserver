package org.geoserver.ogr.core;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

public interface ToolWrapper {

    public String getExecutable();

    public Map<String, String> getEnvironment();

    public String getToolFormatParameter();

    public boolean isInputFirst();

    /**
     * Returns a list of the tool supported formats
     * 
     * @return
     */
    public Set<String> getSupportedFormats();

    /**
     * Returns true if the specified executable command is available and can be run.
     * 
     * @return
     */
    public boolean isAvailable();

    /**
     * Performs the conversion, returns the name of the (main) output file 
     */
    public File convert(File inputData, File outputDirectory, String typeName,
            Format format, CoordinateReferenceSystem crs) throws IOException, InterruptedException;

}
