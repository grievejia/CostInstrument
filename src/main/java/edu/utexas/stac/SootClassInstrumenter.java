package edu.utexas.stac;

import soot.SootClass;
import soot.SootMethod;

import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

public class SootClassInstrumenter {

    private static final String INSTRUMENT_CLASS_NAME = "edu.utexas.stac.Cost";
    private static Logger logger = Logger.getLogger(SootClassInstrumenter.class.getName());

    private SootMethodInstrumenter methodInstrumenter;
    private Set<String> blacklistedClasses;

    public SootClassInstrumenter(SootMethodInstrumenter methodInstrumenter) {
        this(methodInstrumenter, Collections.emptySet());
    }
    public SootClassInstrumenter(SootMethodInstrumenter methodInstrumenter, Set<String> blacklistedClasses) {
        this.methodInstrumenter = methodInstrumenter;
        this.blacklistedClasses = blacklistedClasses;
    }

    public void instrumentClass(SootClass sootClass) {
        String className = sootClass.getName();
        logger.fine("Instrumenting class " + className);

        boolean isInstrumentClass = className.startsWith(INSTRUMENT_CLASS_NAME);
        boolean isBlacklisted = blacklistedClasses.contains(className);
        if (isBlacklisted)
            throw new RuntimeException("haha");

        for (SootMethod method: sootClass.getMethods()) {
            if (method.isAbstract() || method.isNative())
                continue;
            if (isInstrumentClass || isBlacklisted) {
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
