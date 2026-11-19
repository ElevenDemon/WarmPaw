param([string]$OutputPath = '')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not $OutputPath) { $OutputPath = Join-Path $projectRoot 'release/nuanzhua-linux.tar.gz' }
$archivePath = [IO.Path]::GetFullPath($OutputPath)
$archiveDir = Split-Path -Parent $archivePath
New-Item -ItemType Directory -Path $archiveDir -Force | Out-Null
Get-Command tar.exe -ErrorAction Stop | Out-Null
$tarArguments = @(
    '--exclude=node_modules', '--exclude=.npm-cache', '--exclude=.cache', '--exclude=unpackage', '--exclude=target', '--exclude=dist', '--exclude=.m2',
    '--exclude=.venv', '--exclude=.runtime-pg', '--exclude=*.egg-info', '--exclude=venv', '--exclude=__pycache__', '--exclude=.pytest_cache',
    '--exclude=.git', '--exclude=.idea', '--exclude=.vscode', '--exclude=.codex', '--exclude=.agents',
    '--exclude=logs', '--exclude=uploads', '--exclude=test-results', '--exclude=backups',
    '--exclude=.env*', '--exclude=application-ai-local*', '--exclude=*.log',
    '--exclude=.deploy.lock', '--exclude=*.zip', '--exclude=*.tar.gz',
    '-czf', $archivePath, '-C', $projectRoot,
    'deploy.sh', '.dockerignore', 'deploy', '2.petVue', '3.petSpringboot', '4.uni-app', '5.ai'
)
& tar.exe @tarArguments
if ($LASTEXITCODE -ne 0) { throw '部署包生成失败' }
Write-Host "已生成：$archivePath"
Write-Host '上传到 Linux，解压后执行：sudo bash deploy.sh deploy --ip 你的公网IP'
