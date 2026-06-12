#!/usr/bin/env bash
# Start the CJLU Ktor backend on http://0.0.0.0:8080
# Run from Android Studio Terminal or macOS Terminal (project root).

set -euo pipefail
cd "$(dirname "$0")/.."

# Also set in backend-ktor/build.gradle.kts for `./gradlew :backend-ktor:run` (exported here for direct `java` runs).
export CJLU_ALLOW_INSECURE_DEV_DEFAULTS=true

if [[ -z "${JAVA_HOME:-}" ]] && [[ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]]; then
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
fi

echo "Starting backend (Gradle :backend-ktor:run)..."
echo "When you see Netty listening on 8080, test with:"
echo "  curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8080/"
echo ""

./gradlew :backend-ktor:run --console=plain
