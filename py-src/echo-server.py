#!/usr/bin/env python

import socket
import logging
import sys
import json
import asyncore
import threading
import os

TCP_PORT = int(sys.argv[1])
CLIENT_ID = int(sys.argv[2])

# set up the logging
logging.basicConfig(filename='/Users/Shared/lt-echo-server.log',
                    level=logging.INFO)

def safePrint(s):
  sys.stdout.write(s)
  sys.stdout.flush()

def chunks(lst, n):
  for i in range(0, len(lst), n):
    yield lst[i:i+n]

##
## Create the client that we'll use.
##

class Client(asyncore.dispatcher):

  def __init__(self, host, port):
    logging.info('Creating client.')
    asyncore.dispatcher.__init__(self)
    self.create_socket(socket.AF_INET, socket.SOCK_STREAM)
    self.connect( (host, port) )
    self.buffer = ""
    self.cur = ""
    logging.info('Connected to {} on port {}.'.format(host, port))

  def send_message(self, client, command, info):
    logging.info('Sending message')
    tosend = (json.dumps([client, command, info]) + "\n").encode('utf-8')
    logging.info(tosend)
    for chunk in chunks(tosend, 1024):
        self.send(chunk)
    logging.info("Done sending message")

  def handle_message(self, msg_json_str):
    [n, mode, msg] = json.loads(msg_json_str)
    if mode == 'client.close':
        logging.info('Got mode {} so lets close.'.format(mode))
        self.handle_close()
    elif mode == 'editor.eval.ocaml':
        code = msg['code']
        start = msg['meta']['start']
        end = msg['meta']['end']
        self.send_message(n, "editor.eval.ocaml.result", {
          'result': code,
          'meta' : {
            'start': start,
            'end': end
          }
        })
    else:
        logging.info('Ignoring message')

  def handle_connect(self):
    logging.info('handle_connect')
    pass

  def handle_close(self):
    logging.info('About to close')
    self.close()
    safePrint("Disconnected")
    sys.exit()

  def handle_read(self):
    logging.info('Reading')
    self.cur += self.recv(1024).decode('utf-8')
    if self.cur[-1] == '\n':
      logging.info('Received: {}'.format(self.cur))
      self.handle_message(self.cur)
      self.cur = ""

  def writable(self):
    isWritable = (len(self.buffer) > 0)
    logging.info('writable: {}'.format(isWritable))
    return isWritable

  def handle_write(self):
    logging.info('Writing')
    sent = self.send(self.buffer.encode('utf-8'))
    self.buffer = self.buffer[sent:]

  def sendoff(self, msg):
    logging.info('Sendoff')
    self.buffer += msg

#
#
def start():
  logging.info("Start() has been invoked.")
  sys.stdout.flush()

  info = {
    'name' : 'echo-server',
    'client-id' : CLIENT_ID,
    'dir' : os.getcwd(),
    'commands' : ['editor.eval.ocaml']
  }

  msg = json.dumps(info)

  client.send((msg+ "\n").encode('utf-8'));
  logging.info('Send message: {}'.format(msg))
  asyncore.loop()

client = Client('127.0.0.1', TCP_PORT)

## We print this to std.out as the ocaml proc.out
## trigger expects it (see ::on-out)
safePrint("Connected")

## Start the client in it's own thread.
threading.Thread(target=start).start()