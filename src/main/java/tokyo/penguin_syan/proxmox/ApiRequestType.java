package tokyo.penguin_syan.proxmox;

import lombok.Getter;
import tokyo.penguin_syan.PropertiesReader;
import tokyo.penguin_syan.proxmox.httpclient.HttpRequestType;

public enum ApiRequestType {
    START(HttpRequestType.POST, "/api2/json/nodes/%s/qemu/%s/status/start"), STOP(
            HttpRequestType.POST,
            "/api2/json/nodes/%s/qemu/%s/status/stop"), STATUS(HttpRequestType.GET,
                    "/api2/json/nodes/%s/qemu/%s/status/current"), EXEC(HttpRequestType.POST,
                            "/api2/json/nodes/%s/qemu/%s/agent/exec"), EXEC_STATUS(
                                    HttpRequestType.GET,
                                    "/api2/json/nodes/%s/qemu/%s/agent/exec-status?pid=");

    private PropertiesReader propertiesReader = new PropertiesReader();

    @Getter
    HttpRequestType requestType;
    @Getter
    String pathname;

    private ApiRequestType(HttpRequestType requestType, String pathname) {
        this.requestType = requestType;
        this.pathname = String.format(pathname, propertiesReader.getProperty("proxmoxNode"),
                propertiesReader.getProperty("proxmoxVMID"));
    }
}
