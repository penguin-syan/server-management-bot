package tokyo.penguin_syan.server_management_bot.jda;

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
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import tokyo.penguin_syan.server_management_bot.PropertiesReader;
import tokyo.penguin_syan.server_management_bot.service.factorio.FactorioService;
import tokyo.penguin_syan.server_management_bot.service.ktne.KtneService;

public class DiscordBot extends ListenerAdapter {
    // TODO: slf4jの導入
    private static Logger logger = LogManager.getLogger();
    private static JDA jda;
    private static FactorioService factorioService;
    private static KtneService ktneService;
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

        // コマンド変更に対応するため、過去にDiscordへ登録されたコマンドを削除.
        CommandListUpdateAction commands = jda.updateCommands();

        // コマンドの変更に対応するため、動的にコマンド一覧を作成.
        List<SlashCommandData> commandData = new ArrayList<>();
        if (propertiesReader.getProperty("discord.bot.command.ktne.enable").equals("1")) {
            ktneService = new KtneService();
            commandData = ktneService.initialCommands(commandData);
        }
        if (propertiesReader.getProperty("discord.bot.command.factorio.enable").equals("1")) {
            factorioService = new FactorioService();
            commandData = factorioService.initialCommands(commandData);
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

        boolean needMatchCommand = true;
        if (propertiesReader.getProperty("discord.bot.command.ktne.enable").equals("1")) {
            if (ktneService.onSlashCommandHandler(event)) {
                needMatchCommand = false;
            }
        }
        if (needMatchCommand && propertiesReader.getProperty("discord.bot.command.factorio.enable")
                .equals("1")) {
            if (factorioService.onSlashCommandHandler(event)) {
                needMatchCommand = false;
            }
        }
        if (needMatchCommand) {
            logger.error(String.format("イベントハンドラ上で未定義のコマンドが送信されました（%s）", event.getName()));
            event.reply("想定外のエラーが発生しました。").queue();
        }

        logger.info("DiscordBot#onSlashCommandInteraction end");
    }

}
