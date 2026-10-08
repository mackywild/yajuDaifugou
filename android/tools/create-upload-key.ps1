param(
    [string]$Alias = "yaju-upload"
)

$ErrorActionPreference = "Stop"

$Root = Resolve-Path (Join-Path $PSScriptRoot "..")
$SigningDir = Join-Path $Root "signing"
$Keystore = Join-Path $SigningDir "yaju-upload.jks"
$Certificate = Join-Path $SigningDir "yaju-upload-certificate.pem"
$Base64File = Join-Path $SigningDir "yaju-upload.jks.b64"

New-Item -ItemType Directory -Force -Path $SigningDir | Out-Null

if (Test-Path $Keystore) {
    throw "Keystore already exists: $Keystore. Delete it only if you intentionally want a NEW upload key."
}

Write-Host "Creating Google Play upload key..."
Write-Host "You will be prompted for passwords and certificate owner fields."

keytool -genkeypair -v -storetype JKS -keystore $Keystore -alias $Alias -keyalg RSA -keysize 4096 -validity 10000
if ($LASTEXITCODE -ne 0) { throw "keytool failed to create the upload key." }

Write-Host ""
Write-Host "Exporting PUBLIC certificate..."
keytool -export -rfc -keystore $Keystore -alias $Alias -file $Certificate
if ($LASTEXITCODE -ne 0) { throw "Failed to export certificate." }

Write-Host ""
Write-Host "Certificate fingerprints:"
keytool -list -v -keystore $Keystore -alias $Alias | Select-String -Pattern "SHA1:|SHA256:"

[Convert]::ToBase64String([IO.File]::ReadAllBytes($Keystore)) | Set-Content -NoNewline -Encoding ascii $Base64File

Write-Host ""
Write-Host "Created:"
Write-Host "  Private upload key : $Keystore"
Write-Host "  Public certificate : $Certificate"
Write-Host "  GitHub Secret value: $Base64File"
Write-Host ""
Write-Host "Back up the .jks file and passwords somewhere outside this repository."
Write-Host "GitHub Secrets:"
Write-Host "  YAJU_UPLOAD_KEYSTORE_B64 = contents of yaju-upload.jks.b64"
Write-Host "  YAJU_STORE_PASSWORD       = keystore password"
Write-Host "  YAJU_KEY_ALIAS            = $Alias"
Write-Host "  YAJU_KEY_PASSWORD         = key password"
