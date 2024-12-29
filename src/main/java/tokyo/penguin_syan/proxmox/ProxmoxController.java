package tokyo.penguin_syan.proxmox;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import tokyo.penguin_syan.PropertiesReader;
import tokyo.penguin_syan.proxmox.httpclient.HttpRequestType;
import tokyo.penguin_syan.proxmox.httpclient.HttpResponse;

public class ProxmoxController {
    private static PropertiesReader propertiesReader;
    private static Logger logger = LogManager.getLogger();
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
     * 
     * @throws Exception
     */
    public void startVM() throws Exception {
        logger.info("ProxmoxController#startVM start");

        if (!isVmRunning()) {
            // 起動処理
            httpRequest(ApiRequestType.START.getRequestType(),
                    apiBaseUrl + ApiRequestType.START.getPathname());
        } else {
            // 多重に起動処理は送らない
            logger.info("ProxmoxController#startVM canceled (running)");
            throw new ProxmoxControlException("既に起動済みです");
        }

        logger.info("ProxmoxController#startVM end");
    }

    /**
     * 
     * @throws Exception
     */
    public void stopVM() throws Exception {
        logger.info("ProxmoxController#stopVM start");

        if (isVmRunning()) {
            // 停止処理
            httpRequest(ApiRequestType.STOP.getRequestType(),
                    apiBaseUrl + ApiRequestType.STOP.getPathname());
        } else {
            // 多重に停止処理は送らない
            logger.info("ProxmoxController#stopVM canceled (stopped)");
            throw new ProxmoxControlException("既に停止済みです");
        }

        logger.info("ProxmoxController#stopVM end");
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    public boolean isVmRunning() throws Exception {
        logger.info("ProxmoxController#VMstatus start");

        // ステータス確認
        String response = httpRequest(ApiRequestType.STATUS.getRequestType(),
                apiBaseUrl + ApiRequestType.STATUS.getPathname());
        JSONObject responseJson = new JSONObject(response);

        // 結果ごとの戻り値作成
        switch (responseJson.getJSONObject("data").getString("status")) {
            case "running":
                logger.info("ProxmoxController#VMstatus end (running)");
                return true;
            case "stopped":
                logger.info("ProxmoxController#VMstatus end (stopped)");
                return false;
            default:
                logger.error(String.format("想定外のステータスが返されました",
                        responseJson.getJSONObject("data").getString("status")));
                logger.info("ProxmoxController#VMstatus end (abort)");
                throw new Exception("想定外のステータスが返されました");
        }



    }

    public boolean isOsRunning() {
        logger.info("ProxmoxController#isOsRunning start");

        // OS起動状況確認
        try {
            String response = httpRequest(ApiRequestType.EXEC.getRequestType(),
                    apiBaseUrl + ApiRequestType.EXEC.getPathname(), null,
                    ContentType.APPLICATION_JSON);
        } catch (Exception e) {
            if (e.getMessage().contains("Response code is not 200"))
                logger.warn(e.getMessage(), e);
            logger.info("ProxmoxController#isOsRunning end");
            return false;
        }
        return false;
    }

    /**
     * 
     * @param commands 実行するコマンド
     * @param contentType
     * @return
     * @throws Exception
     */
    public int execCommand(String commands, ContentType contentType) throws Exception {
        logger.info("ProxmoxController#execCommand start");

        String response = httpRequest(ApiRequestType.EXEC.getRequestType(),
                apiBaseUrl + ApiRequestType.EXEC.getPathname(), commands, contentType);

        JSONObject responseJson = new JSONObject(response);
        int execPid = Integer.parseInt(responseJson.getJSONObject("data").get("pid").toString());
        logger.debug("exec pid: " + execPid);

        logger.info("ProxmoxController#execCommand end");
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
        logger.info("ProxmoxController#execStatus start");

        String response = httpRequest(ApiRequestType.EXEC_STATUS.getRequestType(),
                apiBaseUrl + ApiRequestType.EXEC_STATUS.getPathname() + pid);

        logger.info("ProxmoxController#execStatus end");
        return response;
    }

    /**
     * 
     * @param requestType
     * @param url
     * @return
     * @throws Exception
     */
    private static String httpRequest(HttpRequestType requestType, String url) throws Exception {
        return httpRequest(requestType, url, null, null);
    }



    /**
     * 
     * @param requestType
     * @param url
     * @param body リクエストボディ（requestTypeがPOSTの時のみ有効）
     * @param bodyContentType リクエストボディのコンテンツタイプ（requestTypeがPOSTの時のみ有効）
     * @return
     * @throws Exception
     */
    private static String httpRequest(HttpRequestType requestType, String url, String body,
            ContentType bodyContentType) throws Exception {
        logger.info("ProxmoxController.httpRequest start");
        logger.debug("ProxmoxController.httpRequest request with type: " + requestType);
        logger.debug("ProxmoxController.httpRequest request to: " + url);

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
                logger.warn("ProxmoxController.httpRequest error");
                throw new Exception(String.format("Response code is not 200 (%d)",
                        responseBuffer.getResponseCode()));
            }
            logger.info("ProxmoxController.httpRequest end");
            return responseBuffer.getResponseBody();

        }
    }
}
