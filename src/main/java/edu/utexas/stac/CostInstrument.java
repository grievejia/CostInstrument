package edu.utexas.stac;

import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CostInstrument {

    private static String extractCostJar(Path dstPath) {
        try {
            InputStream link = ClassLoader.getSystemResourceAsStream("cost" +
                    ".jar");
            Files.copy(link, dstPath, StandardCopyOption.REPLACE_EXISTING);
            return dstPath.toString();
        } catch (IOException e) {
            System.err.println("Error when extracting cost.jar: " + e
                    .getMessage());
            System.exit(-1);
        }
        throw new RuntimeException("Should not reach here");
    }

    private static String extractCostJarToTempdir() {
        Path costJarPath = null;
        try {
            costJarPath = Files.createTempFile("cost", ".jar");
            costJarPath.toFile().deleteOnExit();
        } catch (IOException e) {
            System.err.println("Error when creating temporary jar file: " + e
                    .getMessage());
            System.exit(-1);
        }
        return extractCostJar(costJarPath);
    }

    private static void initLogger(CliOption.LogLevel logLevel) {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] [%4$7s] %5$s%6$s%n");
        Logger rootLogger = Logger.getLogger("");
        switch (logLevel) {
            case SILENT:
                rootLogger.setLevel(Level.OFF);
                rootLogger.getHandlers()[0].setLevel(Level.OFF);
                break;
            case DEFAULT:
                rootLogger.setLevel(Level.INFO);
                rootLogger.getHandlers()[0].setLevel(Level.INFO);
                break;
            case VERBOSE:
                rootLogger.setLevel(Level.FINE);
                rootLogger.getHandlers()[0].setLevel(Level.FINE);
                break;
            case ALL:
                rootLogger.setLevel(Level.ALL);
                rootLogger.getHandlers()[0].setLevel(Level.ALL);
                break;
        }
    }

    private static SootMethodInstrumenter getMethodInstrumenter(CliOption
                                                                        .Strategy strategy) {
        switch (strategy) {
            case HEAVY:
                return new HeavyWeightMethodInstrumenter();
            default:
                return new LightWeightMethodInstrumenter();
        }
    }

    public static void main(String args[]) {
        // Read cmdline options
        CliOption cliOption = new CliOption();
        try {
            cliOption = CommandLine.populateCommand(cliOption, args);
        } catch (CommandLine.MissingParameterException e) {
            System.err.println(e.getMessage());
            CommandLine.usage(cliOption, System.err);
            System.exit(-1);
        }
        if (cliOption.isHelpRequested()) {
            CommandLine.usage(cliOption, System.out);
            System.exit(0);
        }
        cliOption.validate();

        // Initialize logging facilities
        initLogger(cliOption.getLogLevel());

        // Extract cost.jar file
        if (cliOption.isExtractCostJar()) {
            extractCostJar(Paths.get(cliOption.getOutputFile()));
            System.exit(0);
        }
        String costJar = extractCostJarToTempdir();
        List<String> inputPaths =
                cliOption.isExcludeCostJar() ?
                        cliOption.getInputFiles() :
                        SootUtil.getInputPaths(cliOption, costJar);

        // Instrumentation work starts here
        SootUtil.initialize(cliOption, costJar);
        SootMethodInstrumenter methodInstrumenter = getMethodInstrumenter
                (cliOption.getStrategy());
        SootJarFileInstrumenter jarFileInstrumenter = new
                SootJarFileInstrumenter(new SootClassInstrumenter
                (methodInstrumenter));
        for (String jarFile : inputPaths)
            jarFileInstrumenter.instrumentJarFile(jarFile);

        // Dump the result to the output jar
        Manifest outputManifest = null;
        try {
            outputManifest = new JarFile(cliOption.getInputFiles().get(0))
                    .getManifest();
        } catch (IOException e) {
            // Try to carry on the work without the manifest
        }
        JarWriter jarWriter = new JarWriter(cliOption.getOutputFile(),
                cliOption.getOutputJavaVersion(), outputManifest);
        jarWriter.writeJars(inputPaths);
    }
}
