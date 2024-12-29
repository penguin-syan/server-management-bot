package tokyo.penguin_syan.jda;

public class RunningProcessException extends Exception {

    public RunningProcessException() {
        super("実行中のプロセスがあります");
    }

}
