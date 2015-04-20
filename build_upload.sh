#!/bin/sh

rm target/scala-2.10/aiida-computations.jar
# Works provided that you have cached your ssh socket and that $BDUSERNAME is set as 
# your username for the cluster and that $CLUSTER is the hostname of the cluser
sbt assembly && scp target/scala-2.10/structure-parser-assembly-1.0.jar "$BDUSERNAME"@"$CLUSTER":aiida.jar
