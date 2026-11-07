"""PostToolUse kancası: content/ altında bir dosya düzenlenince :engine:validateContent çalışır.
Hata varsa çıkış kodu 2 ve stderr'e özet: Claude bunu görür ve düzeltir. Uyarılar (⚠) sessizdir.
Hızlı kalsın diye yalnız doğrulayıcı çalışır (tam derleme ya da test yok)."""
import json, os, subprocess, sys
sys.stdout.reconfigure(encoding="utf-8")
sys.stderr.reconfigure(encoding="utf-8")

try:
    data = json.load(sys.stdin)
except Exception:
    sys.exit(0)
path = (data.get("tool_input") or {}).get("file_path") or (data.get("tool_response") or {}).get("filePath") or ""
norm = path.replace("\\", "/")
if "/content/" not in norm and not norm.startswith("content/"):
    sys.exit(0)
if not (norm.endswith(".yaml") or norm.endswith(".yml")):
    sys.exit(0)

root = os.environ.get("CLAUDE_PROJECT_DIR") or os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
env = dict(os.environ)
jbr = r"C:\Program Files\Android\Android Studio\jbr"
if not env.get("JAVA_HOME") and os.path.isdir(jbr):
    env["JAVA_HOME"] = jbr
gradlew = os.path.join(root, "gradlew.bat" if os.name == "nt" else "gradlew")
try:
    r = subprocess.run([gradlew, "-q", ":engine:validateContent"], cwd=root, env=env, capture_output=True,
                       text=True, encoding="utf-8", errors="replace", timeout=170)
except subprocess.TimeoutExpired:
    print("validateContent zaman aşımına uğradı (170 sn).", file=sys.stderr)
    sys.exit(0)
if r.returncode == 0:
    sys.exit(0)
out = (r.stdout + "\n" + r.stderr).splitlines()
errors = [l for l in out if "✗" in l or "sorun bulundu" in l] or [l for l in out if l.strip() and not l.lstrip().startswith("⚠")][-10:]
print("İçerik doğrulaması başarısız (" + os.path.basename(path) + "):\n" + "\n".join(errors[-25:]), file=sys.stderr)
sys.exit(2)
