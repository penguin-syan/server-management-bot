package tokyo.penguin_syan.jda;

import lombok.Getter;

public enum Command {
    BOOT("boot", "すべてのサーバ群を起動する"), SHUTDOWN("shutdown", "すべてのサーバ群を停止する"), LICENSE("license",
            "本Botに使われているOSSのラインセンスを確認する"), DEVELOP("develop", "開発時確認用");

    @Getter
    private String command;
    @Getter
    private String description;

    private Command(String command, String description) {
        this.command = command;
        this.description = description;
    }

}
