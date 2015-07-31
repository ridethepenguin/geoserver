/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogr.core;

import java.util.Map;


/**
 * Represents the tool configuration as a whole.
 * Only used for XStream driven de-serialization.
 * 
 * @author Andrea Aime - OpenGeo
 */
public class ToolConfiguration {

    protected String executable;
    protected Map<String, String> environment;
    protected Format[] formats;

    public ToolConfiguration() {
    }

    public ToolConfiguration(String executable, Map<String, String> environment, Format[] formats) {
        super();
        this.executable = executable;
        this.environment = environment;
        this.formats = formats;
    }

    public String getExecutable() {
        return executable;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public Format[] getFormats() {
        return formats;
    }

    public void setExecutable(String executable) {
        this.executable = executable;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    public void setFormats(Format[] formats) {
        this.formats = formats;
    }

}
