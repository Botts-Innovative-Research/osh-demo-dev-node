#!/bin/bash

set -e

TRUSTSTORE="truststore.jks"
PASSWORD="changeit"

rm -f $TRUSTSTORE

DOMAINS=("sta.ci.taiwan.gov.tw" "iapi.wra.gov.tw" "airtw.moenv.gov.tw")
PORTS=(443 443 443)

CERT_DIR="./certs"
mkdir -p "$CERT_DIR"

echo "Creating truststore: $TRUSTSTORE"

for i in "${!DOMAINS[@]}"; do
  DOMAIN="${DOMAINS[$i]}"
  PORT="${PORTS[$i]}"
  ALIAS="site$i"
  CERT_FILE="$CERT_DIR/$DOMAIN.pem"

  echo "Fetching certificate from $DOMAIN:$PORT..."

  echo | openssl s_client -servername "$DOMAIN" -connect "$DOMAIN:$PORT" -showcerts 2>/dev/null |
    openssl x509 -outform PEM > "$CERT_FILE"

  echo "Importing $DOMAIN certificate into truststore..."
  keytool -importcert -noprompt \
    -alias "$ALIAS" \
    -file "$CERT_FILE" \
    -keystore "$TRUSTSTORE" \
    -storepass "$PASSWORD"
done

echo "Truststore $TRUSTSTORE created successfully with ${#DOMAINS[@]} certificates."
