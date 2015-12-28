#!/usr/bin/env python
# encoding: utf-8

"""
PEClientPool.py

Created by Chen, Liang-Heng on 2008-02-05.
Copyright (c) 2008 TFCIS. All rights reserved.
"""

import threading

class PEClientPool(object):
    """A pool that contains clients"""
    pool = {}

    def AddClient(self, ident, client):
        if ident in self.pool.keys():
            the_other, sem = self.pool[ident]

            self.pool[ident] = client
            # release the lock of the other client
            sem.release()
        else:
            # create a semaphore for waiting the other client
            sem = threading.Semaphore(0)

            self.pool[ident] = (client, sem)
            # wait for the other client to release the lock
            sem.acquire()
            the_other = self.pool[ident]
            del self.pool[ident]

        return the_other