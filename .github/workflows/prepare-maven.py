import hashlib
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import urllib.request
import zipfile

guava = '33.7.2-jre'
sha256 = 'b530942257fb935f8b2cfaa5f8eb5bd59c555fd8e8d01b8ce98912e077ea606c'
launcher = shutil.which('mvn')
if launcher is None:
    raise SystemExit('Maven is unavailable')
output = subprocess.check_output([launcher, '--version'], text=True)
output = re.sub(r'\x1b\[[0-9;]*m', '', output)
source = Path(re.search(r'^Maven home: (.+)$', output, re.MULTILINE)[1].strip()).resolve()
installed, = (source/'lib').glob('guava*.jar')
with zipfile.ZipFile(installed) as jar:
    metadata = jar.read('META-INF/maven/com.google.guava/guava/pom.properties').decode()
version = re.search(r'^version=(\d+(?:\.\d+){1,2})-jre$', metadata, re.MULTILINE)
if version is None:
    raise SystemExit(f'Unexpected Maven Guava package in {installed}')
if tuple(map(int, version[1].split('.'))) >= (33, 7, 2):
    print(f'Maven already contains {installed.name}')
else:
    if not (source/'lib'/'failureaccess-1.0.3.jar').is_file():
        raise SystemExit('This Maven distribution lacks the required failureaccess 1.0.3')
    dest = Path(tempfile.mkdtemp(prefix='maven-',dir=os.environ['RUNNER_TEMP']))/'distribution'
    url = f'https://repo.maven.apache.org/maven2/com/google/guava/guava/{guava}/guava-{guava}.jar'
    with urllib.request.urlopen(url, timeout=30) as response:
        data = response.read()
    if hashlib.sha256(data).hexdigest() != sha256:
        raise SystemExit('Guava SHA-256 does not match its verified release')
    shutil.copytree(source, dest)
    (dest/'lib'/installed.name).unlink()
    (dest/'lib'/f'guava-{guava}.jar').write_bytes(data)
    with open(os.environ['GITHUB_PATH'], 'a', encoding='utf-8') as path:
        print(dest/'bin', file=path)
    print(f'Maven uses its original launchers and Guava {guava} in {dest}')
