package net.timzh.photonpaw;

import java.util.List;
import java.util.Map;

public class UiHttpRequest {
    private final String method;
    private final String protocol;
    private final String host;
    private final int port;
    private final Map<String, List<String>> params;
    private final Map<String, List<String>> headers;

    public UiHttpRequest(
            String method,
            String protocol,
            String host,
            int port,
            Map<String, List<String>> params,
            Map<String, List<String>> headers) {
        this.method = method;
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.params = params;
        this.headers = headers;
    }

    public String getMethod() {
        return method;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public Map<String, List<String>> getParams() {
        return params;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }
}
