#!/bin/sh

mainJar=`ls -rt  *jar-with-dependencies*jar`

java -Djava.util.logging.config.file=config/logging.properties -jar $mainJar . ./html
