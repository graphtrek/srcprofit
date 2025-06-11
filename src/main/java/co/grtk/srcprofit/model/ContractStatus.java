package co.grtk.srcprofit.model;

public enum ContractStatus {
    OPEN("O"), CLOSE("C");

    private final String code;

    ContractStatus(String code) { this.code = code; }

    public String getCode() { return code; }

    public static ContractStatus fromCode(String code) {
        for (ContractStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return OPEN;
    }
}
