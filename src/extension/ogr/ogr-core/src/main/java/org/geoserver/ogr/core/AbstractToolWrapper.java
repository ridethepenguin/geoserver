/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogr.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Base class for helpers used to invoke an external tool.
 * 
 * @author Andrea Aime - GeoSolutions
 * @author Stefano Costa - GeoSolutions
 */
public abstract class AbstractToolWrapper implements ToolWrapper {

    private String executable;
    private Map<String, String> environment;

    public AbstractToolWrapper(String executable, Map<String, String> environment) {
        this.executable = executable;
        this.environment = new HashMap<String, String>();
        if (environment != null) {
            this.environment.putAll(environment);
        }
    }

    @Override
    public String getExecutable() {
        return executable;
    }

    @Override
    public Map<String, String> getEnvironment() {
        return new HashMap<String, String>(environment);
    }

    @Override
    public boolean isInputFirst() {
        return true;
    }

    @Override
    public File convert(File inputData, File outputDirectory, String typeName,
            Format format, CoordinateReferenceSystem crs) throws IOException, InterruptedException {
        // build the command line
        List<String> cmd = new ArrayList<String>();
        cmd.add(executable);

        String toolFormatParameter = getToolFormatParameter();
        if (toolFormatParameter != null) {
            cmd.add(toolFormatParameter);
            cmd.add(format.getToolFormat());
        }

        if (format.getOptions() != null) {
            for (String option : format.getOptions()) {
                cmd.add(option);
            }
        }

        onBeforeRun(cmd, inputData, outputDirectory, typeName, format, crs);

        String outFileName = typeName;
        if (format.getFileExtension() != null)
            outFileName += format.getFileExtension();
        if (isInputFirst()) {
            cmd.add(inputData.getAbsolutePath());
            cmd.add(new File(outputDirectory, outFileName).getAbsolutePath());
        } else {
            cmd.add(new File(outputDirectory, outFileName).getAbsolutePath());
            cmd.add(inputData.getAbsolutePath());
        }

        StringBuilder sb = new StringBuilder();
        int exitCode = -1;
        try {
            exitCode = run(cmd, sb);
        } finally {
            onAfterRun(exitCode);
        }

        if (exitCode != 0)
            throw new IOException(executable + " did not terminate successfully, exit code " + exitCode
                    + ". Was trying to run: " + cmd + "\nResulted in:\n" + sb);

        // output may be a directory, handle that case gracefully
        File output = new File(outputDirectory, outFileName);
        if(output.isDirectory()) {
            output = new File(output, outFileName);
        }
        return output;
    }

    /**
     * Utility method to dump a {@link CoordinateReferenceSystem} to a temporary file on disk.
     * 
     * @param parentDir
     * @param crs
     * @return the temp file containing the CRS definition in WKT format
     * @throws IOException 
     */
    protected static File dumpCrs(File parentDir, CoordinateReferenceSystem crs) throws IOException {
      File crsFile = null;
      if (crs != null) {
          // we don't use an EPSG code since there is no guarantee we'll be able to reverse
          // engineer one. Using WKT also ensures the EPSG params such as the TOWGS84 ones are
          // not lost in the conversion
          // We also write to a file because some operating systems cannot take arguments with
          // quotes and spaces inside (and/or ProcessBuilder is not good enough to escape them)
          crsFile = File.createTempFile("srs", "wkt", parentDir);
          String s = crs.toWKT();
          s = s.replaceAll("\n\r", "").replaceAll("  ", "");
          FileUtils.writeStringToFile(crsFile, s);
      }

      return crsFile;
    }

    protected void onBeforeRun(List<String> cmd, File inputData, File outputDirectory, String typeName,
            Format format, CoordinateReferenceSystem crs) throws IOException {
        // default implementation does nothing
    }

    protected void onAfterRun(int exitCode) throws IOException {
        // default implementation does nothing
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
    protected int run(List<String> cmd, StringBuilder sb) throws IOException, InterruptedException {
        // run the process and grab the output for error reporting purposes
        ProcessBuilder builder = new ProcessBuilder(cmd);
        if(environment != null)
            builder.environment().putAll(environment);
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
