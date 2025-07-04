package co.grtk.srcprofit.entity;

public enum AssetClass {
    STK("STK"), OPT("OPT"), CASH("CASH");

    private final String code;

    AssetClass(String code) {
        this.code = code;
    }

    public static AssetClass fromCode(String code) {
        for (AssetClass s : values()) {
            if (s.code.equals(code)) return s;
        }
        return STK;
    }

    public String getCode() {
        return code;
    }
}