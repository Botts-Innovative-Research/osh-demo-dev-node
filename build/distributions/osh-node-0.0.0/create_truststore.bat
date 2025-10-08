@echo off
setlocal EnableDelayedExpansion

REM Truststore settings
set TRUSTSTORE=truststore.jks
set PASSWORD=changeit

REM Delete old truststore
if exist "%TRUSTSTORE%" del /f "%TRUSTSTORE%"

REM Domains and ports
set DOMAINS=sta.ci.taiwan.gov.tw iapi.wra.gov.tw airtw.moenv.gov.tw
set PORTS=443 443 443

REM Certificate output directory
set CERT_DIR=certs
if not exist "%CERT_DIR%" mkdir "%CERT_DIR%"

echo Creating truststore: %TRUSTSTORE%

set i=0
for %%D in (%DOMAINS%) do (
    call set DOMAIN=%%D
    call set PORT=!PORTS:~0,3!
    call set PORTS=!PORTS:~4!

    set ALIAS=site!i!
    set CERT_FILE=%CERT_DIR%\!DOMAIN!.pem

    echo Fetching certificate from !DOMAIN!:!PORT!...

    REM Use OpenSSL to get the cert
    echo | openssl s_client -servername !DOMAIN! -connect !DOMAIN!:!PORT! -showcerts > temp_cert.txt

    REM Extract just the PEM cert (first one only)
    for /f "tokens=*" %%a in ('findstr /r "-----BEGIN CERTIFICATE-----" temp_cert.txt') do (
        (
            echo -----BEGIN CERTIFICATE-----
            for /f "delims=" %%b in ('more +1 temp_cert.txt ^| findstr /v "CONNECTED"') do (
                echo %%b
                if "%%b"=="-----END CERTIFICATE-----" goto :done
            )
        ) > "!CERT_FILE!"
        :done
    )

    del temp_cert.txt

    echo Importing !DOMAIN! certificate into truststore...
    keytool -importcert -noprompt -alias !ALIAS! -file "!CERT_FILE!" -keystore "%TRUSTSTORE%" -storepass %PASSWORD%

    set /a i+=1
)

echo Truststore %TRUSTSTORE% created successfully with %i% certificates.
