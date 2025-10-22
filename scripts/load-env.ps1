param([string]$EnvFile = ".env")

if (!(Test-Path $EnvFile)) {
  Write-Error "Arquivo '$EnvFile' não encontrado."
  exit 1
}

Get-Content $EnvFile | ForEach-Object {
  if ($_ -match '^[\s]*#' -or $_ -match '^[\s]*$') { return }
  $parts = $_ -split '=', 2
  if ($parts.Length -ne 2) { return }
  $name = $parts[0].Trim()
  $value = $parts[1].Trim()
  [Environment]::SetEnvironmentVariable($name, $value, 'Process')
  Set-Item -Path Env:\$name -Value $value
}

Write-Host "Variáveis carregadas na sessão atual do PowerShell."