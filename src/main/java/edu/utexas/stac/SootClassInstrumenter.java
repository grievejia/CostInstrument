package edu.utexas.stac;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

import java.util.logging.Logger;

public class SootClassInstrumenter {

    private static final String INSTRUMENT_CLASS_NAME = "edu.utexas.stac.Cost";
    private static Logger logger = Logger.getLogger(SootClassInstrumenter.class.getName());

    private SootMethodInstrumenter methodInstrumenter;

    public SootClassInstrumenter(SootMethodInstrumenter methodInstrumenter) {
        this.methodInstrumenter = methodInstrumenter;
    }

    public void instrumentClass(String className) {
        logger.fine("Instrumenting class " + className);
        SootClass clazz = Scene.v().getSootClass(className);
        boolean isInstrumentClass = clazz.getName().startsWith(INSTRUMENT_CLASS_NAME);

        for (SootMethod method: clazz.getMethods()) {
            if (method.isAbstract() || method.isNative())
                continue;
            if (isInstrumentClass) {
                // We still need to retrieve the active body since Soot won't serialize the body otherwise
                method.retrieveActiveBody();
            } else {
                methodInstrumenter.instrumentMethod(method);
                // Hope that we did not mess up the Jimple
                method.getActiveBody().validate();
            }
        }
    }
}
