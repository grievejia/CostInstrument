package edu.utexas.stac;

import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.toolkits.graph.LoopNestTree;

import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

public class LightWeightMethodInstrumenter implements SootMethodInstrumenter {

    private static final String INC_METHOD_SIGNATURE = "<edu.utexas.stac.Cost: void inc()>";
    private static final String GET_METHOD_SIGNATURE = "<edu.utexas.stac.Cost: long read()>";

    private static Logger logger = Logger.getLogger(LightWeightMethodInstrumenter.class.getName());

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
            throw new RuntimeException("Cannot find method Cost.read()");
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
        logger.finer("Found " + loopNestTree.size() + " loops");
        logger.finer("Has nested loops = " + loopNestTree.hasNestedLoops());
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

    @Override
    public void instrumentMethod(SootMethod method) {
        logger.finer("Processing method " + method.getSignature());
        Body body = method.retrieveActiveBody();
        logger.finer("Active body for " + method.getSignature() + " retrieved");

        // Don't bother instrumenting empty method
        if (body.getUnits().isEmpty())
            return;

        instrumentMethodEntry(body);
        logger.finer("Method body instrumented");
        instrumentLoops(body);
        logger.finer("Loop bodies instrumented");
    }
}
