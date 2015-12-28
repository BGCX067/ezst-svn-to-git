#!/usr/bin/env python
# encoding: utf-8

import urllib
from xml.dom import minidom

class Tracker(object):
    def __init__(self):
        super(Tracker, self).__init__()

    def __get_peer_attribute(self, peer, attr):
        node = peer.getElementsByTagName(attr)
        return node[0].childNodes[0].nodeValue

    def get_peer(self):
        url = 'http://moovielib.appspot.com/'
        f = urllib.urlopen(url)
        root = minidom.parseString(f.read())
        peers = root.getElementsByTagName('peer')
        results = []

        for peer in peers:
            uid = self.__get_peer_attribute(peer, 'uid')
            ip = self.__get_peer_attribute(peer, 'ip')
            port = self.__get_peer_attribute(peer, 'port')
            results.append({'uid': uid, 'ip': ip, 'port': port})

        return results

    def add_peer(self, **kw):
        url = 'http://moovielib.appspot.com/update?'
        params = []
        for k, v in kw.iteritems():
            params.append('%s=%s' % (k, v))
        urllib.urlopen(url + '&'.join(params))

def main():
    t = Tracker()
    print t.get_peer()
    # t.add_peer( uid='xxx', ip='1234', port='23')

if __name__ == '__main__':
    main()
