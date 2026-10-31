#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes
find src/main/java -name '*.java' > build/sources.txt
javac -encoding UTF-8 --release 17 -d build/classes @build/sources.txt
java -Dfile.encoding=UTF-8 -cp build/classes br.com.assistencia.Main "${1:-data}"
