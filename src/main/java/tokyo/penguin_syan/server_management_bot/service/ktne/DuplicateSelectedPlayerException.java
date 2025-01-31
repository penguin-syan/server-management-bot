package tokyo.penguin_syan.server_management_bot.service.ktne;

public class DuplicateSelectedPlayerException extends Exception {

    DuplicateSelectedPlayerException() {
        super("指示役と解体役のプレイヤー名が同一です");
    }

    public String getMessage(int tryNum) {
        return String.format("%s [試行回数: %d]", super.getMessage(), tryNum);
    }

}
