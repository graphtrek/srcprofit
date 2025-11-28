package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.AlpacaAssetDto;
import co.grtk.srcprofit.dto.AlpacaContractsResponseDto;
import co.grtk.srcprofit.dto.AlpacaMarketDataDto;
import co.grtk.srcprofit.dto.AlpacaOptionSnapshotDto;
import co.grtk.srcprofit.dto.AlpacaOptionSnapshotsResponseDto;
import co.grtk.srcprofit.dto.AlpacaQuotesDto;
import co.grtk.srcprofit.dto.AlpacaSingleAssetDto;
import co.grtk.srcprofit.entity.InstrumentEntity;
import co.grtk.srcprofit.repository.InstrumentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class AlpacaService {
    private static final Logger log = LoggerFactory.getLogger(AlpacaService.class);

    private final RestClient alpacaRestClient;
    private final RestClient alpacaTradingRestClient;
    private final ObjectMapper mapper;
    private final InstrumentRepository instrumentRepository;

    public AlpacaService(@Qualifier("alpacaRestClient") RestClient alpacaRestClient,
                         @Qualifier("alpacaTradingRestClient") RestClient alpacaTradingRestClient,
                         ObjectMapper mapper,
                         InstrumentRepository instrumentRepository) {
        this.alpacaRestClient = alpacaRestClient;
        this.alpacaTradingRestClient = alpacaTradingRestClient;
        this.mapper = mapper;
        this.instrumentRepository = instrumentRepository;
    }

    public AlpacaQuotesDto getStocksLatestQuotes(String symbolsCsv) {
        AlpacaQuotesDto alpacaQuotesDto = alpacaRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/stocks/quotes/latest")
                        .queryParam("symbols", symbolsCsv)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<AlpacaQuotesDto>() {
                });
        int count = (alpacaQuotesDto != null && alpacaQuotesDto.getQuotes() != null) ? alpacaQuotesDto.getQuotes().size() : 0;
        log.info("getStocksLatestQuotes /v2/stocks/quotes/latest returned {}", count);
        return alpacaQuotesDto;
    }

    public AlpacaQuotesDto getOptionsLatestQuotes(String symbolsCsv) throws JsonProcessingException {
        String json = alpacaRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta1/options/quotes/latest")
                        .queryParam("feed", "indicative")
                        .queryParam("symbols", symbolsCsv)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);


        AlpacaQuotesDto alpacaQuotesDto = mapper.readValue(json, AlpacaQuotesDto.class);

        int count = (alpacaQuotesDto != null && alpacaQuotesDto.getQuotes() != null) ? alpacaQuotesDto.getQuotes().size() : 0;
        log.info("getOptionsLatestQuotes /v1beta1/options/quotes/latest returned {}", count);
        return alpacaQuotesDto;
    }

    public AlpacaMarketDataDto getMarketDataSnapshot(String symbolsCsv) {
        Map<String, AlpacaSingleAssetDto> quotes = alpacaRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/stocks/snapshots")
                        .queryParam("symbols", symbolsCsv)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, AlpacaSingleAssetDto>>() {
                });
        int count = (quotes != null) ? quotes.size() : 0;
        log.info("getMarketData /v2/stocks/snapshots returned {}", count);
        AlpacaMarketDataDto alpacaMarketDataDto = new AlpacaMarketDataDto();
        alpacaMarketDataDto.setQuotes(quotes);
        return alpacaMarketDataDto;
    }

    /**
     * Fetch asset metadata from Alpaca Assets API
     *
     * @param symbol The asset symbol or asset ID (e.g., "AAPL", "BTCUSD")
     * @return AlpacaAssetDto containing asset metadata
     * @throws Exception if the API call fails
     */
    public AlpacaAssetDto getAsset(String symbol) {
        try {
            AlpacaAssetDto assetDto = alpacaTradingRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/assets/{symbol}")
                            .build(symbol))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(AlpacaAssetDto.class);

            log.info("getAsset /v2/assets/{} - Symbol: {}, Tradable: {}, Marginable: {}",
                    symbol, assetDto.getSymbol(), assetDto.getTradable(), assetDto.getMarginable());
            return assetDto;
        } catch (Exception e) {
            log.error("Error fetching asset metadata for symbol: {}", symbol, e);
            throw e;
        }
    }

    /**
     * Save Alpaca asset metadata to InstrumentEntity
     *
     * @param assetDto The asset data from Alpaca API
     * @param instrument The InstrumentEntity to update
     */
    public void saveAssetMetadata(AlpacaAssetDto assetDto, InstrumentEntity instrument) {
        instrument.setAlpacaAssetId(assetDto.getId());
        instrument.setAlpacaTradable(assetDto.getTradable());
        instrument.setAlpacaMarginable(assetDto.getMarginable());
        instrument.setAlpacaShortable(assetDto.getShortable());
        instrument.setAlpacaEasyToBorrow(assetDto.getEasyToBorrow());
        instrument.setAlpacaFractionable(assetDto.getFractionable());
        instrument.setAlpacaMaintenanceMarginRequirement(assetDto.getMaintenanceMarginRequirement());
        instrument.setAlpacaExchange(assetDto.getExchange());
        instrument.setAlpacaStatus(assetDto.getStatus());
        instrument.setAlpacaAssetClass(assetDto.getAssetClass());
        instrument.setAlpacaMetadataUpdatedAt(Instant.now());

        log.info("Saved Alpaca metadata for {}: tradable={}, marginable={}, shortable={}",
                assetDto.getSymbol(), assetDto.getTradable(), assetDto.getMarginable(), assetDto.getShortable());
    }

    /**
     * Refresh stale Alpaca asset metadata for all instruments that haven't been updated
     * within the last 24 hours.
     *
     * This is a batch operation that:
     * 1. Finds all instruments with stale metadata (updated >24 hours ago or never)
     * 2. Fetches fresh metadata from Alpaca API for each instrument
     * 3. Updates the instrument in the database
     * 4. Continues on per-instrument failures (doesn't abort on first error)
     *
     * Performance target: <30 seconds for typical 20-50 instruments
     * API limit: Alpaca Assets API allows 200 requests/minute (well below expected load)
     *
     * @return Number of successfully refreshed instruments
     */
    @Transactional
    public int refreshStaleAssetMetadata() {
        Instant threshold = Instant.now().minus(24, ChronoUnit.HOURS);
        List<InstrumentEntity> staleInstruments = instrumentRepository.findStaleAlpacaAssets(threshold);

        if (staleInstruments.isEmpty()) {
            log.info("No stale Alpaca assets to refresh");
            return 0;
        }

        log.debug("Found {} stale instruments to refresh", staleInstruments.size());

        int refreshedCount = 0;
        for (InstrumentEntity instrument : staleInstruments) {
            try {
                AlpacaAssetDto assetDto = getAsset(instrument.getTicker());
                saveAssetMetadata(assetDto, instrument);
                instrumentRepository.save(instrument);
                refreshedCount++;
                log.info("Refreshed Alpaca metadata for ticker: {}", instrument.getTicker());
            } catch (Exception e) {
                log.warn("Failed to refresh Alpaca metadata for ticker {}: {}",
                        instrument.getTicker(), e.getMessage());
                // Continue with next instrument
            }
        }

        log.debug("Completed Alpaca asset metadata refresh: {} of {} successful",
                refreshedCount, staleInstruments.size());
        return refreshedCount;
    }

    /**
     * Fetch option contracts from Alpaca Options Contracts API.
     *
     * Retrieves available option contracts for a specific underlying symbol
     * with optional filtering by expiration date and strike price ranges.
     *
     * @param underlyingSymbol The underlying stock symbol (e.g., "AAPL")
     * @param expirationDateGte Start of expiration date range (YYYY-MM-DD format)
     * @param expirationDateLte End of expiration date range (YYYY-MM-DD format)
     * @param strikePriceGte Minimum strike price (e.g., "80.00")
     * @param strikePriceLte Maximum strike price (e.g., "110.00")
     * @return AlpacaContractsResponseDto containing list of option contracts
     * @throws Exception if the API call fails
     *
     * @see <a href="https://docs.alpaca.markets/reference/get-options-contracts-1">Alpaca Options Contracts API</a>
     */
    public AlpacaContractsResponseDto getOptionContracts(String underlyingSymbol,
                                                         String expirationDateGte,
                                                         String expirationDateLte,
                                                         String strikePriceGte,
                                                         String strikePriceLte) {
        try {
            String json = alpacaTradingRestClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/v1beta1/options/contracts")
                                .queryParam("underlying_symbols", underlyingSymbol)
                                .queryParam("status", "active");

                        if (expirationDateGte != null && !expirationDateGte.isEmpty()) {
                            builder.queryParam("expiration_date_gte", expirationDateGte);
                        }
                        if (expirationDateLte != null && !expirationDateLte.isEmpty()) {
                            builder.queryParam("expiration_date_lte", expirationDateLte);
                        }
                        if (strikePriceGte != null && !strikePriceGte.isEmpty()) {
                            builder.queryParam("strike_price_gte", strikePriceGte);
                        }
                        if (strikePriceLte != null && !strikePriceLte.isEmpty()) {
                            builder.queryParam("strike_price_lte", strikePriceLte);
                        }

                        return builder.build();
                    })
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            AlpacaContractsResponseDto response = mapper.readValue(json, AlpacaContractsResponseDto.class);

            int count = (response != null && response.getOptionContracts() != null)
                    ? response.getOptionContracts().size()
                    : 0;
            log.info("getOptionContracts /v1beta1/options/contracts returned {} contracts for {}",
                    count, underlyingSymbol);
            return response;
        } catch (Exception e) {
            log.error("Error fetching option contracts for symbol {}: {}", underlyingSymbol, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch option contracts for " + underlyingSymbol, e);
        }
    }

    /**
     * Fetch option snapshots from Alpaca Options Snapshots API (Data API).
     *
     * Retrieves latest trading data (prices, quotes, Greeks) for option contracts
     * of a specific underlying symbol. Uses the Data API endpoint (no broker required).
     *
     * Supports filtering by option type and strike price range at API level.
     * Note: Expiration date filtering is not supported by this endpoint,
     * so filtering is done locally after API response.
     *
     * @param underlyingSymbol The underlying stock symbol (e.g., "AAPL")
     * @param type Option type: "call" or "put"
     * @param strikePriceGte Minimum strike price (e.g., "80.00")
     * @param strikePriceLte Maximum strike price (e.g., "110.00")
     * @return AlpacaOptionSnapshotsResponseDto containing map of option snapshots
     * @throws Exception if the API call fails
     *
     * @see <a href="https://docs.alpaca.markets/reference/optionchain">Alpaca Options Snapshots API</a>
     */
    public AlpacaOptionSnapshotsResponseDto getOptionSnapshots(String underlyingSymbol,
                                                               String type,
                                                               String strikePriceGte,
                                                               String strikePriceLte) {
        try {
            String json = alpacaRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta1/options/snapshots/{symbol}")
                            .queryParam("feed", "indicative")
                            .queryParam("type", type)
                            .queryParam("strike_price_gte", strikePriceGte)
                            .queryParam("strike_price_lte", strikePriceLte)
                            .build(underlyingSymbol))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            AlpacaOptionSnapshotsResponseDto response = mapper.readValue(json, AlpacaOptionSnapshotsResponseDto.class);

            int count = (response != null && response.getSnapshots() != null)
                    ? response.getSnapshots().size()
                    : 0;
            log.info("getOptionSnapshots /v1beta1/options/snapshots/{} returned {} snapshots (type={})",
                    underlyingSymbol, count, type);
            return response;
        } catch (Exception e) {
            log.error("Error fetching option snapshots for symbol {} (type={}): {}",
                    underlyingSymbol, type, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch option snapshots for " + underlyingSymbol, e);
        }
    }

}