package biz.donvi.jakesRTP.api;

public interface JakesRtpAPI {

    @SuppressWarnings("ConstantConditions")
    void loadConfigs();

    @SuppressWarnings("ConstantConditions")
    void loadRandomTeleporter();

    void loadLocationCacheFiller();

    @SuppressWarnings("ConstantConditions")
    void loadMessageMap();

}
