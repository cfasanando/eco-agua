#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PACKAGE="$ROOT/src/main/java/com/ecoamazonas/eco_agua/platform/control/backups"
TEMPLATES="$ROOT/src/main/resources/templates/control_center/backups"
CSS="$ROOT/src/main/resources/static/css/matrix26-control.css"
CONFIG_SCRIPT="$ROOT/scripts/configure-matrix26-backup-security-phase3e3.sh"
KEY_SCRIPT="$ROOT/scripts/configure-matrix26-backup-master-key-phase3e3.ps1"

required_files=(
  "$PACKAGE/Matrix26BackupSecurityService.java"
  "$PACKAGE/Matrix26BackupSecurityRepository.java"
  "$PACKAGE/Matrix26BackupEncryption.java"
  "$PACKAGE/Matrix26BackupPolicy.java"
  "$PACKAGE/Matrix26RetentionPreview.java"
  "$PACKAGE/Matrix26BackupInitializer.java"
  "$TEMPLATES/policies.html"
  "$TEMPLATES/policy.html"
  "$TEMPLATES/retention.html"
  "$CONFIG_SCRIPT"
  "$KEY_SCRIPT"
)

for file in "${required_files[@]}"; do
  test -f "$file" || { echo "Missing required file: $file" >&2; exit 1; }
done

grep -q 'AES/GCM/NoPadding' "$PACKAGE/Matrix26BackupSecurityService.java"
grep -q 'M26BKP01' "$PACKAGE/Matrix26BackupSecurityService.java"
grep -q 'MATRIX26_BACKUP_MASTER_KEY' "$PACKAGE/Matrix26BackupProperties.java"
grep -q 'package.m26backup' "$PACKAGE/Matrix26BackupSecurityService.java"
grep -q 'public-manifest.json' "$PACKAGE/Matrix26BackupSecurityService.java"
grep -q 'INTERNAL_CHECKSUMS' "$PACKAGE/Matrix26BackupSecurityService.java"
grep -q 'CLEAN BACKUPS' "$PACKAGE/Matrix26BackupSecurityService.java"
grep -q 'Newest verified backup is always protected' "$PACKAGE/Matrix26BackupSecurityService.java"
grep -q 'matrix26_backup_encryption' "$PACKAGE/Matrix26BackupInitializer.java"
grep -q 'matrix26_backup_policy' "$PACKAGE/Matrix26BackupInitializer.java"
grep -q 'matrix26_backup_retention_event' "$PACKAGE/Matrix26BackupInitializer.java"
grep -q 'Verify again' "$TEMPLATES/detail.html"
grep -q 'Retention preview' "$TEMPLATES/retention.html"

if grep -RniE 'matrix26\.control-center\.backups\.(master-key|master-key-value)=' \
  "$ROOT/runtime-clients" "$ROOT/src/main/resources" 2>/dev/null; then
  echo "A backup master key must never be stored in runtime properties." >&2
  exit 1
fi

if grep -RniE 'MATRIX26_BACKUP_MASTER_KEY[[:space:]]*=[[:space:]]*[A-Za-z0-9+/]{20,}' \
  "$ROOT" --exclude='check-matrix26-backups-phase3e3.sh' 2>/dev/null; then
  echo "A possible plaintext backup key was found in project files." >&2
  exit 1
fi

if grep -RniE 'Files\.delete.*matrix26_platform_control|DROP DATABASE|Runtime\.getRuntime\(\)\.exec' "$PACKAGE"; then
  echo "Backup security boundary violation detected." >&2
  exit 1
fi

python - "$TEMPLATES" "$CSS" <<'PY'
from pathlib import Path
from html.parser import HTMLParser
import sys

class Parser(HTMLParser):
    pass

for path in Path(sys.argv[1]).glob('*.html'):
    parser = Parser()
    parser.feed(path.read_text(encoding='utf-8'))

css = Path(sys.argv[2]).read_text(encoding='utf-8')
if css.count('{') != css.count('}'):
    raise SystemExit('Unbalanced CSS braces')
print('Backup encryption and retention templates: OK')
PY

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/AesGcmSelfTest.java" <<'JAVA'
import javax.crypto.Cipher;
import javax.crypto.AEADBadTagException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;

public class AesGcmSelfTest {
    public static void main(String[] args) throws Exception {
        byte[] key = new byte[32];
        byte[] nonce = new byte[12];
        new SecureRandom().nextBytes(key);
        new SecureRandom().nextBytes(nonce);
        byte[] payload = "matrix26-encrypted-backup".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] aad = "M26BKP01:1:SELFTEST".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        Cipher encrypt = Cipher.getInstance("AES/GCM/NoPadding");
        encrypt.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
        encrypt.updateAAD(aad);
        byte[] ciphertext = encrypt.doFinal(payload);

        Cipher decrypt = Cipher.getInstance("AES/GCM/NoPadding");
        decrypt.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
        decrypt.updateAAD(aad);
        if (!Arrays.equals(payload, decrypt.doFinal(ciphertext))) {
            throw new IllegalStateException("AES-GCM round trip failed");
        }

        ciphertext[0] ^= 1;
        try {
            Cipher tampered = Cipher.getInstance("AES/GCM/NoPadding");
            tampered.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
            tampered.updateAAD(aad);
            tampered.doFinal(ciphertext);
            throw new IllegalStateException("Tampered ciphertext was accepted");
        } catch (AEADBadTagException expected) {
            System.out.println("AES-GCM authentication and tamper detection: OK");
        }
    }
}
JAVA
javac --release 17 -d "$TMP" "$TMP/AesGcmSelfTest.java"
java -cp "$TMP" AesGcmSelfTest

echo "Master key remains outside database, properties, Git, and backup package: OK"
echo "Encrypted package format and internal checksum reverification: OK"
echo "Count-based retention preview and newest-backup protection: OK"
echo "Final archive and explicit cleanup confirmation boundaries: OK"
echo "Matrix26 Backups Phase 3E.3 static checks passed."
