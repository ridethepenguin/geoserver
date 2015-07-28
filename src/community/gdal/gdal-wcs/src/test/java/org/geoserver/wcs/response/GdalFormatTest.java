/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.geoserver.config.impl.GeoServerImpl;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.referencing.CRS;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class GdalFormatTest {

    static final String TEST_RESOURCE = "/sfdem.tif";

    static final double[][] TEST_XYZ_DATA = new double[][] {
            { 589995, 4927995, -9.99999993381581251e+36 },
            { 590025, 4927995, -9.99999993381581251e+36 },
            { 590055, 4927995, -9.99999993381581251e+36 },
            { 590085, 4927995, -9.99999993381581251e+36 },
            { 590115, 4927995, -9.99999993381581251e+36 } };

    static final int TEST_GRID_COLS = 634;
    static final double TEST_GRID_NODATA = -9.9999999338158125107e+36;
    static final String[] TEST_GRID_HEADER_LABEL = new String[] {
        "ncols", "nrows", "xllcorner", "yllcorner", "cellsize", "NODATA_value"
    };
    static final double[] TEST_GRID_HEADER_DATA = new double[] {
        TEST_GRID_COLS, 477, 589980.0, 4913700.0, 30.0, TEST_GRID_NODATA
    };

    static final double EQUALS_TOLERANCE = 1E-12;

    GdalCoverageResponseDelegate gdalCovRespDelegate;

    @Before
    public void setUp() throws Exception {
        // check if we can run the tests
        Assume.assumeTrue(GdalTestUtil.isGdalAvailable());

        // the coverage response delegate
        gdalCovRespDelegate = new GdalCoverageResponseDelegate(new GeoServerImpl());
        // add default formats
        for (GdalFormat format : GdalConfiguration.DEFAULT.formats) {
            gdalCovRespDelegate.addFormat(format);
        }

        gdalCovRespDelegate.setGdalTranslateExecutable(GdalTestUtil.getGdalTranslate());
        gdalCovRespDelegate.setGdalData(GdalTestUtil.getGdalData());
    }

    @Test
    public void testCanProduce() {
        assertTrue(gdalCovRespDelegate.canProduce("GDAL-JPEG2000"));
        assertTrue(gdalCovRespDelegate.canProduce("GDAL-XYZ"));
        // not among default formats
        assertFalse(gdalCovRespDelegate.canProduce("GDAL-MrSID"));
    }

    @Test
    public void testContentTypeZip() {
        assertEquals("application/zip", gdalCovRespDelegate.getMimeType("GDAL-ArcInfoGrid"));
        assertEquals("zip", gdalCovRespDelegate.getFileExtension("GDAL-ArcInfoGrid"));
    }

    @Test
    public void testContentTypeJP2K() {
        assertEquals("image/jp2", gdalCovRespDelegate.getMimeType("GDAL-JPEG2000"));
        assertEquals("jp2", gdalCovRespDelegate.getFileExtension("GDAL-JPEG2000"));
    }

    @Test
    public void testContentTypePDF() {
        assertEquals("application/pdf", gdalCovRespDelegate.getMimeType("GDAL-PDF"));
        assertEquals("pdf", gdalCovRespDelegate.getFileExtension("GDAL-PDF"));
    }

    @Test
    public void testContentTypeText() {
        assertEquals("text/plain", gdalCovRespDelegate.getMimeType("GDAL-XYZ"));
        assertEquals("txt", gdalCovRespDelegate.getFileExtension("GDAL-XYZ"));
    }

    @Test
    public void testXYZ() throws Exception {
        // prepare input
        GridCoverage2DReader covReader = new GeoTiffReader(getClass().getResource(TEST_RESOURCE));
        GridCoverage2D cov = covReader.read(null);

        // write out
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        gdalCovRespDelegate.encode(cov, "GDAL-XYZ", null, bos);

        // parse the text output to check it's really XYZ data
        checkXyzData(new ByteArrayInputStream(bos.toByteArray()));
    }

    private void checkXyzData(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        int maxCount = 5, count = 0;
        String line = null;
        while ((line = reader.readLine()) != null && count < maxCount) {
            String[] cols = line.trim().split(" ");
            assertTrue(cols.length == 3);
            assertEquals(TEST_XYZ_DATA[count][0], (double) Double.valueOf(cols[0]),
                    EQUALS_TOLERANCE);
            assertEquals(TEST_XYZ_DATA[count][1], (double) Double.valueOf(cols[1]),
                    EQUALS_TOLERANCE);
            assertEquals(TEST_XYZ_DATA[count][2], (double) Double.valueOf(cols[2]),
                    EQUALS_TOLERANCE);
            count++;
        }
    }

    @Test
    public void testZippedGrid() throws Exception {
        // prepare input
        GridCoverage2DReader covReader = new GeoTiffReader(getClass().getResource(TEST_RESOURCE));
        GridCoverage2D cov = covReader.read(null);

        // write out
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        gdalCovRespDelegate.encode(cov, "GDAL-ArcInfoGrid", null, bos);

        // unzip the result
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bos.toByteArray()));
        // check contents
        boolean gridFileFound = false, auxFileFound = false, prjFileFound = false;
        ZipEntry entry = null;
        while ((entry = zis.getNextEntry()) != null) {
            if ("sfdem.asc.aux.xml".equals(entry.getName())) {
                auxFileFound = true;
            } else if ("sfdem.prj".equals(entry.getName())) {
                prjFileFound = true;
                // check projection
                checkGridProjection(zis);
            } else if ("sfdem.asc".equals(entry.getName())) {
                gridFileFound = true;
                // check grid content
                checkGridContent(zis);
            }
        }
        assertTrue(gridFileFound);
        assertTrue(auxFileFound);
        assertTrue(prjFileFound);
    }

    private void checkGridProjection(InputStream is) throws Exception {
        String wkt = IOUtils.readLines(is).get(0);
        CoordinateReferenceSystem crs = CRS.parseWKT(wkt);
        assertNotNull(crs);
        assertEquals("NAD_1927_UTM_Zone_13N", crs.getName().getCode());
    }

    private void checkGridContent(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        int row = 0, maxRow = 8;
        String line = null;
        while ((line = reader.readLine()) != null && row < maxRow) {
            String[] cols = line.trim().replaceAll("\\s+", " ").split(" ");
            if (row < TEST_GRID_HEADER_LABEL.length) {
                assertEquals(2, cols.length);
                assertEquals(TEST_GRID_HEADER_LABEL[row], cols[0].trim());
                assertEquals(TEST_GRID_HEADER_DATA[row], Double.valueOf(cols[1].trim()), EQUALS_TOLERANCE);
            } else {
                assertEquals(TEST_GRID_COLS, cols.length);
                assertEquals(TEST_GRID_NODATA, Double.valueOf(cols[0].trim()), EQUALS_TOLERANCE);
            }
            row++;
        }
    }

}
