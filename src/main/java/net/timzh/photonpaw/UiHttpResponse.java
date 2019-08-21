package net.timzh.photonpaw;

public class UiHttpResponse {

    public final String contentType;
    public final String body;

    public UiHttpResponse(String contentType, String body) {
        this.contentType = contentType;
        this.body = body;
    }

    public static UiHttpResponse of(String contentType, String body) {
        return new UiHttpResponse(contentType, body);
    }
}
