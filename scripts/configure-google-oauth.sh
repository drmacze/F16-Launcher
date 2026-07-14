#!/bin/bash
# v6.8.4: Configure Google OAuth in Supabase
#
# PREREQUISITE:
#   1. Go to https://console.cloud.google.com → select project (dlavie-f16 or any)
#   2. APIs & Services → Credentials → Create Credentials → OAuth client ID
#   3. Application type: Web application
#   4. Authorized redirect URIs: https://lvmucsxbmadtsgrxuwmo.supabase.co/auth/v1/callback
#   5. Copy Client ID and Client Secret
#   6. Run this script:
#        ./configure-google-oauth.sh <CLIENT_ID> <CLIENT_SECRET>
#
# After running, Google Sign-In will work in the DLavie Launcher app.

set -euo pipefail

if [ $# -ne 2 ]; then
    echo "Usage: $0 <GOOGLE_CLIENT_ID> <GOOGLE_CLIENT_SECRET>"
    echo ""
    echo "Example:"
    echo "  $0 123456789-abcdef.apps.googleusercontent.com GOCSPX-xxxxxxxxxxxxxxxx"
    exit 1
fi

CLIENT_ID="$1"
CLIENT_SECRET="$2"
# v6.8.4: Use env var for Supabase PAT — never hardcode secrets in scripts
SUPABASE_PAT="${SUPABASE_PAT:?Please set SUPABASE_PAT env var first}"
PROJECT_REF="lvmucsxbmadtsgrxuwmo"

echo "Configuring Google OAuth in Supabase project $PROJECT_REF..."
echo "  Client ID: $CLIENT_ID"
echo "  Redirect URL: dlavie://auth-callback (already in allow list)"
echo ""

# Patch auth config: enable Google + set client ID/secret + ensure deep link in allow list
PAYLOAD=$(cat <<EOF
{
    "external_google_enabled": true,
    "external_google_client_id": "$CLIENT_ID",
    "external_google_secret": "$CLIENT_SECRET",
    "uri_allow_list": "dlavie://auth-callback"
}
EOF
)

RESPONSE=$(curl -s -X PATCH \
    "https://api.supabase.com/v1/projects/$PROJECT_REF/config/auth" \
    -H "Authorization: Bearer $SUPABASE_PAT" \
    -H "Content-Type: application/json" \
    -d "$PAYLOAD")

# Verify
ENABLED=$(echo "$RESPONSE" | python3 -c "import json,sys; print(json.load(sys.stdin).get('external_google_enabled', False))" 2>/dev/null || echo "false")
STORED_ID=$(echo "$RESPONSE" | python3 -c "import json,sys; print(json.load(sys.stdin).get('external_google_client_id', '') or '')" 2>/dev/null || echo "")
URI_LIST=$(echo "$RESPONSE" | python3 -c "import json,sys; print(json.load(sys.stdin).get('uri_allow_list', ''))" 2>/dev/null || echo "")

echo "=== Configuration Result ==="
echo "  Google enabled: $ENABLED"
echo "  Client ID stored: ${STORED_ID:0:20}..."
echo "  URI allow list: $URI_LIST"
echo ""

if [ "$ENABLED" = "True" ] && [ -n "$STORED_ID" ]; then
    echo "✅ Google OAuth configured successfully!"
    echo ""
    echo "Test: open DLavie Launcher → login screen → 'Lanjutkan dengan Google'"
    echo "User akan sign in via Google → auto-redirect ke app via dlavie://auth-callback"
else
    echo "❌ Configuration failed. Check response above."
    exit 1
fi
