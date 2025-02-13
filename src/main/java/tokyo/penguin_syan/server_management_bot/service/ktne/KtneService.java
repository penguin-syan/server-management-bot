package tokyo.penguin_syan.server_management_bot.service.ktne;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import lombok.extern.log4j.Log4j2;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import tokyo.penguin_syan.server_management_bot.service.Service;

@Log4j2
public class KtneService implements Service {
    private ArrayList<String> operatorList = new ArrayList<>();
    private ArrayList<String> defuserList = new ArrayList<>();
    private HashMap<String, String> playerIdList = new HashMap<>();
    private static final String PLAYER_NOTICE_TEXT = """
            プレイヤー：
              指示役> <@%s>
              解体役> <@%s>""";

    /**
     * KtneServiceのコンストラクタ
     */
    public KtneService() {
        resetAllLists();
    }


    @Override
    public List<SlashCommandData> initialCommands(List<SlashCommandData> commandData) {
        log.info("KtneService#initialCommands start");

        for (KtneCommand command : KtneCommand.values()) {
            log.debug(String.format("Initiated command {%s, %s}", command.getCommand(),
                    command.getDescription()));
            commandData.add(Commands.slash(command.getCommand(), command.getDescription()));
        }
        for (KtneCommandWithOption command : KtneCommandWithOption.values()) {
            log.debug(String.format("Initiated command {%s, %s}", command.getCommand(),
                    command.getDescription()));
            commandData.add(Commands.slash(command.getCommand(), command.getDescription())
                    .addOption(command.getOptionType(), command.getOptionName(),
                            command.getOptionExplanation()));
        }

        log.info("KtneService#initialCommands end");
        return commandData;
    }


    @Override
    public boolean onSlashCommandHandler(SlashCommandInteractionEvent event) {
        log.info("KtneService#onSlashCommandHandler start");
        boolean isCommandHit = false;
        User eventExecutedUser = event.getMember().getUser();
        String playerName = eventExecutedUser.getName();
        String playerId = eventExecutedUser.getId();

        if (KtneCommand.INITIAL.getCommand().equals(event.getName())) {
            log.info("KTNEルーレットを初期化します。 [Executed " + eventExecutedUser + "]");
            isCommandHit = true;

            resetAllLists();
            event.reply("@everyone\nKTNEルーレットを初期化しました。\n参加者はjoinコマンドを宣言してください").queue();
        } else if (KtneCommand.LEAVE.getCommand().equals(event.getName())) {
            log.info("KTNEルーレットからの参加を削除します。 [Executed " + eventExecutedUser + "]");
            isCommandHit = true;

            deletePlayer(playerName);
            event.reply("KTNEルーレットの対象から削除しました。").queue();
        } else if (KtneCommand.ROULET.getCommand().equals(event.getName())) {
            log.info("KTNEルーレットで抽選を行います。 [Executed " + eventExecutedUser + "]");
            isCommandHit = true;

            KtnePlayer selectedPlayer;
            try {
                selectedPlayer = roulet();
            } catch (NotEnoughPlayerException | MaxRetryAttemptsException e) {
                log.warn(e.getMessage());
                event.reply(e.getMessage()).queue();
                return isCommandHit;
            }

            event.reply(String.format(PLAYER_NOTICE_TEXT,
                    playerIdList.get(selectedPlayer.getOperater()),
                    playerIdList.get(selectedPlayer.getDefuser()))).queue();;
        } else if (KtneCommandWithOption.JOIN.getCommand().equals(event.getName())) {
            log.info("KTNEルーレットへ追加します。 [Executed " + eventExecutedUser + "]");
            isCommandHit = true;

            boolean defuserFlg;
            if (Objects.isNull(event.getOption(KtneCommandWithOption.JOIN.getOptionName()))) {
                defuserFlg = true;
            } else {
                defuserFlg =
                        event.getOption(KtneCommandWithOption.JOIN.getOptionName()).getAsBoolean();
            }

            try {
                if (defuserFlg) {
                    this.addPlayer(playerName, playerId);
                    event.reply("KTNEルーレットへ追加しました。").queue();
                } else {
                    this.addOperator(playerName, playerId);
                    event.reply("KTNEルーレット（指示役のみ）へ追加しました。").queue();
                }
            } catch (DuplicateNewPlayerException e) {
                log.warn(e.getMessage());
                event.reply(e.getMessage()).queue();
            }
        }

        log.info("KtneService#onSlashCommandHandler end");
        return isCommandHit;
    }


