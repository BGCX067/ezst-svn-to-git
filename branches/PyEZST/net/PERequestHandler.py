#!/usr/bin/env python
# encoding: utf-8

"""
PERequestHandler.py

Created by Chen, Liang-Heng on 2008-02-05.
Copyright (c) 2008 TFCIS. All rights reserved.
"""

from SocketServer import StreamRequestHandler
from PEClientPool import PEClientPool

class PERequestHandler(StreamRequestHandler, PEClientPool):
    """PyEZST server request handler"""

    def handle(self):
        """Handle of incoming clients"""
        print 'Connection from ', self.client_address
        self.ReadHeader()
        self.Taiji()

    def ReadHeader(self):
        """Read client informations"""
        # Phase 1
        self.wfile.write('Welcome\n')

        # Phase 2
        ident = self.rfile.readline().strip()
        self.the_other = self.AddClient(ident, self)

        # Phase 3
        self.wfile.write('Connected\n')

    def Taiji(self):
        """Send received data"""
        # TODO: Need re-implement
        try:
            while True:
                data = self.rfile.read(4096)
                if not data:
                    break

                self.the_other.wfile.write(data)
        except:
            pass
