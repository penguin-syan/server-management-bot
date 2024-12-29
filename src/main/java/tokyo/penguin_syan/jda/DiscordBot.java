package tokyo.penguin_syan.jda;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLHandshakeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import tokyo.penguin_syan.PropertiesReader;
import tokyo.penguin_syan.aws.Ec2ControlException;
import tokyo.penguin_syan.aws.Ec2Controller;
import tokyo.penguin_syan.proxmox.ProxmoxControlException;
import tokyo.penguin_syan.proxmox.ProxmoxController;

public class DiscordBot extends ListenerAdapter {
    // TODO: slf4jの導入
    private static Logger logger = LogManager.getLogger();
    private static JDA jda;
    private static Ec2Controller aws;
    private static ProxmoxController prmx;
    private static PropertiesReader propertiesReader;
    private static boolean isProcessing;
    private static boolean isEc2Running, isVmRunning, isFactorioRunning;
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * DiscordBotのコンストラクタ.
     * 
     * 各種設定ファイルを読み込み、Discord用のBotを起動する.
     */
    public static void initial() {
        logger.info("DiscordBot#initial start");

        // 各種設定は外部ファイルから読み込む
        propertiesReader = new PropertiesReader();
        String botToken = propertiesReader.getProperty("discordBotToken");
        String botStatus = propertiesReader.getProperty("discordBotStatus");

        jda = JDABuilder.createLight(botToken, Collections.emptyList())
                .setActivity(Activity.customStatus(botStatus)).addEventListeners(new DiscordBot())
                .build();

        aws = new Ec2Controller();
        prmx = new ProxmoxController();

        isProcessing = false;

        // コマンド変更に対応するため、過去にDiscordへ登録されたコマンドを削除.
        CommandListUpdateAction commands = jda.updateCommands();

        // コマンドの変更に対応するため、動的にコマンド一覧を作成.
        List<SlashCommandData> commandData = new ArrayList<>();
        for (Command command : Command.values()) {
            logger.debug(String.format("Initiated command {%s, %s}", command.getCommand(),
                    command.getDescription()));
            commandData.add(Commands.slash(command.getCommand(), command.getDescription()));
        }
        commands.addCommands(commandData).queue();

        logger.info("DiscordBot#initial end");
    }

    /**
     * スラッシュコマンドのイベントハンドラー.
     * 
     * Discordのチャット上でスラッシュコマンドが実行された際に実行される.
     */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        logger.info("DiscordBot#onSlashCommandInteraction start");

        try {
            // 複数プロセスの同時起動を防止
            if (isProcessing) {
                logger.warn("プロセス実行中");
                throw new RunningProcessException();
            }

            // コマンド別に処理を実行
            if (Command.BOOT.getCommand().equals(event.getName())) {
                logger.info("サーバの起動を開始します [Executed " + event.getMember().getUser() + "]");
                bootProcess();
                event.reply("サーバの起動を開始します").queue();
            } else if (Command.SHUTDOWN.getCommand().equals(event.getName())) {
                logger.info("サーバの停止を開始します [Executed " + event.getMember().getUser() + "]");
                shutdownProcess();
                event.reply("サーバの停止を開始します").queue();
            } else if (Command.DEVELOP.getCommand().equals(event.getName())) {
                // int pid = prmx.execCommand(
                // "{\"command\": [\"systemctl \", \"status\", \"factorio\"]}",
                // ContentType.APPLICATION_JSON);
                // prmx.execStatus(pid);
                event.reply("develop").queue();
            } else if (Command.LICENSE.getCommand().equals(event.getName())) {
                event.reply("""
                        LISENCE1: https://example.com/1
                        LISENCE2: https://example.com/2
                        """).queue();
            } else {
                throw new Exception(
                        String.format("イベントハンドラ上で未定義のコマンドが送信されました（%s）", event.getName()));
            }
        } catch (Ec2ControlException e) {
            // なんか変わるのか？
            event.reply(e.getMessage()).queue();
        } catch (ProxmoxControlException e) {
            // なんか変わるのか？
            event.reply(e.getMessage()).queue();
        } catch (SSLHandshakeException e) {
            logger.error("リクエスト先の証明書が信頼できません", e.getMessage());
            event.reply("リクエストの処理に失敗しました（TLS証明書 認証エラー）").queue();
        } catch (RunningProcessException e) {
            // なんかしないとね
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            event.reply("想定外のエラーが発生しました").queue();
        }

        logger.info("DiscordBot#onSlashCommandInteraction end");
    }

    /**
     * 
     * @throws Exception
     */
    private void bootProcess() throws Exception {
        logger.info("DiscordBot#bootProcess start");

        String ec2InstanceId = propertiesReader.getProperty("awsInstanceId");
        initStatus(false);

        try {
            aws.startInstance(ec2InstanceId);
        } catch (Ec2ControlException e) {
            logger.warn(String.format("インスタンスの操作を中断しました（%s）", e.getMessage()));

            switch (e.getMessage()) {
                case "既に起動済みです":
                    logger.info(e.getMessage());
                    isEc2Running = true;
                    break;
                default:
                    logger.error(e.getMessage());
                    throw new Exception(e);
            }
        }

        try {
            prmx.startVM();
            logger.debug(prmx.isVmRunning());
        } catch (ProxmoxControlException e) {
            logger.warn(String.format("VMの操作を中断しました（%s）", e.getMessage()));

            switch (e.getMessage()) {
                case "既に起動済みです":
                    logger.info(e.getMessage());
                    isVmRunning = true;
                    break;
                default:
                    logger.error(e.getMessage());
                    throw new Exception(e);
            }
        }

        // インスタンスが起動済みの場合はfactorioを起動する
        if (isEc2Running && isVmRunning) {
            // factorioのステータス確認
        } else {
            scheduler.scheduleAtFixedRate(() -> {
                // ec2とvmの起動を確認する
                if (!isEc2Running) {
                    isEc2Running = aws.isAllInstanceCheckPassed(ec2InstanceId);
                }

                if (!isVmRunning) {

                }

            }, 20, 15, TimeUnit.SECONDS);
        }

        // 完全に起動中だった場合はその旨をExceptionで戻す
        isProcessing = false;
        logger.info("DiscordBot#bootProcess end");
    }

    /**
     * 
     * @throws Exception
     */
    private void shutdownProcess() throws Exception {
        String ec2InstanceId = propertiesReader.getProperty("awsInstanceId");

        aws.stopInstance(ec2InstanceId);
        prmx.stopVM();
    }

    /**
     * 
     * @param setStatus
     */
    private void initStatus(boolean setStatus) {
        isProcessing = !setStatus;
        isEc2Running = setStatus;
        isVmRunning = setStatus;
        isFactorioRunning = setStatus;
    }

}
