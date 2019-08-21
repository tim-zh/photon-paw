package net.timzh.photonpaw;

import java.util.List;
import java.util.Map;

public class UiHttpRequest {

    public final String method;
    public final String protocol;
    public final String host;
    public final int port;
    public final Map<String, List<String>> params;
    public final Map<String, List<String>> headers;

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
}
