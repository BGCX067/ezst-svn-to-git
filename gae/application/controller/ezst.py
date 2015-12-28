import cgi

from google.appengine.ext import db

from gaeo.controller import BaseController

from model.ezst import Ezst

class EzstController(BaseController):
    def destroy(self):
        r = Ezst.get_by_key_name(self.params.get('uid'))
        if r is not None:
            r.delete()
        self.redirect('/')

    def list(self):
        query = Ezst.all()
        for prop in Ezst.properties():
            value = self.params.get(prop, None)
            if value: # bypass 'None' and empty values.
                query.filter('%s =' % (prop), cgi.escape(value))        

        query.order('-last_seen')
        self.results = query.fetch(limit=1000)

        content = [
            '<?xml version="1.0" encoding="utf-8"?>',
            '<list length="%d">' % len(self.results),
        ]
        for rec in self.results:
            content.append('\t<peer update="%s">' % (rec.last_seen))
            content.append('\t\t<uid>%s</uid>' % (rec.uid))
            content.append('\t\t<ip>%s</ip>' % (rec.ip))
            content.append('\t\t<port>%s</port>' % (rec.port))
            content.append('\t</peer>')
        content.append('</list>')

        self.render(xml='\n'.join(content))

    def update(self):
        try:
            uid = self.params['uid']
            ip = self.params['ip']
            port = int(self.params['port'])

            r = Ezst.get_by_key_name(uid)
            if r is None:
                r = Ezst(key_name=uid, uid=uid, ip=ip, port=port)
            else:
                r.ip = ip
                r.port = port
            r.put()
        except:
            pass
        self.redirect('/')
