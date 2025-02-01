package tokyo.penguin_syan.server_management_bot.service.factorio;

public class RunningProcessException extends Exception {

    public RunningProcessException() {
        super("実行中のプロセスがあります");
    }

}
