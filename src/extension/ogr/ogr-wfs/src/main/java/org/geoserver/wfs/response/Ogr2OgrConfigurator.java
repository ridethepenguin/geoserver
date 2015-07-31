/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.util.Map;

import org.geoserver.ogr.core.AbstractToolConfigurator;
import org.geoserver.ogr.core.ToolConfiguration;
import org.geoserver.ogr.core.ToolWrapper;

import com.thoughtworks.xstream.XStream;

/**
 * Loads the ogr2ogr.xml configuration file and configures the output format accordingly.
 *
 * <p>Also keeps tabs on the configuration file, reloading the file as needed.
 * @author Administrator
 *
 */
public class Ogr2OgrConfigurator extends AbstractToolConfigurator {

    public Ogr2OgrConfigurator(Ogr2OgrOutputFormat format) {
        super(format);
    }

    @Override
    protected String getConfigurationFile() {
        return "ogr2ogr.xml";
    }

    @Override
    protected ToolConfiguration getDefaultConfiguration() {
        return OgrConfiguration.DEFAULT;
    }

    @Override
    protected ToolWrapper createWrapper(String executable, Map<String, String> environment) {
        return new OGRWrapper(executable, environment);
    }

    @Override
    protected XStream buildXStream() {
        XStream xstream = super.buildXStream();
        // setup OGR-specific aliases
        xstream.alias("OgrConfiguration", OgrConfiguration.class);
        xstream.alias("Format", OgrFormat.class);

        return xstream;
    }

}
