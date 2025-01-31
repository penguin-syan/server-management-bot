package tokyo.penguin_syan.server_management_bot.service;

import java.util.List;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public interface Service {

    /**
     * DiscordBotにおけるコマンドの初期化
     * 
     * @param commandDatas コマンドの一覧追加先
     * @return 一覧追加後のコマンドリスト
     * @see tokyo.penguin_syan.server_management_bot.jda.DiscordBot
     */
    public List<SlashCommandData> initialCommands(List<SlashCommandData> commandDatas);

    /**
     * スラッシュコマンドの処理を行うイベントハンドラ
     * 
     * @param event DiscordBotが受信したスラッシュコマンドイベント
     * @return イベントハンドラ内での処理の有無
     * @see tokyo.penguin_syan.server_management_bot.jda.DiscordBot
     */
    public boolean onSlashCommandHandler(SlashCommandInteractionEvent event);

}
