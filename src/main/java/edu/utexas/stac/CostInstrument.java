package edu.utexas.stac;

import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CostInstrument {

    private static String extractUtilJar() {
        try {
            Path utilJarPath = Files.createTempFile("util", ".jar");
            utilJarPath.toFile().deleteOnExit();

            InputStream link = ClassLoader.getSystemResourceAsStream("util.jar");
            Files.copy(link, utilJarPath, StandardCopyOption.REPLACE_EXISTING);
            return utilJarPath.toString();
        } catch (IOException e) {
            System.err.println("Error when extracting util.jar: " + e.getMessage());
            System.exit(-1);
        }
        throw new RuntimeException("Should not reach here");
    }

    private static void initLogger(CliOption.LogLevel logLevel) {
        System.setProperty(
                "java.util.logging.SimpleFormatter.format",
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

        // Extract utility jar file
        String utilJar = extractUtilJar();
        cliOption.appendInputFile(utilJar);

        // Instrumentation work starts here
        SootUtil.initialize(cliOption);
        SootJarFileInstrumenter jarFileInstrumenter = new SootJarFileInstrumenter(new SootClassInstrumenter(new
                LightWeightMethodInstrumenter()));
        for (String jarFile: cliOption.getInputFiles())
            jarFileInstrumenter.instrumentJarFile(jarFile);

        // Dump the result to the output jar
        Manifest outputManifest = null;
        try {
            outputManifest= new JarFile(cliOption.getInputFiles().get(0))
                    .getManifest();
        } catch (IOException e) {
            // Try to carry on the work without the manifest
        }
        JarWriter jarWriter = new JarWriter(cliOption.getOutputFile(),
                cliOption.getOutputJavaVersion(), outputManifest);
        jarWriter.writeJars(cliOption.getInputFiles());
    }
}
