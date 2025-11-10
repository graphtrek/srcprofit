package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.EarningDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class AlphaVintageService {

    private static final Logger log = LoggerFactory.getLogger(AlphaVintageService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final RestClient alphaVintageRestClient;
    private final Environment environment;

    public AlphaVintageService(RestClient alphaVintageRestClient, Environment environment) {
        this.alphaVintageRestClient = alphaVintageRestClient;
        this.environment = environment;
    }

    public String getEarningsCalendar() {
        return alphaVintageRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("query")
                        .queryParam("apikey", environment.getRequiredProperty("ALPHA_VINTAGE_API_KEY"))
                        .queryParam("function", "EARNINGS_CALENDAR")
                        .queryParam("horizon", "12month")
                        .build())
                .retrieve()
                .body(String.class);
    }

    /**
     * Fetch and parse earnings calendar data from Alpha Vantage API (CSV format).
     * Returns a list of EarningDto objects parsed from the API response.
     *
     * CSV Format (pipe-delimited):
     * symbol|name|reportDate|fiscalDateEnding|estimate|currency
     * AAPL|Apple Inc|2024-01-30|2023-12-31|1.25|USD
     *
     * Handles common API errors:
     * - Rate limiting (max 5 requests/minute for free tier)
     * - Invalid/expired API keys
     * - Error messages in response
     *
     * @return List of EarningDto objects containing parsed earnings calendar data
     *         (empty list if API error or rate limit)
     */
    public List<EarningDto> fetchEarningsCalendar() {
        List<EarningDto> earnings = new ArrayList<>();

        try {
            String response = getEarningsCalendar();

            if (response == null || response.trim().isEmpty()) {
                log.warn("Alpha Vantage API returned empty response");
                return earnings;
            }

            // Check for error messages in response
            if (response.contains("Error Message") || response.contains("Note")) {
                log.warn("Alpha Vantage API error response: {}", response.substring(0, Math.min(200, response.length())));
                return earnings;
            }

            // Parse CSV response (pipe-delimited)
            String[] lines = response.trim().split("\n");

            if (lines.length < 2) {
                log.warn("Alpha Vantage API returned no data rows (only header or empty)");
                return earnings;
            }

            // First line is header, skip it
            String headerLine = lines[0];
            log.debug("Alpha Vantage CSV header: {}", headerLine);

            // Parse data rows starting from line 1
            for (int i = 1; i < lines.length; i++) {
                try {
                    String line = lines[i].trim();
                    if (line.isEmpty()) {
                        continue;
                    }

                    String[] fields = line.split("\\|");

                    // CSV format: symbol|name|reportDate|fiscalDateEnding|estimate|currency
                    if (fields.length >= 6) {
                        EarningDto earningDto = new EarningDto();
                        earningDto.setSymbol(fields[0].trim());
                        earningDto.setName(fields[1].trim());
                        earningDto.setReportDate(LocalDate.parse(fields[2].trim(), DATE_FORMATTER));
                        earningDto.setFiscalDateEnding(LocalDate.parse(fields[3].trim(), DATE_FORMATTER));
                        earningDto.setEstimate(fields[4].trim().isEmpty() ? "" : fields[4].trim());
                        earningDto.setCurrency(fields[5].trim().isEmpty() ? "USD" : fields[5].trim());

                        earnings.add(earningDto);
                    } else {
                        log.debug("Skipping row with insufficient fields (expected 6, got {}): {}", fields.length, line);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse earning record at line {}: {} - Error: {}",
                             i + 1, lines[i], e.getMessage());
                }
            }

            log.debug("Fetched {} earnings records from Alpha Vantage API", earnings.size());

        } catch (Exception e) {
            log.error("Error fetching earnings calendar from Alpha Vantage: {}", e.getMessage(), e);
        }

        return earnings;
    }

}
