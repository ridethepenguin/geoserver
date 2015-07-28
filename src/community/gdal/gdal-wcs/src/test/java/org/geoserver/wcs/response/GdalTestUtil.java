/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.util.logging.Logging;

public class GdalTestUtil {
    private static Logger LOGGER = Logging.getLogger(GdalTestUtil.class);

    private static Boolean IS_GDAL_AVAILABLE;
    private static String GDAL_TRANSLATE;
    private static String GDAL_DATA;

    public static boolean isGdalAvailable() {

        // check this just once
        if (IS_GDAL_AVAILABLE == null) {
            try {
                File props = new File("./src/test/resources/gdal_translate.properties");
                Properties p = new Properties();
                p.load(new FileInputStream(props));
                
                GDAL_TRANSLATE = p.getProperty("gdal_translate");
                // assume it's in the path if the property file hasn't been configured
                if(GDAL_TRANSLATE == null)
                    GDAL_TRANSLATE = "gdal_translate";
                GDAL_DATA = p.getProperty("gdalData");
                
                GdalWrapper gdal = new GdalWrapper(GDAL_TRANSLATE, GDAL_DATA);
                IS_GDAL_AVAILABLE = gdal.isAvailable();
            } catch (Exception e) {
                IS_GDAL_AVAILABLE = false;
                e.printStackTrace();
                LOGGER.log(Level.SEVERE,
                        "Disabling gdal_translate output format tests, as gdal_translate lookup failed", e);
            }
        }

        return IS_GDAL_AVAILABLE;
    }
    
    public static String getGdalTranslate() {
        if(isGdalAvailable())
            return GDAL_TRANSLATE;
        else
            return null;
    }
    
    public static String getGdalData() {
        if(isGdalAvailable())
            return GDAL_DATA;
        else
            return null;
    }
    
    
}
