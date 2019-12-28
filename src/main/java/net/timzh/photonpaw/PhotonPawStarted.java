package net.timzh.photonpaw;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.StringEscapeUtils;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;
import java.util.function.Consumer;

/**
 * PhotonPaw in 'Started' phase
 */
public class PhotonPawStarted implements AutoCloseable {

    private static final int START_PORT = 19080;

    private final PhotonPaw paw;

    private final String messagePartsDelimiter;
    private final String unloadEvent;

    private final Log log;

    private final UiServer server;

    private final Object unloadWaitLock;
    private final Object interruptionWaitLock;

    PhotonPawStarted(PhotonPaw paw) {
        this.paw = paw;

        messagePartsDelimiter = "\n";
        unloadEvent = "__unload__";

        log = LogFactory.getLog(PhotonPaw.class);

        unloadWaitLock = new Object();
        interruptionWaitLock = new Object();

        log.info("creating ui server");
        server = paw.createUiServer();
    }

    PhotonPawStarted start(Consumer<PhotonPawStarted> onStart) {
        paw.boundPaths.forEach(server::bindPath);
        paw.handleCommand(unloadEvent, (msg, out) -> {
            log.info("processing unload");
            synchronized (unloadWaitLock) {
                unloadWaitLock.notifyAll();
            }
        });
        if (paw.port == -1) {
            paw.port = firstAvailablePort(START_PORT);
        }
        if (paw.wsPort == -1) {
            paw.wsPort = firstAvailablePort(paw.port + 1);
        }
        String jsClient = readFile("photonpaw_client.js")
                .replace("PORT", paw.wsPort + "")
                .replace("MESSAGE_PARTS_DELIMITER", StringEscapeUtils.escapeJava(messagePartsDelimiter))
                .replace("UNLOAD_EVENT", unloadEvent);
        server.bindPath("/photonpaw_client.js", request -> {
            log.info("sending js client");
            return UiHttpResponse.of("application/javascript", jsClient);
        });
        log.info("starting, port=" + paw.port + ", wsPort=" + paw.wsPort);
        server.start(paw.port, paw.wsPort, paw.resourcesRoot, msg -> {
            String[] parts = msg.split(messagePartsDelimiter, 3);
            if (parts.length == 3) {
                String eventName = parts[0];
                String correlationId = parts[1];
                String data = parts[2];

                if (! correlationId.isEmpty() && paw.queryHandlers.containsKey(eventName)) {
                    log.info("processing query:\n" + msg);
                    String result = paw.queryHandlers.get(eventName).apply(data);
                    send(eventName, correlationId, result);
                } else {
                    log.info("processing " +
                            (paw.commandHandlers.containsKey(eventName) ? "command:\n" : "unknown command:\n") + msg);
                    if (paw.commandHandlers.containsKey(eventName)) {
                        paw.commandHandlers.get(eventName).accept(data, this);
                    } else {
                        paw.defaultHandler.accept(msg, this);
                    }
                }
            } else {
                paw.defaultHandler.accept(msg, this);
            }
        }, () -> onStart.accept(this));
        return this;
    }

    private static int firstAvailablePort(int startFrom) {
        while (! isAvailable(startFrom)) {
            ++startFrom;
        }
        return startFrom;
    }

    private static boolean isAvailable(int portToCheck) {
        try (ServerSocket ss = new ServerSocket(portToCheck); DatagramSocket ds = new DatagramSocket(portToCheck)) {
            ss.setReuseAddress(true);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static String readFile(String path) {
        InputStream stream = PhotonPawStarted.class.getClassLoader().getResourceAsStream(path);
        Scanner s = new Scanner(stream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private void send(String event, String correlationId, String message) {
        String msg = event + messagePartsDelimiter + correlationId + messagePartsDelimiter + message;
        log.info("sending message:\n" + msg);
        server.send(msg);
    }

    /**
     * Send an event to ui
     *
     * @param event   event name
     * @param message event body
     */
    public void send(String event, String message) {
        send(event, "", message);
    }

    /**
     * Convenient way to call {@link System#out}'s {@link java.io.PrintStream#println(Object) println}
     *
     * @param o object to print
     * @return this instance
     */
    public PhotonPawStarted println(Object o) {
        System.out.println(o);
        return this;
    }

    /**
     * Open system default browser
     *
     * @param path path to open, starting with {@code /}
     * @return this instance
     */
    public PhotonPawStarted openBrowser(String path) {
        if (! GraphicsEnvironment.isHeadless()) {
            try {
                log.info("opening browser");
                Desktop.getDesktop().browse(new URI("http://localhost:" + paw.port + path));
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    /**
     * Wait for any input from {@link System#in}
     *
     * @return this instance
     */
    public PhotonPawStarted waitForInput() {
        try {
            log.info("waiting for input");
            System.in.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * Wait for {@link PhotonPawStarted#unloadEvent} from ui (sent when the tab is closed or refreshed)
     *
     * @return this instance
     */
    public PhotonPawStarted waitForTabToUnload() {
        synchronized (unloadWaitLock) {
            try {
                log.info("waiting for tab unload");
                unloadWaitLock.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    /**
     * Wait for {@link PhotonPawStarted#interrupt()}
     *
     * @return this instance
     */
    public PhotonPawStarted waitForInterruption() {
        synchronized (interruptionWaitLock) {
            try {
                log.info("waiting for interruption");
                interruptionWaitLock.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    /**
     * Interrupt {@link PhotonPawStarted#waitForInterruption()}
     */
    public void interrupt() {
        synchronized (interruptionWaitLock) {
            log.info("interrupting");
            interruptionWaitLock.notifyAll();
        }
    }

    /**
     * Stop the ui server
     *
     * @return this instance
     */
    public PhotonPawStarted stop() {
        log.info("stopping");
        server.stop();
        return this;
    }

    @Override
    public void close() {
        stop();
    }

    /**
     * Main port
     *
     * @return main port
     */
    public int getPort() {
        return paw.port;
    }

    /**
     * Websocket port
     *
     * @return websocket port
     */
    public int getWsPort() {
        return paw.wsPort;
    }
}
