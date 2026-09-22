#!/usr/bin/env bash
set -euo pipefail
# Run from the repository root after downloading the pinned JARs.
# Hash-qualified versions prevent different private JARs sharing a Maven cache key.
sha256sum --check .github/dependencies.sha256

mvn -B --no-transfer-progress org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file \
    -Dfile="libs/gson-2.10.1.jar" -DgroupId="local" -DartifactId="gson" \
    -Dversion="2.10.1-tfmc-4241c14a7727" -Dpackaging=jar -DgeneratePom=true "$@"
