package tokyo.penguin_syan.server_management_bot.service.ktne;

public class MaxRetryAttemptsException extends Exception {

    MaxRetryAttemptsException() {
        super("既定の試行回数以内にプレイヤーが決まりませんでした");
    }
}
