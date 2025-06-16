package co.grtk.srcprofit.entity;

public enum OptionStatus {
    PENDING("PENDING"), OPEN("OPEN"), CLOSED("CLOSED");

    private final String code;

    OptionStatus(String code) { this.code = code; }

    public String getCode() { return code; }

    public static OptionStatus fromCode(String code) {
        for (OptionStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return PENDING;
    }
}
