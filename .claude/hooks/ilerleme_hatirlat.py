"""Stop kancası: çalışma ağacında kod/içerik değişikliği varken docs/PROGRESS.md değişmediyse bir kez hatırlatır.
Döngüye girmez (stop_hook_active). Yalnız git status bakar; hızlıdır."""
import json, os, subprocess, sys
sys.stdout.reconfigure(encoding="utf-8")
sys.stderr.reconfigure(encoding="utf-8")

try:
    data = json.load(sys.stdin)
except Exception:
    data = {}
if data.get("stop_hook_active"):
    sys.exit(0)
root = os.environ.get("CLAUDE_PROJECT_DIR") or os.getcwd()
try:
    st = subprocess.run(["git", "status", "--porcelain"], cwd=root, capture_output=True, text=True, encoding="utf-8", timeout=10).stdout
except Exception:
    sys.exit(0)
files = [l[3:].strip().strip('"') for l in st.splitlines() if l.strip()]
work = [f for f in files if f.startswith(("engine/", "app/", "content/", "tools/"))]
if work and not any(f.endswith("docs/PROGRESS.md") for f in files):
    print(json.dumps({
        "decision": "block",
        "reason": f"Hatırlatma: {len(work)} kod/içerik dosyası değişti ama docs/PROGRESS.md güncellenmedi. "
                  "Bu oturumun ilerlemesi oraya yazılmalıysa güncelle; gerek yoksa kısaca belirt ve bitir.",
    }))
sys.exit(0)
