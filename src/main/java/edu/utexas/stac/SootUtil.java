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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    private static void setOptions(CliOption cliOption, String costJarPath) {
        Options sootOptions = Options.v();
        sootOptions.set_prepend_classpath(true);
        sootOptions.set_allow_phantom_refs(true);
        sootOptions.set_full_resolver(true);
        sootOptions.set_no_output_source_file_attribute(false);
        sootOptions.set_write_local_annotations(true);

        List<String> inputPaths = getInputPaths(cliOption, costJarPath);
        String sootClassPath = String.join(":", inputPaths);
        logger.fine("Soot classpath: " + sootClassPath);
        sootOptions.set_soot_classpath(sootClassPath);

        sootOptions.set_process_dir(inputPaths);
    }

    public static List<String> getInputPaths(CliOption cliOption, String costJarPath) {
        List<String> inputPaths = new ArrayList<>(cliOption.getInputFiles());
        inputPaths.add(costJarPath);
        return inputPaths;
    }

    public static void initialize(CliOption cliOption, String costJarPath) {
        setOptions(cliOption, costJarPath);
        loadClasses();
    }
}
