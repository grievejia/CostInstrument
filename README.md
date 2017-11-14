# CostInstrument
Cost instrumenter for jar files

## Build
```
> ant jar
```

## Usage
Show full help message:
```
java -jar CostInstrument.jar
```
Instrument with default strategy (cost+1 at method entry and loop header):
```
java -jar CostInstrument.jar -o <output jar> <jars to process>
```
Instrument with alternative strategy (cost+1 for each Soot instruction. Note that instrumentation happens only at the end of each basic block):
```
java -jar CostInstrument.jar -s HEAVY -o <output jar> <jars to process>
```