package edu.utexas.stac;

import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.BriefBlockGraph;

import java.util.Arrays;
import java.util.logging.Logger;

public class HeavyWeightMethodInstrumenter implements SootMethodInstrumenter {

    private static final String INC_METHOD_SIGNATURE = "<edu.utexas.stac" +
            ".Cost: void inc(int)>";

    private static Logger logger = Logger.getLogger(HeavyWeightMethodInstrumenter.class.getName());

    private static SootMethodRef getInstrumentMethodRef() {
        SootMethod method = Scene.v().getMethod(INC_METHOD_SIGNATURE);
        if (method == null)
            throw new RuntimeException("Cannot find method Cost.inc()");
        return method.makeRef();
    }

    private static InvokeStmt getInstrumentMethodInvokeStmt(int cost) {
        Constant costVal = IntConstant.v(cost);
        InvokeExpr invokeExpr = Jimple.v().newStaticInvokeExpr
                (getInstrumentMethodRef(), Arrays.asList(costVal));
        return Jimple.v().newInvokeStmt(invokeExpr);
    }

    private void instrumentBasicBlock(Block block) {
        // Count the number of stmts
        int numUnits = 0;
        for (Unit unit: block) {
            ++numUnits;
        }

        InvokeStmt invokeStmt = getInstrumentMethodInvokeStmt(numUnits);
        Unit lastUnit = block.getTail();
        if (lastUnit instanceof GotoStmt ||
                lastUnit instanceof IfStmt ||
                lastUnit instanceof SwitchStmt ||
                lastUnit instanceof ReturnStmt ||
                lastUnit instanceof ReturnVoidStmt) {
            // We must increase the cost before control flow changes
            block.insertBefore(invokeStmt, lastUnit);
        } else {
            block.insertAfter(invokeStmt, lastUnit);
        }
    }

    @Override
    public void instrumentMethod(SootMethod method) {
        logger.finer("Processing method " + method.getSignature());
        Body body = method.retrieveActiveBody();
        logger.finer("Active body for " + method.getSignature() + " retrieved");

        // Don't bother instrumenting empty method
        if (body.getUnits().isEmpty())
            return;

        BlockGraph blockGraph = new BriefBlockGraph(body);
        for (Block block: blockGraph) {
            instrumentBasicBlock(block);
        }
    }
}
