package tokyo.penguin_syan.jda;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

public class DiscordBot extends ListenerAdapter {
    // TODO: slf4jの導入
    private static Logger logger = LogManager.getLogger();
    private static JDA jda = null;
    private static Ec2Controller aws = null;
    private static PropertiesReader propertiesReader;

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
        String ec2InstanceId = propertiesReader.getProperty("awsInstanceId");

        try {
            if (Command.BOOT.getCommand().equals(event.getName())) {
                logger.info("サーバの起動を開始します [Executed " + event.getMember().getUser() + "]");
                aws.startInstance(ec2InstanceId);
                event.reply("サーバの起動を開始します").queue();
            } else if (Command.SHUTDOWN.getCommand().equals(event.getName())) {
                logger.info("サーバの停止を開始します [Executed " + event.getMember().getUser() + "]");
                aws.stopInstance(ec2InstanceId);
                event.reply("サーバの停止を開始します").queue();
            } else {
                throw new Exception(
                        String.format("イベントハンドラ上で未定義のコマンドが送信されました（%s）", event.getName()));
            }
        } catch (Ec2ControlException e) {
            logger.warn(String.format("インスタンスの操作を中断しました（%s）", e.getMessage()));
            event.reply(e.getMessage()).queue();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            event.reply("想定外のエラーが発生しました").queue();
        }

        logger.info("DiscordBot#onSlashCommandInteraction end");
    }

}
