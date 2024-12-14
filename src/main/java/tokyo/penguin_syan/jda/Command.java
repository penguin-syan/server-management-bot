package tokyo.penguin_syan.jda;

import lombok.Getter;

public enum Command {
    BOOT("boot", "すべてのサーバ群を起動する"), SHUTDOWN("shutdown", "すべてのサーバ群を停止する"), REBOOT("reboot",
            "すべてのサーバ群を再起動する"), KEEP("keep", "サーバ群のシャットダウンまでのカウントを延長する");

    @Getter
    private String command;
    @Getter
    private String description;

    private Command(String command, String description) {
        this.command = command;
        this.description = description;
    }

}
