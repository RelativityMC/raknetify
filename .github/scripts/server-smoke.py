import argparse
import json
import queue
import socket
import struct
import subprocess
import sys
import threading
import time
import urllib.request
from pathlib import Path
import shutil


MAGIC = bytes.fromhex('00ffff00fefefefefdfdfdfd12345678')
PROTOCOLS = {'26.1': 775, '26.1.1': 775, '26.1.2': 775, '26.2': 776, '26.3': 777,
             '26.4-snapshot-7': 1073742169}


def varint(value):
    value &= 0xffffffff
    result = bytearray()
    while value > 127:
        result.append((value & 127) | 128)
        value >>= 7
    return bytes(result + bytes([value]))


def read_varint(stream):
    value = 0
    for shift in range(0, 35, 7):
        part = stream.read(1)
        if not part:
            raise EOFError('Incomplete VarInt')
        value |= (part[0] & 127) << shift
        if part[0] < 128:
            return value
    raise ValueError('VarInt exceeds five bytes')


def check_tcp(port, version):
    host = b'127.0.0.1'
    handshake = b'\x00' + varint(PROTOCOLS[version]) + varint(len(host)) + host + struct.pack('>H', port) + b'\x01'
    with socket.create_connection(('127.0.0.1', port), timeout=10) as connection:
        connection.sendall(varint(len(handshake)) + handshake + b'\x01\x00')
        stream = connection.makefile('rb')
        length = read_varint(stream)
        assert 0 < length < 1048576, length
        assert read_varint(stream) == 0
        size = read_varint(stream)
        assert 0 < size < length
        status = json.loads(stream.read(size))
        assert status['version']['name'] == version.replace('-snapshot-', ' Snapshot '), status
        assert status['version']['protocol'] == PROTOCOLS[version], status


def check_udp(port):
    # Open Connection Request 1 negotiates the RakNet transport before Minecraft login.
    request = (b'\x05' + MAGIC + b'\x09').ljust(1200, b'\x00')
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as connection:
        connection.settimeout(10)
        connection.connect(('127.0.0.1', port))
        connection.send(request)
        reply = connection.recv(8192)
        assert len(reply) == 28 and reply[0] == 6, reply.hex()
        assert reply[1:17] == MAGIC and reply[25] == 0, reply.hex()
        assert 128 <= struct.unpack('>H', reply[26:28])[0] <= 1200


def start_server(folder, log, events):
    process = subprocess.Popen(['java', '-Xms256M', '-Xmx1G', '-jar', 'fabric-server-launch.jar', 'nogui'],
                               cwd=folder, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                               stderr=subprocess.STDOUT, text=True, encoding='utf-8', errors='replace')

    def collect():
        with log.open('w', encoding='utf-8') as output:
            for line in process.stdout:
                output.write(line)
                output.flush()
                events.put(line)

    reader = threading.Thread(target=collect)
    reader.start()
    return process, reader


def check_server(folder, version, native):
    port = 25871
    (folder / 'eula.txt').write_text('eula=true\n')
    (folder / 'server.properties').write_text(
        'server-ip=127.0.0.1\nserver-port=25871\nonline-mode=false\n'
        'view-distance=2\nsimulation-distance=2\nspawn-chunk-radius=0\n'
        'level-type=minecraft:flat\nmax-players=2\n'
        'use-native-transport=' + str(native).lower() + '\nlevel-name=world-' + str(native).lower() + '\n')
    log = folder / ('native.log' if native else 'nio.log')
    events = queue.Queue()
    process, reader = start_server(folder, log, events)
    try:
        deadline = time.monotonic() + 180
        while time.monotonic() < deadline:
            if process.poll() is not None:
                raise RuntimeError('Server exited before readiness: ' + str(process.returncode))
            try:
                if 'Done (' in events.get(timeout=1):
                    break
            except queue.Empty:
                pass
        else:
            raise TimeoutError('Server startup exceeded 180 seconds')
        for attempt in range(10):
            try:
                check_tcp(port, version)
                break
            except (EOFError, OSError):
                if attempt == 9:
                    raise
                time.sleep(0.5)
        check_udp(port)
        dump = subprocess.check_output(['jcmd', str(process.pid), 'Thread.print'], text=True)
        (folder / ('native-threads.log' if native else 'nio-threads.log')).write_text(dump)
        transport = 'Epoll' if native and sys.platform.startswith('linux') else 'NIO'
        assert 'Netty Server ' + transport + ' Raknetify' in dump, transport
        process.stdin.write('stop\n')
        process.stdin.flush()
        assert process.wait(timeout=45) == 0
        reader.join(timeout=10)
        text = log.read_text()
        for failure in ('Critical injection failure', 'Mixin apply failed', 'NoClassDefFoundError', 'LEAK:'):
            assert failure not in text, failure
        print('PASS', version, transport, 'startup, TCP status, RakNet UDP response, shutdown', flush=True)
    finally:
        if process.poll() is None:
            try:
                process.stdin.write('stop\n')
                process.stdin.flush()
                process.wait(timeout=45)
            except (OSError, subprocess.TimeoutExpired):
                process.kill()
                process.wait(timeout=10)
        reader.join(timeout=10)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--jar', type=Path, required=True)
    parser.add_argument('--work-dir', type=Path, required=True)
    parser.add_argument('--versions', nargs='+', default=['26.1', '26.1.1', '26.1.2', '26.2', '26.3', '26.4-snapshot-7'])
    args = parser.parse_args()
    for version in args.versions:
        folder = args.work_dir / version
        (folder / 'mods').mkdir(parents=True, exist_ok=True)
        shutil.copy2(args.jar, folder / 'mods' / args.jar.name)
        launcher = folder / 'fabric-server-launch.jar'
        if not launcher.exists():
            url = f'https://meta.fabricmc.net/v2/versions/loader/{version}/0.20.1/1.1.1/server/jar'
            with urllib.request.urlopen(url, timeout=60) as response:
                launcher.write_bytes(response.read())
        check_server(folder, version, True)
        check_server(folder, version, False)


if __name__ == '__main__':
    main()
