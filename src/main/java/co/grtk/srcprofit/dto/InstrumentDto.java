package co.grtk.srcprofit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InstrumentDto {
    @JsonProperty("ST")
    private String ST;
    @JsonProperty("C")
    private String C;
    private long conid;
    private String name;
    private String fullName;
    private String assetClass;
    private String ticker;

    public String getST() {
        return ST;
    }

    public void setST(String ST) {
        this.ST = ST;
    }

    public String getC() {
        return C;
    }

    public void setC(String c) {
        C = c;
    }

    public long getConid() {
        return conid;
    }

    public void setConid(long conid) {
        this.conid = conid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAssetClass() {
        return assetClass;
    }

    public void setAssetClass(String assetClass) {
        this.assetClass = assetClass;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }
}
