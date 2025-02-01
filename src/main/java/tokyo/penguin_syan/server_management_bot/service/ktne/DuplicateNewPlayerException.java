package tokyo.penguin_syan.server_management_bot.service.ktne;

public class DuplicateNewPlayerException extends Exception {

    DuplicateNewPlayerException(String playerName) {
        super("既に追加済みのプレイヤーです [プレイヤー名: " + playerName + "]");
    }

}
