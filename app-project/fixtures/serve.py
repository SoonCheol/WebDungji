from http.server import ThreadingHTTPServer, SimpleHTTPRequestHandler
from pathlib import Path
import os
os.chdir(Path(__file__).parent)
class Handler(SimpleHTTPRequestHandler):
 def log_message(self, fmt, *args):
  pass
ThreadingHTTPServer(('127.0.0.1',8873),Handler).serve_forever()
