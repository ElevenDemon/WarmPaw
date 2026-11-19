param(
    # 显式启用演示账号、演示支付和退款。
    [switch]$Demo,
    # 使用现有JAR，跳过构建和测试。
    [switch]$SkipBuild
)
$ErrorActionPreference = 'Stop'
$previousDemo = $env:DEMO_ENABLED
Push-Location $PSScriptRoot
try {
    if ($Demo) { $env:DEMO_ENABLED = 'true' }
    if (-not $SkipBuild) {
        # 本地仓库可重复使用；新机器构建会自动下载依赖。
        & mvn -gs settings.xml -s settings.xml "-Dmaven.repo.local=$PSScriptRoot/.m2" -B -ntp package
        if ($LASTEXITCODE -ne 0) { throw 'Maven构建失败，请检查输出。' }
    }
    if (-not (Test-Path -LiteralPath 'target/pet-backend-1.0.0.jar')) { throw '没有找到JAR，请先构建。' }
    & java -jar target/pet-backend-1.0.0.jar
    if ($LASTEXITCODE -ne 0) { throw '应用启动或运行失败，请检查日志。' }
}
finally {
    # 恢复调用终端原有环境，避免脚本改变后续启动方式。
    $env:DEMO_ENABLED = $previousDemo
    Pop-Location
}
