#!/bin/bash
# ─────────────────────────────────────────────────────────────
# HRMS Attrition System — Step 1 Build & Run Script
# Owner: Employee Data Management + Exit Management
# ─────────────────────────────────────────────────────────────
set -e

JAVA_HOME="/c/Program Files/Java/jdk-25"
export PATH="$JAVA_HOME/bin:$PATH"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="$SCRIPT_DIR/src"
OUT_DIR="$SCRIPT_DIR/out"
LIB_DIR="$SCRIPT_DIR/lib"
JAR_NAME="hrms_step1.jar"
MAIN_CLASS="com.hrms.ui.MainDashboard"
SQLITE_JAR="$LIB_DIR/sqlite-jdbc.jar"
HRMS_DB_JAR="$LIB_DIR/hrms-database.jar"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  HRMS Attrition — Employee & Exit Management (Step 1)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Check required JARs
if [ ! -f "$SQLITE_JAR" ]; then
    echo "❌ sqlite-jdbc.jar not found at $SQLITE_JAR"
    echo "   Run: wget https://github.com/xerial/sqlite-jdbc/releases/download/3.45.1.0/sqlite-jdbc-3.45.1.0.jar -O $SQLITE_JAR"
    exit 1
fi
if [ ! -f "$HRMS_DB_JAR" ]; then
    echo "❌ hrms-database.jar not found at $HRMS_DB_JAR"
    echo "   Copy the DB team's JAR to: $HRMS_DB_JAR"
    exit 1
fi

# Create output dir
mkdir -p "$OUT_DIR"

# Find all Java source files
JAVA_FILES=$(find "$SRC_DIR" -name "*.java")
echo "📂 Found $(echo "$JAVA_FILES" | wc -l) Java source files"

# Compile
echo "🔨 Compiling..."
javac -cp "$SQLITE_JAR:$HRMS_DB_JAR" -d "$OUT_DIR" $JAVA_FILES
echo "✅ Compilation successful!"

# Package into JAR
echo "📦 Packaging into $JAR_NAME..."
MANIFEST_FILE="$SCRIPT_DIR/MANIFEST.MF"
echo "Main-Class: $MAIN_CLASS" > "$MANIFEST_FILE"
echo "Class-Path: lib/sqlite-jdbc.jar lib/hrms-database.jar" >> "$MANIFEST_FILE"
echo "" >> "$MANIFEST_FILE"

jar cfm "$SCRIPT_DIR/$JAR_NAME" "$MANIFEST_FILE" -C "$OUT_DIR" .
echo "✅ JAR created: $JAR_NAME"

# Run
echo ""
echo "🚀 Launching HRMS Employee Management UI..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
java -cp "$SCRIPT_DIR/$JAR_NAME:$SQLITE_JAR:$HRMS_DB_JAR:$LIB_DIR/slf4j-api.jar:$LIB_DIR/slf4j-simple.jar" "$MAIN_CLASS"
