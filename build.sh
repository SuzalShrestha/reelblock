#!/bin/sh
# Builds Reel Block without Gradle: ReelBlock.apk (GitHub, adb) and ReelBlock.aab (Google Play), then installs
# the APK over adb. Usage: ./build.sh [serial]. Set NO_INSTALL=1 to only build.
# Needs JDK 17+, curl and the Android SDK (build-tools + one platform). Override the picks with
# BUILD_TOOLS=/path/to/build-tools/<version> and ANDROID_JAR=/path/to/android.jar
set -e
cd "$(dirname "$0")"
SDK=${ANDROID_HOME:-$HOME/Library/Android/sdk}
BT=${BUILD_TOOLS:-$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)}
JAR=${ANDROID_JAR:-$(ls "$SDK"/platforms/android-*/android.jar | sort -V | tail -1)}
OUT=out

# Three downloads, pinned by SHA-256 and kept in libs/ (not in git). A changed file stops the build.
# Billing 8.x is supported on Play until 31 Aug 2027. Its POM also lists Google's telemetry (datatransport),
# play-services and AndroidX Activity; Reel Block leaves them out. In place of the telemetry, stubs/ has
# stand-in classes whose TransportRuntime.initialize throws; the billing code catches it ("Skipping logging
# since initialization failed") and sends nothing. (Leaving the classes out works on some runtimes only: the
# JVM fails to load the billing class before its catch runs.) Only ProxyBillingActivityV2, which is not
# declared, needs AndroidX Activity. androidx.core is needed (BundleCompat, ContextCompat); R8 keeps only
# the parts the billing code calls.
BILLING_VERSION=8.3.0
LIBS=libs
fetch() { # url sha256 file
  [ -f "$LIBS/$3" ] || { mkdir -p $LIBS && curl -fsSL -o "$LIBS/$3.part" "$1" && mv "$LIBS/$3.part" "$LIBS/$3"; }
  echo "$2  $LIBS/$3" | shasum -a 256 -c -s || { echo "$LIBS/$3: SHA-256 is not $2"; rm -f "$LIBS/$3"; exit 1; }
}
M=https://dl.google.com/android/maven2
fetch $M/com/android/billingclient/billing/$BILLING_VERSION/billing-$BILLING_VERSION.aar \
  9810b5cd47e20bc82b354f3583c9d3bf7c42b33e64e192dce0495d036a85f505 billing.aar
fetch $M/androidx/core/core/1.13.1/core-1.13.1.aar 2c27de199535675005553066597a4b20fa1eea7c228ab4ef6b32b5fe39ca1f59 core.aar
fetch https://github.com/google/bundletool/releases/download/1.18.3/bundletool-all-1.18.3.jar \
  a099cfa1543f55593bc2ed16a70a7c67fe54b1747bb7301f37fdfd6d91028e29 bundletool.jar
grep -q "billingclient.version\" android:value=\"$BILLING_VERSION\"" AndroidManifest.xml \
  || { echo "AndroidManifest.xml: set com.google.android.play.billingclient.version to $BILLING_VERSION"; exit 1; }

rm -rf $OUT && mkdir -p $OUT/classes $OUT/gen $OUT/lib $OUT/base/manifest $OUT/base/dex
unzip -qo -d $OUT/lib/billing $LIBS/billing.aar classes.jar proguard.txt
unzip -qo -d $OUT/lib/core $LIBS/core.aar classes.jar proguard.txt
CP=$OUT/lib/billing/classes.jar:$OUT/lib/core/classes.jar

"$BT/aapt2" compile --dir res -o $OUT/res.zip
"$BT/aapt2" link -I "$JAR" --manifest AndroidManifest.xml --java $OUT/gen -o $OUT/linked.apk $OUT/res.zip
# Smaller APK: short file paths inside it, and one shared name for all resources (the app never looks a
# resource up by name). About 11 KB less. The R ids in the code are unchanged.
"$BT/aapt2" optimize --shorten-resource-paths --collapse-resource-names -o $OUT/unsigned.apk $OUT/linked.apk
javac -source 8 -target 8 -Xlint:-options -bootclasspath "$JAR" -classpath "$BT/core-lambda-stubs.jar:$CP" -d $OUT/classes \
  $(find src stubs $OUT/gen -name '*.java') 2>&1 | grep -v 'bootstrap\|^1 warning' || true
# javac copies every R.* id into the code (they are compile-time constants), so the R classes are not needed at
# run time. R8 (inside build-tools) dexes the app with the two libraries and removes the library code nothing
# calls. No renaming: stack traces stay readable, and the code stays easy to check against the source.
cat > $OUT/rules.pro <<'EOF'
-dontobfuscate
-keep class dev.local.reelblock.** { *; }
-keep class com.android.billingclient.api.ProxyBillingActivity { *; }
# Missing on purpose, checked with R8 without these lines: annotations, which have no run-time effect.
# Any other missing class fails the build.
-dontwarn androidx.annotation.**
-dontwarn kotlin.Metadata
EOF
java -cp "$BT/lib/d8.jar" com.android.tools.r8.R8 --release --min-api 26 --lib "$JAR" --output $OUT --map-diagnostics info none \
  --pg-conf $OUT/rules.pro --pg-conf $OUT/lib/billing/proguard.txt \
  $(find $OUT/classes -name '*.class' ! -name 'R.class' ! -name 'R$*.class') $OUT/lib/billing/classes.jar $OUT/lib/core/classes.jar
(cd $OUT && zip -q unsigned.apk classes.dex)
"$BT/zipalign" -f 4 $OUT/unsigned.apk $OUT/aligned.apk
[ -f debug.keystore ] || keytool -genkeypair -keystore debug.keystore -storepass android -keypass android \
  -alias reelblock -keyalg RSA -keysize 2048 -validity 10000 -dname CN=reelblock >/dev/null 2>&1
"$BT/apksigner" sign --v1-signing-enabled false --ks debug.keystore --ks-pass pass:android --out ReelBlock.apk $OUT/aligned.apk

# Google Play takes an Android App Bundle: the same resources in protobuf form, plus the same classes.dex.
"$BT/aapt2" link --proto-format -I "$JAR" --manifest AndroidManifest.xml -o $OUT/proto.apk $OUT/res.zip
(cd $OUT/base && unzip -q ../proto.apk && mv AndroidManifest.xml manifest/ && cp ../classes.dex dex/ && zip -qr ../base.zip .)
java -jar $LIBS/bundletool.jar build-bundle --modules=$OUT/base.zip --output=ReelBlock.aab --overwrite
# Signed with the same key as the APK. Play uses this signature to check the upload (docs/play-store.md).
jarsigner -sigalg SHA256withRSA -digestalg SHA-256 -keystore debug.keystore -storepass android ReelBlock.aab reelblock >/dev/null
java -jar $LIBS/bundletool.jar validate --bundle=ReelBlock.aab >/dev/null

[ -n "$NO_INSTALL" ] || adb ${1:+-s $1} install -r ReelBlock.apk
