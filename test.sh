#!/bin/sh
set -eu
if [ -z "$PLUGIN_API_CLASSPATH" ]; then
  echo "Set PLUGIN_API_CLASSPATH to Paper, EssentialsX, Gson, and required API libraries." >&2
  exit 2
fi
./build.sh
mkdir -p build/test-classes
find src/test/java -name '*.java' -print0 | xargs -0 javac --release 21 -encoding UTF-8 -cp "$PLUGIN_API_CLASSPATH:build/classes" -d build/test-classes
java -cp "$PLUGIN_API_CLASSPATH:build/classes:build/test-classes" CommandTest
test_file=$(mktemp)
trap 'rm -f "$test_file"' EXIT
java -cp "$PLUGIN_API_CLASSPATH:build/classes:build/test-classes" DurabilityTest "$test_file"
test "$(cat "$test_file")" = "durable test"
