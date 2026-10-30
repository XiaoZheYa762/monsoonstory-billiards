"""Package only reviewed source roots; runtime files and credentials are excluded."""
from pathlib import Path
import hashlib, json, re, shutil, subprocess, sys, zipfile
from release_config import TARGETS, release_jar, version

ROOT=Path(__file__).resolve().parents[1]
VERSION=version()
OUTPUT=ROOT/'dist'/VERSION
DIRS={'common','fabric','forge','neoforge','neoforge-26.1','art','tools','gradle','.github','docs'}
FILES={'README.md','README.zh-CN.md','LICENSE','ASSET-LICENSE.md','ASSETS_LICENSE.md','NOTICE',
       'CONTRIBUTING.md','THIRD_PARTY_NOTICES.md','AI_USAGE.md','CHANGELOG.md',
       '.gitignore','.gitattributes','table_geometry.json'}
EXCLUDED={'.git','.gradle','.tools','build','out','run','runs','logs','crash-reports','saves','world','verification-world','cache','__pycache__','.idea','.vscode','music-library','user-music'}
PRIVATE_FILES={'ops.json','usercache.json','whitelist.json','banned-players.json','banned-ips.json','server.properties','eula.txt','level.dat','level.dat_old','local.properties'}
SECRET=re.compile(r'(?:gh[pousr]_[A-Za-z0-9]{25,}|github_pat_[A-Za-z0-9_]{30,}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----)')

def allowed(path):
    rel=path.relative_to(ROOT)
    if rel.parts[0] not in DIRS and rel.as_posix() not in FILES:return False
    if any(part in EXCLUDED for part in rel.parts):return False
    if path.name.lower() in PRIVATE_FILES:return False
    if path.name=='.env' or path.name.startswith('.env.'):return False
    if 'monsoonstory_billiards/music/' in rel.as_posix():return False
    if path.suffix.lower() in ('.pem','.key','.p12','.pfx','.jks','.keystore'):return False
    if path.suffix in ('.pyc','.log','.gz','.hprof','.mca','.mcr','.part','.dltmp'):return False
    if path.suffix=='.jar' and path.name!='gradle-wrapper.jar':return False
    return path.is_file()

def sha(path):
    with path.open('rb') as source:return hashlib.file_digest(source,'sha256').hexdigest()

def main():
    jars=[]
    for target in TARGETS:
        if version(target[0]) != VERSION:raise SystemExit(f'Version mismatch: {target[0]}')
        jar=release_jar(target)
        if not jar.exists():raise SystemExit(f'Build the installable JAR first: {jar}')
        jars.append(jar)
    subprocess.run([sys.executable,str(ROOT/'tools/verify_public.py'),*[str(p) for p in jars]],check=True)
    sources=sorted((p for p in ROOT.rglob('*') if allowed(p)),key=lambda p:p.relative_to(ROOT).as_posix())
    for p in sources:
        if p.name in FILES or p.suffix in ('.java','.gradle','.properties','.json','.md','.py','.yml','.yaml','.toml'):
            content=p.read_text(encoding='utf-8')
            if SECRET.search(content):raise AssertionError(f'Credential-like content in {p.relative_to(ROOT)}')
            if p.suffix!='.md' and re.search(r'\b[A-Z]:[\\/]',content):raise AssertionError(f'Absolute host path in {p.relative_to(ROOT)}')
    OUTPUT.mkdir(parents=True,exist_ok=True)
    for jar in jars:shutil.copy2(jar,OUTPUT/jar.name)
    source_zip=OUTPUT/f'monsoonstory_billiards-{VERSION}-github-source.zip'
    manifest={p.relative_to(ROOT).as_posix():sha(p) for p in sources}
    with zipfile.ZipFile(source_zip,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as archive:
        for p in sources:
            name=p.relative_to(ROOT).as_posix()
            info=zipfile.ZipInfo(name,(2026,10,25,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED
            info.external_attr=(0o100755 if p.name=='gradlew' else 0o100644)<<16
            archive.writestr(info,p.read_bytes())
    with zipfile.ZipFile(source_zip) as archive:
        assert set(archive.namelist())==set(manifest)
        for name,digest in manifest.items():assert hashlib.sha256(archive.read(name)).hexdigest()==digest
    (OUTPUT/'SOURCE_MANIFEST.json').write_text(json.dumps(manifest,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    outputs=[OUTPUT/jar.name for jar in jars]+[source_zip,OUTPUT/'SOURCE_MANIFEST.json']
    (OUTPUT/'SHA256SUMS.txt').write_text(''.join(f'{sha(p)}  {p.name}\n' for p in outputs),encoding='utf-8')
    print(f'PUBLIC_RELEASE_PACKAGED source_files={len(sources)}')
    for p in outputs:print(p.name)

if __name__=='__main__':main()
