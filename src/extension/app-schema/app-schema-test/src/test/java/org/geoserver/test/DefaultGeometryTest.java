package org.geoserver.test;

import static org.geotools.data.complex.ComplexFeatureConstants.DEFAULT_GEOMETRY_LOCAL_NAME;
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
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class DefaultGeometryTest extends AbstractAppSchemaTestSupport {

    private static final String GML31_PREFIX = "gml31";

    private static final String GML32_PREFIX = "gml32";

    private static final String XPATH_BASE_GML31 = "/wfs:FeatureCollection/gml:featureMember";

    private static final String XPATH_BASE_GML32 = "/wfs:FeatureCollection/wfs:member";

    private static final String STATION_FEATURE = "st_${GML_PREFIX}:Station_${GML_PREFIX}";

    private static final String STATION_WITH_MEASUREMENTS_FEATURE = "st_${GML_PREFIX}:StationWithMeasurements_${GML_PREFIX}";

    private static final String XPATH_STATION = "/st_${GML_PREFIX}:Station_${GML_PREFIX}";

    private static final String XPATH_STATION_FAKE_GEOM = XPATH_STATION + "/st_${GML_PREFIX}:"
            + DEFAULT_GEOMETRY_LOCAL_NAME;

    private static final String XPATH_STATION_GEOM = XPATH_STATION
            + "/st_${GML_PREFIX}:location/st_${GML_PREFIX}:position/gml:Point";

    private static final String XPATH_STATION_WITH_MEASUREMENTS = "/st_${GML_PREFIX}:StationWithMeasurements_${GML_PREFIX}";

    private static final String XPATH_STATION_WITH_MEASUREMENTS_FAKE_GEOM = XPATH_STATION_WITH_MEASUREMENTS
            + "/st_${GML_PREFIX}:" + DEFAULT_GEOMETRY_LOCAL_NAME;

    private static final String XPATH_STATION_WITH_MEASUREMENTS_GEOM = XPATH_STATION_WITH_MEASUREMENTS
            + "/st_${GML_PREFIX}:measurements/ms_${GML_PREFIX}:Measurement_${GML_PREFIX}/ms_${GML_PREFIX}:sampledArea/ms_${GML_PREFIX}:SampledArea/ms_${GML_PREFIX}:geometry";

    // xpath engines used to check WFS responses
    private XpathEngine WFS11_XPATH_ENGINE;

    private XpathEngine WFS20_XPATH_ENGINE;

    @Before
    public void beforeTest() {
        // instantiate WFS 1.1 xpath engine
        WFS11_XPATH_ENGINE = StationsMockData.buildXpathEngine(getTestData().getNamespaces(), "wfs",
                "http://www.opengis.net/wfs", "gml", "http://www.opengis.net/gml");
        // instantiate WFS 2.0 xpath engine
        WFS20_XPATH_ENGINE = StationsMockData.buildXpathEngine(getTestData().getNamespaces(), "ows",
                "http://www.opengis.net/ows/1.1", "wfs", "http://www.opengis.net/wfs/2.0", "gml",
                "http://www.opengis.net/gml/3.2");
    }

    @Override
    protected StationsMockData createTestData() {
        // instantiate our custom complex types
        StationsMockData mockData = new MockData();
        mockData.addStyle("Default_Point", "styles/default_point.sld");
        mockData.addStyle("Default_Polygon", "styles/default_polygon.sld");
        return mockData;
    }

    private static final class MockData extends StationsMockData {

        @Override
        public void addContent() {
            // add GML 3.1 namespaces
            putNamespace(STATIONS_PREFIX_GML31, STATIONS_URI_GML31);
            putNamespace(MEASUREMENTS_PREFIX_GML31, MEASUREMENTS_URI_GML31);
            // add GML 3.2 namespaces
            putNamespace(STATIONS_PREFIX_GML32, STATIONS_URI_GML32);
            putNamespace(MEASUREMENTS_PREFIX_GML32, MEASUREMENTS_URI_GML32);
            // add GML 3.1 feature types
            Map<String, String> gml31Parameters = new HashMap<>();
            gml31Parameters.put("GML_PREFIX", "gml31");
            gml31Parameters.put("GML_NAMESPACE", "http://www.opengis.net/gml");
            gml31Parameters.put("GML_LOCATION",
                    "http://schemas.opengis.net/gml/3.1.1/base/gml.xsd");
            addStationFeatureType(STATIONS_PREFIX_GML31, "gml31", "Station", "stations",
                    "defaultGeometryMappings/stations.xml", gml31Parameters);
            addStationFeatureType(STATIONS_PREFIX_GML31, "gml31", "StationWithMeasurements",
                    "stations", "defaultGeometryMappings/stationsWithMeasurements.xml",
                    "measurements", "defaultGeometryMappings/measurements.xml", gml31Parameters);
            addMeasurementFeatureType(MEASUREMENTS_PREFIX_GML31, "gml31", "measurements",
                    "defaultGeometryMappings/measurements.xml", gml31Parameters);
            // add GML 3.2 feature types
            Map<String, String> gml32Parameters = new HashMap<>();
            gml32Parameters.put("GML_PREFIX", "gml32");
            gml32Parameters.put("GML_NAMESPACE", "http://www.opengis.net/gml/3.2");
            gml32Parameters.put("GML_LOCATION", "http://schemas.opengis.net/gml/3.2.1/gml.xsd");
            addStationFeatureType(STATIONS_PREFIX_GML32, "gml32", "Station", "stations",
                    "defaultGeometryMappings/stations.xml", gml32Parameters);
            addStationFeatureType(STATIONS_PREFIX_GML32, "gml32", "StationWithMeasurements",
                    "stations", "defaultGeometryMappings/stationsWithMeasurements.xml",
                    "measurements", "defaultGeometryMappings/measurements.xml", gml32Parameters);
            addMeasurementFeatureType(MEASUREMENTS_PREFIX_GML32, "gml32", "measurements",
                    "defaultGeometryMappings/measurements.xml", gml32Parameters);
        }

        protected void addStationFeatureType(String namespacePrefix, String gmlPrefix,
                String featureType, String mappingsName, String mappingsPath,
                Map<String, String> parameters) {
            // create root directory
            File gmlDirectory = new File(TEST_ROOT_DIRECTORY, gmlPrefix);
            gmlDirectory.mkdirs();
            // add the necessary files
            File stationsMappings = new File(gmlDirectory,
                    String.format("%s_%s.xml", mappingsName, gmlPrefix));
            File stationsProperties = new File(gmlDirectory,
                    String.format("stations_%s.properties", gmlPrefix));
            File stationsSchema = new File(gmlDirectory,
                    String.format("stations_%s.xsd", gmlPrefix));
            // perform the parameterization
            substituteParameters("/test-data/stations/" + mappingsPath, parameters,
                    stationsMappings);
            substituteParameters("/test-data/stations/data/stationsDefaultGeometry.properties",
                    parameters, stationsProperties);
            substituteParameters("/test-data/stations/schemas/stationsDefaultGeometry.xsd",
                    parameters, stationsSchema);
            // create station feature type
            addFeatureType(namespacePrefix, String.format("%s_%s", featureType, gmlPrefix),
                    stationsMappings.getAbsolutePath(), stationsProperties.getAbsolutePath(),
                    stationsSchema.getAbsolutePath());
        }

        /**
         * Helper method that will add the station feature type customizing it for the desired GML version.
         */
        protected void addStationFeatureType(String namespacePrefix, String gmlPrefix,
                String stationsFeatureType, String stationsMappingsName,
                String stationsMappingsPath, String measurementsMappingsName,
                String measurementsMappingsPath, Map<String, String> parameters) {
            // create root directory
            File gmlDirectory = new File(TEST_ROOT_DIRECTORY, gmlPrefix);
            gmlDirectory.mkdirs();
            // add the necessary files
            File stationsMappings = new File(gmlDirectory,
                    String.format("%s_%s.xml", stationsMappingsName, gmlPrefix));
            File stationsProperties = new File(gmlDirectory,
                    String.format("stations_%s.properties", gmlPrefix));
            File stationsSchema = new File(gmlDirectory,
                    String.format("stations_%s.xsd", gmlPrefix));
            File measurementsSchema = new File(gmlDirectory,
                    String.format("measurements_%s.xsd", gmlPrefix));
            // perform the parameterization
            substituteParameters("/test-data/stations/" + stationsMappingsPath, parameters,
                    stationsMappings);
            substituteParameters("/test-data/stations/data/stationsDefaultGeometry.properties",
                    parameters, stationsProperties);
            substituteParameters("/test-data/stations/schemas/stationsDefaultGeometry.xsd",
                    parameters, stationsSchema);
            substituteParameters("/test-data/stations/schemas/measurementsDefaultGeometry.xsd",
                    parameters, measurementsSchema);
            // create station feature type
            addFeatureType(namespacePrefix, String.format("%s_%s", stationsFeatureType, gmlPrefix),
                    stationsMappings.getAbsolutePath(), stationsProperties.getAbsolutePath(),
                    stationsSchema.getAbsolutePath(), measurementsSchema.getAbsolutePath());
        }

        @Override
        protected void addMeasurementFeatureType(String namespacePrefix, String gmlPrefix,
                String mappingsName, String mappingsPath, Map<String, String> parameters) {
            // create root directory
            File gmlDirectory = new File(TEST_ROOT_DIRECTORY, gmlPrefix);
            gmlDirectory.mkdirs();
            // add the necessary files
            File measurementsMappings = new File(gmlDirectory,
                    String.format("%s_%s.xml", mappingsName, gmlPrefix));
            File measurementsProperties = new File(gmlDirectory,
                    String.format("measurements_%s.properties", gmlPrefix));
            File measurementsSchema = new File(gmlDirectory,
                    String.format("measurements_%s.xsd", gmlPrefix));
            // perform the parameterization
            substituteParameters("/test-data/stations/" + mappingsPath, parameters,
                    measurementsMappings);
            substituteParameters("/test-data/stations/data/measurementsDefaultGeometry.properties",
                    parameters, measurementsProperties);
            substituteParameters("/test-data/stations/schemas/measurementsDefaultGeometry.xsd",
                    parameters, measurementsSchema);
            // create measurements feature type
            addFeatureType(namespacePrefix, String.format("Measurement_%s", gmlPrefix),
                    measurementsMappings.getAbsolutePath(),
                    measurementsProperties.getAbsolutePath(), measurementsSchema.getAbsolutePath());
        }

    }

    @Test
    public void testWfs11GetFeatureDefaultGeometry() throws Exception {
        testWfsGetFeature("1.1.0", STATION_FEATURE, XPATH_STATION, XPATH_STATION_FAKE_GEOM,
                XPATH_STATION_GEOM);
        testWfsGetFeature("1.1.0", STATION_WITH_MEASUREMENTS_FEATURE,
                XPATH_STATION_WITH_MEASUREMENTS, XPATH_STATION_WITH_MEASUREMENTS_FAKE_GEOM,
                XPATH_STATION_WITH_MEASUREMENTS_GEOM);
    }

    @Test
    public void testWfs20GetFeatureDefaultGeometry() throws Exception {
        testWfsGetFeature("2.0", STATION_FEATURE, XPATH_STATION, XPATH_STATION_FAKE_GEOM,
                XPATH_STATION_GEOM);
        testWfsGetFeature("2.0", STATION_WITH_MEASUREMENTS_FEATURE, XPATH_STATION_WITH_MEASUREMENTS,
                XPATH_STATION_WITH_MEASUREMENTS_FAKE_GEOM, XPATH_STATION_WITH_MEASUREMENTS_GEOM);
    }

    public void testWfsGetFeature(String wfsVersion, String featureType, String xpathFeature,
            String xpathFakeGeom, String xpathActualGeom) throws Exception {
        String gmlPrefix;
        String xpathBase;
        XpathEngine xpathEngine;
        
        if (wfsVersion.equals("2.0")) {
            gmlPrefix = GML32_PREFIX;
            xpathBase = XPATH_BASE_GML32;
            xpathEngine = WFS20_XPATH_ENGINE;
        } else {
            gmlPrefix = GML31_PREFIX;
            xpathBase = XPATH_BASE_GML31;
            xpathEngine = WFS11_XPATH_ENGINE;
        }

        String getFeatureUrl = buildGetFeatureUrl(wfsVersion,
                featureType.replace("${GML_PREFIX}", gmlPrefix));
        Document document = getAsDOM(getFeatureUrl);

        String xpath = null;
        try {
            xpath = xpathBase + xpathFeature.replace("${GML_PREFIX}", gmlPrefix);
            assertEquals(3, xpathEngine.getMatchingNodes(xpath, document).getLength());
            // attribute specified as default geometry must be encoded
            xpath = xpathBase + xpathActualGeom.replace("${GML_PREFIX}", gmlPrefix);
            assertEquals(3, xpathEngine.getMatchingNodes(xpath, document).getLength());
            // fake default geometry attribute must not be encoded, as it's not present
            // in the XML schema type
            xpath = xpathBase + xpathFakeGeom.replace("${GML_PREFIX}", gmlPrefix);
            assertEquals(0, xpathEngine.getMatchingNodes(xpath, document).getLength());
        } catch (Exception exception) {
            throw new RuntimeException("Error evaluating xpath.", exception);
        }
    }

    @Test
    public void testWmsGetMapDefaultGeometry() throws IOException {
        String layer = STATION_FEATURE.replace("${GML_PREFIX}", GML31_PREFIX);
        try (InputStream is = getBinary(buildGetMapUrl(layer, "Default_Point"))) {
            BufferedImage imageBuffer = ImageIO.read(is);
            assertNotBlank("app-schema test getmap nested default geom", imageBuffer, Color.WHITE);
        }

        layer = STATION_FEATURE.replace("${GML_PREFIX}", GML32_PREFIX);
        try (InputStream is = getBinary(
                buildGetMapUrl("st_gml32:Station_gml32", "Default_Point"))) {
            BufferedImage imageBuffer = ImageIO.read(is);
            assertNotBlank("app-schema test getmap nested default geom", imageBuffer, Color.WHITE);
        }
    }

    @Test
    public void testWmsGetMapDefaultGeometryInChainedFeature() throws IOException {
        String layer = STATION_WITH_MEASUREMENTS_FEATURE.replace("${GML_PREFIX}", GML31_PREFIX);
        try (InputStream is = getBinary(buildGetMapUrl(layer, "Default_Polygon"))) {
            BufferedImage imageBuffer = ImageIO.read(is);
            assertNotBlank("app-schema test getmap nested default geom feature chaining",
                    imageBuffer, Color.WHITE);
        }

        layer = STATION_WITH_MEASUREMENTS_FEATURE.replace("${GML_PREFIX}", GML32_PREFIX);
        try (InputStream is = getBinary(buildGetMapUrl(layer, "Default_Polygon"))) {
            BufferedImage imageBuffer = ImageIO.read(is);
            assertNotBlank("app-schema test getmap nested default geom feature chaining",
                    imageBuffer, Color.WHITE);
        }
    }

    public String buildGetFeatureUrl(String wfsVersion, String featureType) {
        String getFeatureUrl = new StringBuilder().append("wfs?request=GetFeature").append("&")
                .append("version=").append(wfsVersion).append("&").append("typename=")
                .append(featureType).toString();

        return getFeatureUrl;
    }

    public String buildGetMapUrl(String layers, String style) {
        String getMapUrl = new StringBuilder().append("wms?request=GetMap").append("&")
                .append("SRS=EPSG:4326").append("&").append("layers=").append(layers).append("&")
                .append("styles=").append(style).append("&").append("BBOX=-10,-10,10,10")
                .append("&").append("WIDTH=256").append("&").append("HEIGHT=256").append("&")
                .append("FORMAT=image/png").toString();

        return getMapUrl;
    }
}
