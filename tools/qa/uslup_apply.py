import sys; sys.stdout.reconfigure(encoding="utf-8")
"""Üslup dilimi uygulayıcı / ön denetleyici.

Kullanım:
  python uslup_apply.py check <yaml> <dilim.json>   # yalnız denetle, dosyaya yazma
  python uslup_apply.py apply <yaml> <dilim.json>   # denetle ve hata yoksa yaz

dilim.json: [{"line": 123, "tr": "yeni metin"}, ...]  (line: dosyadaki 1 tabanlı satır numarası;
"tr": çözülmüş metin; içteki çift tırnak JSON'da \" olarak yazılır). Yalnız değişen satırlar yeterli.
"""
import json, re, sys

RX = re.compile(r'(\btr:\s*")((?:[^"\\]|\\.)*)(")')
RXEN = re.compile(r'(\ben:\s*")((?:[^"\\]|\\.)*)(")')
TOK = re.compile(r'\{[^}]*\}')
SENT = re.compile(r'(?<=[.!?…])["\'”»)]*\s+')
YOR = re.compile(r'yor(um|sun|uz|sunuz|lar)?$')


def dec(s): return s.replace('\\"', '"').replace('\\\\', '\\')
def enc(s): return s.replace('\\', '\\\\').replace('"', '\\"')
def toks(s): return sorted(t[1:-1].split('|')[0] for t in TOK.findall(s))
def bozuk(s): return sorted(w for w in s.split() if any('̀' <= c <= 'ͯ' or '▀' <= c <= '▟' for c in w))


def first(s):
    for c in s:
        if c == '{': return None
        if c.isalpha(): return c
    return None


def last_word(sent):
    t = TOK.sub(' ', sent).strip()
    t = re.sub(r'[^\w]+$', '', t)
    return t.split(' ')[-1].lower() if t else ''


def style(s):
    out = []
    if s.lstrip().startswith('['): return out
    if s.count(';') > 1: out.append('noktalı virgül >1')
    run = best = 0
    srun = sbest = 0
    for sent in SENT.split(s):
        w = last_word(sent)
        if w:
            run = run + 1 if YOR.search(w) else 0
            best = max(best, run)
        n = sum(1 for x in TOK.sub('x', sent).split() if any(c.isalnum() for c in x))
        if 1 <= n <= 3: srun += 1; sbest = max(sbest, srun)
        elif n > 3: srun = 0
    if best >= 3: out.append('art arda 3 -yor')
    if sbest >= 2: out.append('art arda kısa cümle')
    low = s.lower()
    for p, lab in [(r'(^|[.!?…]["\'”»)]*\s+)Bu, ', '"Bu, "'), (r'etrafında\s+kur', 'etrafında kur'), (r'komik\s+derecede', 'komik derecede'),
                   (r',\s*aynı anda', ', aynı anda'), (r',\s+ve\s', ', ve'), (r'\bbir şekilde\b', 'bir şekilde'),
                   (r'\bmuhtemelen\b', 'muhtemelen'), (r'fark ediyorsun', 'fark ediyorsun')]:
        if re.search(p, s if lab == '"Bu, "' else low): out.append(lab)
    return out


def main():
    mode, path, slice_path = sys.argv[1:4]
    raw = open(path, encoding='utf-8', newline='').read()
    crlf = '\r\n' in raw
    lines = raw.replace('\r\n', '\n').split('\n')
    edits = json.load(open(slice_path, encoding='utf-8'))
    errs = warns = 0
    seen = set()
    for e in edits:
        n, new = e['line'], e['tr']
        where = f'{path}:{n}'
        if n in seen:
            print(f'HATA {where}: satır iki kez verilmiş'); errs += 1; continue
        seen.add(n)
        if not (1 <= n <= len(lines)):
            print(f'HATA {where}: satır yok'); errs += 1; continue
        m = RX.findall(lines[n - 1])
        if len(m) != 1:
            print(f'HATA {where}: satırda tam bir tr değeri yok ({len(m)})'); errs += 1; continue
        old = dec(m[0][1])
        if '\n' in new or not new.strip():
            print(f'HATA {where}: boş ya da çok satırlı'); errs += 1; continue
        if toks(old) != toks(new):
            print(f'HATA {where}: belirteçler {toks(old)} → {toks(new)}'); errs += 1
        if bozuk(old) != bozuk(new):
            print(f'HATA {where}: bozuk yazı {bozuk(old)} → {bozuk(new)}'); errs += 1
        fo, fn = first(old), first(new)
        if fo and fn and fo.islower() and fn.isupper():
            print(f'HATA {where}: küçük harfli (Yankı) satır büyük harfle başlıyor'); errs += 1
        if fo and fn and fo.isupper() and fn.islower():
            print(f'uyarı {where}: büyük harfli satır küçük harfe dönmüş'); warns += 1
        if len(old) >= 20 and not (0.6 <= len(new) / len(old) <= 1.6):
            print(f'uyarı {where}: uzunluk oranı {len(new) / len(old):.2f}'); warns += 1
        for w in style(new):
            print(f'uyarı {where}: üslup: {w} » {new[:120]}'); warns += 1
        if 'en' in e:
            if 'kronik' not in lines[n - 1]:
                print(f'HATA {where}: en yalnız kronik satırlarında değişebilir'); errs += 1
            else:
                me = RXEN.findall(lines[n - 1])
                if len(me) != 1 or toks(dec(me[0][1])) != toks(e['en']) or not e['en'].strip():
                    print(f'HATA {where}: kronik en belirteçleri/biçimi'); errs += 1
                elif mode == 'apply':
                    lines[n - 1] = RXEN.sub(lambda mm: mm.group(1) + enc(e['en']) + mm.group(3), lines[n - 1], count=1)
        if mode == 'apply' and old != new:
            lines[n - 1] = RX.sub(lambda mm: mm.group(1) + enc(new) + mm.group(3), lines[n - 1], count=1)
    print(f'{slice_path}: {len(edits)} düzenleme · {errs} hata · {warns} uyarı')
    if mode == 'apply':
        if errs:
            print('Hata var; dosyaya yazılmadı.'); sys.exit(1)
        out = '\n'.join(lines)
        if crlf: out = out.replace('\n', '\r\n')
        open(path, 'w', encoding='utf-8', newline='').write(out)
        print(f'{path} yazıldı.')
    sys.exit(1 if errs else 0)


if __name__ == '__main__':
    main()
