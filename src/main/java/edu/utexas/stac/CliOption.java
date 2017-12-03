package edu.utexas.stac;

import edu.utexas.stac.ant.InstrumentTask;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CliOption {

    public enum LogLevel {
        ALL, VERBOSE, DEFAULT, SILENT
    }

    public enum Strategy {
        LIGHT, HEAVY
    }

    @CommandLine.Parameters(arity = "1..*", paramLabel = "FILE", description
            = "Jar file(s) to process.")
    private List<String> inputFiles;

    @CommandLine.Option(names = {"-l", "--log-level"}, description = "Specify" +
            " the log level: SILENT, DEFAULT, " + "VERBOSE, or ALL.")
    private LogLevel logLevel = LogLevel.DEFAULT;

    @CommandLine.Option(names = {"-j", "--output-java-version"}, description =
            "Specify the Java version of the output " + "file. Valid range is" +
                    " from 0 to 8, where 0 means the default.")
    private int outputJavaVersion = 0;

    @CommandLine.Option(names = {"-s", "--strategy"}, description = "Specify " +
            "the instrumentation strategy. The LIGHT strategy only increase " +
            "the cost by 1 for each method entry and loop header. The HEAVY " +
            "strategy will count the number of Soot instructions for each " +
            "basic block and increase the cost by that number at the end of " +
            "each block. Default to LIGHT.")
    private Strategy strategy = Strategy.LIGHT;

    @CommandLine.Option(names = {"-e", "--extract-cost-jar"}, description = "Extract cost.jar to the path specified " +
            "in --output, and quit immediately. No instrumentation is performed.")
    private boolean extractCostJar = false;

    @CommandLine.Option(names = {"-x", "--exclude-cost-from-output"}, description = "Prevent cost.jar from getting " +
            "included in the output jar.")
    private boolean excludeCostJar = false;

    @CommandLine.Option(names = {"-p", "--preserve-names"}, description = "Preserve debugging info such as variable " +
            "names as much as possible. Note that this option only provides a best-effort guarantee.")
    private boolean preserveNames = false;

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true,
            description = "Displays this help message and quits.")
    private boolean helpRequested = false;

    @CommandLine.Option(names = {"-o", "--output"}, description = "The output" +
            " jar file (default to ./instrumented.jar). Note that the " +
            "manifest file of the first input jar, if exists, will be copied " +
            "into this output jar.")
    private String output = "instrumented.jar";

    @CommandLine.Option(names = {"--exclude-classes"}, split = ",", description = "Stop instrumenting the classes " +
            "with specified patterns. Patterns are separated by comma. This option is mainly used to get around bugs " +
            "in Soot.")
    private List<String> blacklistedClasses = new ArrayList<>();

    public List<String> getInputFiles() {
        return Collections.unmodifiableList(inputFiles);
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public int getOutputJavaVersion() {
        return outputJavaVersion;
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public boolean isExcludeCostJar() {
        return excludeCostJar;
    }

    public boolean isExtractCostJar() {
        return extractCostJar;
    }

    public boolean isPreserveNames() {
        return preserveNames;
    }

    public boolean isHelpRequested() {
        return helpRequested;
    }

    public String getOutputFile() {
        return output;
    }

    public List<String> getBlacklistedClasses() {
        return Collections.unmodifiableList(blacklistedClasses);
    }

    public void validate() {
        if (outputJavaVersion < 0 || outputJavaVersion > 8)
            throw new RuntimeException("Illegal output Java version: " +
                    outputJavaVersion);
    }

    // TODO: decouple cli options from instrumentation options
    private String customCostJarLocation = null;
    public String getCustomCostJarLocation() { return customCostJarLocation; }

    public static CliOption createForAntTask(List<String> inputFiles, String outputFile,
                                             String customCostJarLocation, List<InstrumentTask.Pattern> blacklist) {
        CliOption option = new CliOption();
        option.inputFiles = inputFiles;
        option.output = outputFile;
        option.blacklistedClasses = blacklist.stream().map(p -> p.getPattern()).collect(Collectors.toList());
        option.excludeCostJar = true;
        option.logLevel = LogLevel.SILENT;
        option.customCostJarLocation = customCostJarLocation;
        option.outputJavaVersion = 8;
        return option;
    }
}
