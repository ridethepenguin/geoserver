/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Helper used to invoke gdal_translate.
 * 
 * @author Stefano Costa, GeoSolutions
 * 
 */
public class GdalWrapper {

    private static final Logger LOGGER = Logging.getLogger(GdalWrapper.class);

    private String gdalTranslateExecutable;
    private String gdalData;

    /**
     * @param gdalTranslateExecutable full path to gdal_translate
     * @param gdalData full path to GDAL_DATA folder
     */
    public GdalWrapper(String gdalTranslateExecutable, String gdalData) {
        this.gdalTranslateExecutable = gdalTranslateExecutable;
        this.gdalData = gdalData;
    }

    /**
     * Performs the conversion, returns the (main) output file 
     */
    public File convert(File inputData, File outputDirectory, String typeName,
            GdalFormat format, CoordinateReferenceSystem crs) throws IOException, InterruptedException {
        // build the command line
        List<String> cmd = new ArrayList<String>();
        cmd.add(gdalTranslateExecutable);
        cmd.add("-of");
        cmd.add(format.gdalFormat);

        File crsFile = null;
        if (crs != null) {
            // we don't use an EPSG code since there is no guarantee we'll be able to reverse
            // engineer one. Using WKT also ensures the EPSG params such as the TOWGS84 ones are
            // not lost in the conversion
            // We also write to a file because some operating systems cannot take arguments with
            // quotes and spaces inside (and/or ProcessBuilder is not good enough to escape them)
            crsFile = File.createTempFile("gdal_srs", "wkt", inputData.getParentFile());
            cmd.add("-a_srs");
            String s = crs.toWKT();
            s = s.replaceAll("\n\r", "").replaceAll("  ", "");
            FileUtils.writeStringToFile(crsFile, s);
            cmd.add(crsFile.getAbsolutePath());
        }
        if (format.options != null) {
            for (String option : format.options) {
                cmd.add(option);
            }
        }
        String outFileName = typeName;
        if (format.fileExtension != null)
            outFileName += format.fileExtension;
        cmd.add(inputData.getAbsolutePath());
        cmd.add(new File(outputDirectory, outFileName).getAbsolutePath());

        StringBuilder sb = new StringBuilder();
        int exitCode = run(cmd, sb);
        if (crsFile != null) {
            crsFile.delete();
        }

        if (exitCode != 0)
            throw new IOException("gdal_translate did not terminate successfully, exit code " + exitCode
                    + ". Was trying to run: " + cmd + "\nResulted in:\n" + sb);
        
        // TODO: do I really need this?
        // output may be a directory, try to handle that case gracefully
        File output = new File(outputDirectory, outFileName);
        if(output.isDirectory()) {
            output = new File(output, outFileName);
        }
        
        return output;
    }

    /**
     * Returns a list of the gdal_translate supported formats (i.e. what must be passed to gdal_translate via its -of parameter)
     * 
     * @return
     */
    public Set<String> getSupportedFormats() {
        try {
            // this works with gdal_translate v. 1.11.2
            // TODO: test with other GDAL versions
            List<String> commands = new ArrayList<String>();
            commands.add(gdalTranslateExecutable);
            commands.add("--long-usage");

            Set<String> formats = new HashSet<String>();
            addFormats(commands, formats);

            return formats;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "Could not get the list of output formats supported by gdal_translate", e);
            return Collections.emptySet();
        }
    }

    /**
     * Runs the provided command and parses its output to extract a set of supported formats. 
     *  
     * @param commands the command to run
     * @param formats the parsed formats will be added to this set 
     * @throws IOException
     * @throws InterruptedException
     */
    private void addFormats(List<String> commands, Set<String> formats) throws IOException,
            InterruptedException {
        StringBuilder sb = new StringBuilder();
        // can't trust the exit code, --help exits with -1 on my pc
        run(commands, sb);

        Pattern formatRegExp = Pattern.compile("^\\s{2}(\\w+)\\:\\s");
        String[] lines = sb.toString().split("\n");
        for (String line : lines) {
            Matcher formatMatcher = formatRegExp.matcher(line);
            if (formatMatcher.find()) {
                String format = formatMatcher.group(1);
                formats.add(format);
            }
        }
    }

    /**
     * Returns true if gdal_translate is available, that is, if executing
     * "gdal_translate --version" returns 0 as the exit code
     * 
     * @return
     */
    public boolean isAvailable() {
        List<String> commands = new ArrayList<String>();
        commands.add(gdalTranslateExecutable);
        commands.add("--version");

        try {
            return run(commands, null) == 0;
        } catch(Exception e) {
            LOGGER.log(Level.SEVERE, "gdal_translate is not available", e);
            return false;
        }
    }

    /**
     * Runs the specified command appending the output to the string builder and
     * returning the exit code
     * 
     * @param cmd
     * @param sb
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    int run(List<String> cmd, StringBuilder sb) throws IOException, InterruptedException {
        // run the process and grab the output for error reporting purposes
        ProcessBuilder builder = new ProcessBuilder(cmd);
        if(gdalData != null)
            builder.environment().put("GDAL_DATA", gdalData);
        builder.redirectErrorStream(true);
        Process p = builder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (sb != null) {
                sb.append("\n");
                sb.append(line);
            }
        }
        return p.waitFor();
    }
}
