package co.grtk.srcprofit.model;

public enum OptionType {
    PUT("PUT"), CALL("CALL");

    private final String code;

    OptionType(String code) { this.code = code; }

    public String getCode() { return code; }

    public static OptionType fromCode(String code) {
        for (OptionType s : values()) {
            if (s.code.equals(code)) return s;
        }
        return PUT;
    }
}
