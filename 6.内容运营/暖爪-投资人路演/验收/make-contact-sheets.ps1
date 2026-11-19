Add-Type -AssemblyName System.Drawing
$preview=Join-Path $PSScriptRoot 'dynamic-powerpoint\preview'
foreach($start in @(1,5,9)){
    $bitmap=[Drawing.Bitmap]::new(1600,900)
    $g=[Drawing.Graphics]::FromImage($bitmap)
    $g.Clear([Drawing.Color]::FromArgb(218,218,214))
    $g.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    for($j=0;$j -lt 4;$j++){
        $im=[Drawing.Image]::FromFile((Join-Path $preview ('{0:00}.png' -f ($start+$j))))
        $g.DrawImage($im,[Drawing.Rectangle]::new(($j%2)*800+4,[Math]::Floor($j/2)*450+4,792,442))
        $im.Dispose()
    }
    $bitmap.Save((Join-Path (Split-Path -Parent $preview) ('contact-{0:00}.png' -f $start)),[Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose();$bitmap.Dispose()
}
