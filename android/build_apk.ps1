param(
    [string]$SdkRoot = "D:\Android",
    [string]$BuildToolsVer = "34.0.0",
    [string]$PlatformVer = "android-34",
    [string]$LibRoot = "D:\SauceStandalone_android\lib",
    [string]$OutDir = "D:\SauceStandalone_android",
    [string]$WorkDir = "D:\SauceBUILD_android_apk"
)

$ErrorActionPreference = "Stop"

$JdkBin = "C:\Program Files\Java\jdk-21.0.12\bin"
if (Test-Path $JdkBin) {
    $env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.12"
    $env:PATH = "$JdkBin;$env:PATH"
}

$BuildTools = Join-Path $SdkRoot "build-tools\$BuildToolsVer"
$AndroidJar = Join-Path $SdkRoot "platforms\$PlatformVer\android.jar"
$Aapt2 = Join-Path $BuildTools "aapt2.exe"
$D8 = Join-Path $BuildTools "d8.bat"
$Zipalign = Join-Path $BuildTools "zipalign.exe"
$Apksigner = Join-Path $BuildTools "apksigner.bat"

foreach ($tool in @($AndroidJar, $Aapt2, $D8, $Zipalign, $Apksigner)) {
    if (-not (Test-Path $tool)) {
        throw "Missing required Android build file: $tool"
    }
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ResDir = Join-Path $ScriptDir "res"
$AssetsDir = Join-Path $ScriptDir "assets"
$Manifest = Join-Path $ScriptDir "AndroidManifest.xml"
$SrcDir = Join-Path $ScriptDir "src"
$PackScript = Join-Path $ScriptDir "pack_apk.py"

if (Test-Path $WorkDir) {
    Remove-Item $WorkDir -Recurse -Force
}
$GenDir = Join-Path $WorkDir "gen"
$ResOutDir = Join-Path $WorkDir "compiled_res"
$ClassesDir = Join-Path $WorkDir "classes"
$DexOutDir = Join-Path $WorkDir "dex"
New-Item -ItemType Directory -Force -Path $WorkDir, $GenDir, $ResOutDir, $ClassesDir, $DexOutDir, $OutDir | Out-Null

Write-Host "[1/6] Compiling Android resources..."
$CompiledZip = Join-Path $ResOutDir "res.zip"
& $Aapt2 compile --dir $ResDir -o $CompiledZip
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed" }

Write-Host "[2/6] Linking resources and manifest..."
$BaseApk = Join-Path $WorkDir "base_unaligned.apk"
& $Aapt2 link `
    -o $BaseApk `
    -I $AndroidJar `
    --manifest $Manifest `
    -A $AssetsDir `
    --java $GenDir `
    --auto-add-overlay `
    $CompiledZip
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

Write-Host "[3/6] Compiling Java sources..."
$JavaFiles = @()
$JavaFiles += Get-ChildItem $SrcDir -Recurse -Filter "*.java" | ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' }
$JavaFiles += Get-ChildItem $GenDir -Recurse -Filter "*.java" | ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' }
$JavaArgFile = Join-Path $WorkDir "java_sources.txt"
$JavaFiles | Set-Content -Path $JavaArgFile -Encoding ASCII

& javac `
    -encoding UTF-8 `
    -source 11 -target 11 `
    -Xlint:-options `
    -classpath $AndroidJar `
    -d $ClassesDir `
    "@$JavaArgFile"
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Write-Host "[4/6] Converting JVM bytecode to DEX with d8..."
$ClassesJar = Join-Path $WorkDir "classes.jar"
$StripScript = Join-Path $ScriptDir "strip_method_params.py"
& python $StripScript $ClassesDir
if ($LASTEXITCODE -ne 0) { throw "strip_method_params.py failed" }
& jar cf $ClassesJar -C $ClassesDir .
if ($LASTEXITCODE -ne 0) { throw "jar failed" }

& cmd /c "`"$D8`" --lib `"$AndroidJar`" --min-api 21 --output `"$DexOutDir`" `"$ClassesJar`""
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }
$DexFile = Join-Path $DexOutDir "classes.dex"
if (-not (Test-Path $DexFile)) { throw "classes.dex not produced" }

Write-Host "[5/6] Ensuring debug signing keystore..."
$Keystore = Join-Path $SdkRoot "debug.keystore"
if (-not (Test-Path $Keystore)) {
    & keytool -genkeypair `
        -v `
        -keystore $Keystore `
        -storepass android `
        -alias androiddebugkey `
        -keypass android `
        -keyalg RSA `
        -keysize 2048 `
        -validity 10000 `
        -dname "CN=Android Debug,O=Android,C=US"
    if ($LASTEXITCODE -ne 0) { throw "keytool failed" }
}

$Targets = @(
    @{ Name = "hl2-sauce-universal.apk"; Abis = @("arm64-v8a", "armeabi-v7a") },
    @{ Name = "hl2-sauce-arm64-v8a.apk"; Abis = @("arm64-v8a") },
    @{ Name = "hl2-sauce-armeabi-v7a.apk"; Abis = @("armeabi-v7a") }
)

Write-Host "[6/6] Packaging, aligning, and signing APKs..."
foreach ($t in $Targets) {
    $ApkName = $t.Name
    $UnsignedApk = Join-Path $WorkDir ($ApkName + ".unsigned")
    $AlignedApk = Join-Path $WorkDir ($ApkName + ".aligned")
    $FinalApk = Join-Path $OutDir $ApkName

    & python $PackScript `
        --base-apk $BaseApk `
        --dex $DexFile `
        --lib-root $LibRoot `
        --abis $t.Abis `
        --out $UnsignedApk
    if ($LASTEXITCODE -ne 0) { throw "pack_apk.py failed for $ApkName" }

    & $Zipalign -p -f 4 $UnsignedApk $AlignedApk
    if ($LASTEXITCODE -ne 0) { throw "zipalign failed for $ApkName" }

    & cmd /c "`"$Apksigner`" sign --ks `"$Keystore`" --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out `"$FinalApk`" `"$AlignedApk`""
    if ($LASTEXITCODE -ne 0) { throw "apksigner sign failed for $ApkName" }

    & cmd /c "`"$Apksigner`" verify --verbose `"$FinalApk`"" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "apksigner verify failed for $ApkName" }

    $SizeMB = [math]::Round((Get-Item $FinalApk).Length / 1MB, 2)
    Write-Host "  Built & verified: $FinalApk ($SizeMB MB) [ABIs: $($t.Abis -join ', ')]"
}
