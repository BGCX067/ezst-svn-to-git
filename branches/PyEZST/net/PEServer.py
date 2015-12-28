#!/usr/bin/env python
# encoding: utf-8
"""
PEServer.py

Created by Chen, Liang-Heng on 2008-02-08.
Copyright (c) 2008 TFCIS. All rights reserved.
"""

from SocketServer import ThreadingMixIn, TCPServer
from PERequestHandler import PERequestHandler

class PEServer(ThreadingMixIn, TCPServer):
    """PyEZST server"""
    allow_reuse_address = 1

def main():
    addr = ('', 3257)
    srv = PEServer(addr, PERequestHandler)

    try:
        srv.serve_forever()
    except KeyboardInterrupt:
        pass

if __name__ == '__main__':
    main()

