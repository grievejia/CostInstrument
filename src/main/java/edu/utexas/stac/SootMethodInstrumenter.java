package edu.utexas.stac;

import soot.SootMethod;

public interface SootMethodInstrumenter {
    void instrumentMethod(SootMethod method);
}
