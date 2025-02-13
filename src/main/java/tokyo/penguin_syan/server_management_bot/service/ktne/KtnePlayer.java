package tokyo.penguin_syan.server_management_bot.service.ktne;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
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
        log.info("KtnePlayer#setOperater start (operater :", operater, ")");
        if (this.defuser != null && this.defuser == operater) {
            log.warn(operater);
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
