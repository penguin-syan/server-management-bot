package tokyo.penguin_syan.server_management_bot.proxmox.httpclient;

import java.io.IOException;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import lombok.Getter;

public class HttpResponse {
    @Getter
    int responseCode;

    @Getter
    String responseBody;

    public HttpResponse(int responseCode, HttpEntity responseBody)
            throws UnsupportedOperationException, IOException, ParseException {
        this.responseCode = responseCode;
        this.responseBody = EntityUtils.toString(responseBody);
    }
}
