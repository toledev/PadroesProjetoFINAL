#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/test-classes
find src/main/java src/test/java -name '*.java' > build/test-sources.txt
javac -encoding UTF-8 --release 17 -d build/test-classes @build/test-sources.txt
java -Dfile.encoding=UTF-8 -cp build/test-classes br.com.assistencia.TestesNegocio
