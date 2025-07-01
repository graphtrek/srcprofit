package co.grtk.srcprofit.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class FlexStatementResponse {
    @JacksonXmlProperty(isAttribute = true, localName = "timestamp")
    private String timestamp;

    @JacksonXmlProperty(localName = "Status")
    private String status;

    @JacksonXmlProperty(localName = "ReferenceCode")
    private String referenceCode;

    @JacksonXmlProperty(localName = "Url")
    private String url;

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "FlexStatementResponse{" +
                "timestamp='" + timestamp + '\'' +
                ", status='" + status + '\'' +
                ", referenceCode='" + referenceCode + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
