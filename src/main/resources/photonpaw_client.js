{
    let handlers = []; //event name to callback
    let defaultHandler = (eventName, message) => console.log("unprocessed message:\n" + eventName + "\n" + message);
    let ws;
    let askMap = []; //correlation id to promise resolve
    let correlationIdSeed = 0;
    window.PhotonPaw = {
        subscribe: (eventName, callback) => {
            handlers[eventName] = callback;
            return PhotonPaw;
        },
        setDefaultHandler: function (callback) {
            defaultHandler = callback;
            return PhotonPaw;
        },
        start: onStarted => {
            ws = new WebSocket("ws://localhost:PORT/");
            ws.onmessage = message => {
                let parts = message.data.split("\n", 3);
                if (parts.length === 3) {
                    let eventName = parts[0];
                    let correlationId = parts[1];
                    let data = parts[2];

                    if (askMap[correlationId]) {
                        askMap[correlationId](data);
                    } else if (handlers[eventName]) {
                        handlers[eventName](data);
                    } else {
                        defaultHandler(eventName, data);
                    }
                } else {
                    defaultHandler("", message.data);
                }
            };
            ws.onopen = onStarted;
            return PhotonPaw;
        },
        send: (eventName, message) => {
            ws.send(eventName + "MESSAGE_DELIMITER" + "MESSAGE_DELIMITER" + message);
        },
        ask: (eventName, message) => {
            return new Promise((resolve, reject) => {
                let correlationId = correlationIdSeed;
                correlationIdSeed += 1;
                ws.send(eventName + "MESSAGE_DELIMITER" + correlationId + "MESSAGE_DELIMITER" + message);
                askMap[correlationId] = resolve;
            });
        }
    };
}
