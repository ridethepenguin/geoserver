package org.geoserver.ogr.core;

import java.util.List;
import java.util.Map;

public interface FormatConverter {

    public String getExecutable();

    public void setExecutable(String executable);

    public Map<String, String> getEnvironment();

    public void setEnvironment(Map<String, String> environment);

    /**
     * Adds a format among the supported ones
     * 
     * @param format
     */
    public void addFormat(Format format);

    /**
     * Get a list of supported formats
     *
     * @return
     */
    public List<Format> getFormats();

    /**
     * Programmatically removes all formats
     */
    public void clearFormats();

    /**
     * Replaces currently supported formats with the provided list.
     *  
     * @param formats
     */
    public void replaceFormats(List<Format> formats);

}
