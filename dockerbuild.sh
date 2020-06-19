#!/usr/bin/env bash
mvn clean package -DskipTests\
    -Prelease

docker build -t ccr.ccs.tencentyun.com/io.eden/filepreview:v2.1.11 .