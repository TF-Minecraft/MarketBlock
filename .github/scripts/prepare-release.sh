#!/usr/bin/env bash
set -euo pipefail
: "${GH_TOKEN:?Set DEPS_TOKEN with Contents read access to TF-Minecraft/ServerAssets}"
ref=883970ddc256f0e4c3bcaee87e6cf33171b9d4a4
mkdir -p libs
curl --fail --location --silent --show-error --retry 3 -H "Authorization: Bearer $GH_TOKEN" -H "Accept: application/vnd.github.raw+json" "https://api.github.com/repos/TF-Minecraft/ServerAssets/contents/jars/4241c14a7727/gson-2.10.1.jar?ref=$ref" > "libs/gson-2.10.1.jar"
curl --fail --location --silent --show-error --retry 3 -H "Authorization: Bearer $GH_TOKEN" -H "Accept: application/vnd.github.raw+json" "https://api.github.com/repos/TF-Minecraft/ServerAssets/contents/jars/b7156eab5677/spigot-api.jar?ref=$ref" > "libs/spigot-api.jar"
curl --fail --location --silent --show-error --retry 3 -H "Authorization: Bearer $GH_TOKEN" -H "Accept: application/vnd.github.raw+json" "https://api.github.com/repos/TF-Minecraft/ServerAssets/contents/jars/22dd8755e769/denareconomy-0.1.8.jar?ref=$ref" > "libs/denareconomy-0.1.8.jar"
curl --fail --location --silent --show-error --retry 3 -H "Authorization: Bearer $GH_TOKEN" -H "Accept: application/vnd.github.raw+json" "https://api.github.com/repos/TF-Minecraft/ServerAssets/contents/jars/c2b208566fb6/cooking-0.1.5-ALPHA.jar?ref=$ref" > "libs/cooking-0.1.5-ALPHA.jar"
sha256sum --check .github/dependencies.sha256
