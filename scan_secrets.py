import glob
import re

secret_patterns = [
    (r'(?i)api[_-]?key\s*=\s*["\']([^"\']+)["\']', 'api_key'),
    (r'(?i)secret\s*=\s*["\']([^"\']+)["\']', 'secret'),
    (r'(?i)password\s*=\s*["\']([^"\']+)["\']', 'password'),
]

audit_files = glob.glob("FinTrack_Audit_*.txt")

for fpath in audit_files:
    with open(fpath, "r", encoding="utf-8") as f:
        content = f.read()
    
    modified = content
    # Check for actual key strings (excluding build logic / placeholders like keystore pass "android")
    # In keystore creation script, password is "android" which is standard debug keystore param
    # Let's check if any actual private credentials or tokens exist
    matches = re.findall(r'AIzaSy[A-Za-z0-9_-]{33}', modified)
    if matches:
        print(f"Found Google API Key in {fpath}, redacting...")
        modified = re.sub(r'AIzaSy[A-Za-z0-9_-]{33}', '[REDACTED]', modified)
    
    if modified != content:
        with open(fpath, "w", encoding="utf-8") as f:
            f.write(modified)
        print(f"Redacted secrets in {fpath}")

print("Scan complete.")
