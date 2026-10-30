"""Check the NeoForge mixin target names/descriptors against a mapped Minecraft JAR."""
from pathlib import Path
import argparse
import re
import struct
import zipfile

def members(data):
    offset=8
    def u2():
        nonlocal offset
        value=struct.unpack_from('>H',data,offset)[0];offset+=2;return value
    def skip(n):
        nonlocal offset
        offset+=n
    cp=[None]*u2();index=1
    while index<len(cp):
        tag=data[offset];offset+=1
        if tag==1:
            length=u2();cp[index]=data[offset:offset+length].decode('utf-8','replace');skip(length)
        elif tag in (3,4,9,10,11,12,17,18):skip(4)
        elif tag in (5,6):skip(8);index+=1
        elif tag in (7,8,16,19,20):skip(2)
        elif tag==15:skip(3)
        else:raise ValueError('Unknown constant-pool tag '+str(tag))
        index+=1
    skip(6);skip(2*u2())
    def read_members():
        result={}
        for _ in range(u2()):
            skip(2);name=cp[u2()];descriptor=cp[u2()]
            result.setdefault(name,set()).add(descriptor)
            for _ in range(u2()):
                skip(2);length=struct.unpack_from('>I',data,offset)[0];skip(4+length)
        return result
    fields=read_members();methods=read_members()
    return fields,methods

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('mapped_minecraft_jar',type=Path);parser.add_argument('--module',choices=['neoforge','forge','neoforge-26.1'],default='neoforge');args=parser.parse_args()
    repo=Path(__file__).resolve().parents[1];source=repo/args.module/'src/main/java/dev/monsoonstory/billiards/mixin';checks=0
    with zipfile.ZipFile(args.mapped_minecraft_jar) as jar:
        for path in sorted(source.glob('*.java')):
            text=path.read_text(encoding='utf-8');target=re.search(r'@Mixin\(\s*(?:value\s*=\s*)?([\w$]+)\.class',text)
            if not target:raise AssertionError('Missing mixin target: '+path.name)
            simple=target.group(1);imports=re.findall(r'^import ([\w.$]+);',text,re.M);matches=[name for name in imports if name.rsplit('.',1)[-1]==simple]
            if len(matches)!=1:raise AssertionError('Ambiguous mixin target: '+path.name)
            target_name=matches[0].replace('.','/');fields,methods=members(jar.read(target_name+'.class'))
            names=[]
            for annotation in re.findall(r'@Inject\(\s*method\s*=\s*(\{[^}]*\}|"[^"]*")',text):names.extend(re.findall(r'"([^"]+)"',annotation))
            for name in names:
                method,descriptor=name.split('(',1) if '(' in name else (name,None)
                if method not in methods:raise AssertionError(f'{path.name}: target method {method} absent from {target_name}')
                if descriptor is not None and '('+descriptor not in methods[method]:raise AssertionError(f'{path.name}: descriptor {name} absent from {target_name}')
                checks+=1
            for invoker in re.findall(r'@Invoker\("([^"]+)"\)',text):
                if invoker not in methods:raise AssertionError(f'{path.name}: missing invoker {invoker}')
                checks+=1
            for accessor in re.findall(r'@Accessor\("([^"]+)"\)',text):
                if accessor not in fields:raise AssertionError(f'{path.name}: missing field {accessor}')
                checks+=1
    print(f'MONSOON_MIXIN_TARGETS_PASSED module={args.module} checks={checks}')

if __name__=='__main__':main()
