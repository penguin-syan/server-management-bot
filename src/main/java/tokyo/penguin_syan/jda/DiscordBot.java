package tokyo.penguin_syan.jda;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLHandshakeException;
import org.apache.hc.core5.http.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
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
    private static Ec2Controller ec2;
    private static ProxmoxController prmx;
    private static PropertiesReader propertiesReader;
    private static ScheduledExecutorService scheduledExecutor;
    private static MessageChannel messageChannel;
    private static String s3Storage;
    private static boolean isDevelopMode;
    private static int scheduledExecutorCounter;
    private static final String savedataNoticeMessage = """
            接続情報：
              IPv4> `%s`
              IPv6> `%s`
            """;

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

        ec2 = new Ec2Controller();
        prmx = new ProxmoxController();
        s3Storage = propertiesReader.getProperty("awsS3BucketName")
                + propertiesReader.getProperty("awsS3SavedataPath");

        isDevelopMode = "1".equals(propertiesReader.getProperty("developMode")) ? true : false;

        // コマンド変更に対応するため、過去にDiscordへ登録されたコマンドを削除.
        CommandListUpdateAction commands = jda.updateCommands();

        // コマンドの変更に対応するため、動的にコマンド一覧を作成.
        List<SlashCommandData> commandData = new ArrayList<>();
        for (Command command : Command.values()) {
            if ("develop".equals(command.getCommand()) && !isDevelopMode) {
                continue;
            }
            logger.debug(String.format("Initiated command {%s, %s}", command.getCommand(),
                    command.getDescription()));
            commandData.add(Commands.slash(command.getCommand(), command.getDescription()));
        }
        for (CommandWithOption command : CommandWithOption.values()) {
            logger.debug(String.format("Initiated command {%s, %s}", command.getCommand(),
                    command.getDescription()));
            commandData.add(Commands.slash(command.getCommand(), command.getDescription())
                    .addOption(command.getOptionType(), command.getOptionName(),
                            command.getOptionExplanation()));
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
        String ec2InstanceId = propertiesReader.getProperty("awsInstanceId");

        try {
            // コマンド別に処理を実行
            if (Command.BOOT.getCommand().equals(event.getName())) {
                logger.info("サーバの起動を開始します [Executed " + event.getMember().getUser() + "]");
                String[] command = {"sh", "/usr/local/bin/factorio_start.sh"};
                ec2.startInstance(ec2InstanceId);
                prmx.execCommand(command, ContentType.APPLICATION_JSON);
                event.reply("サーバの起動を開始します").queue();

                // インスタンス起動直後はPublicIPが割り振られないため、別スレッドで継続取得
                scheduledExecutorCounter = 0;
                messageChannel = event.getMessageChannel();
                // Poolしているスレッド数が不足するので、都度インスタンス化しなおす
                scheduledExecutor = Executors.newScheduledThreadPool(1);
                scheduledExecutor.scheduleAtFixedRate(() -> {
                    logger.info("DiscordBot#scheduledExecutor start");
                    String ipv4 = ec2.instancePublicIpv4(ec2InstanceId);
                    String ipv6 = ec2.instancePublicIpv6(ec2InstanceId);
                    if (ipv4 != null && ipv6 != null) {
                        logger.debug("IPv4 of started instance: " + ipv4);
                        messageChannel.sendMessage(String.format(savedataNoticeMessage, ipv4, ipv6))
                                .queue();
                        logger.info("DiscordBot#scheduledExecutor end");
                        scheduledExecutor.shutdownNow();
                        scheduledExecutor = null;
                    } else if (scheduledExecutorCounter > 10) {
                        logger.warn("起動したインスタンスのパブリックIPを取得できませんでした");
                        logger.info("DiscordBot#scheduledExecutor end (with error)");
                        scheduledExecutor.shutdownNow();
                        scheduledExecutor = null;
                    } else {
                        scheduledExecutorCounter++;
                        logger.info("DiscordBot#scheduledExecutor end (retry later)");
                    }
                }, 5, 8, TimeUnit.SECONDS);
            } else if (Command.SHUTDOWN.getCommand().equals(event.getName())) {
                logger.info("サーバの停止を開始します [Executed " + event.getMember().getUser() + "]");
                String[] command = {"sh", "/usr/local/bin/factorio_stop.sh", s3Storage};
                ec2.stopInstance(ec2InstanceId);
                int pid = prmx.execCommand(command, ContentType.APPLICATION_JSON);
                event.reply("サーバの停止を開始します").queue();
                prmx.execStatus(pid);
            } else if (Command.DEVELOP.getCommand().equals(event.getName())) {
                String[] command = {"systemctl", "status", "factorio"};
                int pid = prmx.execCommand(command, ContentType.APPLICATION_JSON);
                prmx.execStatus(pid);
                event.reply("develop").queue();
            } else if (Command.LICENSE.getCommand().equals(event.getName())) {
                event.reply(
                        """
                                **This**
                                  https://github.com/penguin-syan/server-management-bot/blob/main/pom.xml
                                **Imported OSS Licenses**
                                  AWS SKD for Java2: https://github.com/aws/aws-sdk-java-v2/blob/master/LICENSE.txt
                                  Discord JDA: https://github.com/discord-jda/JDA/blob/master/LICENSE
                                  Apache HttpClient5: https://github.com/apache/httpcomponents-client/blob/master/httpclient5/src/main/java/org/apache/hc/client5/http/classic/HttpClient.java
                                """)
                        .queue();
            } else {
                throw new Exception(
                        String.format("イベントハンドラ上で未定義のコマンドが送信されました（%s）", event.getName()));
            }
        } catch (Ec2ControlException e) {
            logger.warn(String.format("インスタンスの操作を中断しました（%s）", e.getMessage()));
            event.reply(e.getMessage()).queue();
        } catch (ProxmoxControlException e) {
            logger.warn(String.format("VMの操作を中断しました（%s）", e.getMessage()));
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

}
