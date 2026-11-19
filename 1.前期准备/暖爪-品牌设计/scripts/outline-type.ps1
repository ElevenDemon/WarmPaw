Add-Type -AssemblyName System.Drawing
$ErrorActionPreference = 'Stop'
$culture = [Globalization.CultureInfo]::InvariantCulture
function Convert-Outline([string]$Text, [string]$Family, [float]$Size, [System.Drawing.FontStyle]$Style) {
    $font = [System.Drawing.FontFamily]::new($Family)
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $path.AddString($Text, $font, [int]$Style, $Size, [System.Drawing.PointF]::new(0,0), [System.Drawing.StringFormat]::GenericTypographic)
    $bounds = $path.GetBounds()
    $points = $path.PathPoints
    $types = $path.PathTypes
    $parts = [Collections.Generic.List[string]]::new()
    for ($i = 0; $i -lt $points.Length; $i++) {
        $type = $types[$i] -band 7
        $x = ($points[$i].X - $bounds.X).ToString('0.###', $culture)
        $y = ($points[$i].Y - $bounds.Y).ToString('0.###', $culture)
        if ($type -eq 0) { $parts.Add("M$x $y") }
        elseif ($type -eq 1) { $parts.Add("L$x $y") }
        elseif ($type -eq 3) {
            $x2 = ($points[$i+1].X - $bounds.X).ToString('0.###', $culture)
            $y2 = ($points[$i+1].Y - $bounds.Y).ToString('0.###', $culture)
            $x3 = ($points[$i+2].X - $bounds.X).ToString('0.###', $culture)
            $y3 = ($points[$i+2].Y - $bounds.Y).ToString('0.###', $culture)
            $parts.Add("C$x $y $x2 $y2 $x3 $y3")
            $i += 2
        }
        if (($types[$i] -band 128) -ne 0) { $parts.Add('Z') }
    }
    $result = @{d=($parts -join ' '); width=$bounds.Width; height=$bounds.Height}
    $path.Dispose(); $font.Dispose()
    return $result
}
$data = @{
    chinese = Convert-Outline '暖爪' 'Microsoft YaHei' 160 ([System.Drawing.FontStyle]::Bold)
    english = Convert-Outline 'WARM PAW' 'Arial' 40 ([System.Drawing.FontStyle]::Bold)
}
$data | ConvertTo-Json -Depth 4 | Set-Content -Encoding UTF8 (Join-Path $PSScriptRoot 'type-outlines.json')
