package edu.utexas.stac;

import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CliOption {

    public enum LogLevel {
        ALL, VERBOSE, DEFAULT, SILENT
    }

    @CommandLine.Parameters(arity = "1..*", paramLabel = "FILE", description
            = "Jar file(s) to process.")
    private ArrayList<String> inputFiles;

    @CommandLine.Option(names = {"-l", "--log-level"}, description = "Specify" +
            " the log level: SILENT, DEFAULT, " + "VERBOSE, or ALL")
    private LogLevel logLevel = LogLevel.DEFAULT;

    @CommandLine.Option(names = {"--output-java-version"}, description =
            "Specify the Java version of the output " + "file. Valid range is" +
                    " from 1 to 8, and 0 means the default")
    private int outputJavaVersion = 0;

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true,
            description = "Displays this help message and quits.")
    private boolean helpRequested = false;

    @CommandLine.Option(names = {"-o", "--output"}, description = "The output" +
            " jar file (default to ./instrumented.jar). Note that the " +
            "manifest file of the first input jar, if exists, will be copied " +
            "into this output jar.")
    private String output = "instrumented.jar";

    public List<String> getInputFiles() {
        return Collections.unmodifiableList(inputFiles);
    }

    void appendInputFile(String file) {
        inputFiles.add(file);
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public int getOutputJavaVersion() {
        return outputJavaVersion;
    }

    public boolean isHelpRequested() {
        return helpRequested;
    }

    public String getOutputFile() {
        return output;
    }

    public void validate() {
        if (outputJavaVersion < 0 || outputJavaVersion > 8)
            throw new RuntimeException("Illegal output Java version: " +
                    outputJavaVersion);
    }
}