    /**
     * 登録済みプレイヤーをすべて削除する
     */
    public void resetAllLists() {
        log.info("KtneService#resetAllLists start");

        this.operatorList.clear();
        this.defuserList.clear();
        this.playerIdList.clear();

        log.info("KtneService#resetAllLists end");
    }


    /**
     * 指示役のプレイヤーを追加する
     * 
     * @param playerName 指示役のプレイヤー名
     * @throws DuplicateNewPlayerException
     */
    public void addOperator(String playerName, String playerId) throws DuplicateNewPlayerException {
        log.info("KtneService#addOperator start");

        // プレイヤー名の重複を防止するため、登録済みのプレイヤー名はエラーとする
        if (this.operatorList.indexOf(playerName) != -1) {
            throw new DuplicateNewPlayerException(playerName);
        }
        this.operatorList.add(playerName);

        this.playerIdList.put(playerName, playerId);

        log.info("KtneService#addOperator end");
    }


    /**
     * 解体役のプレイヤーを追加する
     * 
     * @param playerName 解体役のプレイヤー名
     * @throws DuplicateNewPlayerException
     */
    public void addDefuser(String playerName) throws DuplicateNewPlayerException {
        log.info("KtneService#addOperator start");

        // プレイヤー名の重複を防止するため、登録済みのプレイヤー名はエラーとする
        if (this.defuserList.indexOf(playerName) != -1) {
            throw new DuplicateNewPlayerException(playerName);
        }
        this.defuserList.add(playerName);

        log.info("KtneService#addOperator end");
    }

    /**
     * 指示役・解体役の両方にプレイヤーを追加する
     * 
     * @param playerName 指示役・解体役の両方を行うプレイヤー名
     * @throws DuplicateNewPlayerException
     */
    public void addPlayer(String playerName, String playerId) throws DuplicateNewPlayerException {
        log.info("KtneService#addPlayer start");

        this.addOperator(playerName, playerId);
        this.addDefuser(playerName);

        log.info("KtneService#addPlayer end");
    }

    /**
     * 指示役・解体役の両方からプレイヤーを削除する
     * 
     * @param playerName 指示役・解体役から削除するプレイヤー名
     */
    public void deletePlayer(String playerName) {
        log.info("KtneService#deletePlayer start");

        if (operatorList.indexOf(playerName) != -1) {
            operatorList.remove(playerName);
        }
        if (defuserList.indexOf(playerName) != -1) {
            defuserList.remove(playerName);
        }

        this.playerIdList.remove(playerName);

        log.info("KtneService#deletePlayer end");
    }

    /**
     * 指示役・解体役のプレイヤーをランダムで選出する
     * 
     * @return 指示役・解体役のプレイヤー
     * @throws NotEnoughPlayerException
     * @throws MaxRetryAttemptsException
     */
    public KtnePlayer roulet() throws NotEnoughPlayerException, MaxRetryAttemptsException {
        log.info("KtneService#roulet start");

        // 登録済み人数のチェックと、だめならExceptionを返すようにする
        int operatorNum = operatorList.size();
        int defuserNum = defuserList.size();
        log.info(String.format("指示役: %s人 / 解体役: %s人", operatorNum, defuserNum));
        if (operatorNum < 2 || defuserNum < 1) {
            throw new NotEnoughPlayerException();
        }

        KtnePlayer rouletResult = new KtnePlayer();

        Random random = new Random();
        try {
            int operatorRnd = random.nextInt(operatorNum);
            log.debug("指示役: " + operatorList.get(operatorRnd));
            rouletResult.setOperater(operatorList.get(operatorRnd));
        } catch (DuplicateSelectedPlayerException e) { // ぶっちゃけ絶対Exceptionにならない
            log.error(e.getMessage());
        }

        // ユーザ数等に起因する無限ループを防ぐため、最大試行回数を10回とする
        int defuserRouletCount = 0;
        int maxRoulet = 10;
        boolean isPlayerDuplicate = true;
        do {
            try {
                int defuserRnd = random.nextInt(defuserNum);
                log.debug("解体役: " + defuserList.get(defuserRnd));
                rouletResult.setDefuser(defuserList.get(defuserRnd));
                isPlayerDuplicate = false;
            } catch (DuplicateSelectedPlayerException e) {
                defuserRouletCount++;
                log.warn(e.getMessage(defuserRouletCount));
            }
        } while (isPlayerDuplicate && defuserRouletCount < maxRoulet);

        if (isPlayerDuplicate) {
            log.error("規定の試行回数ではルーレット処理が完了しませんでした");
            throw new MaxRetryAttemptsException();
        }


        log.info("KtneService#roulet end");
        return rouletResult;
    }

}
