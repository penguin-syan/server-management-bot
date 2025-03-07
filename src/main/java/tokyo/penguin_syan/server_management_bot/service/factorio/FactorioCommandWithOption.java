package tokyo.penguin_syan.server_management_bot.service.factorio;

import lombok.Getter;
import net.dv8tion.jda.api.interactions.commands.OptionType;

public enum FactorioCommandWithOption {
    ROLLBACK("rollback", "セーブデータを指定ファイルにロールバックする", OptionType.STRING, "セーブデータ", "セーブデータのS3 URI",
            true);

    @Getter
    private String command;
    @Getter
    private String description;
    @Getter
    private OptionType optionType;
    @Getter
    private String optionName;
    @Getter
    private String optionExplanation;
    @Getter
    private boolean required;

    private FactorioCommandWithOption(String command, String description, OptionType optionType,
            String optionName, String optionExplanation, boolean required) {
        this.command = command;
        this.description = description;
        this.optionType = optionType;
        this.optionName = optionName;
        this.optionExplanation = optionExplanation;
        this.required = required;
    }
}
