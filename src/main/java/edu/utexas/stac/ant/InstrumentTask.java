package edu.utexas.stac.ant;

import edu.utexas.stac.CliOption;
import edu.utexas.stac.CostInstrument;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InstrumentTask extends Task {

    private String input = null;
    private String output = null;
    private String costJar = null;
    private boolean fullMode = false;

    public static class Pattern {
        public Pattern() {}

        String pattern;
        public void setPattern(String pattern) { this.pattern = pattern; }
        public String getPattern() { return pattern; }
    }
    private List<Pattern> blacklist = new ArrayList<>();

    public void setInput(String input) {
        this.input = input;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public void setCostJar(String costJar) {
        this.costJar = costJar;
    }

    public void setFullMode(boolean b) { fullMode = b; }

    public Pattern createBlacklist() {
        Pattern pattern = new Pattern();
        blacklist.add(pattern);
        return pattern;
    }

    public void execute() {
        if (input == null || output == null) {
            throw new BuildException("Must specify input and output jar file " +
                    "for instrumentation task");
        }
        String logMsg = String.format("CostInstrument input = %s, output = " +
                "%s, costjar = %s, fullMode = %b", input, output, costJar, fullMode);
        log(logMsg);
        if (!blacklist.isEmpty()) {
            for (Pattern p: blacklist)
                log("Blacklist pattern: " + p.getPattern());
        }

        CliOption option = CliOption.createForAntTask(Collections.singletonList
                (input), output, costJar, blacklist, fullMode);
        CostInstrument.runInstrument(option);
    }
}
