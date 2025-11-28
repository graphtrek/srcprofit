package co.grtk.srcprofit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for Alpaca Options Contracts API.
 *
 * Wraps the list of option contracts returned by the Alpaca API endpoint
 * GET /v1beta1/options/contracts
 *
 * @see <a href="https://docs.alpaca.markets/reference/get-options-contracts-1">Alpaca Options Contracts API</a>
 */
public class AlpacaContractsResponseDto {

    /**
     * List of option contracts returned by the API.
     */
    @JsonProperty("option_contracts")
    private List<AlpacaContractDto> optionContracts;

    /**
     * Pagination token for retrieving the next page of results.
     *
     * Null if no more pages are available.
     */
    @JsonProperty("next_page_token")
    private String nextPageToken;

    // Constructors
    public AlpacaContractsResponseDto() {
    }

    public AlpacaContractsResponseDto(List<AlpacaContractDto> optionContracts) {
        this.optionContracts = optionContracts;
    }

    public AlpacaContractsResponseDto(List<AlpacaContractDto> optionContracts, String nextPageToken) {
        this.optionContracts = optionContracts;
        this.nextPageToken = nextPageToken;
    }

    // Getters and Setters
    public List<AlpacaContractDto> getOptionContracts() {
        return optionContracts;
    }

    public void setOptionContracts(List<AlpacaContractDto> optionContracts) {
        this.optionContracts = optionContracts;
    }

    public String getNextPageToken() {
        return nextPageToken;
    }

    public void setNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
    }

    @Override
    public String toString() {
        return "AlpacaContractsResponseDto{" +
                "optionContracts=" + (optionContracts != null ? optionContracts.size() : 0) +
                ", nextPageToken='" + nextPageToken + '\'' +
                '}';
    }
}
