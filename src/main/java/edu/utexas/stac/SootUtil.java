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

    private static final String INSTRUMENT_CLASS_NAME = "edu.utexas.stac.Cost";
    private static final String INC_METHOD_SIGNATURE = "<edu.utexas.stac.Cost: void inc()>";
    private static final String GET_METHOD_SIGNATURE = "<edu.utexas.stac.Cost: long get()>";

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

    private static void setLogLevel(CliOption.LogLevel logLevel) {
        switch (logLevel) {
            case SILENT:
                logger.setLevel(Level.OFF);
                break;
            case DEFAULT:
                logger.setLevel(Level.INFO);
                break;
            case VERBOSE:
                logger.setLevel(Level.ALL);
                break;
        }
    }

    public static void initialize(CliOption cliOption) {
        setLogLevel(cliOption.getLogLevel());
        setOptions(cliOption);
        loadClasses();
    }

    public static void instrumentJar(String jarFile) {
        logger.info("Processing " + jarFile);
        for (String cl : SourceLocator.v().getClassesUnder(jarFile)) {
            SootClass clazz = Scene.v().getSootClass(cl);
            boolean isInstrumentClass = clazz.getName().startsWith(INSTRUMENT_CLASS_NAME);

            for (SootMethod method: clazz.getMethods()) {
                if (method.isAbstract() || method.isNative())
                    continue;
                if (isInstrumentClass) {
                    // We still need to retrieve the active body since Soot won't serialize the body otherwise
                    method.retrieveActiveBody();
                } else {
                    instrumentMethod(method);
                }
            }
        }
        logger.info("Finished instrumenting " + jarFile);
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

    private static SootMethodRef getInstrumentMethodRef() {
        SootMethod method = Scene.v().getMethod(INC_METHOD_SIGNATURE);
        if (method == null)
            throw new RuntimeException("Cannot find method Cost.inc()");
        return method.makeRef();
    }

    private static InvokeStmt getInstrumentMethodInvokeStmt() {
        InvokeExpr invokeExpr = Jimple.v().newStaticInvokeExpr(getInstrumentMethodRef(), Collections.emptyList());
        return Jimple.v().newInvokeStmt(invokeExpr);
    }

    private static SootMethodRef getCostMethodRef() {
        SootMethod method = Scene.v().getMethod(GET_METHOD_SIGNATURE);
        if (method == null)
            throw new RuntimeException("Cannot find method Cost.get()");
        return method.makeRef();
    }

    private static InvokeStmt getCostMethodInvokeStmt() {
        InvokeExpr invokeExpr = Jimple.v().newStaticInvokeExpr(getCostMethodRef(), Collections.emptyList());
        return Jimple.v().newInvokeStmt(invokeExpr);
    }

    private static void instrumentMethodEntry(Body body) {
        PatchingChain<Unit> units = body.getUnits();
        Unit firstInstrumentPoint = units.getFirst();
        while (firstInstrumentPoint != null && firstInstrumentPoint instanceof IdentityUnit) {
            firstInstrumentPoint = units.getSuccOf(firstInstrumentPoint);
        }
        if (firstInstrumentPoint != null)
            units.insertBefore(getInstrumentMethodInvokeStmt(), firstInstrumentPoint);
    }

    private static Set<Loop> findAllLoops(Body body) {
        LoopNestTree loopNestTree = new LoopNestTree(body);
        logger.fine("Found " + loopNestTree.size() + "loops");
        logger.fine("Has nested loops = " + loopNestTree.hasNestedLoops());
        return loopNestTree;
    }

    private static void instrumentLoop(Body body, Loop loop) {
        PatchingChain<Unit> units = body.getUnits();
        units.insertAfter(getInstrumentMethodInvokeStmt(), loop.getHead());
    }

    private static void instrumentLoops(Body body) {
        Set<Loop> loops = findAllLoops(body);
        for (Loop loop: loops)
            instrumentLoop(body, loop);
    }

    private static void instrumentMethod(SootMethod method) {
        logger.fine("Processing method " + method.getSignature());
        Body body = method.retrieveActiveBody();
        logger.fine("Active body for " + method.getSignature() + " retrieved");

        // Don't bother instrumenting empty method
        if (body.getUnits().isEmpty())
            return;

        instrumentMethodEntry(body);
        logger.fine("Method body instrumented");
        instrumentLoops(body);
        logger.fine("Loop bodies instrumented");

        // Hope that we did not mess up the Jimple
        body.validate();
    }
}
