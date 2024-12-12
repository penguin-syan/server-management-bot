package tokyo.penguin_syan.jda;

public enum Command {
    BOOT("boot", "すべてのサーバ群を起動する"), SHUTDOWN("shutdown", "すべてのサーバ群を停止する"), REBOOT("reboot",
            "すべてのサーバ群を再起動する"), KEEP("keep", "サーバ群のシャットダウンまでのカウントを延長する");

    private String command;
    private String description;

    private Command(String command, String description) {
        this.command = command;
        this.description = description;
    }

    public String getCommand() {
        return this.command;
    }

    public String getDescription() {
        return this.description;
    }
}
