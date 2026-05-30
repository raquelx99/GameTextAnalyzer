#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
#  GameText Analyzer — Build & Run Script
# ─────────────────────────────────────────────────────────────────────────────
set -e

SRC_DIR="src/main/java"
OUT_DIR="out"
LIB_DIR="lib"
MAIN_CLASS="gamelog.Main"

# ── Classpath ─────────────────────────────────────────────────────────────────
CP="$OUT_DIR"
if ls "$LIB_DIR"/*.jar 1>/dev/null 2>&1; then
    for jar in "$LIB_DIR"/*.jar; do
        CP="$CP:$jar"
    done
    echo "  Jars found in lib/: $(ls $LIB_DIR/*.jar)"
fi

# ── Compile ───────────────────────────────────────────────────────────────────
echo ""
echo "  Compiling sources..."
mkdir -p "$OUT_DIR"
find "$SRC_DIR" -name "*.java" | xargs javac --release 21 -d "$OUT_DIR" -cp "$CP"
echo "  ✓ Compilation successful"

# ── Run ───────────────────────────────────────────────────────────────────────
echo ""
echo "  Starting GameText Analyzer GUI..."
echo "  Use ./build.sh --console to run the terminal version."
echo ""
if [ "$1" = "--console" ]; then
    java -Djava.awt.headless=true -cp "$CP" "$MAIN_CLASS" --console
else
    java -cp "$CP" "$MAIN_CLASS"
fi
