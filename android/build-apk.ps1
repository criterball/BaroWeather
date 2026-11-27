# Build a sideloaded BaroWeather APK with local SDK tools only (no Gradle/network).
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$project = $PSScriptRoot
$tools = if ($env:ANDROID_TOOLS_DIR) { $env:ANDROID_TOOLS_DIR } else { 'C:\Users\default.DP3650\AppData\Local\Temp\opencode\android-tools' }

$jdk = Get-ChildItem (Join-Path $tools 'jdk17') -Directory | Where-Object Name -like 'jdk-*' | Select-Object -First 1 -ExpandProperty FullName
$buildTools = if ($env:ANDROID_BUILD_TOOLS) { $env:ANDROID_BUILD_TOOLS } else { 'D:\opencode_home_win\tools_android\sdk\build-tools\35.0.0' }
$platform = if ($env:ANDROID_PLATFORM) { $env:ANDROID_PLATFORM } else { 'D:\opencode_home_win\tools_android\sdk\platforms\android-35' }
$androidJar = Join-Path $platform 'android.jar'
$adb = 'C:\Users\default.DP3650\AppData\Local\Temp\opencode\platform-tools\platform-tools\adb.exe'

foreach ($required in @($jdk, $buildTools, $androidJar)) {
    if (-not (Test-Path $required)) { throw "Missing build input: $required" }
}

$build = Join-Path $project 'build'
if (Test-Path $build) { Remove-Item $build -Recurse -Force }
$gen = Join-Path $build 'gen'
$classes = Join-Path $build 'classes'
$nativeStage = Join-Path $build 'native'
foreach ($dir in @($build, $gen, $classes, $nativeStage)) {
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
}

$env:JAVA_HOME = $jdk
$env:PATH = "$jdk\bin;$buildTools;$env:PATH"
$aapt2 = Join-Path $buildTools 'aapt2.exe'
$flat = Join-Path $build 'compiled.flata'
$baseApk = Join-Path $build 'base.apk'
& $aapt2 compile --dir (Join-Path $project 'res') -o $flat
if ($LASTEXITCODE -ne 0) { throw 'aapt2 compile failed' }
& $aapt2 link -o $baseApk -I $androidJar --manifest (Join-Path $project 'AndroidManifest.xml') --java $gen --min-sdk-version 21 --target-sdk-version 35 --version-code 6 --version-name 0.4.3 $flat
if ($LASTEXITCODE -ne 0) { throw 'aapt2 link failed' }

$sources = (Get-ChildItem (Join-Path $project 'src') -Recurse -Filter *.java).FullName
$sources += (Get-ChildItem $gen -Recurse -Filter *.java).FullName
& (Join-Path $jdk 'bin\javac.exe') -encoding UTF-8 -source 8 -target 8 -bootclasspath $androidJar -classpath $androidJar -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw 'javac failed' }

$classesJar = Join-Path $build 'classes.jar'
& (Join-Path $jdk 'bin\jar.exe') cf $classesJar -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'jar classes failed' }

$d8 = Join-Path $buildTools 'd8.bat'
& $d8 --lib $androidJar --output $build --min-api 21 $classesJar
if ($LASTEXITCODE -ne 0) { throw 'd8 failed' }

$dexFiles = Get-ChildItem $build -Filter 'classes*.dex' | Sort-Object Name
if (-not $dexFiles) { throw 'd8 produced no dex files' }
foreach ($dex in $dexFiles) {
    & (Join-Path $jdk 'bin\jar.exe') uf $baseApk -C $build $dex.Name
    if ($LASTEXITCODE -ne 0) { throw "jar $($dex.Name) failed" }
}

# Package the Rust core as Android native libraries.
foreach ($abi in @('arm64-v8a', 'armeabi-v7a', 'x86_64')) {
    $so = Join-Path $project "jniLibs\$abi\libbaroweather_core.so"
    if (-not (Test-Path $so)) { throw "Missing native library: $so (run build-native.ps1 first)" }
    $targetDir = Join-Path $nativeStage "lib\$abi"
    New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
    Copy-Item $so (Join-Path $targetDir 'libbaroweather_core.so') -Force
    & (Join-Path $jdk 'bin\jar.exe') uf $baseApk -C $nativeStage "lib\$abi\libbaroweather_core.so"
    if ($LASTEXITCODE -ne 0) { throw "jar native lib failed for $abi" }
}

$aligned = Join-Path $build 'aligned.apk'
& (Join-Path $buildTools 'zipalign.exe') -p -f 4 $baseApk $aligned
if ($LASTEXITCODE -ne 0) { throw 'zipalign failed' }

$keystore = Join-Path $project 'debug.jks'
if (-not (Test-Path $keystore)) {
    & (Join-Path $jdk 'bin\keytool.exe') -genkeypair -v -keystore $keystore -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname 'CN=BaroWeather local debug, OU=Local, O=BaroWeather, L=Local, ST=Local, C=US'
    if ($LASTEXITCODE -ne 0) { throw 'keytool failed' }
}

$outDir = Join-Path $root 'dist\android'
New-Item -ItemType Directory -Path $outDir -Force | Out-Null
$apk = Join-Path $outDir 'baroweather-0.4.3.apk'
$apksigner = Join-Path $buildTools 'apksigner.bat'
& $apksigner sign --ks $keystore --ks-pass pass:android --key-pass pass:android --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true --out $apk $aligned
if ($LASTEXITCODE -ne 0) { throw 'apksigner failed' }
& $apksigner verify --verbose --print-certs $apk
if ($LASTEXITCODE -ne 0) { throw 'apksigner verify failed' }

Write-Output "APK: $apk"
Write-Output "Size: $((Get-Item $apk).Length) bytes"
if (Test-Path $adb) {
    & $adb devices
}
