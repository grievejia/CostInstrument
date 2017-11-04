package edu.utexas.stac;

import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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

    public static void main(String args[]) {
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

        // Set up logging format
        System.setProperty(
                "java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] [%4$7s] %5$s%6$s%n");
        // Extract utility jar file
        String utilJar = extractUtilJar();
        cliOption.appendInputFile(utilJar);

        SootUtil.initialize(cliOption);
        for (String jarFile: cliOption.getInputFiles())
            SootUtil.instrumentJar(jarFile);
        SootUtil.writeJar(cliOption);
    }
}
