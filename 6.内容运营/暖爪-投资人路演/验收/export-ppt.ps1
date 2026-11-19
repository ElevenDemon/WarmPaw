$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$deckRoot = Split-Path -Parent $PSScriptRoot
$buildDir = Join-Path $PSScriptRoot 'powerpoint-build'
$previewDir = Join-Path $buildDir 'ppt-preview'
New-Item -ItemType Directory -Path $buildDir, $previewDir -Force | Out-Null
$finalFile = Join-Path (Split-Path -Parent $deckRoot) '暖爪-投资人路演-12页.ppt'
$html = [System.IO.File]::ReadAllText((Join-Path $deckRoot 'index.html'))
$notesMatch = [regex]::Match($html, 'const SPEAKER_NOTES = (\[[\s\S]*?\]);\s*window\.__SPEAKER_NOTES__')
if (-not $notesMatch.Success) { throw 'Speaker notes not found' }
$speakerNotes = ConvertFrom-Json $notesMatch.Groups[1].Value
if ($speakerNotes.Count -ne 12) { throw 'Unexpected slide count' }
$pptApp = $null
$presentation = $null
try {
    for ($i=1; $i -le 12; $i++) {
        $source = Join-Path $PSScriptRoot ('{0:00}.png' -f $i)
        $bitmap = New-Object System.Drawing.Bitmap($source)
        $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
        try {
            # Remove browser keyboard controls and page dots from the approved screenshots.
            if ($i -eq 1) {
                $patch = $bitmap.Clone([System.Drawing.Rectangle]::new(1150,829,420,18), $bitmap.PixelFormat)
                for ($y=847; $y -lt 900; $y+=18) { $graphics.DrawImageUnscaled($patch,1150,$y) }
                $patch.Dispose()
                $patch = $bitmap.Clone([System.Drawing.Rectangle]::new(695,829,210,18), $bitmap.PixelFormat)
                for ($y=865; $y -lt 900; $y+=18) { $graphics.DrawImageUnscaled($patch,695,$y) }
                $patch.Dispose()
            } else {
                $brush = [System.Drawing.SolidBrush]::new($bitmap.GetPixel(1590,840))
                $graphics.FillRectangle($brush,1145,847,435,53)
                $brush.Dispose()
                if ($i -in @(11,12)) {
                    $brush = [System.Drawing.SolidBrush]::new($bitmap.GetPixel(810,840))
                    $graphics.FillRectangle($brush,800,867,110,33)
                    $brush.Dispose()
                    if ($i -eq 12) {
                        $patch = $bitmap.Clone([System.Drawing.Rectangle]::new(695,829,105,18), $bitmap.PixelFormat)
                        for ($y=865; $y -lt 900; $y+=18) { $graphics.DrawImageUnscaled($patch,695,$y) }
                        $patch.Dispose()
                    } else {
                        $brush = [System.Drawing.SolidBrush]::new($bitmap.GetPixel(700,850))
                        $graphics.FillRectangle($brush,695,867,105,33)
                        $brush.Dispose()
                    }
                } else {
                    $brush = [System.Drawing.SolidBrush]::new($bitmap.GetPixel(700,850))
                    $graphics.FillRectangle($brush,695,867,215,33)
                    $brush.Dispose()
                }
            }
            if ($i -eq 6) {
                $patch = $bitmap.Clone([System.Drawing.Rectangle]::new(1354,495,145,34), $bitmap.PixelFormat)
                $graphics.DrawImageUnscaled($patch,1354,430)
                $patch.Dispose()
            }
            $bitmap.Save((Join-Path $buildDir ('slide-{0:00}.png' -f $i)), [System.Drawing.Imaging.ImageFormat]::Png)
        } finally { $graphics.Dispose(); $bitmap.Dispose() }
    }
    $pptApp = New-Object -ComObject PowerPoint.Application
    $pptApp.DisplayAlerts = 1
    $presentation = $pptApp.Presentations.Add(0)
    $presentation.PageSetup.SlideWidth = 960
    $presentation.PageSetup.SlideHeight = 540
    for ($i=1; $i -le 12; $i++) {
        $slide = $presentation.Slides.Add($i,12)
        $note = $speakerNotes[$i-1]
        $slide.Name = ('{0:00}-{1}' -f $i,$note.id)
        $image = $slide.Shapes.AddPicture((Join-Path $buildDir ('slide-{0:00}.png' -f $i)),0,-1,0,0,960,540)
        $image.Name = 'Verified slide artwork'
        $image.AlternativeText = $note.title
        $noteText = $note.title + "`r`n`r`n" + ($note.talk -join "`r`n") + "`r`n`r`n转场：" + $note.transition
        $slide.NotesPage.Shapes.Placeholders.Item(2).TextFrame.TextRange.Text = $noteText
    }
    # ppSaveAsPresentation (1) writes genuine PowerPoint 97-2003 binary format.
    $presentation.SaveAs($finalFile,1)
    $presentation.Close()
    $presentation = $null
    $presentation = $pptApp.Presentations.Open($finalFile, -1, 0, 0)
    if ($presentation.Slides.Count -ne 12) { throw 'Reopened deck does not have 12 slides' }
    for ($i=1; $i -le 12; $i++) {
        $presentation.Slides.Item($i).Export((Join-Path $previewDir ('{0:00}.png' -f $i)), 'PNG',1600,900)
    }
    $bytes = [System.IO.File]::ReadAllBytes($finalFile)
    $signature = [System.BitConverter]::ToString($bytes[0..7])
    if ($signature -ne 'D0-CF-11-E0-A1-B1-1A-E1') { throw 'Not a binary .ppt file' }
    $report = [ordered]@{output=$finalFile;slides=$presentation.Slides.Count;width=$presentation.PageSetup.SlideWidth;height=$presentation.PageSetup.SlideHeight;signature=$signature;notes=$speakerNotes.Count;pageArtwork='Static images';verifiedIn='Microsoft PowerPoint '+$pptApp.Version}
    $report | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $buildDir 'ppt-validation.json') -Encoding UTF8
    $report | ConvertTo-Json
} finally {
    if ($presentation) { $presentation.Close(); [System.Runtime.InteropServices.Marshal]::ReleaseComObject($presentation) | Out-Null }
    if ($pptApp) { $pptApp.Quit(); [System.Runtime.InteropServices.Marshal]::ReleaseComObject($pptApp) | Out-Null }
}
