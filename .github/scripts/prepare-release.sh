#!/usr/bin/env bash
set -euo pipefail
: "${GH_TOKEN:?Set DEPS_TOKEN with Contents read access to TF-Minecraft/ServerAssets}"
ref=883970ddc256f0e4c3bcaee87e6cf33171b9d4a4
mkdir -p libs
curl --fail --location --silent --show-error --retry 3 -H "Authorization: Bearer $GH_TOKEN" -H "Accept: application/vnd.github.raw+json" "https://api.github.com/repos/TF-Minecraft/ServerAssets/contents/jars/4241c14a7727/gson-2.10.1.jar?ref=$ref" > "libs/gson-2.10.1.jar"
sha256sum --check .github/dependencies.sha256
