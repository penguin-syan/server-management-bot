package tokyo.penguin_syan.proxmox.httpclient;

import java.io.IOException;
import org.apache.hc.core5.http.HttpEntity;
import lombok.Getter;

public class HttpResponse {
    @Getter
    int responseCode;

    @Getter
    String responseBody;

    public HttpResponse(int responseCode, HttpEntity responseBody)
            throws UnsupportedOperationException, IOException {
        this.responseCode = responseCode;
        this.responseBody = new String(responseBody.getContent().readAllBytes());
    }
}
