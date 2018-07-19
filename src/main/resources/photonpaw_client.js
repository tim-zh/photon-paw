{
    let handlers = []; //event name to callback
    let defaultHandler = (eventName, message) => console.log("unprocessed message:\n" + eventName + "\n" + message);

    let ws;

    let askMap = []; //correlation id to promise resolve
    let correlationIdSeed = 0;

    let started = false;
    function mustBeStarted(flag) {
        if (flag !== started) {
            let must = flag ? "must" : "must not";
            throw "PhotonPaw " + must + " be started";
        }
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
            defaultHandler = handler;
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
            ws.onopen = onStart || (() => {});
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
            ws.send(eventName + "MESSAGE_PARTS_DELIMITER" + "MESSAGE_PARTS_DELIMITER" + message);
        },

        /**
         * Send an event to ui server and get a response to process
         *
         * @param {string} eventName - event name
         * @param {string} message - event body
         * @returns {Promise<string>} promise with an answer from ui server
         */
        ask: (eventName, message) => {
            mustBeStarted(true);
            return new Promise((resolve, reject) => {
                let correlationId = correlationIdSeed;
                correlationIdSeed += 1;
                ws.send(eventName + "MESSAGE_PARTS_DELIMITER" + correlationId + "MESSAGE_PARTS_DELIMITER" + message);
                askMap[correlationId] = resolve;
            });
        }
    };
}
