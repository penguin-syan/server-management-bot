package tokyo.penguin_syan.server_management_bot.service.factorio;

import lombok.Getter;

public enum FactorioCommand {
    BOOT("boot", "すべてのサーバ群を起動する"), SHUTDOWN("shutdown", "すべてのサーバ群を停止する"), LICENSE("license",
            "本Botに使われているOSSのラインセンスを確認する");

    @Getter
    private String command;
    @Getter
    private String description;

    private FactorioCommand(String command, String description) {
        this.command = command;
        this.description = description;
    }

}
