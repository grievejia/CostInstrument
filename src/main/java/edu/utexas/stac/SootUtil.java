package edu.utexas.stac;

import soot.*;
import soot.baf.BafASMBackend;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.options.Options;
import soot.toolkits.graph.LoopNestTree;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

public class SootUtil {

    private static Logger logger = Logger.getLogger(SootUtil.class.getName());

    private static void loadClasses() {
        Scene scene = Scene.v();
        scene.loadBasicClasses();
        scene.loadNecessaryClasses();
        logger.fine("Soot class loaded");
    }

    private static void setOptions(CliOption cliOption) {
        Options sootOptions = Options.v();
        sootOptions.set_prepend_classpath(true);
        sootOptions.set_allow_phantom_refs(true);
        sootOptions.set_full_resolver(true);

        String sootClassPath = String.join(":", cliOption.getInputFiles());
        logger.fine("Soot classpath: " + sootClassPath);
        sootOptions.set_soot_classpath(sootClassPath);

        sootOptions.set_process_dir(cliOption.getInputFiles());
    }

    public static void initialize(CliOption cliOption) {
        setOptions(cliOption);
        loadClasses();
    }

    private static File getOutputFile(CliOption cliOption) throws IOException {
        Path path = Paths.get(cliOption.getOutputFile());
        if(Files.deleteIfExists(path)){
            logger.warning("Deleted existing file \"" + path + "\".");
        }
        logger.info("Writing into " + path);
        Files.createDirectories(path.toAbsolutePath().getParent());
        return Files.createFile(path).toFile();
    }

    private static String getFileNameForClass(SootClass sootClass) {
        return sootClass.getName().replace('.', '/') + ".class";
    }

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

    private static void writeClassesFromInput(JarOutputStream jarStream, String inputFile, CliOption cliOption) throws IOException {
        for (String cl : SourceLocator.v().getClassesUnder(inputFile)) {
            SootClass clazz = Scene.v().getSootClass(cl);
            String clazzFileName = getFileNameForClass(clazz);
            JarEntry entry = new JarEntry(clazzFileName);
            entry.setMethod(ZipEntry.DEFLATED);
            jarStream.putNextEntry(entry);

            new BafASMBackend(clazz, getSootJavaVersion(cliOption.getOutputJavaVersion())).generateClassFile(jarStream);
            jarStream.closeEntry();
        }
    }

    public static void writeJar(CliOption cliOption) {
        try {
            File outFile = getOutputFile(cliOption);
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            JarOutputStream jarStream = new JarOutputStream(new FileOutputStream(outFile), manifest);
            for (String inputFile: cliOption.getInputFiles())
                writeClassesFromInput(jarStream, inputFile, cliOption);
            jarStream.close();
            logger.info("Successfully created " + cliOption.getOutputFile());
        } catch (IOException e) {
            logger.severe("I/O exception occurred while writing jar file: " + e.getMessage());
        }
    }
}
