{
    let handlers = [];
    let defaultHandler = message => console.log("unprocessed message:\n" + message);
    let ws;
    window.GiveMeUi = {
        addHandler: (eventName, callback) => {
            handlers[eventName] = callback;
            return GiveMeUi;
        },
        setDefaultHandler: function (callback) {
            defaultHandler = callback;
            return GiveMeUi;
        },
        start: onStarted => {
            ws = new WebSocket("ws://localhost:PORT/");
            ws.onmessage = message => {
                let parts = message.data.split("\n", 2);
                let handler = handlers[parts[0]];
                if (parts.length === 2 && handler) {
                    handler(parts[1]);
                } else {
                    defaultHandler(message.data);
                }
            };
            ws.onopen = onStarted;
            return GiveMeUi;
        },
        send: (eventName, message) => {
            ws.send(eventName + "MESSAGE_DELIMITER" + message);
        }
    };
}
