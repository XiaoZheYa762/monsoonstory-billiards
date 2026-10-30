"""Check public resources and optionally installable JARs, using only Python."""
from pathlib import Path
import json, re, sys, tomllib, zipfile
from release_config import TARGETS, release_jar, version

ROOT = Path(__file__).resolve().parents[1]
MOD = 'monsoonstory_billiards'
RES = ROOT / 'common/src/main/resources'
ASSETS = RES / 'assets' / MOD
checks = 0
LEGAL_FILES = ('LICENSE', 'ASSET-LICENSE.md', 'ASSETS_LICENSE.md', 'NOTICE', 'THIRD_PARTY_NOTICES.md')
CODE_LICENSE = 'Jeefeng Source License v1.0; original assets separately licensed'
PROJECT_URL = 'https://github.com/XiaoZheYa762/monsoonstory-billiards'

def check(condition, message):
    global checks
    if not condition:
        raise AssertionError(message)
    checks += 1

for file in RES.rglob('*.json'):
    json.loads(file.read_text(encoding='utf-8'))
    check(True, f'JSON: {file.name}')
    check('xiaozhebilliards' not in file.read_text(encoding='utf-8'), f'Old namespace in {file}')

languages = {file.stem: json.loads(file.read_text(encoding='utf-8')) for file in (ASSETS/'lang').glob('*.json')}
check(set(languages['zh_cn']) == set(languages['en_us']), 'English and Chinese language keys differ')

recipes = list((RES/'data'/MOD/'recipes').glob('*.json'))
check(len(recipes) == 11, 'Expected eleven survival recipes')
for recipe in recipes:
    data = json.loads(recipe.read_text(encoding='utf-8'))
    check(data['result']['item'] == MOD+':'+recipe.stem, f'Recipe output: {recipe.name}')
    check((ASSETS/'models/item'/(recipe.stem+'.json')).exists(), f'Missing recipe item model {recipe.name}')
    check((RES/'data'/MOD/'advancements/recipes'/(recipe.stem+'.json')).exists(), f'Missing recipe book unlock {recipe.name}')
    used = set(''.join(data['pattern'])) - {' '}
    check(used == set(data['key']), f'Unused/missing ingredient key {recipe.name}')

for model in (ASSETS/'models').rglob('*.json'):
    data=json.loads(model.read_text(encoding='utf-8'))
    parent=data.get('parent', '')
    if parent.startswith(MOD+':'):
        check((ASSETS/'models'/(parent.split(':',1)[1]+'.json')).exists(), f'Missing parent {parent}')
    for texture in data.get('textures',{}).values():
        if isinstance(texture,str) and texture.startswith(MOD+':'):
            check((ASSETS/'textures'/(texture.split(':',1)[1]+'.png')).exists(), f'Missing texture {texture}')

sound_data=json.loads((ASSETS/'sounds.json').read_text(encoding='utf-8'))
for sound in sound_data.values():
    for entry in sound['sounds']:
        name=entry if isinstance(entry,str) else entry['name']
        if name.startswith(MOD+':'):
            check((ASSETS/'sounds'/(name.split(':',1)[1]+'.ogg')).exists(), f'Missing sound {name}')

check((ROOT/'AI_USAGE.md').read_text(encoding='utf-8').strip() == '大量代码来自 AI。', 'AI disclosure text changed')
for file in RES.rglob('*'):
    if file.is_file():
        check(not any(value in file.as_posix().lower() for value in ('caromhall','table_logo','_weapon','/textures/item/cues/','/models/cue/')), f'Excluded asset {file.name}')

