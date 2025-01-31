package tokyo.penguin_syan.server_management_bot.service.ktne;

import lombok.Getter;

public class KtnePlayer {
    @Getter
    private String operater;

    @Getter
    private String defuser;

    /**
     * 指示役のユーザを設定する
     * 
     * @param operater 指示役のユーザ名
     * @throws DuplicateSelectedPlayerException
     */
    protected void setOperater(String operater) throws DuplicateSelectedPlayerException {
        if (this.defuser != null && this.defuser == operater) {
            throw new DuplicateSelectedPlayerException();
        } else {
            this.operater = operater;
        }
    }

    /**
     * 解体役のユーザを設定する
     * 
     * @param defuser 解体役のユーザ名
     * @throws DuplicateSelectedPlayerException
     */
    protected void setDefuser(String defuser) throws DuplicateSelectedPlayerException {
        if (this.operater != null && this.operater == defuser) {
            throw new DuplicateSelectedPlayerException();
        } else {
            this.defuser = defuser;
        }
    }
}
