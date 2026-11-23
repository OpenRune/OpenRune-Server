#!/usr/bin/env bash
set -euo pipefail

mkdir -p release-bundle/.data
cp server/app/build/libs/server.jar game.yml release-bundle/
rsync -a --exclude 'raw-cache' .data/ release-bundle/.data/
cd release-bundle
zip -r ../openrune-server-release.zip .
