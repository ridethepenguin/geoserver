package org.geoserver.test;

import static org.junit.Assert.assertEquals;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geotools.image.test.ImageAssert;
import org.geotools.util.URLs;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class NestedDefaultGeometryTest extends AbstractAppSchemaTestSupport {

    // GML 3.1 namespaces
    private static final String STATIONS_PREFIX_GML31 = "st_gml31";
    // GML 3.2 namespaces
    private static final String STATIONS_PREFIX_GML32 = "st_gml32";

    // xpath engines used to check WFS responses
    private XpathEngine WFS11_XPATH_ENGINE;
    private XpathEngine WFS20_XPATH_ENGINE;

    @Before
    public void beforeTest() {
        // instantiate WFS 1.1 xpath engine
        WFS11_XPATH_ENGINE = StationsMockData.buildXpathEngine(
                getTestData().getNamespaces(),
                "wfs", "http://www.opengis.net/wfs",
                "gml", "http://www.opengis.net/gml");
        // instantiate WFS 2.0 xpath engine
        WFS20_XPATH_ENGINE = StationsMockData.buildXpathEngine(
                getTestData().getNamespaces(),
                "ows", "http://www.opengis.net/ows/1.1",
                "wfs", "http://www.opengis.net/wfs/2.0",
                "gml", "http://www.opengis.net/gml/3.2");
    }

    @Override
    protected StationsMockData createTestData() {
        // instantiate our custom complex types
        StationsMockData mockData = new MockData();
        mockData.addStyle("Default", "styles/default_point.sld");
        return mockData;
    }

    private static final class MockData extends StationsMockData {

        @Override
        public void addContent() {
            // add GML 3.1 namespaces
            putNamespace(STATIONS_PREFIX_GML31, STATIONS_URI_GML31);
            // add GML 3.2 namespaces
            putNamespace(STATIONS_PREFIX_GML32, STATIONS_URI_GML32);
            // add GML 3.1 feature type
            Map<String, String> gml31Parameters = new HashMap<>();
            gml31Parameters.put("GML_PREFIX", "gml31");
            gml31Parameters.put("GML_NAMESPACE", "http://www.opengis.net/gml");
            gml31Parameters.put("GML_LOCATION", "http://schemas.opengis.net/gml/3.1.1/base/gml.xsd");
            addStationFeatureType(STATIONS_PREFIX_GML31, "gml31", "stations", "nestedGeometryMappings/stations.xml", gml31Parameters);
            // add GML 3.2 feature type
            Map<String, String> gml32Parameters = new HashMap<>();
            gml32Parameters.put("GML_PREFIX", "gml32");
            gml32Parameters.put("GML_NAMESPACE", "http://www.opengis.net/gml/3.2");
            gml32Parameters.put("GML_LOCATION", "http://schemas.opengis.net/gml/3.2.1/gml.xsd");
            addStationFeatureType(STATIONS_PREFIX_GML32, "gml32", "stations", "nestedGeometryMappings/stations.xml", gml32Parameters);
        }
        
        @Override
        protected void addStationFeatureType(String namespacePrefix, String gmlPrefix,
                String mappingsName, String mappingsPath, Map<String, String> parameters) {
            // create root directory
            File gmlDirectory = new File(TEST_ROOT_DIRECTORY, gmlPrefix);
            gmlDirectory.mkdirs();
            // add the necessary files
            File stationsMappings = new File(gmlDirectory, String.format("%s_%s.xml", mappingsName, gmlPrefix));
            File stationsProperties = new File(gmlDirectory, String.format("stations_%s.properties", gmlPrefix));
            File stationsSchema = new File(gmlDirectory, String.format("stations_%s.xsd", gmlPrefix));
            // perform the parameterization
            substituteParameters("/test-data/stations/" + mappingsPath, parameters, stationsMappings);
            substituteParameters("/test-data/stations/data/stationsNestedGeometry.properties", parameters, stationsProperties);
            substituteParameters("/test-data/stations/schemas/stationsNestedGeometry.xsd", parameters, stationsSchema);
            // create station feature type
            addFeatureType(namespacePrefix, String.format("Station_%s", gmlPrefix),
                    stationsMappings.getAbsolutePath(),
                    stationsProperties.getAbsolutePath(),
                    stationsSchema.getAbsolutePath());
        }

    }
    
    @Test
    public void testWfsGetFeature() throws XpathException {
        Document document = getAsDOM("wfs?request=GetFeature&version=2.0&typename=st_gml32:Station_gml32");
        
        try {
            String stationXpath = "/wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32";
            assertEquals(3, WFS20_XPATH_ENGINE.getMatchingNodes(stationXpath, document).getLength());
        } catch (Exception exception) {
            throw new RuntimeException("Error evaluating xpath.", exception);
        }
    }
    
    @Test
    public void testWmsGetMap() throws IOException {
        String getMapUrl = new StringBuilder().append("wms?request=GetMap").append("&")
            .append("SRS=EPSG:4326").append("&")
            .append("layers=st_gml32:Station_gml32").append("&")
            .append("styles=Default").append("&")
            .append("BBOX=-180,-90,180,90").append("&")
            .append("WIDTH=256").append("&")
            .append("HEIGHT=256").append("&")
            .append("FORMAT=image/png")
            .toString();

        try (InputStream is = getBinary(getMapUrl)) {
            BufferedImage imageBuffer = ImageIO.read(is);
            assertNotBlank("app-schema test getmap nested default geom", imageBuffer, Color.WHITE);
        }
    }
}
