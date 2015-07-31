/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogr.core;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceListener;
import org.geoserver.platform.resource.ResourceNotification;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.extended.NamedMapConverter;

/**
 * Loads the tool configuration file and configures the output formats accordingly.
 *
 * <p>Also keeps tabs on the configuration file, reloading it as needed.
 * 
 * @author Andrea Aime - GeoSolutions
 * @author Stefano Costa - GeoSolutions
 */
public abstract class AbstractToolConfigurator implements ApplicationListener<ContextClosedEvent> {
    private static final Logger LOGGER = Logging.getLogger(AbstractToolConfigurator.class);

    public FormatConverter of;

    protected Resource configFile;

    // ConfigurationPoller
    protected ResourceListener listener = new ResourceListener() {
        public void changed(ResourceNotification notify) {
            loadConfiguration();
        }
    };

    public AbstractToolConfigurator(FormatConverter format) {
        this.of = format;

        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        configFile = loader.get(getConfigurationFile());
        loadConfiguration();
        configFile.addListener( listener );
    }

    protected abstract String getConfigurationFile();

    protected abstract ToolConfiguration getDefaultConfiguration();

    protected abstract ToolWrapper createWrapper(String executable, Map<String, String> environment);

    public void loadConfiguration() {
        // start with the default configuration, override if we can load the file
        ToolConfiguration configuration = getDefaultConfiguration();
        try {
            if (configFile.getType() == Type.RESOURCE) {
                InputStream in = configFile.in();
                try {
                    XStream xstream = buildXStream();
                    configuration = (ToolConfiguration) xstream.fromXML( in);
                }
                finally {
                    in.close();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error reading the " + getConfigurationFile() + " configuration file", e);
        }

        if (configuration == null) {
            LOGGER.log(Level.INFO,
                            "Could not find/load the " + getConfigurationFile() + " configuration file, using internal defaults");
            configuration = getDefaultConfiguration();
        }

        // should never happen, but just in case...
        if (configuration == null) {
            throw new IllegalStateException("No default configuration available, giving up");
        }

        // let's load the configuration
        ToolWrapper wrapper = createWrapper(configuration.getExecutable(), configuration.getEnvironment());
        Set<String> supported = wrapper.getSupportedFormats();
        of.setExecutable(configuration.getExecutable());
        of.setEnvironment(configuration.getEnvironment());
        of.clearFormats();
        for (Format format : configuration.getFormats()) {
            if (supported.contains(format.getToolFormat())) {
                of.addFormat(format);
            } else {
                LOGGER.severe("Skipping '" + format.getGeoserverFormat() + "' as its tool format '"
                        + format.getToolFormat() + "' is not among the ones supported by "
                        + configuration.getExecutable());
            }
        }
    }

    /**
     * Builds and configures the XStream used for de-serializing the configuration
     * @return
     */
    protected XStream buildXStream() {
        XStream xstream = new XStream();
        xstream.alias("ToolConfiguration", ToolConfiguration.class);
        xstream.alias("Format", Format.class);
        xstream.addImplicitCollection(Format.class, "options", "option", String.class);
        NamedMapConverter environmentConverter = new NamedMapConverter(xstream
                .getMapper(), "variable", "name", String.class, "value", String.class,
                true, true, xstream.getConverterLookup());
        xstream.registerConverter(environmentConverter);

        return xstream;
    }

    /**
     * Kill all threads on web app context shutdown to avoid permgen leaks
     */
    public void onApplicationEvent(ContextClosedEvent event) {
        if( configFile != null ){
            configFile.removeListener(listener);
        }
    }

}
