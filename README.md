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

## Tips

- This simple instrumentation tool is based on the [Soot](https://github.com/Sable/soot) library. Unfortunately, Soot is not a super robust piece of software, and in our experience every now and then Soot may crash on some real-world bytecode files we feed to it. In cases like this, our solution is to use the `--exclude-classes` option to instruct the tool to avoid touching the bytecode files of those problematic classes. 
