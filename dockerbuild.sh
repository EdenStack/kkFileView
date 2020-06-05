#!/usr/bin/env bash
#mvn clean package -Dmaven.test.skip=true\
#    -Pdocker docker:build\
#    -Ddocker.registry=harbor.djuat.ict.cmcc:5001\
#    -Ddocker.image.prefix=djuat\
#    -DpushImageTag

mvn clean package -DskipTests\
    -Prelease
    
docker build -t ccr.ccs.tencentyun.com/io.eden/filepreview:v2.1.0 .
