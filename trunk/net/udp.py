#!/usr/bin/env python
# encoding: utf-8

import logging
import new
import re
import select
import socket
import sys
import thread
import traceback


class BaseUDPHandler(object):
    def __init__(self):
        super(BaseUDPHandler, self).__init__()

        self.__host = ''
        self.__port = 0

        self.__socket = None
        self.__poll = None

        self.__mutex = thread.allocate_lock()

        self.__outgoing_queue = []

        self.logger = logging.getLogger(self.__get_class_name())

    def bind(self, host, port):
        self.__host = host
        self.__port = port

        self.__socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.__socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.__socket.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
        self.__socket.bind((host, port))

        try:
            self.__poll = select.poll()
            self.__poll.register(self.__socket.fileno(), select.POLLIN | select.POLLERR | select.POLLHUP)
        except AttributeError:
            self.logger.warning('poll() is not supported.')

    def __check_by_poll(self):
        results = self.__poll.poll(50)
        if results:
            if results[0][1] == select.POLLIN:
                return True
        return False

    def __check_by_select(self):
        infds, outfds, errfds = select.select([self.__socket], [], [self.__socket], 0.5)
        if infds:
            return True
        return False

    def __get_class_name(self):
        return re.findall("<class '.*\.(.*)'>", str(self.__class__))[0]

    def before_run(self):
        pass

    def run(self):
        self.before_run()

        has_input = self.__check_by_poll if self.__poll else self.__check_by_select
        while 1:
            if has_input():
                message, address = self.read()
                isSuccess = self.handle(message, address)
                if not isSuccess:
                    self.logger.error('%s %s' % (message, address))
            self.flush()

    def read(self, length=8192):
        if self.__socket:
            message, address = self.__socket.recvfrom(length)
            self.logger.debug('[I] %s from %s' % (message, address))
            return message, address
        return None

    def __write(self, message, address):
        if self.__socket:
            self.logger.debug('[O] %s to %s' % (message, address))
            return self.__socket.sendto(message, address)
        return None

    def write(self, message, address):
        self.__mutex.acquire()
        self.__outgoing_queue.append((message, address))
        self.__mutex.release()

    def flush(self):
        self.__mutex.acquire()
        queue = self.__outgoing_queue
        self.__outgoing_queue = []
        self.__mutex.release()

        for message, address in queue:
            self.__write(message, address)

    def broadcast(self, message, port=3257):
        self.write(message, ('<broadcast>', port))

    def handle(self, message, address):
        pass

def test():
    logging.basicConfig(level=logging.DEBUG)

    try:
        p = BaseUDPHandler()
        p.bind('', 3257)
        p.broadcast('TEST')
        p.run()
    except KeyboardInterrupt:
        pass

if __name__ == '__main__':
    test()
