#!/usr/bin/env bash
set -euo pipefail

mvn -Pnative -DskipTests package
