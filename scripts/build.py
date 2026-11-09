#!/usr/bin/env python3
"""Build the unified external display helper and its libxposed entry in one APK."""
import argparse
from pathlib import Path
import os
import subprocess
import tempfile
import zipfile

p=argparse.ArgumentParser(description=__doc__)
p.set_defaults(project=Path(__file__).resolve().parents[1])
p.add_argument('--tools',type=Path,required=True)
p.add_argument('--out',type=Path,required=True)
p.set_defaults(xposed=True)
p.add_argument('--version-code',default='10101')
p.add_argument('--version-name',default='1.1.1')
p.add_argument('--keystore',type=Path)
p.add_argument('--password-file',type=Path)
p.add_argument('--lineage',type=Path)
a=p.parse_args()
project=a.project.resolve(); tools=a.tools.resolve(); out=a.out.resolve()
if out.exists(): raise SystemExit('Output exists; choose a new filename')
homes=list((tools/'jdk21').glob('*/Contents/Home'))
if len(homes)!=1: raise SystemExit('Expected one JDK21 installation')
jdk=homes[0]; bt=tools/'android-sdk/build-tools/36.0.0'
android=tools/'android-sdk/platforms/android-36/android.jar'
api=tools/'libxposed-api-101.0.1/classes.jar'
key=a.keystore.resolve() if a.keystore else tools/'debug.keystore'
required=[jdk/'bin/javac',bt/'aapt2',bt/'d8',bt/'zipalign',bt/'apksigner',android,key]
if a.xposed:required.append(api)
for f in required:
 if not f.is_file():raise SystemExit('Missing build dependency: '+str(f))
env=os.environ.copy();env['JAVA_HOME']=str(jdk);env['PATH']=str(jdk/'bin')+os.pathsep+env['PATH']
def run(args):subprocess.run([str(x) for x in args],env=env,check=True)
out.parent.mkdir(parents=True,exist_ok=True)
with tempfile.TemporaryDirectory(prefix='external-display-build-') as tmp:
 b=Path(tmp);classes=b/'classes';dex=b/'dex';classes.mkdir();dex.mkdir()
 sources=sorted((project/'app/src/main/java').rglob('*.java'))
 if not sources:raise SystemExit('No Java sources')
 cp=os.pathsep.join(str(x) for x in ([android,api] if a.xposed else [android]))
 run([jdk/'bin/javac','-encoding','UTF-8','--release','17','-classpath',cp,'-d',classes,*sources])
 link=[bt/'aapt2','link','-o',b/'unsigned.apk','-I',android,'--manifest',project/'app/src/main/AndroidManifest.xml','--min-sdk-version','35','--target-sdk-version','36','--version-code',a.version_code,'--version-name',a.version_name]
 res=project/'app/src/main/res'
 if res.exists():
  run([bt/'aapt2','compile','--dir',res,'-o',b/'res.zip'])
  link += [b/'res.zip']
 run(link)
 cmd=[bt/'d8','--min-api','35','--lib',android,'--output',dex]
 if a.xposed:cmd+=['--classpath',api]
 run(cmd+sorted(classes.rglob('*.class')))
 with zipfile.ZipFile(b/'unsigned.apk','a',compression=zipfile.ZIP_DEFLATED) as z:
  for f in dex.glob('*.dex'):z.write(f,f.name)
  resources=project/'app/src/main/resources'
  if resources.exists():
   for f in sorted(resources.rglob('*')):
    if f.is_file():z.write(f,f.relative_to(resources).as_posix())
 run([bt/'zipalign','-f','4',b/'unsigned.apk',b/'aligned.apk'])
 sign=[bt/'apksigner','sign','--ks',key,'--ks-pass',('file:'+str(a.password_file.resolve()) if a.password_file else 'pass:android')]
 if a.lineage:sign += ['--lineage',a.lineage.resolve(),'--rotation-min-sdk-version','28','--v1-signing-enabled','false','--v2-signing-enabled','false']
 run(sign+['--out',b/'signed.apk',b/'aligned.apk'])
 run([bt/'apksigner','verify','--verbose',b/'signed.apk'])
 with out.open('xb') as dest:dest.write((b/'signed.apk').read_bytes())
print(out)
