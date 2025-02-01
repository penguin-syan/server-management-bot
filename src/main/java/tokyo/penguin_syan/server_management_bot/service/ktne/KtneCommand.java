package tokyo.penguin_syan.server_management_bot.service.ktne;

import lombok.Getter;

enum KtneCommand {
    INITIAL("init", "KTNEルーレットの初期化"), LEAVE("leave", "KTNEルーレットからの退出"), ROULET("roulet",
            "KTNEルーレットを実施");

    @Getter
    private String command;
    @Getter
    private String description;

    private KtneCommand(String command, String description) {
        this.command = command;
        this.description = description;
    }

}
