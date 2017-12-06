GiveMeUi = {
  _handlers: [],
  _defaultHandler: message => {},
  addHandler: (eventName, callback) => {
    GiveMeUi._handlers[eventName] = callback;
    return GiveMeUi;
  },
  setDefaultHandler: function (callback) {
    GiveMeUi._defaultHandler = callback;
    return GiveMeUi;
  },
  start: onStarted => {
    GiveMeUi.ws = new WebSocket("ws://HOST:PORT/");
    GiveMeUi.ws.onmessage = message => {
      let parts = message.data.split("\n", 2);
      let handler = GiveMeUi._handlers[parts[0]];
      if (parts.length === 2 && handler) {
        handler(parts[1]);
      } else {
        GiveMeUi._defaultHandler(message.data);
      }
    };
    GiveMeUi.ws.onopen = onStarted;
    return GiveMeUi;
  },
  send: (eventName, message) => {
    GiveMeUi.ws.send(eventName + "MESSAGE_DELIMITER" + message);
  }
};