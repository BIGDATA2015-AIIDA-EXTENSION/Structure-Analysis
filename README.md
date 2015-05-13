# Structure Parser [![Build Status](https://travis-ci.org/BIGDATA2015-AIIDA-EXTENSION/Structure-Analysis.svg?branch=travis)](https://travis-ci.org/BIGDATA2015-AIIDA-EXTENSION/Structure-Analysis)

A parser for Martin's structures.

## Build and run

```Scala
sbt
> run
```

## Use with spark
Use `sbt assembly` instead of `sbt package` to generate the jar file. `sbt package` only works if the project has no dependency but the ones provided by spark.
