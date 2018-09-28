{
    let handlers = []; //event name -> callback
    let _defaultHandler = (eventName, message) => {};

    let ws;

    let askMap = {}; //correlation id -> promise resolve
    let correlationIdSeed = 0;

    let started = false;
    function mustBeStarted(flag) {
        if (flag !== started) {
            let must = flag ? "must" : "must not";
            throw "PhotonPaw " + must + " be started";
        }
    }

    function withTimeout(timeout, promiseHandler) {
        return Promise.race([
            new Promise(promiseHandler),
            new Promise((resolve, reject) =>
                setTimeout(() => reject('timeout after ' + timeout + ' ms'), timeout)
            )
        ]);
    }

    window.PhotonPaw = {
        /**
         * Add a handler for commands from ui server by name
         *
         * @param {string} eventName - event name
         * @param {function(string)} handler - command handler
         * @returns {Window.PhotonPaw} PhotonPaw
         */
        handleCommand: (eventName, handler) => {
            mustBeStarted(false);
            handlers[eventName] = handler;
            return PhotonPaw;
        },

        /**
         * Handler for all unprocessed messages
         *
         * @param {function(string, string)} handler - default handler
         * @returns {Window.PhotonPaw} PhotonPaw
         */
        defaultHandler: function (handler) {
            mustBeStarted(false);
            _defaultHandler = handler;
            return PhotonPaw;
        },

        /**
         * Start the ui client
         *
         * @param {function()} onStart - callback to execute after establishing a connection with ui
         * @returns {Window.PhotonPaw} PhotonPaw
         */
        start: onStart => {
            mustBeStarted(false);
            started = true;
            ws = new WebSocket("ws://localhost:PORT/");
            ws.onmessage = message => {
                let parts = message.data.split("\n", 3);
                if (parts.length === 3) {
                    let eventName = parts[0];
                    let correlationId = parts[1];
                    let data = parts[2];

                    if (askMap[correlationId]) {
                        PhotonPaw.log("queryResponse", eventName, data, correlationId);
                        askMap[correlationId](data);
                        delete askMap[correlationId];
                    } else if (handlers[eventName]) {
                        PhotonPaw.log("command", eventName, data);
                        handlers[eventName](data);
                    } else {
                        PhotonPaw.log("defaultHandler", eventName, data);
                        _defaultHandler(eventName, data);
                    }
                } else {
                    PhotonPaw.log("defaultHandler", "", message.data);
                    _defaultHandler("", message.data);
                }
            };
            ws.onopen = onStart || (() => {});
            window.addEventListener('beforeunload', () => {
                PhotonPaw.send("UNLOAD_EVENT", "");
            });
            return PhotonPaw;
        },

        /**
         * Send an event to ui server
         *
         * @param {string} eventName - event name
         * @param {string} message - event body
         */
        send: (eventName, message) => {
            mustBeStarted(true);
            PhotonPaw.log("send", eventName, message);
            ws.send(eventName + "MESSAGE_PARTS_DELIMITER" + "MESSAGE_PARTS_DELIMITER" + message);
        },

        /**
         * Time in milliseconds to wait for {@link PhotonPaw.ask} completion
         */
        askTimeout: 5000,

        /**
         * Send an event to ui server and get a response to process
         *
         * @param {string} eventName - event name
         * @param {string} message - event body
         * @returns {Promise<string>} promise with an answer from ui server
         */
        ask: (eventName, message) => {
            mustBeStarted(true);
            let correlationId = correlationIdSeed;
            correlationIdSeed += 1;
            return withTimeout(PhotonPaw.askTimeout, resolve => {
                PhotonPaw.log("ask", eventName, message, correlationId);
                ws.send(eventName + "MESSAGE_PARTS_DELIMITER" + correlationId + "MESSAGE_PARTS_DELIMITER" + message);
                askMap[correlationId] = resolve;
            }).catch(err => {
                PhotonPaw.log("askError", eventName, err, correlationId);
                delete askMap[correlationId];
            });
        },

        /**
         * Write event log. Full format: "description, eventName=..., correlationId=..., message=..."
         *
         * @param {string} description - log message
         * @param {string} [eventName] - event name
         * @param {string} [eventMessage] - event message
         * @param {number} [correlationId] - correlation id
         */
        log: (description, eventName, eventMessage, correlationId) => {
            let s = "PhotonPaw: " + description;
            if (eventName) {
                s += ", eventName=" + eventName;
            }
            if (correlationId >= 0) {
                s += ", correlationId=" + correlationId;
            }
            if (eventMessage) {
                s += ", eventMessage=" + eventMessage;
            }
            console.log(s);
        }
    };
}
