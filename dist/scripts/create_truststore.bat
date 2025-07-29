@echo off
setlocal enabledelayedexpansion

REM === Truststore variables ===
set TRUSTSTORE=truststore.jks
set PASSWORD=changeit

REM === Clean previous truststore and temp files ===
if exist %TRUSTSTORE% del %TRUSTSTORE%
if not exist certs mkdir certs

REM === Domains list ===
set DOMAINS[0]=sta.ci.taiwan.gov.tw
set DOMAINS[1]=iapi.wra.gov.tw
set DOMAINS[2]=airtw.moenv.gov.tw

set COUNT=0

:loop
set DOMAIN=!DOMAINS[%COUNT%]!
if "!DOMAIN!"=="" goto done

set CERTFILE=certs\!DOMAIN!.crt
echo Fetching certificate from !DOMAIN!...

REM === Fetch certificate using OpenSSL ===
echo | openssl s_client -servername !DOMAIN! -connect !DOMAIN!:443 -showcerts > temp_cert.txt

REM === Extract PEM certificate block ===
setlocal DisableDelayedExpansion
(
    set "COPY=0"
    for /f "usebackq delims=" %%L in ("temp_cert.txt") do (
        set "LINE=%%L"
        setlocal EnableDelayedExpansion
        if "!LINE!"=="-----BEGIN CERTIFICATE-----" (
            set "COPY=1"
        )
        if !COPY!==1 echo !LINE!
        if "!LINE!"=="-----END CERTIFICATE-----" (
            echo !LINE!
            set "COPY=0"
        )
        endlocal
    )
) > "!CERTFILE!"
endlocal

REM === Import certificate to truststore ===
echo Importing !DOMAIN! certificate...
keytool -importcert -noprompt -alias site%COUNT% -file "!CERTFILE!" -keystore %TRUSTSTORE% -storepass %PASSWORD%

set /a COUNT+=1
goto loop

:done
if exist temp_cert.txt del temp_cert.txt
echo.
echo Truststore %TRUSTSTORE% created with %COUNT% certificates.
pause
endlocal
