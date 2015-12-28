#!/usr/bin/env python
# encoding: utf-8
import sys
import os
import unittest
import logging

class Route(object):
    def __init__(self):
        super(Route, self).__init__()
        self.__table = {}

        self.logger = logging.getLogger('Route')

    def __str__(self):
        entries = [str(x) for x in self.list()]
        return '\n'.join(entries)

    def clear(self):
        self.__table = {}

    def add(self, uid, nexthop):
        if uid in self.__table:
            if nexthop not in self.__table[uid]:
                self.__table[uid].insert(0, nexthop)
                self.logger.info('Add %s %s' % (uid, nexthop))
        else:
            self.__table[uid] = [nexthop]
            self.logger.info('Create %s %s' % (uid, nexthop))

    def remove(self, uid, nexthop):
        if uid in self.__table:
            if nexthop in self.__table:
                self.__table.remove(nexthop)

    def remove_all(self, uid):
        if uid in self.__table:
            self.__table[uid] = []

    def get(self, uid):
        if uid in self.__table:
            return self.__table[uid]
        return []

    def list(self):
        # Make a copy.
        entries = [entry for entry in self.__table.items()]

        for k, v in entries:
            for address in v:
                yield k, address

class routeTests(unittest.TestCase):
    def setUp(self):
        pass

if __name__ == '__main__':
    unittest.main()
