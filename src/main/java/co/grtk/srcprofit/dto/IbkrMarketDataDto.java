package co.grtk.srcprofit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IbkrMarketDataDto {
    @JsonProperty("conid")
    private Long conid;

    @JsonProperty("55")
    private String ticker;

    @JsonProperty("31")
    private String priceStr;

    @JsonProperty("_updated")
    private Long updated;

    @JsonProperty("82")
    private Double change;

    @JsonProperty("83")
    private Double changePercent;

    @JsonProperty("7051")
    private String companyName;

    public Long getConid() {
        return conid;
    }

    public void setConid(Long conid) {
        this.conid = conid;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getPriceStr() {
        return priceStr;
    }

    public void setPriceStr(String priceStr) {
        this.priceStr = priceStr;
    }

    public Long getUpdated() {
        return updated;
    }

    public void setUpdated(Long updated) {
        this.updated = updated;
    }

    public Double getChange() {
        return change;
    }

    public void setChange(Double change) {
        this.change = change;
    }

    public Double getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(Double changePercent) {
        this.changePercent = changePercent;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    @Override
    public String toString() {
        return "IbkrMarketDataDto{" +
                "conid=" + conid +
                ", ticker='" + ticker + '\'' +
                ", priceStr='" + priceStr + '\'' +
                ", updated=" + updated +
                '}';
    }
}
