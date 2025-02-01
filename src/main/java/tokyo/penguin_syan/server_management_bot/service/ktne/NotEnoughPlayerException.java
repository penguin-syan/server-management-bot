package tokyo.penguin_syan.server_management_bot.service.ktne;

public class NotEnoughPlayerException extends Exception {

    NotEnoughPlayerException() {
        super("参加中のプレイヤー数が不足しています");
    }

}
