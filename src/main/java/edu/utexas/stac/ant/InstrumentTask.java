package edu.utexas.stac.ant;

import edu.utexas.stac.CliOption;
import edu.utexas.stac.CostInstrument;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.util.Collections;

public class InstrumentTask extends Task {

    private String input = null;
    private String output = null;
    private String costJar = null;

    public void setInput(String input) {
        this.input = input;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public void setCostJar(String costJar) {
        this.costJar = costJar;
    }

    public void execute() {
        if (input == null || output == null) {
            throw new BuildException("Must specify input and output jar file " +
                    "for instrumentation task");
        }
        String logMsg = String.format("CostInstrument input = %s, output = " +
                "%s, costjar = %b", input, output, costJar);
        log(logMsg);

//        try {
            CliOption option = CliOption.create(Collections.singletonList
                            (input), output, costJar);
            CostInstrument.runInstrument(option);
//        } catch (Exception e) {
//            throw new BuildException("Instrumentation task stopped with an " +
//                    "error: " + e.getMessage());
//        }
    }
}
