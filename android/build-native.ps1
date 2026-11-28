# Build the Rust core static libraries and link them into Android JNI .so files.
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$core = Join-Path $root 'core'
$jni = Join-Path $PSScriptRoot 'jni\bw_jni.c'
$jniOut = Join-Path $PSScriptRoot 'jniLibs'

$ndk = if ($env:ANDROID_NDK_HOME) { $env:ANDROID_NDK_HOME } else { 'D:\opencode_home_win\tools_android\sdk\ndk\27.2.12479018' }
$prebuilt = Join-Path $ndk 'toolchains\llvm\prebuilt\windows-x86_64'
$bin = Join-Path $prebuilt 'bin'
$jniInclude = Join-Path $prebuilt 'sysroot\usr\include'
if (-not (Test-Path (Join-Path $jniInclude 'jni.h'))) { throw "NDK jni.h not found at $jniInclude" }

$cargo = 'C:\Users\default.DP3650\.cargo\bin\cargo.exe'
$targets = @(
    @{ Name = 'arm64-v8a'; Rust = 'aarch64-linux-android'; Cc = 'aarch64-linux-android27-clang.cmd' },
    @{ Name = 'armeabi-v7a'; Rust = 'armv7-linux-androideabi'; Cc = 'armv7a-linux-androideabi27-clang.cmd' },
    @{ Name = 'x86_64'; Rust = 'x86_64-linux-android'; Cc = 'x86_64-linux-android27-clang.cmd' }
)

foreach ($t in $targets) {
    Write-Host "Building Rust core for $($t.Rust)..."
    & $cargo build --offline --release --manifest-path (Join-Path $core 'Cargo.toml') --target $t.Rust
    if ($LASTEXITCODE -ne 0) { throw "cargo build failed for $($t.Rust)" }

    $staticLib = Join-Path $core "target\$($t.Rust)\release\libbaroweather_core.a"
    if (-not (Test-Path $staticLib)) { throw "Missing static library: $staticLib" }

    $outDir = Join-Path $jniOut $t.Name
    New-Item -ItemType Directory -Path $outDir -Force | Out-Null
    $so = Join-Path $outDir 'libbaroweather_core.so'

    # Android 15 devices can use 16 KB memory pages. NDK r27 defaults to 4 KB
    # ELF alignment, which can crash native loads on those devices. Rebuild every
    # .so with 16 KB LOAD/RELRO alignment; this remains valid on 4 KB devices.
    & (Join-Path $bin $t.Cc) `
        -shared -fPIC `
        '-Wl,-z,max-page-size=16384' `
        '-Wl,-z,common-page-size=16384' `
        -I $jniInclude `
        -o $so $jni $staticLib `
        -llog -lm -ldl
    if ($LASTEXITCODE -ne 0) { throw "clang link failed for $($t.Name)" }

    $readelf = Join-Path $bin 'llvm-readelf.exe'
    if (Test-Path $readelf) {
        $loadLines = & $readelf -l -W $so | Select-String -Pattern '^\s*LOAD\s'
        foreach ($line in $loadLines) {
            if ($line.ToString() -notmatch '0x4000\s*$') {
                throw "Native library is not 16 KB aligned: $so -> $line"
            }
        }
    }

    Write-Host "  -> $so ($((Get-Item $so).Length) bytes)"
}
