#!/usr/bin/env python
# encoding: utf-8
"""
PEHash.py

Created by Chen, Liang-Heng on 2008-02-04.
Copyright (c) 2008 TFCIS. All rights reserved.
"""

from hashlib import sha256

class PEHash(object):
    """PyEZST hash function"""
    def __init__(self, phrase):
        super(PEHash, self).__init__()
        try:
            self.key = sha256(phrase)
        except TypeError:
            if type(self) == type(phrase):
                self.key = sha256(phrase.key.digest())
            else:
                self.key = sha256(str(phrase))
    def __str__(self):
        return self.key.hexdigest()

if __name__ == '__main__':
    phrase = raw_input('Phrase: ')
    print 'Key   = %s' % (PEHash(phrase))
    print 'Ident = %s' % (PEHash(PEHash(phrase)))
