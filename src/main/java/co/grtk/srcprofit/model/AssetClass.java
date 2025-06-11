package co.grtk.srcprofit.model;

public enum AssetClass {
    STOCK("STK"), OPTION("OPT"),  CASH("CASH");

    private final String code;

    AssetClass(String code) { this.code = code; }

    public String getCode() { return code; }

    public static AssetClass fromCode(String code) {
        for (AssetClass s : values()) {
            if (s.code.equals(code)) return s;
        }
        return STOCK;
    }
}