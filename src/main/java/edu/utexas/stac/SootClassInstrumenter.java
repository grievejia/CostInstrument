package edu.utexas.stac;

import soot.SootClass;
import soot.SootMethod;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SootClassInstrumenter {

    private static final String INSTRUMENT_CLASS_NAME = "edu.utexas.stac.Cost";
    private static Logger logger = Logger.getLogger(SootClassInstrumenter.class.getName());

    private SootMethodInstrumenter methodInstrumenter;
    private List<Pattern> blacklistedClassPatterns;

    public SootClassInstrumenter(SootMethodInstrumenter methodInstrumenter) {
        this(methodInstrumenter, Collections.emptyList());
    }

    public SootClassInstrumenter(SootMethodInstrumenter methodInstrumenter, List<String> blacklistedClassPatterns) {
        this.methodInstrumenter = methodInstrumenter;
        this.blacklistedClassPatterns = blacklistedClassPatterns.stream().map(s -> Pattern.compile(s)).collect
                (Collectors.toList());
    }

    public void instrumentClass(SootClass sootClass) {
        String className = sootClass.getName();
        logger.fine("Instrumenting class " + className);

        boolean isInstrumentClass = className.startsWith(INSTRUMENT_CLASS_NAME);
        boolean isBlacklisted = blacklistedClassPatterns.stream().anyMatch(pattern -> pattern.matcher(className)
                .matches());
        if (isBlacklisted)
            logger.finer("Skipped instrumentation due to blacklist: " + className);

        for (SootMethod method : sootClass.getMethods()) {
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
