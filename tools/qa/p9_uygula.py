"""tools/qa/is/p9/*.json çıktılarını içerik dosyalarına uygular (ad: content/ sonrası yol, '/' yerine '__').
Kullanım: python tools/qa/p9_uygula.py [check|apply] [dosya_adı_öneki ...]"""
import glob, os, subprocess, sys
sys.stdout.reconfigure(encoding='utf-8')
mode = sys.argv[1] if len(sys.argv) > 1 else 'check'
only = sys.argv[2:]
done = set(open('tools/qa/is/p9/.uygulandi', encoding='utf-8').read().split()) if os.path.exists('tools/qa/is/p9/.uygulandi') else set()
for j in sorted(glob.glob('tools/qa/is/p9/*.json')):
    name = os.path.basename(j)[:-5]
    if name.startswith('R__'):
        continue  # okuma denetimi çıktıları ayrı uygulanır
    if only and not any(name.startswith(o) for o in only):
        continue
    if mode == 'apply' and name in done:
        continue
    y = 'content/' + name.replace('__', '/') + '.yaml'
    if not os.path.exists(y):
        print('?? yaml yok:', y); continue
    r = subprocess.run([sys.executable, 'tools/qa/uslup_apply.py', mode, y, j], capture_output=True, text=True, encoding='utf-8')
    last = [l for l in r.stdout.splitlines() if 'düzenleme' in l or l.startswith('HATA')]
    print(name, '|', ' / '.join(last[-2:]))
    if mode == 'apply' and r.returncode == 0:
        done.add(name)
if mode == 'apply':
    open('tools/qa/is/p9/.uygulandi', 'w', encoding='utf-8').write('\n'.join(sorted(done)))
