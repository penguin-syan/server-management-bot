package tokyo.penguin_syan.server_management_bot.service.factorio;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLHandshakeException;
import org.apache.hc.core5.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import tokyo.penguin_syan.server_management_bot.PropertiesReader;
import tokyo.penguin_syan.server_management_bot.aws.Ec2ControlException;
import tokyo.penguin_syan.server_management_bot.aws.Ec2Controller;
import tokyo.penguin_syan.server_management_bot.proxmox.ProxmoxControlException;
import tokyo.penguin_syan.server_management_bot.proxmox.ProxmoxController;
import tokyo.penguin_syan.server_management_bot.service.Service;

@Slf4j
public class FactorioService implements Service {
    private static Ec2Controller ec2;
    private static ProxmoxController prmx;
    private static PropertiesReader propertiesReader;
    private static ScheduledExecutorService scheduledExecutor;
    private static MessageChannel messageChannel;
    private static String s3Storage;
    private static int scheduledExecutorCounter;
    private static final String SAVEDATA_NOTICE_MESSAGE = """
            接続情報：
              IPv4> `%s`
              IPv6> `%s`
            """;


    /**
     * FactorioServiceのコンストラクタ
     */
    public FactorioService() {
        log.info("FactorioService#<init> start");
        propertiesReader = new PropertiesReader();

        ec2 = new Ec2Controller();
        prmx = new ProxmoxController();
        s3Storage = propertiesReader.getProperty("awsS3BucketName")
                + propertiesReader.getProperty("awsS3SavedataPath");

        log.info("FactorioService#<init> end");
    }


    @Override
    public List<SlashCommandData> initialCommands(List<SlashCommandData> commandData) {
        log.info("FactorioService#initialCommands start");

        for (FactorioCommand command : FactorioCommand.values()) {
            log.debug(String.format("Initiated command {%s, %s}", command.getCommand(),
                    command.getDescription()));
            commandData.add(Commands.slash(command.getCommand(), command.getDescription()));
        }
        for (FactorioCommandWithOption command : FactorioCommandWithOption.values()) {
            log.debug(String.format("Initiated command {%s, %s}", command.getCommand(),
                    command.getDescription()));
            commandData.add(Commands.slash(command.getCommand(), command.getDescription())
                    .addOption(command.getOptionType(), command.getOptionName(),
                            command.getOptionExplanation()));
        }

        log.info("FactorioService#initialCommands end");
        return commandData;
    }


    /**
     * スラッシュコマンドのイベントハンドラー.
     * 
     * Discordのチャット上でスラッシュコマンドが実行された際に実行される.
     */
    @Override
    public boolean onSlashCommandHandler(SlashCommandInteractionEvent event) {
        log.info("FactorioService#onSlashCommandInteraction start");
        String ec2InstanceId = propertiesReader.getProperty("awsInstanceId");
        boolean isCommandHit = false;

        try {
            // コマンド別に処理を実行
            if (FactorioCommand.BOOT.getCommand().equals(event.getName())) {
                log.info("サーバの起動を開始します [Executed " + event.getMember().getUser() + "]");
                isCommandHit = true;

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
                    log.info("DiscordBot#scheduledExecutor start");
                    String ipv4 = ec2.instancePublicIpv4(ec2InstanceId);
                    String ipv6 = ec2.instancePublicIpv6(ec2InstanceId);
                    if (ipv4 != null && ipv6 != null) {
                        log.debug("IPv4 of started instance: " + ipv4);
                        messageChannel
                                .sendMessage(String.format(SAVEDATA_NOTICE_MESSAGE, ipv4, ipv6))
                                .queue();
                        log.info("DiscordBot#scheduledExecutor end");
                        scheduledExecutor.shutdownNow();
                        scheduledExecutor = null;
                    } else if (scheduledExecutorCounter > 10) {
                        log.warn("起動したインスタンスのパブリックIPを取得できませんでした");
                        log.info("DiscordBot#scheduledExecutor end (with error)");
                        scheduledExecutor.shutdownNow();
                        scheduledExecutor = null;
                    } else {
                        scheduledExecutorCounter++;
                        log.info("DiscordBot#scheduledExecutor end (retry later)");
                    }
                }, 5, 8, TimeUnit.SECONDS);
            } else if (FactorioCommand.SHUTDOWN.getCommand().equals(event.getName())) {
                log.info("サーバの停止を開始します [Executed " + event.getMember().getUser() + "]");
                isCommandHit = true;

                String[] command = {"sh", "/usr/local/bin/factorio_stop.sh", s3Storage};
                ec2.stopInstance(ec2InstanceId);
                int pid = prmx.execCommand(command, ContentType.APPLICATION_JSON);
                event.reply("サーバの停止を開始します").queue();
                prmx.execStatus(pid);
            } else if (FactorioCommand.LICENSE.getCommand().equals(event.getName())) {
                log.info("ライセンスを表示します[Executed " + event.getMember().getUser() + "]");
                isCommandHit = true;
                event.reply(
                        """
                                **This**
                                  https://github.com/penguin-syan/server-management-bot/blob/main/LICENSE
                                **Imported OSS Licenses**
                                  AWS SKD for Java2: https://github.com/aws/aws-sdk-java-v2/blob/master/LICENSE.txt
                                  Discord JDA: https://github.com/discord-jda/JDA/blob/master/LICENSE
                                  Apache HttpClient5: https://github.com/apache/httpcomponents-client/blob/master/httpclient5/src/main/java/org/apache/hc/client5/http/classic/HttpClient.java
                                """)
                        .queue();
            }
        } catch (Ec2ControlException e) {
            log.warn(String.format("インスタンスの操作を中断しました（%s）", e.getMessage()));
            event.reply(e.getMessage()).queue();
        } catch (ProxmoxControlException e) {
            log.warn(String.format("VMの操作を中断しました（%s）", e.getMessage()));
            event.reply(e.getMessage()).queue();
            ec2StopWithProxmoxControlException(event, ec2InstanceId);
        } catch (SSLHandshakeException e) {
            log.error("リクエスト先の証明書が信頼できません", e);
            event.reply("リクエストの処理に失敗しました（TLS証明書 認証エラー）").queue();
            ec2StopWithProxmoxControlException(event, ec2InstanceId);
        } catch (Ec2Exception e) {
            log.error(e.getMessage(), e);
            event.reply("想定外のエラーが発生しました\n※短時間の間に複数操作を行ったため、踏み台サーバが操作を受け付けなかった可能性があります").queue();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            event.reply("想定外のエラーが発生しました").queue();
            ec2StopWithProxmoxControlException(event, ec2InstanceId);
        }

        log.info("FactorioService#onSlashCommandInteraction end");
        return isCommandHit;
    }

    /**
     * VM操作時にエラーが出た場合、起動コマンド実行時か判定し、先に起動したEC2を停止する
     * 
     * @param event スラッシュコマンドのイベント
     * @param ec2InstanceId 停止するEC2のインスタンスID
     */
    private void ec2StopWithProxmoxControlException(SlashCommandInteractionEvent event,
            String ec2InstanceId) {
        log.info("DescordBot#ec2StopWithProxmoxControlException start");
        if (FactorioCommand.BOOT.getCommand().equals(event.getName())) {
            try {
                log.info("DiscordBot#ec2StopWithProxmoxControlException stop ec2");
                ec2.stopInstance(ec2InstanceId);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        log.info("DiscordBot#ec2StopWithProxmoxControlException stop");
    }


}
