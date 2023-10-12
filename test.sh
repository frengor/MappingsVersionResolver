#!/bin/bash

mvn clean install

echo ""

mvn com.frengor:mappings-version-resolver-maven-plugin:resolve-mappings-version -DmappingsVersionResolver.server=org.spigotmc:spigot:1.20.2-R0.1-SNAPSHOT:jar \
        help:evaluate -Dexpression=resolvedMappingsVersion -q -DforceStdout

echo ""
