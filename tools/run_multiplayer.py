"""Isolated production-JAR acceptance: dedicated server + 3 real remote clients.
No user game installation or existing worlds are modified.
"""
import os,json,pathlib,subprocess,time,zipfile,hashlib,uuid,sys,shutil,socket
root=pathlib.Path(__file__).resolve().parents[1]/'fabric'
info=json.loads((root/'build/verification-classpath.json').read_text())
cache=pathlib.Path(info['gradleHome'])/'caches'
modules=cache/'modules-2/files-2.1'
def artifact(group,name,version):return next(p for p in (modules/group/name/version).glob('*/*.jar') if not p.name.endswith('-sources.jar'))
libraries=[p for p in info['libraries'] if not any(s in p.replace('\\','/') for s in ['/remapped_mods/','/minecraftMaven/','/mappings.jar','/dev-launch-injector/'])]
libraries+=[str(artifact('net.fabricmc','intermediary','1.20.1')),str(cache/'fabric-loom/1.20.1/minecraft-merged.jar')]
game=cache/'fabric-loom/1.20.1/minecraft-merged.jar'
version=next(line.split('=',1)[1].strip() for line in (root/'gradle.properties').read_text().splitlines() if line.startswith('mod_version='))
mods=[root/f'build/libs/monsoonstory_billiards-fabric-1.20.1-{version}.jar',root/f'build/libs/monsoonstory_billiards-fabric-1.20.1-{version}-verification.jar',artifact('net.fabricmc.fabric-api','fabric-api','0.92.2+1.20.1')]
with socket.socket() as port_probe:
    port_probe.bind(('127.0.0.1',0));test_port=port_probe.getsockname()[1]
stamp=time.strftime('%Y%m%d-%H%M%S')
run=root/'run'/('multiplayer-'+stamp);run.mkdir(parents=True)
java=pathlib.Path(shutil.which('java') or 'java')
processes=[];logs={};handles=[]
def start(name,server=False,restart=False):
    folder=run/name;folder.mkdir(exist_ok=True)
    (folder/'mods').mkdir(exist_ok=True)
    for mod in mods:shutil.copy2(mod,folder/'mods'/mod.name)
    if server:
        (folder/'eula.txt').write_text('eula=true\n')
        (folder/'server.properties').write_text(f'online-mode=false\nserver-ip=127.0.0.1\nserver-port={test_port}\nlevel-type=minecraft:flat\ngenerator-settings={{"biome":"minecraft:plains","layers":[{{"block":"minecraft:bedrock","height":1}},{{"block":"minecraft:stone","height":2}},{{"block":"minecraft:grass_block","height":1}}],"features":false,"lakes":false,"structure_overrides":[]}}\ngenerate-structures=false\nspawn-protection=0\nview-distance=4\nsimulation-distance=5\nallow-flight=true\nmax-players=4\nspawn-animals=false\nspawn-monsters=false\nlevel-name=verification-world\nsync-chunk-writes=true\n')
    else:
        (folder/'options.txt').write_text('lang:zh_cn\nmaxFps:120\nrenderDistance:4\nsimulationDistance:5\nguiScale:3\ngamma:0.5\ntutorialStep:none\njoinedFirstServer:true\npauseOnLostFocus:false\nsoundCategory_master:0.65\n')
        natives=folder/'natives';natives.mkdir()
        for lib in libraries:
            if 'natives-windows' in lib and 'arm64' not in lib and 'x86' not in lib:
                with zipfile.ZipFile(lib) as z:
                    for entry in z.namelist():
                        if entry.endswith('.dll'):(natives/pathlib.Path(entry).name).write_bytes(z.read(entry))
    args=['-Xmx1536M','-Dfile.encoding=UTF-8','-Dfabric.development=false','-Dfabric.gameJarPath='+str(game),'-Djava.library.path=natives','-Dbilliards.multiplayerPort='+str(test_port),'-Dbilliards.multiplayerTest='+str(server).lower(),'-Dbilliards.clientTest='+str(not server).lower(),'-cp',os.pathsep.join(libraries),'net.fabricmc.loader.impl.launch.knot.KnotServer' if server else 'net.fabricmc.loader.impl.launch.knot.KnotClient']
    if restart:args.insert(1,'-Dbilliards.restartCheck=true')
    if server:args+=['nogui']
    else:args+=['--username',name,'--uuid',str(uuid.uuid3(uuid.NAMESPACE_DNS,name)).replace('-',''),'--accessToken','0','--version','1.20.1','--gameDir','.','--assetsDir',str(cache/'fabric-loom/assets'),'--assetIndex','1.20.1-5','--width','1280','--height','900']
    argfile=folder/'launch.args';argfile.write_text('\n'.join('"'+a.replace('\\','/').replace('"','\\"')+'"' for a in args),encoding='utf-8')
    log=folder/('restart.log' if restart else 'console.log');handle=log.open('w',encoding='utf-8');handles.append(handle)
    proc=subprocess.Popen([str(java),'@'+str(argfile)],cwd=folder,stdout=handle,stderr=subprocess.STDOUT,creationflags=getattr(subprocess,"CREATE_NO_WINDOW",0))
    processes.append(proc);logs[name]=log
    print('Started',name,'PID',proc.pid,flush=True)
    return proc
