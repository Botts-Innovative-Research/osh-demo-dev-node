#!/bin/bash

# Exit on any error
set -e

# Truststore settings
TRUSTSTORE="truststore.jks"
PASSWORD="changeit"

# Clean up any existing truststore
rm -f $TRUSTSTORE

# List of domains to fetch certs from
declare -A SITES=(
  ["sta.ci.taiwan.gov.tw"]=443
  ["iapi.wra.gov.tw"]=443
  ["airtw.moenv.gov.tw"]=443
)

# Temp cert folder
CERT_DIR="./certs"
mkdir -p "$CERT_DIR"

echo "Creating truststore: $TRUSTSTORE"

i=0
for DOMAIN in "${!SITES[@]}"; do
  PORT=${SITES[$DOMAIN]}
  ALIAS="site$i"
  CERT_FILE="$CERT_DIR/$DOMAIN.crt"
  echo "Fetching certificate from $DOMAIN:$PORT..."

  # Extract the cert
  echo | openssl s_client -servername "$DOMAIN" -connect "$DOMAIN:$PORT" -showcerts 2>/dev/null |
    openssl x509 -outform PEM > "$CERT_FILE"

  echo "Importing $DOMAIN certificate into truststore..."
  keytool -importcert -noprompt \
    -alias "$ALIAS" \
    -file "$CERT_FILE" \
    -keystore "$TRUSTSTORE" \
    -storepass "$PASSWORD"

  ((i++))
done

echo "Truststore $TRUSTSTORE created successfully with ${#SITES[@]} certificates."
