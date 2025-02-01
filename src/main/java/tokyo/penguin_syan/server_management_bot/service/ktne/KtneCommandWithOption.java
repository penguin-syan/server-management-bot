package tokyo.penguin_syan.server_management_bot.service.ktne;

import lombok.Getter;
import net.dv8tion.jda.api.interactions.commands.OptionType;

enum KtneCommandWithOption {
    JOIN("join", "KTNEルーレットへの参加", OptionType.BOOLEAN, "解体者役", "爆弾解体役の可否", false);

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

    private KtneCommandWithOption(String command, String description, OptionType optionType,
            String optionName, String optionExplanation, boolean required) {
        this.command = command;
        this.description = description;
        this.optionType = optionType;
        this.optionName = optionName;
        this.optionExplanation = optionExplanation;
        this.required = required;
    }
}
