#!/usr/bin/env python
# encoding: utf-8
"""
PEPacker.py

Created by Chen, Liang-Heng on 2008-03-05.
Copyright (c) 2008 TFCIS. All rights reserved.
"""

import os, stat, tarfile

class PEBasePacker(object):
    """PyEZST file packer base"""
    def __init__(self):
        self.tar = None

    def __del__(self):
        self.close()

    def open(self, *args):
        raise NotImplementedError

    def close(self):
        if self.tar:
            self.tar.close()
            self.tar = None

    def list(self, *args):
        raise NotImplementedError

class PEPacker(PEBasePacker):
    """PyEZST file packer"""
    def __init__(self):
        super(PEPacker, self).__init__()
        self.pool = []

    def add(self, dorf):
        if os.access(dorf, os.F_OK):
            self.pool.append(dorf)
            return True
        return False

    def open(self, obj):
        self.tar = tarfile.open(mode='w|', fileobj=stream)

        # If false, add symbolic and hard links to archive. If true, add the
        # content of the target files to the archive. This has no effect on
        # systems that do not support symbolic links.
        self.tar.dereference = True

    def push(self):
        for f in self.pool:
            # tar.add(name[, arcname[, recursive]])
            self.tar.add(f, recursive=True)

    def list(self):
        for f in self.pool:
            print f

def main():
    p = PEPacker()

    import socket
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect(('localhost', 13579))
    p.open(s.makefile('wb'))

    p.add('.')
    p.list()
    p.push()
    p.close()

if __name__ == '__main__':
    main()

