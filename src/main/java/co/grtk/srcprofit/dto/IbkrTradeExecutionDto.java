package co.grtk.srcprofit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IbkrTradeExecutionDto {

    @JsonProperty("execution_id")
    private String executionId;

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("supports_tax_opt")
    private String supportsTaxOpt; // "1"/"0" miatt String

    @JsonProperty("side")
    private String side;

    @JsonProperty("order_description")
    private String orderDescription;

    @JsonProperty("trade_time")
    private String tradeTime;

    @JsonProperty("trade_time_r")
    private long tradeTimeR;

    @JsonProperty("size")
    private int size;

    @JsonProperty("price")
    private String price;

    @JsonProperty("order_ref")
    private String orderRef;

    @JsonProperty("submitter")
    private String submitter;

    @JsonProperty("exchange")
    private String exchange;

    @JsonProperty("commission")
    private String commission;

    @JsonProperty("net_amount")
    private double netAmount;

    @JsonProperty("account")
    private String account;

    @JsonProperty("accountCode")
    private String accountCode;

    @JsonProperty("account_allocation_name")
    private String accountAllocationName;

    @JsonProperty("company_name")
    private String companyName;

    @JsonProperty("contract_description_1")
    private String contractDescription1;

    @JsonProperty("sec_type")
    private String secType;

    @JsonProperty("listing_exchange")
    private String listingExchange;

    @JsonProperty("conid")
    private int conid;

    @JsonProperty("conidEx")
    private String conidEx;

    @JsonProperty("clearing_id")
    private String clearingId;

    @JsonProperty("clearing_name")
    private String clearingName;

    @JsonProperty("liquidation_trade")
    private String liquidationTrade; // "1"/"0" miatt String

    @JsonProperty("is_event_trading")
    private String isEventTrading; // "1"/"0" miatt String

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSupportsTaxOpt() {
        return supportsTaxOpt;
    }

    public void setSupportsTaxOpt(String supportsTaxOpt) {
        this.supportsTaxOpt = supportsTaxOpt;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getOrderDescription() {
        return orderDescription;
    }

    public void setOrderDescription(String orderDescription) {
        this.orderDescription = orderDescription;
    }

    public String getTradeTime() {
        return tradeTime;
    }

    public void setTradeTime(String tradeTime) {
        this.tradeTime = tradeTime;
    }

    public long getTradeTimeR() {
        return tradeTimeR;
    }

    public void setTradeTimeR(long tradeTimeR) {
        this.tradeTimeR = tradeTimeR;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getOrderRef() {
        return orderRef;
    }

    public void setOrderRef(String orderRef) {
        this.orderRef = orderRef;
    }

    public String getSubmitter() {
        return submitter;
    }

    public void setSubmitter(String submitter) {
        this.submitter = submitter;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getCommission() {
        return commission;
    }

    public void setCommission(String commission) {
        this.commission = commission;
    }

    public double getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(double netAmount) {
        this.netAmount = netAmount;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public String getAccountAllocationName() {
        return accountAllocationName;
    }

    public void setAccountAllocationName(String accountAllocationName) {
        this.accountAllocationName = accountAllocationName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getContractDescription1() {
        return contractDescription1;
    }

    public void setContractDescription1(String contractDescription1) {
        this.contractDescription1 = contractDescription1;
    }

    public String getSecType() {
        return secType;
    }

    public void setSecType(String secType) {
        this.secType = secType;
    }

    public String getListingExchange() {
        return listingExchange;
    }

    public void setListingExchange(String listingExchange) {
        this.listingExchange = listingExchange;
    }

    public int getConid() {
        return conid;
    }

    public void setConid(int conid) {
        this.conid = conid;
    }

    public String getConidEx() {
        return conidEx;
    }

    public void setConidEx(String conidEx) {
        this.conidEx = conidEx;
    }

    public String getClearingId() {
        return clearingId;
    }

    public void setClearingId(String clearingId) {
        this.clearingId = clearingId;
    }

    public String getClearingName() {
        return clearingName;
    }

    public void setClearingName(String clearingName) {
        this.clearingName = clearingName;
    }

    public String getLiquidationTrade() {
        return liquidationTrade;
    }

    public void setLiquidationTrade(String liquidationTrade) {
        this.liquidationTrade = liquidationTrade;
    }

    public String getIsEventTrading() {
        return isEventTrading;
    }

    public void setIsEventTrading(String isEventTrading) {
        this.isEventTrading = isEventTrading;
    }

    @Override
    public String toString() {
        return "IbkrTradeExecutionDto{" +
                "executionId='" + executionId + '\'' +
                ", symbol='" + symbol + '\'' +
                ", supportsTaxOpt='" + supportsTaxOpt + '\'' +
                ", side='" + side + '\'' +
                ", orderDescription='" + orderDescription + '\'' +
                ", tradeTime='" + tradeTime + '\'' +
                ", tradeTimeR=" + tradeTimeR +
                ", size=" + size +
                ", price='" + price + '\'' +
                ", orderRef='" + orderRef + '\'' +
                ", submitter='" + submitter + '\'' +
                ", exchange='" + exchange + '\'' +
                ", commission='" + commission + '\'' +
                ", netAmount=" + netAmount +
                ", account='" + account + '\'' +
                ", accountCode='" + accountCode + '\'' +
                ", accountAllocationName='" + accountAllocationName + '\'' +
                ", companyName='" + companyName + '\'' +
                ", contractDescription1='" + contractDescription1 + '\'' +
                ", secType='" + secType + '\'' +
                ", listingExchange='" + listingExchange + '\'' +
                ", conid=" + conid +
                ", conidEx='" + conidEx + '\'' +
                ", clearingId='" + clearingId + '\'' +
                ", clearingName='" + clearingName + '\'' +
                ", liquidationTrade='" + liquidationTrade + '\'' +
                ", isEventTrading='" + isEventTrading + '\'' +
                '}';
    }
}