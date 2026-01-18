Add-Type @"
using System;
using System.Runtime.InteropServices;
public class User32 {
    [DllImport("user32.dll", SetLastError = true)]
    public static extern bool MoveWindow(IntPtr hWnd, int X, int Y, int nWidth, int nHeight, bool bRepaint);
}
"@


function Move-Window-Safe {
    param (
        [Parameter(Mandatory=$true)]
        [System.Diagnostics.Process]$Process,
        [int]$X, [int]$Y, [int]$W, [int]$H
    )


    $timeout = 0
    while ($Process.MainWindowHandle -eq [IntPtr]::Zero -and $timeout -lt 40) {
        Start-Sleep -Milliseconds 250
        $Process.Refresh() 
        $timeout++
    }

    $hwnd = $Process.MainWindowHandle
    if ($hwnd -ne [IntPtr]::Zero) {
        $null = [User32]::MoveWindow($hwnd, $X, $Y, $W, $H, $true)
        Write-Host "Ventana movida: $($Process.Id)" -ForegroundColor DarkGray
    } else {
        Write-Warning "No se pudo mover el proceso ID $($Process.Id) (¿La ventana tardó mucho en abrir?)"
    }
}


$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Definition
$rabbit = "localhost"
$db = "localhost"
$dbUser = "root"
$dbPass = '""'
$dbPort = 3306
$api = "https://pedvalar.webs.upv.es/iap/rest/sntn"


Clear-Host
Write-Host "--- INICIANDO PROYECTO TRANSIAP - GRUPO 8 ---" -ForegroundColor Cyan

Write-Host @"
  _____          _____      _____ _____  _    _ _____   ____    ___  
 |_   _|   /\   |  __ \    / ____|  __ \| |  | |  __ \ / __ \  / _ \ 
   | |    /  \  | |__) |  | |  __| |__) | |  | | |__) | |  | || (_) |
   | |   / /\ \ |  ___/   | | |_ |  _  /| |  | |  ___/| |  | | > _ < 
  _| |_ / ____ \| |       | |__| | | \ \| |__| | |    | |__| || (_) |
 |_____/_/    \_\_|        \_____|_|  \_\\____/|_|     \____/  \___/ 
                                                                     
"@ -ForegroundColor Yellow


Write-Host "Lanzando Infraestructura..." -ForegroundColor White
$v = Start-Process powershell -ArgumentList "-NoExit","-Command","`$Host.UI.RawUI.WindowTitle = 'Visualizador'; java -jar '$scriptPath\Visualizador.jar' $rabbit" -PassThru
$r = Start-Process powershell -ArgumentList "-NoExit","-Command","`$Host.UI.RawUI.WindowTitle = 'RegistroBD'; java -jar '$scriptPath\RegistroBD.jar' $rabbit $db $dbPort $dbUser $dbPass" -PassThru
$m = Start-Process powershell -ArgumentList "-NoExit","-Command","`$Host.UI.RawUI.WindowTitle = 'SoporteLogistica'; java -jar '$scriptPath\SoporteLogistica.jar' $rabbit $api" -PassThru


Start-Sleep -Seconds 2 
Write-Host "Lanzando Generadores..." -ForegroundColor White
$g1 = Start-Process powershell -ArgumentList "-NoExit","-Command","`$Host.UI.RawUI.WindowTitle = 'GenCSV'; java -jar '$scriptPath\GeneradorCSV.jar' $rabbit 1234-ABC 39.48 -0.34" -PassThru
$g2 = Start-Process powershell -ArgumentList "-NoExit","-Command","`$Host.UI.RawUI.WindowTitle = 'GenJSON'; java -jar '$scriptPath\GeneradorJSON.jar' $rabbit 5678-DEF 39.46 -0.37" -PassThru
$g3 = Start-Process powershell -ArgumentList "-NoExit","-Command","`$Host.UI.RawUI.WindowTitle = 'GenKML'; java -jar '$scriptPath\GeneradorKML.jar' $rabbit 9012-GHI 39.45 -0.32" -PassThru


Write-Host "Organizando ventanas..." -ForegroundColor Cyan

Move-Window-Safe $v 0 0 640 540
Move-Window-Safe $r 640 0 640 540
Move-Window-Safe $m 1280 0 640 540
Move-Window-Safe $g1 0 540 640 500
Move-Window-Safe $g2 640 540 640 500
Move-Window-Safe $g3 1280 540 640 500


Write-Host "`nSISTEMA OPERATIVO. Escribe 'quit' para salir." -ForegroundColor Green

while ($true) {
    $input = Read-Host ">"
    if ($input -match "^(quit|q)$") {
        Write-Host "Iniciando secuencia de apagado total..." -ForegroundColor Yellow
        
        
        $todosLosProcesos = @($v, $r, $m, $g1, $g2, $g3)

        foreach ($proc in $todosLosProcesos) {
           
            if ($proc -and $proc.Id) {
                # /F = Forzar bruscamente
                # /T = Mata el árbol de procesos 
                # /PID = El ID del proceso
                Write-Host "Eliminando proceso ID $($proc.Id)..." -ForegroundColor DarkGray
                cmd /c "taskkill /F /T /PID $($proc.Id)" | Out-Null
            }
        }
        
      
        $titulos = "Visualizador", "RegistroBD", "SoporteLogistica", "GenCSV", "GenJSON", "GenKML"
        foreach ($t in $titulos) {
            Get-Process | Where-Object { $_.MainWindowTitle -eq $t } | Stop-Process -Force -ErrorAction SilentlyContinue
        }

        Write-Host "Sistema cerrado." -ForegroundColor Green
        break
    }
}