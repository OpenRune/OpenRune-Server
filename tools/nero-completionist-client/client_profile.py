"""Opt-in particles client; stages a dedicated profile without editing RSProx or existing clients."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import socket
import subprocess
import sys
import threading
from http.server import BaseHTTPRequestHandler, HTTPServer

ROOT = Path(__file__).resolve().parent

def sha(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()

def require_hash(path, expected):
    if not path.is_file() or sha(path) != expected:
        raise RuntimeError("Qualified artifact missing or changed: " + path.name)

def checked_copy(source, destination):
    destination.parent.mkdir(parents=True, exist_ok=True)
    if destination.exists():
        require_hash(destination, sha(source))
    else:
        shutil.copyfile(source, destination)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--libraries", type=Path, default=Path.home()/".runelite/repository2")
    parser.add_argument("--java", type=Path, required=True, help="Qualified Java 11 java.exe")
    parser.add_argument("--server", type=Path)
    parser.add_argument("--profile", type=Path)
    parser.add_argument("--config-port", type=int, default=45602)
    parser.add_argument("--appearance-port", type=int, default=46602)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--stage", action="store_true")
    args = parser.parse_args()
    meta = json.loads((ROOT/"qualified-client.json").read_text())
    for name, expected in meta["bundleHashes"].items():
        file = (ROOT/name).resolve()
        if not file.is_relative_to(ROOT): raise RuntimeError("Artifact outside bundle")
        require_hash(file, expected)
    for name, expected in meta["clientLibraries"].items():
        require_hash(args.libraries/name, expected)
    version = subprocess.run([str(args.java),"-version"], capture_output=True, text=True, check=True)
    if not re.search(r'version "11[.]', version.stderr+version.stdout):
        raise RuntimeError("This opt-in client is qualified for Java 11")
    if args.check:
        print("PASS_CLIENT_PROFILE_CHECK: 34 pinned libraries and all supplied artifacts; no client files changed")
        return
    if args.server is None or args.profile is None:
        parser.error("--server and --profile are required for staging/launch")
    server = args.server.resolve(strict=True)
    profile = args.profile.resolve()
    if profile == Path.home() or not profile.name.startswith("completionist-client"):
        raise RuntimeError("Use a dedicated directory named completionist-client...")
    config = (server/"game.yml").read_text()
    match = re.search(r"^game-port:\s*(\d+)", config, re.M)
    if not match: raise RuntimeError("No game-port in server configuration")
    port = int(match[1])
    if not all(1024 <= value <= 65535 for value in (port,args.config_port,args.appearance_port)):
        raise RuntimeError("Invalid local port")
    key = server/".data/client.key"
    if not key.is_file(): raise RuntimeError("Existing public client.key required; no RSA keys are generated")
    profile.mkdir(parents=True, exist_ok=True)
    home = profile/"home"
    helper = profile/"helper-classes"
    helper.mkdir(exist_ok=True)
    compiler = args.java.with_name("javac.exe")
    if not compiler.is_file(): raise RuntimeError("The dedicated copy helper requires the qualified Java 11 JDK")
    subprocess.run([str(compiler),"--release","11","-cp",str(ROOT/"runtime/nero-compatibility-runtime.jar"),"-d",str(helper),str(ROOT/"NativeClientCopy.java")],check=True)
    repo = home/".runelite/repository2"
    repo.mkdir(parents=True, exist_ok=True)
    for name in meta["clientLibraries"]: checked_copy(args.libraries/name,repo/name)
    injected = next(name for name in meta["classpath"] if name.startswith("injected-client-"))
    gamepack = repo/"nero-completionist-gamepack.jar"
    fingerprint = sha(key)+":"+str(port)+":"+meta["clientLibraries"][injected]
    marker = profile/"gamepack-copy.json"
    if gamepack.exists():
        info = json.loads(marker.read_text())
        if info["input"] != fingerprint: raise RuntimeError("Public key/port changed; stage a fresh profile")
        require_hash(gamepack,info["sha256"])
    else:
        subprocess.run([str(args.java),"-cp",str(helper)+";"+str(ROOT/"runtime/nero-compatibility-runtime.jar"),"NativeClientCopy",str(repo/injected),str(gamepack),str(key),str(port)],check=True)
        marker.write_text(json.dumps({"input":fingerprint,"sha256":sha(gamepack)}))
    plugin_dir = home/".rlcustom/sideloaded-plugins"
    checked_copy(ROOT/"client-plugin/Nero-Particle-Preview.jar",plugin_dir/"Nero-Particle-Preview.jar")
    settings = home/".rlcustom/settings.properties"
    if not settings.exists(): settings.write_text("runelite.gpuplugin=false\nrunelite.gameSize=765x503\nrunelite.enablePluginHub=true\n")
    javconfig = re.sub(r"^codebase=.*$",f"codebase=http://127.0.0.1:{args.config_port}/",(ROOT/"client/javconfig-template.ws").read_text(),flags=re.M).encode()
    if args.stage:
        print("PASS_CLIENT_PROFILE_STAGE: dedicated copy prepared; existing clients and RSProx unchanged")
        return
    with socket.socket() as probe:
        if probe.connect_ex(("127.0.0.1",port)) != 0: raise RuntimeError("Start the corresponding cape server first")
    import urllib.request
    with urllib.request.urlopen(f"http://127.0.0.1:{args.appearance_port}/appearance", timeout=2) as response:
        if json.load(response).get("schema") != 1: raise RuntimeError("The server cape appearance feed is not ready")
    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            if self.path != "/javconfig.ws": self.send_error(404); return
            self.send_response(200); self.send_header("Content-Type","text/plain"); self.end_headers(); self.wfile.write(javconfig)
        def log_message(self,*args): pass
    http = HTTPServer(("127.0.0.1",args.config_port),Handler)
    thread = threading.Thread(target=http.serve_forever,daemon=True)
    thread.start()
    temp = profile/"tmp";temp.mkdir(exist_ok=True)
    libs = [repo/("nero-completionist-gamepack.jar" if name==injected else name) for name in meta["classpath"]]
    command = [str(args.java),f"-Duser.home={home}",f"-Djava.io.tmpdir={temp}","-Xmx768m","-Xss2m",
        f"-javaagent:{ROOT/'runtime/nero-compatibility-runtime.jar'}",f"-Dnero.compat.profile={ROOT/'runtime/runtime-240.properties'}",
        f"-Dnero.compat.bindings={ROOT/'runtime/bindings.properties'}",f"-Dnero.compat.definitions={ROOT/'runtime/definitions.json'}",
        f"-Dnero.compat.log={profile/'runtime.jsonl'}","-Dnero.compat.enabled=true",f"-Dnero.compat.appearance.url=http://127.0.0.1:{args.appearance_port}/appearance",
        "-cp",";".join(map(str,libs)),"net.runelite.client.RuneLite","--developer-mode","--disable-telemetry",f"--jav_config=http://127.0.0.1:{args.config_port}/javconfig.ws"]
    environment = dict(os.environ)
    for name in list(environment):
        if any(token in name.upper() for token in ("JAGEX","RUNELITE","OPENRUNE")):environment.pop(name)
    environment.update(LOCALAPPDATA=str(home/"AppData/Local"),APPDATA=str(home/"AppData/Roaming"),TEMP=str(temp),TMP=str(temp))
    try:
        with (profile/"client.log").open("ab") as log:
            client = subprocess.Popen(command,cwd=profile,env=environment,stdout=log,stderr=log,creationflags=subprocess.CREATE_NO_WINDOW)
            (profile/"owned-client.json").write_text(json.dumps({"pid":client.pid,"profile":str(profile)},indent=2))
            client.wait()
    finally:
        http.shutdown();http.server_close();thread.join(timeout=3)

if __name__ == "__main__":main()
