package co.grtk.srcprofit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class AlpacaContractDto {
    private String id;
    private String symbol;
    private String name;
    private String status;
    private boolean tradable;

    @JsonProperty("expiration_date")
    private String expirationDate;

    @JsonProperty("root_symbol")
    private String rootSymbol;

    @JsonProperty("underlying_symbol")
    private String underlyingSymbol;

    @JsonProperty("underlying_asset_id")
    private String underlyingAssetId;

    private String type;
    private String style;

    @JsonProperty("strike_price")
    private String strikePrice;

    private String multiplier;
    private String size;

    @JsonProperty("open_interest")
    private String openInterest;

    @JsonProperty("open_interest_date")
    private String openInterestDate;

    @JsonProperty("close_price")
    private String closePrice;

    @JsonProperty("close_price_date")
    private String closePriceDate;

    private List<DeliverableDto> deliverables;

    private boolean ppind;

    // Getters és setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isTradable() { return tradable; }
    public void setTradable(boolean tradable) { this.tradable = tradable; }

    public String getExpirationDate() { return expirationDate; }
    public void setExpirationDate(String expirationDate) { this.expirationDate = expirationDate; }

    public String getRootSymbol() { return rootSymbol; }
    public void setRootSymbol(String rootSymbol) { this.rootSymbol = rootSymbol; }

    public String getUnderlyingSymbol() { return underlyingSymbol; }
    public void setUnderlyingSymbol(String underlyingSymbol) { this.underlyingSymbol = underlyingSymbol; }

    public String getUnderlyingAssetId() { return underlyingAssetId; }
    public void setUnderlyingAssetId(String underlyingAssetId) { this.underlyingAssetId = underlyingAssetId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }

    public String getStrikePrice() { return strikePrice; }
    public void setStrikePrice(String strikePrice) { this.strikePrice = strikePrice; }

    public String getMultiplier() { return multiplier; }
    public void setMultiplier(String multiplier) { this.multiplier = multiplier; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public String getOpenInterest() { return openInterest; }
    public void setOpenInterest(String openInterest) { this.openInterest = openInterest; }

    public String getOpenInterestDate() { return openInterestDate; }
    public void setOpenInterestDate(String openInterestDate) { this.openInterestDate = openInterestDate; }

    public String getClosePrice() { return closePrice; }
    public void setClosePrice(String closePrice) { this.closePrice = closePrice; }

    public String getClosePriceDate() { return closePriceDate; }
    public void setClosePriceDate(String closePriceDate) { this.closePriceDate = closePriceDate; }

    public List<DeliverableDto> getDeliverables() { return deliverables; }
    public void setDeliverables(List<DeliverableDto> deliverables) { this.deliverables = deliverables; }

    public boolean isPpind() { return ppind; }
    public void setPpind(boolean ppind) { this.ppind = ppind; }

    // Nested class a deliverables-hez
    public static class DeliverableDto {
        private String type;
        private String symbol;

        @JsonProperty("asset_id")
        private String assetId;

        private String amount;

        @JsonProperty("allocation_percentage")
        private String allocationPercentage;

        @JsonProperty("settlement_type")
        private String settlementType;

        @JsonProperty("settlement_method")
        private String settlementMethod;

        @JsonProperty("delayed_settlement")
        private boolean delayedSettlement;

        // Getters és setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }

        public String getAssetId() { return assetId; }
        public void setAssetId(String assetId) { this.assetId = assetId; }

        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }

        public String getAllocationPercentage() { return allocationPercentage; }
        public void setAllocationPercentage(String allocationPercentage) { this.allocationPercentage = allocationPercentage; }

        public String getSettlementType() { return settlementType; }
        public void setSettlementType(String settlementType) { this.settlementType = settlementType; }

        public String getSettlementMethod() { return settlementMethod; }
        public void setSettlementMethod(String settlementMethod) { this.settlementMethod = settlementMethod; }

        public boolean isDelayedSettlement() { return delayedSettlement; }
        public void setDelayedSettlement(boolean delayedSettlement) { this.delayedSettlement = delayedSettlement; }
    }
}
