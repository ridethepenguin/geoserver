/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceListener;
import org.geoserver.platform.resource.ResourceNotification;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

import com.thoughtworks.xstream.XStream;

/**
 * Loads the gdal_translate.xml configuration file and configures the output format accordingly.
 *
 * <p>Also keeps tabs on the configuration file, reloading the file as needed.
 * 
 * @author Stefano Costa, GeoSolutions
 *
 */
public class GdalConfigurator implements ApplicationListener<ContextClosedEvent> {
    private static final Logger LOGGER = Logging.getLogger(GdalConfigurator.class);

    /**
     * The {@link CoverageResponseDelegate} implementation doing the actual conversion
     */
    public GdalCoverageResponseDelegate responseDelegate;

    GdalWrapper wrapper;

    Resource configFile;

    // ConfigurationPoller
    private ResourceListener listener = new ResourceListener() {
        public void changed(ResourceNotification notify) {
            loadConfiguration();
        }
    };

    public GdalConfigurator(GdalCoverageResponseDelegate responseDelegate) {
        this.responseDelegate = responseDelegate;

        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        configFile = loader.get("gdal_translate.xml");
        loadConfiguration();
        configFile.addListener( listener );
    }

    public void loadConfiguration() {
        // start with the default configuration, override if we can load the file
        GdalConfiguration configuration = GdalConfiguration.DEFAULT;
        try {
            if (configFile.getType() == Type.RESOURCE) {
                InputStream in = configFile.in();
                try {
                    XStream xstream = buildXStream();
                    configuration = (GdalConfiguration) xstream.fromXML(in);
                }
                finally {
                    in.close();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error reading the gdal_translate.xml configuration file", e);
        }

        if (configuration == null) {
            LOGGER.log(Level.INFO,
                            "Could not find/load the gdal_translate.xml configuration file, using internal defaults");
        }

        // let's load the configuration
        GdalWrapper wrapper = new GdalWrapper(configuration.gdalTranslateLocation, configuration.gdalData);
        Set<String> supported = wrapper.getSupportedFormats();
        responseDelegate.setGdalTranslateExecutable(configuration.gdalTranslateLocation);
        responseDelegate.setGdalData(configuration.gdalData);
        List<GdalFormat> formatsToAdd = new ArrayList<GdalFormat>(supported.size());
        for (GdalFormat format : configuration.formats) {
            if (supported.contains(format.gdalFormat)) {
                formatsToAdd.add(format);
            } else {
                LOGGER.severe("Skipping '" + format.formatName + "' as its GDAL format '"
                        + format.gdalFormat + "' is not among the ones supported by "
                        + configuration.gdalTranslateLocation);
            }
        }
        responseDelegate.replaceFormats(formatsToAdd);
    }

    /**
     * Builds and configures the XStream used for de-serializing the configuration
     * @return
     */
    static XStream buildXStream() {
        XStream xstream = new XStream();
        xstream.alias("GdalConfiguration", GdalConfiguration.class);
        xstream.alias("Format", GdalFormat.class);
        xstream.addImplicitCollection(GdalFormat.class, "options", "option", String.class);
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
