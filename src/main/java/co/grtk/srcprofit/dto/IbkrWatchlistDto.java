package co.grtk.srcprofit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class IbkrWatchlistDto {
    private String id;
    private String hash;
    private String name;

    @JsonProperty("readOnly")
    private boolean readOnly;

    private List<IbkrInstrumentDto> instruments;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public List<IbkrInstrumentDto> getInstruments() {
        return instruments;
    }

    public void setInstruments(List<IbkrInstrumentDto> instruments) {
        this.instruments = instruments;
    }
}