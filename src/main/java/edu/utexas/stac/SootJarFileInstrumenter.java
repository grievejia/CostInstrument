package edu.utexas.stac;

import soot.Scene;
import soot.SootClass;
import soot.SourceLocator;

import java.util.logging.Logger;

public class SootJarFileInstrumenter {

    private static Logger logger = Logger.getLogger(SootJarFileInstrumenter.class.getName());

    private SootClassInstrumenter classInstrumenter;

    public SootJarFileInstrumenter(SootClassInstrumenter classInstrumenter) {
        this.classInstrumenter = classInstrumenter;
    }

    public void instrumentJarFile(String jarFile) {
        logger.info("Processing jar file: " + jarFile);
        for (String cl : SourceLocator.v().getClassesUnder(jarFile)) {
            SootClass sootClass = Scene.v().getSootClass(cl);
            classInstrumenter.instrumentClass(sootClass);
        }
        logger.info("Finished instrumenting jar file " + jarFile);
    }
}
