param(
    [ValidateSet('Build', 'Test', 'Run', 'FetchDriver')]
    [string]$Task = 'Build'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$projectRoot = $PSScriptRoot
$driverName = 'mssql-jdbc-13.6.0.jre11.jar'
$driverSha256 = '4c566e4022c4dcf1489aeb639fe49a0f6ffae3d796fdc2f56b5b8882d4c3bf5a'
$driverPath = Join-Path $projectRoot "lib/$driverName"
$driverUrl = "https://repo.maven.apache.org/maven2/com/microsoft/sqlserver/mssql-jdbc/13.6.0.jre11/$driverName"

function Get-JavaTool([string]$Name) {
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin/$Name"
        if (Test-Path -LiteralPath "$candidate.exe") { return "$candidate.exe" }
        if (Test-Path -LiteralPath $candidate) { return $candidate }
    }
    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if (-not $command) { throw "JDK 11+ diperlukan. Atur JAVA_HOME atau tambahkan bin JDK ke PATH ($Name tidak ditemukan)." }
    return $command.Source
}

function Invoke-JavaTool([string]$Name, [string[]]$Arguments) {
    $executable = Get-JavaTool $Name
    & $executable @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Name gagal (exit $LASTEXITCODE)." }
}

function Reset-BuildDirectory([string]$Path) {
    $resolved = [IO.Path]::GetFullPath($Path)
    $allowed = [IO.Path]::GetFullPath((Join-Path $projectRoot 'build')) + [IO.Path]::DirectorySeparatorChar
    if (-not $resolved.StartsWith($allowed, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Direktori output berada di luar build/.'
    }
    if (Test-Path -LiteralPath $resolved) { Remove-Item -LiteralPath $resolved -Recurse -Force }
    New-Item -ItemType Directory -Path $resolved -Force | Out-Null
}

function Test-Driver {
    return (Test-Path -LiteralPath $driverPath) -and
        ((Get-FileHash -LiteralPath $driverPath -Algorithm SHA256).Hash -eq $driverSha256)
}

Push-Location -LiteralPath $projectRoot
try {
    if ($Task -eq 'FetchDriver') {
        if (-not (Test-Driver)) {
            New-Item -ItemType Directory -Path (Join-Path $projectRoot 'lib') -Force | Out-Null
            $downloadPath = "$driverPath.download"
            try {
                Invoke-WebRequest -Uri $driverUrl -OutFile $downloadPath -UseBasicParsing
                if ((Get-FileHash -LiteralPath $downloadPath -Algorithm SHA256).Hash -ne $driverSha256) {
                    throw 'Checksum driver tidak sesuai; hasil unduhan tidak dipasang.'
                }
                Move-Item -LiteralPath $downloadPath -Destination $driverPath -Force
            } finally {
                if (Test-Path -LiteralPath $downloadPath) { Remove-Item -LiteralPath $downloadPath -Force }
            }
        }
        Write-Host "Driver siap: lib/$driverName (SHA256 terverifikasi)."
        exit 0
    }

    $classes = Join-Path $projectRoot 'build/classes'
    Reset-BuildDirectory $classes
    $sources = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src') -Filter '*.java' -Recurse -File |
        ForEach-Object { $_.FullName })
    if (-not $sources.Count) { throw 'Tidak ada source Java di src/.' }
    Invoke-JavaTool 'javac' (@('--release', '11', '-encoding', 'UTF-8', '-d', $classes) + $sources)

    $distribution = Join-Path $projectRoot 'dist'
    New-Item -ItemType Directory -Path $distribution -Force | Out-Null
    $manifest = Join-Path $projectRoot 'build/MANIFEST.MF'
    [IO.File]::WriteAllText($manifest,
        "Manifest-Version: 1.0`nMain-Class: shoe.store.login_page`nClass-Path: ../lib/$driverName`n`n",
        [Text.UTF8Encoding]::new($false))
    Invoke-JavaTool 'jar' @('--create', '--file', (Join-Path $distribution 'shoe-store.jar'), '--manifest', $manifest, '-C', $classes, '.')
    Write-Host 'Build berhasil: dist/shoe-store.jar (target Java 11).'

    if ($Task -eq 'Test') {
        $testClasses = Join-Path $projectRoot 'build/test/classes'
        Reset-BuildDirectory $testClasses
        $testSources = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'test') -Filter '*.java' -Recurse -File)
        if (-not $testSources.Count) { throw 'Tidak ada test Java di test/.' }
        Invoke-JavaTool 'javac' (@('--release', '11', '-encoding', 'UTF-8', '-cp', $classes, '-d', $testClasses) + @($testSources.FullName))
        $classpath = $classes + [IO.Path]::PathSeparator + $testClasses
        $tests = @($testSources | Where-Object { $_.BaseName.EndsWith('Test') })
        foreach ($test in $tests) {
            $source = [IO.File]::ReadAllText($test.FullName)
            $package = [regex]::Match($source, '(?m)^\s*package\s+([\w.]+)\s*;')
            $class = if ($package.Success) { $package.Groups[1].Value + '.' + $test.BaseName } else { $test.BaseName }
            Invoke-JavaTool 'java' @('-Djava.awt.headless=true', '-ea', '-cp', $classpath, $class)
        }
        Write-Host "Test berhasil: $($tests.Count) test class, tanpa koneksi database."
    } elseif ($Task -eq 'Run') {
        if (-not (Test-Driver)) { throw 'Driver belum tersedia atau checksum berbeda. Jalankan .\build.ps1 -Task FetchDriver.' }
        Invoke-JavaTool 'java' @('-jar', (Join-Path $distribution 'shoe-store.jar'))
    }
} finally {
    Pop-Location
}