for arg in sys.argv[1:]:
    jar=Path(arg)
    target=next((t for t in TARGETS if jar.name==release_jar(t).name),None)
    check(target is not None, f'Unknown installable artifact: {jar.name}')
    module,loader,game,expected=target
    with zipfile.ZipFile(jar) as archive:
        names=archive.namelist()
        for legal_file in LEGAL_FILES:
            check(names.count(legal_file)==1, f'Missing/duplicate legal file in {jar.name}: {legal_file}')
            check(archive.read(legal_file)==(ROOT/legal_file).read_bytes(), f'Stale legal file in {jar.name}: {legal_file}')
        check('assets/'+MOD+'/lang/en_us.json' in names, f'Missing resources in {jar.name}')
        check('dev/monsoonstory/billiards/BilliardsMod.class' in names, f'Missing entrypoint in {jar.name}')
        check(not any(v in n.lower() for n in names for v in ('caromhall','table_logo','_weapon','billiards_verification','publicsafetyverification')), f'Excluded content in {jar.name}')
        check(not any(n.startswith(('run/','build/','.gradle/')) for n in names), f'Runtime files in {jar.name}')
        verification_names=('NativeVerification','ClientSmoke','ServerSmoke','LogicVerification','PredictionLogic','PowerLogic','MusicDecoderVerification','MusicIntegrationVerification','MultiplayerVerification','ClientVerification')
        check(not any('/verification/' in n or any(n.rsplit('/',1)[-1].startswith(v) for v in verification_names) for n in names),f'Test classes in {jar.name}')
        classes=[n for n in names if n.endswith('.class')]
        for name in classes:
            bytecode=archive.read(name)
            major=int.from_bytes(bytecode[6:8],'big')
            check(major <= expected, f'Wrong Java bytecode: {name}, {major}')
        if loader=='fabric':
            meta=json.loads(archive.read('fabric.mod.json'))
            check(meta['license']==CODE_LICENSE, 'Fabric source/asset license metadata')
            check(meta['contact']['sources']==PROJECT_URL and meta['contact']['issues']==PROJECT_URL+'/issues', 'Fabric official project links')
            check(meta['id']==MOD and meta['version']==version(module), 'Fabric identity/version')
            check(meta['depends']['minecraft']==game, 'Fabric game version')
        else:
            metadata='META-INF/mods.toml' if loader=='forge' else 'META-INF/neoforge.mods.toml'
            check(metadata in names, f'{loader} metadata missing')
            meta=tomllib.loads(archive.read(metadata).decode('utf-8'))
            check(meta['license']==CODE_LICENSE, f'{loader} source/asset license metadata')
            check(meta['issueTrackerURL']==PROJECT_URL+'/issues' and any(m.get('displayURL')==PROJECT_URL for m in meta['mods']), f'{loader} official project links')
            check(any(m['modId']==MOD and m['version']==version(module) for m in meta['mods']), f'{loader} identity/version')
            check(any(d['modId']=='minecraft' and d['versionRange']==f'[{game}]' for d in meta['dependencies'][MOD]), f'{loader} game version')
        if game=='1.20.1':
            check('data/'+MOD+'/recipes/pool_table.json' in names, f'{loader} recipes missing')
            result=json.loads(archive.read('data/'+MOD+'/recipes/pool_table.json'))['result']
            check(result.get('item')==MOD+':pool_table', f'{loader} result codec')
        else:
            check('data/'+MOD+'/recipe/pool_table.json' in names, 'NeoForge recipes missing')
            check('data/'+MOD+'/recipes/pool_table.json' not in names, 'Old data path in NeoForge')
            result=json.loads(archive.read('data/'+MOD+'/recipe/pool_table.json'))['result']
            check(result.get('id')==MOD+':pool_table', 'NeoForge result codec')
        if game=='26.1':
            check('assets/'+MOD+'/items/cue.json' in names, '26.1 item definitions missing')
            item_definitions=[n for n in names if n.startswith('assets/'+MOD+'/items/') and n.endswith('.json')]
            check(len(item_definitions)==28, '26.1 public item definitions incomplete')
            check('assets/minecraft/atlases/items.json' not in names, '26.1 must not duplicate block materials in the item atlas')
            room_atlas=json.loads(archive.read('assets/minecraft/atlases/blocks.json'))
            check(any(s.get('source')=='room' for s in room_atlas['sources']), '26.1 room atlas missing')
            for name in item_definitions:
                model=json.loads(archive.read(name))['model']
                check(model['type']=='minecraft:model', f'Unsupported item definition: {name}')
                namespace,path=model['model'].split(':',1)
                model_path=f'assets/{namespace}/models/{path}.json'
                check(model_path in names, f'Missing geometry: {name}')
                for sprite in json.loads(archive.read(model_path)).get('textures',{}).values():
                    if isinstance(sprite,str) and not sprite.startswith('#'):
                        check(sprite.split(':',1)[1].startswith(('item/','block/','room/')), f'Item material lacks an atlas source: {sprite}')
            for name in names:
                if name.startswith('data/'+MOD+'/recipe/') and name.endswith('.json'):
                    value=json.loads(archive.read(name))
                    check(all(isinstance(ingredient,str) for ingredient in value['key'].values()), f'26.1 ingredient codec: {name}')
        for name in names:
            if name.endswith('.json'):
                json.loads(archive.read(name))
                check(True, f'Packaged JSON: {name}')
print(f'PUBLIC_CONTENT_PASSED checks={checks}')
