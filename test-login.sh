#!/bin/bash
./gradlew test --tests 'br.dev.demoraes.abrolhos.GetTotpTest' > /dev/null
TOTP=$(cat /tmp/totp.txt)
echo "Generating TOTP... $TOTP"

payload="{\"username\": \"anisiolemos\", \"password\": \"abrolhos\", \"totpCode\": \"$TOTP\"}"

for i in {1..6}; do
  echo "Request $i:"
  curl -s -i http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d "$payload"
  echo ""
  echo "---"
  sleep 0.5
done
