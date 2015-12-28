from google.appengine.ext import db
from gaeo.model import BaseModel, SearchableBaseModel

class Ezst(BaseModel):
    uid = db.StringProperty(required=True)
    ip = db.StringProperty(required=True)
    port = db.IntegerProperty(required=True)
    last_seen = db.DateTimeProperty(auto_now=True)
