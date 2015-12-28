#!/usr/bin/env python
# encoding: utf-8

import hashlib
import logging
import random
import string
import sys
import traceback
from threading import Thread

from route import Route
from tracker import Tracker
from udp import BaseUDPHandler


class BaseEZSTHandler(BaseUDPHandler, Thread):
    def __init__(self, **kw):
        super(BaseEZSTHandler, self).__init__()

        self.isDebug = kw.get('debug', False)
        self.__uid = None

    @property
    def uid(self):
        if self.__uid is None:
            n = random.randint(16, 64)
            s = ''.join([random.choice(string.printable) for i in range(n)])
            self.__uid = hashlib.sha256(s).hexdigest()
        return self.__uid

    @staticmethod
    def check_vlan(self, uid1, uid2):
        return uid1[:1] == uid2[:1]

    def parseCommand(self, command):
        try:
            cmd, param = string.split(command, maxsplit=1)
        except ValueError:
            cmd, param = command.strip(), ''

        return cmd.capitalize(), param

    def handle(self, message, address):
        '''
            Handle incoming packets.
        '''
        cmd, param = self.parseCommand(message)

        func = 'on%s' % (cmd)
        self.logger.debug('on%s(%s)' % (cmd, param))
        try:
            getattr(self, func)(address, param)
            return True
        except:
            logging.error(traceback.format_exc())
        return False

    def command(self, message):
        cmd, param = self.parseCommand(message)

        func = 'do%s' % (cmd)
        if hasattr(self, func) and callable(getattr(self, func)):
            self.logger.debug('do%s(%s)' % (cmd, param))
            try:
                getattr(self, func)(param)
            except:
                logging.error(traceback.format_exc())
                help(getattr(self, func))


class EZSTHandler(BaseEZSTHandler):
    def __init__(self, **kw):
        super(EZSTHandler, self).__init__(**kw)

        self.__local_route = Route()
        self.__vlan_route = Route()

    def bind(self, host='', port=3257):
        super(BaseEZSTHandler, self).bind(host, port)
        self.broadcast('JOIN %s' % (self.uid))

    def doList(self, param):
        if param.upper() != 'VLAN':
            self.logger.info('Local Routing:\n%s' % (str(self.__local_route)))
        if param.upper() != 'LOCAL':
            self.logger.info('Vlan Routing:\n%s' % (str(self.__vlan_route)))

    def doPing(self, param):
        '''
            PING <host>:<port>
        '''
        address, port = param.split(':')
        port = int(port)
        self.logger.info('PING to %s:%d' % (address, port))
        self.write('PING', (address, port))

    def onPing(self, address, param):
        self.logger.info('PING from %s' % (str(address)))
        self.write('PONG', address)

    def onPong(self, address, param):
        self.logger.info('PONG from %s' % (str(address)))

    def onJoin(self, address, uid):
        if uid != self.uid:
            self.__local_route.add(uid, address)
            self.write('OK %s' % self.uid, address)

    def onOk(self, address, uid):
        self.__local_route.add(uid, address)
        self.write('QUERY', address)

    def onQuery(self, address, param):
        '''Query for global Unique ID
        '''
        safe_counter = 99 # Not to send peer information more than 99 times.

        for uid, addr in self.__vlan_route.list():
            self.write('PEER %s %s' % (uid, addr), address)

            if safe_counter > 0:
                break
            safe_counter -= 1

    def onPeer(self, address, param):
        # For QUERY 
        uid, sep, addr = param.partition(' ')
        addr = eval(addr)
        self.__vlan_route.add(uid, address)
        if BaseEZSTHandler.check_vlan(self.uid, param):
            logging.debug('Addr = %s' % (str(addr)))
            self.write('CONNECT %s' % (self.uid), addr)

    def onConnect(self, address, param):
        self.write('SUCCESS %s' % (self.uid), address)

    def onSuccess(self, address, param):
        pass

    def doSend(self, param, from_uid=None):
        try:
            to_uid, message = string.split(param, maxsplit=1)
        except ValueError:
            to_uid, message = param.strip(), ''

        address = None
        nexthops = self.__local_route.get(to_uid) + self.__vlan_route.get(to_uid)
        if from_uid is None:
            from_uid = self.uid

        for nexthop in nexthops:
            self.write('SEND %s %s %s' % (from_uid, to_uid, message), nexthop)
            break

    def onSend(self, address, param):
        from_uid, to_uid, message = string.split(param, maxsplit=2)
        if to_uid == self.uid:
            self.logger.info('SEND from %s: %s' % (address, message))
        else:
            self.doSend('%s %s' % (to_uid, message), from_uid)

def main():
    isDebug = True

    if isDebug:
        logging.basicConfig(level=logging.DEBUG)
    else:
        logging.basicConfig(level=logging.INFO)

    p = EZSTHandler(debug=isDebug)
    p.bind()

    try:
        p.start()

        while True:
            cmd = raw_input('command: ')
            p.command(cmd)

    except (KeyboardInterrupt, EOFError):
        sys.exit(0)

    p.broadcast('PING')


if __name__ == '__main__':
    main()
