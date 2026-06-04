Add-Type -AssemblyName System.Drawing

function New-Canvas {
    param(
        [int]$Width,
        [int]$Height
    )

    $bitmap = New-Object System.Drawing.Bitmap($Width, $Height)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    return [PSCustomObject]@{
        Bitmap = $bitmap
        Graphics = $graphics
    }
}

function New-Brush {
    param([string]$Hex)

    return New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml($Hex))
}

function New-PointF {
    param(
        [float]$X,
        [float]$Y
    )

    return [System.Drawing.PointF]::new($X, $Y)
}

function Fill-RoundedRectangle {
    param(
        [System.Drawing.Graphics]$Graphics,
        [System.Drawing.Brush]$Brush,
        [float]$X,
        [float]$Y,
        [float]$Width,
        [float]$Height,
        [float]$Radius
    )

    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $diameter = $Radius * 2
    $path.AddArc($X, $Y, $diameter, $diameter, 180, 90)
    $path.AddArc($X + $Width - $diameter, $Y, $diameter, $diameter, 270, 90)
    $path.AddArc($X + $Width - $diameter, $Y + $Height - $diameter, $diameter, $diameter, 0, 90)
    $path.AddArc($X, $Y + $Height - $diameter, $diameter, $diameter, 90, 90)
    $path.CloseFigure()
    $Graphics.FillPath($Brush, $path)
    $path.Dispose()
}

function Draw-FMark {
    param(
        [System.Drawing.Graphics]$Graphics,
        [float]$Scale,
        [float]$OffsetX,
        [float]$OffsetY
    )

    $teal = New-Brush "#00D1B2"
    $dark = New-Brush "#061014"
    $gold = New-Brush "#FFC857"

    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $points = [System.Drawing.PointF[]]@(
        (New-PointF ($OffsetX + 22 * $Scale) ($OffsetY + 18 * $Scale)),
        (New-PointF ($OffsetX + 80 * $Scale) ($OffsetY + 18 * $Scale)),
        (New-PointF ($OffsetX + 92 * $Scale) ($OffsetY + 21 * $Scale)),
        (New-PointF ($OffsetX + 98 * $Scale) ($OffsetY + 32 * $Scale)),
        (New-PointF ($OffsetX + 94 * $Scale) ($OffsetY + 43 * $Scale)),
        (New-PointF ($OffsetX + 80 * $Scale) ($OffsetY + 48 * $Scale)),
        (New-PointF ($OffsetX + 46 * $Scale) ($OffsetY + 48 * $Scale)),
        (New-PointF ($OffsetX + 46 * $Scale) ($OffsetY + 58 * $Scale)),
        (New-PointF ($OffsetX + 74 * $Scale) ($OffsetY + 58 * $Scale)),
        (New-PointF ($OffsetX + 86 * $Scale) ($OffsetY + 61 * $Scale)),
        (New-PointF ($OffsetX + 92 * $Scale) ($OffsetY + 72 * $Scale)),
        (New-PointF ($OffsetX + 88 * $Scale) ($OffsetY + 83 * $Scale)),
        (New-PointF ($OffsetX + 74 * $Scale) ($OffsetY + 88 * $Scale)),
        (New-PointF ($OffsetX + 22 * $Scale) ($OffsetY + 88 * $Scale))
    )
    $path.AddPolygon($points)
    $Graphics.FillPath($teal, $path)
    $path.Dispose()

    $Graphics.FillRectangle($dark, $OffsetX + 44 * $Scale, $OffsetY + 29 * $Scale, 32 * $Scale, 10 * $Scale)
    $Graphics.FillRectangle($dark, $OffsetX + 44 * $Scale, $OffsetY + 68 * $Scale, 27 * $Scale, 10 * $Scale)

    $play = New-Object System.Drawing.Drawing2D.GraphicsPath
    $play.AddPolygon([System.Drawing.PointF[]]@(
        (New-PointF ($OffsetX + 52 * $Scale) ($OffsetY + 45 * $Scale)),
        (New-PointF ($OffsetX + 79 * $Scale) ($OffsetY + 60 * $Scale)),
        (New-PointF ($OffsetX + 52 * $Scale) ($OffsetY + 75 * $Scale))
    ))
    $Graphics.FillPath($gold, $play)
    $play.Dispose()

    $teal.Dispose()
    $dark.Dispose()
    $gold.Dispose()
}

function Save-LauncherIcon {
    param(
        [string]$Path,
        [int]$Size
    )

    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Path) | Out-Null
    $canvas = New-Canvas $Size $Size
    $bitmap = $canvas.Bitmap
    $graphics = $canvas.Graphics

    $dark = New-Brush "#061014"
    $panel = New-Brush "#0B2A35"
    $graphics.FillRectangle($dark, 0, 0, $Size, $Size)
    Fill-RoundedRectangle $graphics $panel ($Size * 0.095) ($Size * 0.095) ($Size * 0.81) ($Size * 0.81) ($Size * 0.06)
    Draw-FMark $graphics ($Size / 108) 0 0

    $bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose()
    $bitmap.Dispose()
    $dark.Dispose()
    $panel.Dispose()
}

function Save-Banner {
    param([string]$Path)

    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Path) | Out-Null
    $canvas = New-Canvas 320 180
    $bitmap = $canvas.Bitmap
    $graphics = $canvas.Graphics

    $top = New-Brush "#0B2A35"
    $bottom = New-Brush "#12383D"
    $white = New-Brush "#EAF8FF"
    $muted = New-Brush "#A8B8C2"
    $teal = New-Brush "#00D1B2"

    $graphics.FillRectangle($top, 0, 0, 320, 96)
    $graphics.FillRectangle($bottom, 0, 96, 320, 84)
    Draw-FMark $graphics 1 24 24

    $fontFamily = New-Object System.Drawing.FontFamily("Arial")
    $titleFont = New-Object System.Drawing.Font($fontFamily, 34, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $subtitleFont = New-Object System.Drawing.Font($fontFamily, 13, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
    $graphics.DrawString("FPLAYER", $titleFont, $white, 134, 58)
    $graphics.DrawString("IPTV Player", $subtitleFont, $muted, 138, 100)
    $graphics.FillRectangle($teal, 138, 124, 66, 6)

    $bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose()
    $bitmap.Dispose()
    $top.Dispose()
    $bottom.Dispose()
    $white.Dispose()
    $muted.Dispose()
    $teal.Dispose()
    $titleFont.Dispose()
    $subtitleFont.Dispose()
    $fontFamily.Dispose()
}

$icons = @{
    "app/src/main/res/mipmap-mdpi/ic_fplayer_launcher.png" = 48
    "app/src/main/res/mipmap-hdpi/ic_fplayer_launcher.png" = 72
    "app/src/main/res/mipmap-xhdpi/ic_fplayer_launcher.png" = 96
    "app/src/main/res/mipmap-xxhdpi/ic_fplayer_launcher.png" = 144
    "app/src/main/res/mipmap-xxxhdpi/ic_fplayer_launcher.png" = 192
}

foreach ($entry in $icons.GetEnumerator()) {
    Save-LauncherIcon -Path $entry.Key -Size $entry.Value
}

Save-Banner -Path "app/src/main/res/drawable-nodpi/ic_fplayer_tv_banner.png"
