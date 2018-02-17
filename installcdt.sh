#!/bin/bash
mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=lib/org.eclipse.cdt.core_6.0.0.201607151550.jar -DgroupId=org.eclipse.cdt -DartifactId=cdt-core -Dversion=6.0.0 -Dpackaging=jar
