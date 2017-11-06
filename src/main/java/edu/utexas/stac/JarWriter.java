package edu.utexas.stac;

import soot.Scene;
import soot.SootClass;
import soot.SourceLocator;
import soot.baf.BafASMBackend;
import soot.options.Options;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

public class JarWriter {

    private static Logger logger = Logger.getLogger(JarWriter.class.getName());

    private String outFileName;
    private Manifest manifest;
    private int sootJavaVersion;

    private static int getSootJavaVersion(int javaVersion) {
        switch (javaVersion) {
            case 0:
                return Options.java_version_default;
            case 1:
                return Options.java_version_1;
            case 2:
                return Options.java_version_2;
            case 3:
                return Options.java_version_3;
            case 4:
                return Options.java_version_4;
            case 5:
                return Options.java_version_5;
            case 6:
                return Options.java_version_6;
            case 7:
                return Options.java_version_7;
            case 8:
                return Options.java_version_8;
        }
        throw new RuntimeException("Unrecognized java version: " + javaVersion);
    }

    private static File getOutputFile(String fileName) throws IOException {
        Path path = Paths.get(fileName);
        if (Files.deleteIfExists(path)) {
            logger.warning("Deleted existing file \"" + path + "\".");
        }
        logger.info("Writing into " + path);
        Files.createDirectories(path.toAbsolutePath().getParent());
        return Files.createFile(path).toFile();
    }

    private static String getFileNameForClass(SootClass sootClass) {
        return sootClass.getName().replace('.', '/') + ".class";
    }

    public JarWriter(String outFileName) {
        this(outFileName, 0, null);
    }

    public JarWriter(String outFileName, int javaVersion, Manifest manifest) {
        this.outFileName = outFileName;
        this.sootJavaVersion = getSootJavaVersion(javaVersion);
        if (manifest == null) {
            Manifest newManifest = new Manifest();
            newManifest.getMainAttributes().put(Attributes.Name
                    .MANIFEST_VERSION, "1.0");
        } else {
            this.manifest = manifest;
        }
    }

    public void writeJars(List<String> inputJars) {
        File outFile;
        try {
            outFile = getOutputFile(outFileName);
        } catch (IOException e) {
            logger.severe("I/O exception occurred while opening output jar "
                    + "file: " + e.getMessage());
            return;
        }

        try (JarOutputStream jarStream = new JarOutputStream(new
                FileOutputStream(outFile), manifest)) {
            for (String inputJar : inputJars) {
                logger.info("Writing classes from input: " + inputJar);
                writeClassesFromInput(jarStream, inputJar);
            }
            logger.info("Successfully dumped all instrumented classes into " +
                    "the output jar");
        } catch (IOException e) {
            logger.severe("I/O exception occurred while writing jar file: " +
                    e.getMessage());
        }
    }

    private void writeClassesFromInput(JarOutputStream jarStream, String
            inputFile) throws IOException {
        for (String cl : SourceLocator.v().getClassesUnder(inputFile)) {
            SootClass clazz = Scene.v().getSootClass(cl);
            String clazzFileName = getFileNameForClass(clazz);
            JarEntry entry = new JarEntry(clazzFileName);
            entry.setMethod(ZipEntry.DEFLATED);
            jarStream.putNextEntry(entry);

            new BafASMBackend(clazz, sootJavaVersion).generateClassFile
                    (jarStream);
            jarStream.closeEntry();
        }
    }
}