report={'run':str(run),'releaseSha256':hashlib.sha256(mods[0].read_bytes()).hexdigest(),'clients':[]}
try:
    server=start('server',True)
    deadline=time.time()+180
    while time.time()<deadline:
        text=logs['server'].read_text(encoding='utf-8',errors='replace')
        if 'Done (' in text:break
        if server.poll() is not None:raise RuntimeError('Server exited before ready')
        time.sleep(1)
    else:raise RuntimeError('Server startup timeout')
    clients=[]
    for suffix in 'ABC':
        name='Billiards'+suffix
        client=start(name);clients.append(client)
        deadline=time.time()+120
        while time.time()<deadline:
            if name+' joined the game' in logs['server'].read_text(encoding='utf-8',errors='replace'):break
            if client.poll() is not None:raise RuntimeError(name+' exited before joining')
            time.sleep(1)
        else:raise RuntimeError(name+' login timeout')
    deadline=time.time()+600
    last=''
    while time.time()<deadline:
        text=logs['server'].read_text(encoding='utf-8',errors='replace')
        markers=[l for l in text.splitlines() if 'MP_CHECK' in l or 'BILLIARDS_MULTIPLAYER_' in l]
        if markers and markers[-1]!=last:last=markers[-1];print(last,flush=True)
        if server.poll() is not None:break
        if any('BILLIARDS_CLIENT_FAILED' in p.read_text(encoding='utf-8',errors='replace') for n,p in logs.items() if n!='server'):raise RuntimeError('Client verification failed; inspect logs')
        time.sleep(1)
    else:raise RuntimeError('Multiplayer acceptance timeout')
    report['serverPassed']='BILLIARDS_MULTIPLAYER_PASSED' in text and 'BILLIARDS_MULTIPLAYER_FAILED' not in text
    for name in ['BilliardsA','BilliardsB','BilliardsC']:
        text=logs[name].read_text(encoding='utf-8',errors='replace')
        report['clients'].append({'name':name,'passed':'BILLIARDS_CLIENT_PASSED' in text and 'BILLIARDS_CLIENT_FAILED' not in text,'screenshots':[str(p) for p in (run/name/'screenshots').glob('*.png')]})
    report['passed']=report['serverPassed'] and all(c['passed'] for c in report['clients'])
    if report['passed']:
        server.wait(timeout=20)
        restart=start('server',True,True)
        restart.wait(timeout=90)
        report['restartPassed']='BILLIARDS_RESTART_PASSED' in logs['server'].read_text(encoding='utf-8',errors='replace')
        report['passed']=report['passed'] and report['restartPassed']
except Exception as e:
    report['passed']=False;report['error']=str(e);print('FAIL:',e,flush=True)
finally:
    for proc in processes:
        if proc.poll() is None:proc.terminate()
    for proc in processes:
        try:proc.wait(timeout=15)
        except subprocess.TimeoutExpired:proc.kill()
    for h in handles:h.close()
    (root/'build/multiplayer-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({k:v for k,v in report.items() if k!='clients'}|{'clients':[{'name':c['name'],'passed':c['passed'],'screenshots':len(c['screenshots'])} for c in report['clients']]},ensure_ascii=False),flush=True)
sys.exit(0 if report['passed'] else 1)
