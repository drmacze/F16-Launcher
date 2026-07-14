#!/usr/bin/env python3
"""
Auto-upload APK ke release v26 public repo + update manifest.json
Dipanggil dari GitHub Actions workflow F16-Launcher setelah build sukses.

Environment variables (auto-set by GitHub Actions):
  GITHUB_TOKEN: PAT dengan akses ke drmacze/DLavie-Launcher-Data
  APK_PATH: path ke APK yang sudah di-build
  VERSION_CODE: version code dari build.gradle
  VERSION_NAME: version name dari build.gradle
"""
import json
import os
import sys
import urllib.request
import urllib.error
import urllib.parse
import base64
from pathlib import Path

TOKEN = os.environ.get('GITHUB_TOKEN') or os.environ.get('GH_TOKEN')
DATA_REPO = 'drmacze/DLavie-Launcher-Data'
RELEASE_TAG = 'v26'
BRANCH = 'main'

APK_PATH = os.environ.get('APK_PATH', '')
VERSION_CODE = os.environ.get('VERSION_CODE', '')
VERSION_NAME = os.environ.get('VERSION_NAME', '')

if not TOKEN:
    print('ERROR: GITHUB_TOKEN not set')
    sys.exit(1)
if not APK_PATH or not Path(APK_PATH).exists():
    print(f'ERROR: APK not found at {APK_PATH}')
    sys.exit(1)

print(f'=== Auto-upload Launcher v{VERSION_NAME} (code {VERSION_CODE}) ===')
print(f'  APK: {APK_PATH}')
print(f'  Repo: {DATA_REPO}')
print(f'  Release: {RELEASE_TAG}')
print()

def github_request(method, url, data=None, content_type='application/json'):
    headers = {
        'Authorization': f'token {TOKEN}',
        'Accept': 'application/vnd.github+json',
        'X-GitHub-Api-Version': '2022-11-28',
    }
    if content_type:
        headers['Content-Type'] = content_type
    body = None
    if data is not None:
        body = data if isinstance(data, bytes) else json.dumps(data).encode('utf-8')
    req = urllib.request.Request(url, data=body, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=300) as resp:
            text = resp.read().decode('utf-8', errors='replace')
            return resp.status, json.loads(text) if text else {}
    except urllib.error.HTTPError as e:
        err = e.read().decode('utf-8', errors='replace')
        return e.code, {'error': err}

# Step 1: Get release v26 info
print('Step 1: Fetch release v26 info...')
url = f'https://api.github.com/repos/{DATA_REPO}/releases/tags/{RELEASE_TAG}'
status, body = github_request('GET', url)
if status != 200:
    print(f'  ERROR: {body}')
    sys.exit(1)
release_id = body['id']
upload_url = body['upload_url'].split('{')[0]
existing_assets = body.get('assets', [])
print(f'  Release ID: {release_id}')

# Step 2: Determine APK asset name (always v{VERSION_CODE}.apk)
asset_name = f'DLavie26-Launcher-v{VERSION_CODE}.apk'
print(f'\nStep 2: Asset name = {asset_name}')

# Step 3: Delete old Launcher APKs di release v26 (keep only latest)
print('\nStep 3: Delete old Launcher APK assets...')
for asset in existing_assets:
    if asset.get('name', '').startswith('DLavie26-Launcher-v') and asset['name'] != asset_name:
        print(f'  Hapus: {asset["name"]}')
        status, _ = github_request('DELETE', f'https://api.github.com/repos/{DATA_REPO}/releases/assets/{asset["id"]}')
        print(f'    HTTP {status}')
    elif asset.get('name') == asset_name:
        # Hapus yang sama juga, akan di-upload ulang
        print(f'  Hapus (replace): {asset["name"]}')
        status, _ = github_request('DELETE', f'https://api.github.com/repos/{DATA_REPO}/releases/assets/{asset["id"]}')
        print(f'    HTTP {status}')

# Step 4: Upload APK baru
print(f'\nStep 4: Upload {asset_name}...')
apk_bytes = Path(APK_PATH).read_bytes()
upload_url_with_name = f'{upload_url}?name={urllib.parse.quote(asset_name)}'
status, body = github_request('POST', upload_url_with_name, data=apk_bytes, content_type='application/vnd.android.package-archive')
if status not in (200, 201):
    print(f'  ERROR HTTP {status}: {body}')
    sys.exit(1)
apk_download_url = body.get('browser_download_url')
print(f'  ✓ Uploaded: {apk_download_url}')
print(f'    Size: {body.get("size"):,} bytes')

# Step 5: Update manifest.json
print(f'\nStep 5: Update manifest.json (launcher section)...')
manifest_url = f'https://api.github.com/repos/{DATA_REPO}/contents/manifest.json?ref={BRANCH}'
status, body = github_request('GET', manifest_url)
if status != 200:
    print(f'  ERROR fetching manifest: {body}')
    sys.exit(1)
existing_sha = body['sha']
manifest_content = base64.b64decode(body['content']).decode('utf-8')
manifest = json.loads(manifest_content)

# Update launcher section
manifest['launcher'] = {
    'min_version_code': 206,
    'latest_version_code': int(VERSION_CODE),
    'latest_version_name': VERSION_NAME,
    'apk_url': apk_download_url,
    'release_notes': [
        f'v{VERSION_NAME}: Latest build from F16-Launcher CI',
        'v7.9.40: Manifest-based app update fallback',
        'v7.9.39: DLavie Integrity Analyzer — anti-piracy + cleanup system'
    ]
}

# Update timestamp
from datetime import datetime, timezone
manifest['updated_at'] = datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')

# Upload manifest
content_b64 = base64.b64encode(json.dumps(manifest, indent=2).encode('utf-8')).decode('ascii')
payload = {
    'message': f'chore(launcher): auto-bump to v{VERSION_NAME} (code {VERSION_CODE})\n\nAuto-uploaded by F16-Launcher GitHub Actions workflow.\nAPK URL: {apk_download_url}',
    'content': content_b64,
    'branch': BRANCH,
    'sha': existing_sha,
}
status, body = github_request('PUT', manifest_url, data=payload)
if status not in (200, 201):
    print(f'  ERROR updating manifest: {body}')
    sys.exit(1)
print(f'  ✓ Manifest updated! Commit: {body["commit"]["sha"][:8]}')

# Summary
print(f'\n=== SELESAI ===')
print(f'  Launcher v{VERSION_NAME} (code {VERSION_CODE}) live di public repo')
print(f'  APK URL: {apk_download_url}')
print(f'  Manifest: https://raw.githubusercontent.com/{DATA_REPO}/{BRANCH}/manifest.json')
print(f'  User Launcher lama akan dapat popup update otomatis')
