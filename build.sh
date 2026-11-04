#!/bin/sh
# Builds Reel Block without Gradle and installs it over adb. Usage: ./build.sh [serial]
# Set NO_INSTALL=1 to only build ReelBlock.apk.
# Needs a JDK and the Android SDK (build-tools + one platform). Override the picks with
# BUILD_TOOLS=/path/to/build-tools/<version> and ANDROID_JAR=/path/to/android.jar
set -e
cd "$(dirname "$0")"
SDK=${ANDROID_HOME:-$HOME/Library/Android/sdk}
BT=${BUILD_TOOLS:-$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)}
JAR=${ANDROID_JAR:-$(ls "$SDK"/platforms/android-*/android.jar | sort -V | tail -1)}
OUT=out
rm -rf $OUT && mkdir -p $OUT/classes $OUT/gen
"$BT/aapt2" compile --dir res -o $OUT/res.zip
"$BT/aapt2" link -I "$JAR" --manifest AndroidManifest.xml --java $OUT/gen -o $OUT/unsigned.apk $OUT/res.zip
javac -source 8 -target 8 -bootclasspath "$JAR" -classpath "$BT/core-lambda-stubs.jar" -d $OUT/classes $(find src $OUT/gen -name '*.java') 2>&1 | grep -v 'bootstrap\|^1 warning' || true
"$BT/d8" --lib "$JAR" --min-api 26 --output $OUT $(find $OUT/classes -name '*.class')
(cd $OUT && zip -q unsigned.apk classes.dex)
"$BT/zipalign" -f 4 $OUT/unsigned.apk $OUT/aligned.apk
[ -f debug.keystore ] || keytool -genkeypair -keystore debug.keystore -storepass android -keypass android \
  -alias reelblock -keyalg RSA -keysize 2048 -validity 10000 -dname CN=reelblock >/dev/null 2>&1
"$BT/apksigner" sign --v1-signing-enabled false --ks debug.keystore --ks-pass pass:android --out ReelBlock.apk $OUT/aligned.apk
[ -n "$NO_INSTALL" ] || adb ${1:+-s $1} install -r ReelBlock.apk
