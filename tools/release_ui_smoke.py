#!/usr/bin/env python3
"""Isolated :179 input helpers and a localhost-only fake VRAP token endpoint."""
import argparse
from http.server import BaseHTTPRequestHandler, HTTPServer
import os
from pathlib import Path
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / 'run/release-evidence'


def cmd(*args):
    return subprocess.check_output([str(a) for a in args], text=True).strip()


def click(x, y):
    assert os.environ.get('DISPLAY') == ':179'
    cmd('xdotool', 'mousemove', '--sync', x * 2, y * 2)
    time.sleep(.15)
    cmd('xdotool', 'click', 1)
    time.sleep(.4)


def key(value):
    assert os.environ.get('DISPLAY') == ':179'
    cmd('xdotool', 'key', '--clearmodifiers', value)
    time.sleep(.4)


def capture(name):
    assert os.environ.get('DISPLAY') == ':179'
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    path = EVIDENCE / (name + '.png')
    cmd('import', '-window', 'root', path)
    return path


def window():
    assert os.environ.get('DISPLAY') == ':179'
    ids = cmd('xdotool', 'search', '--class', 'Minecraft|Lwjgl3ifyMinecraft').splitlines()
    ids = [i for i in ids if int(dict(line.split('=', 1) for line in
           cmd('xdotool', 'getwindowgeometry', '--shell', i).splitlines())['WIDTH']) > 100]
    assert len(ids) == 1, ids
    cmd('xdotool', 'windowmove', ids[0], 0, 0, 'windowsize', ids[0], 1000, 700,
        'windowraise', ids[0], 'windowfocus', ids[0])
    time.sleep(1)


class TokenFixture(BaseHTTPRequestHandler):
    def do_GET(self):
        data = b'{"ok":true,"token":"release-smoke-fixture-not-a-real-token"}'
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('stage', choices=['token-server', 'window', 'capture'])
    parser.add_argument('--name', default='screen')
    args = parser.parse_args()
    if args.stage == 'token-server':
        HTTPServer(('127.0.0.1', 18796), TokenFixture).serve_forever()
    elif args.stage == 'window':
        window()
    else:
        print(capture(args.name))
