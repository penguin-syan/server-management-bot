package tokyo.penguin_syan.server_management_bot.proxmox;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;
import lombok.extern.log4j.Log4j2;
import tokyo.penguin_syan.server_management_bot.PropertiesReader;
import tokyo.penguin_syan.server_management_bot.proxmox.httpclient.HttpRequestType;
import tokyo.penguin_syan.server_management_bot.proxmox.httpclient.HttpResponse;

@Log4j2
public class ProxmoxController {
    private static PropertiesReader propertiesReader;
    private static String apiBaseUrl;

    /**
     * ProxmoxControllerクラスのコンストラクタ
     */
    public ProxmoxController() {
        propertiesReader = new PropertiesReader();
        apiBaseUrl = propertiesReader.getProperty("proxmoxProtocol") + "://"
                + propertiesReader.getProperty("proxmoxHostAndPort");
    }

    /**
     * 引数で渡したコマンドをQEMUエージェントを用いてVM上で実行する
     * 
     * @param commands 実行するコマンド
     * @param contentType ContentType.APPLICATION_JSONに固定
     * @return QEMUエージェントがコマンドを実行した際のプロセスID
     * @throws Exception
     */
    public int execCommand(String[] command, ContentType contentType) throws Exception {
        log.info("ProxmoxController#execCommand start");

        String requestBody = execRequestBody(command);
        log.debug(requestBody);

        String response = httpRequest(ApiRequestType.EXEC.getRequestType(),
                apiBaseUrl + ApiRequestType.EXEC.getPathname(), requestBody, contentType);

        JSONObject responseJson = new JSONObject(response);
        int execPid = Integer.parseInt(responseJson.getJSONObject("data").get("pid").toString());
        log.debug("exec pid: " + execPid);

        log.info("ProxmoxController#execCommand end");
        return execPid;
    }


    /**
     * execCommandの戻り値であるpidを元に、コマンド実行結果を取得する
     * 
     * @param pid
     * @return
     * @throws Exception
     */
    public String execStatus(int pid) throws Exception {
        log.info("ProxmoxController#execStatus start");

        String response = httpRequest(ApiRequestType.EXEC_STATUS.getRequestType(),
                apiBaseUrl + ApiRequestType.EXEC_STATUS.getPathname() + pid);

        log.info("ProxmoxController#execStatus end");
        return response;
    }


    /**
     * execCommandメソッド用のJSON形式のリクエストボディを作成する
     * 
     * @param command execCommandメソッドで実行するコマンド
     * @return String型に変換したJSON形式のリクエストボディ
     */
    private String execRequestBody(String[] command) {
        log.info("ProxmoxController#execRequestBody start");

        JSONObject requestBody = new JSONObject();
        JSONArray commandValue = new JSONArray(command);
        requestBody.put("command", commandValue);

        String result = requestBody.toString();

        log.info("ProxmoxController#execRequestBody end");
        return result;
    }


    /**
     * httpclient5を用いてhttpリクエストを行う
     * 
     * @param requestType リクエスト型（GET or POST）
     * @param url リクエスト先のURL
     * @return レスポンス
     * @throws Exception
     */
    private static String httpRequest(HttpRequestType requestType, String url) throws Exception {
        return httpRequest(requestType, url, null, null);
    }



    /**
     * httpclient5を用いてhttpリクエストを行う
     * 
     * @param requestTypeリクエスト型（GET or POST）
     * @param url リクエスト先のURL
     * @param body リクエストボディ（requestTypeがPOSTの時のみ有効）
     * @param bodyContentType リクエストボディのコンテンツタイプ（requestTypeがPOSTの時のみ有効）
     * @return レスポンス
     * @throws Exception
     */
    private static String httpRequest(HttpRequestType requestType, String url, String body,
            ContentType bodyContentType) throws Exception {
        log.info("ProxmoxController.httpRequest start");
        log.debug("ProxmoxController.httpRequest request with type: " + requestType);
        log.debug("ProxmoxController.httpRequest request to: " + url);

        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpUriRequestBase httpRequest;

            if (requestType == HttpRequestType.GET) {
                httpRequest = new HttpGet(url);
            } else {
                httpRequest = new HttpPost(url);
                if (body != null) {
                    httpRequest.setEntity(new StringEntity(body, bodyContentType));
                }
            }

            httpRequest.setHeader("Authorization",
                    "PVEAPIToken=" + propertiesReader.getProperty("proxmoxApiToken"));

            HttpResponse responseBuffer = httpClient.execute(httpRequest, response -> {
                return new HttpResponse(response.getCode(), response.getEntity());
            });
            httpClient.close();

            System.out.println(responseBuffer.getResponseBody());

            if (responseBuffer.getResponseCode() != 200) {
                log.warn("ProxmoxController.httpRequest error");
                throw new Exception(String.format("Response code is not 200 (%d)",
                        responseBuffer.getResponseCode()));
            }
            log.info("ProxmoxController.httpRequest end");
            return responseBuffer.getResponseBody();

        }
    }
}
