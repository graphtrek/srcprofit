package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.AlpacaAssetDto;
import co.grtk.srcprofit.dto.InstrumentDto;
import co.grtk.srcprofit.dto.PositionDto;
import co.grtk.srcprofit.entity.InstrumentEntity;
import co.grtk.srcprofit.mapper.PositionMapper;
import co.grtk.srcprofit.repository.InstrumentRepository;
import co.grtk.srcprofit.service.AlpacaService;
import co.grtk.srcprofit.service.InstrumentService;
import co.grtk.srcprofit.service.OptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static co.grtk.srcprofit.mapper.MapperUtils.round2Digits;
import static co.grtk.srcprofit.mapper.MapperUtils.toLocalDate;

@Controller
public class PositionController {

    private static final Logger log = LoggerFactory.getLogger(PositionController.class);
    private static final String POSITION_FORM_PATH = "position-form_jte";
    private static final String MODEL_ATTRIBUTE_DTO = "positionDto";
    private static final String MODEL_ATTRIBUTE_OPTION_OPEN = "openOptions";
    private static final String MODEL_ATTRIBUTE_OPTION_HISTORY = "optionHistory";
    private static final String MODEL_ATTRIBUTE_SUCCESS = "success";


    private final OptionService optionService;
    private final InstrumentService instrumentService;
    private final AlpacaService alpacaService;
    private final InstrumentRepository instrumentRepository;

    public PositionController(
            OptionService optionService,
            InstrumentService instrumentService,
            AlpacaService alpacaService,
            InstrumentRepository instrumentRepository) {
        this.optionService = optionService;
        this.instrumentService = instrumentService;
        this.alpacaService = alpacaService;
        this.instrumentRepository = instrumentRepository;
    }

    @GetMapping("/calculatePosition")
    public String getPositionForm(Model model) {
        model.addAttribute(MODEL_ATTRIBUTE_SUCCESS, null);
        PositionDto positionDto = new PositionDto();
        positionDto.setTradeDate(LocalDate.now());
        model.addAttribute(MODEL_ATTRIBUTE_DTO, positionDto);
        return POSITION_FORM_PATH;
    }

    @PostMapping(path = "/calculatePosition", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String calculatePosition(@RequestBody MultiValueMap<String, String> formData, Model model) {
        log.info("calculatePosition formData {}", formData);
        PositionDto positionDto = PositionMapper.mapFromData(formData);

        // Fetch live market data from Alpaca
        getMarketValue(positionDto);

        // Manual recalculation: use ONLY form values (ISSUE-026)
        optionService.calculateSinglePosition(positionDto);

        // Load related data for template
        fillPositionFormData(positionDto, model);
        model.addAttribute(MODEL_ATTRIBUTE_DTO, positionDto);

        return POSITION_FORM_PATH;
    }

    @GetMapping(path = "/getPosition/{ticker}")
    public String getPosition(@PathVariable String ticker, Model model) {
        log.info("getPosition ticker {}", ticker);
        PositionDto positionDto = new PositionDto();
        positionDto.setTicker(ticker);
        getMarketValue(positionDto);
        loadPositionData(positionDto, model);
        return POSITION_FORM_PATH;
    }

    /**
     * Load related data for position form (open/closed positions, instrument info)
     * Called by both GET /getPosition/{ticker} and POST /calculatePosition
     *
     * This method loads database positions and calculates aggregated metrics
     * (Used when viewing existing ticker positions, not for what-if analysis)
     */
    private void loadPositionData(PositionDto positionDto, Model model) {
        List<PositionDto> optionHistory = optionService.getClosedOptionsByTicker(positionDto.getTicker());
        model.addAttribute(MODEL_ATTRIBUTE_OPTION_HISTORY, optionHistory);

        List<PositionDto> openOptions = optionService.getOpenOptionsByTicker(positionDto.getTicker());
        model.addAttribute(MODEL_ATTRIBUTE_OPTION_OPEN, openOptions);
        InstrumentDto instrumentDto = instrumentService.loadInstrumentByTicker(positionDto.getTicker());
        Optional.ofNullable(instrumentDto).ifPresent(instrumentDto1 ->
                {
                    positionDto.setEarningDate(instrumentDto.getEarningDate());
                });

        optionService.calculatePosition(positionDto, openOptions, optionHistory);
        model.addAttribute(MODEL_ATTRIBUTE_DTO, positionDto);
    }

    /**
     * Load template data without calculating aggregated metrics.
     * Used by POST /calculatePosition for what-if analysis (ISSUE-026).
     *
     * Loads open/closed positions for display but does NOT recalculate metrics
     * (metrics already calculated by calculateSinglePosition).
     */
    private void fillPositionFormData(PositionDto positionDto, Model model) {
        List<PositionDto> optionHistory = optionService.getClosedOptionsByTicker(positionDto.getTicker());
        model.addAttribute(MODEL_ATTRIBUTE_OPTION_HISTORY, optionHistory);

        List<PositionDto> openOptions = optionService.getOpenOptionsByTicker(positionDto.getTicker());
        model.addAttribute(MODEL_ATTRIBUTE_OPTION_OPEN, openOptions);

        InstrumentDto instrumentDto = instrumentService.loadInstrumentByTicker(positionDto.getTicker());
        Optional.ofNullable(instrumentDto).ifPresent(instrumentDto1 ->
                {
                    positionDto.setEarningDate(instrumentDto.getEarningDate());
                });
    }

    /**
     * Get market value for a position with cache-first asset metadata loading.
     *
     * Phase 1: Fetch market data snapshot from Alpaca
     * Phase 2: Check if asset metadata is cached, if not fetch and save
     *
     * ISSUE-014 Implementation
     */
    private void getMarketValue(PositionDto positionDto) {
        String ticker = positionDto.getTicker();

        // Fetch market data snapshot (Phase 1)
        Optional.ofNullable(alpacaService.getMarketDataSnapshot(ticker))
                .map(data -> {
                    instrumentService.saveAlpacaQuotes(data);

                    // Phase 2: Load asset metadata from cache or API
                    loadAssetMetadata(ticker);

                    return data.getQuotes();
                })
                .map(quotes -> quotes.get(ticker))
                .map(alpacaSingleAssetDto -> alpacaSingleAssetDto.getLatestTrade().getPrice())
                .ifPresent(price -> positionDto.setMarketValue(round2Digits(price * 100)));
    }

    /**
     * Load Alpaca asset metadata with cache-first strategy.
     *
     * Strategy:
     * 1. Check if asset already exists in database
     * 2. If not found, fetch from Alpaca API and save
     * 3. Log validation warnings if tradable/marginable flags are problematic
     */
    private void loadAssetMetadata(String ticker) {
        InstrumentEntity instrument = instrumentRepository.findByTicker(ticker);

        if (instrument == null) {
            log.debug("Asset {} not found in cache, fetching from Alpaca API", ticker);
            return; // Asset not in database, cannot fetch without existing entity
        }

        // Check if asset metadata is already cached
        if (instrument.getAlpacaAssetId() != null && instrument.getAlpacaMetadataUpdatedAt() != null) {
            log.debug("Asset {} found in cache, metadata updated at {}",
                    ticker, instrument.getAlpacaMetadataUpdatedAt());
            logAssetValidationWarnings(instrument);
            return;
        }

        // Asset metadata not cached, fetch from Alpaca API
        try {
            log.info("Asset {} not cached, fetching from Alpaca and saving", ticker);
            AlpacaAssetDto assetDto = alpacaService.getAsset(ticker);
            alpacaService.saveAssetMetadata(assetDto, instrument);
            instrumentRepository.save(instrument);
            log.info("Asset {} metadata saved: tradable={}, marginable={}",
                    ticker, assetDto.getTradable(), assetDto.getMarginable());
            logAssetValidationWarnings(instrument);
        } catch (Exception e) {
            log.warn("Failed to fetch asset metadata for {} from Alpaca API", ticker, e);
            // Continue gracefully, market data is still available
        }
    }

    /**
     * Log validation warnings for asset constraints
     */
    private void logAssetValidationWarnings(InstrumentEntity instrument) {
        String ticker = instrument.getTicker();

        if (instrument.getAlpacaTradable() != null && !instrument.getAlpacaTradable()) {
            log.warn("Asset {} is NOT tradable on Alpaca - trades may be rejected", ticker);
        }

        if (instrument.getAlpacaMarginable() != null && !instrument.getAlpacaMarginable()) {
            log.warn("Asset {} is NOT marginable on Alpaca - margin purchases may be rejected", ticker);
        }

        if (instrument.getAlpacaShortable() != null && !instrument.getAlpacaShortable()) {
            log.warn("Asset {} is NOT shortable on Alpaca - short positions may be rejected", ticker);
        }

        if (instrument.getAlpacaEasyToBorrow() != null && !instrument.getAlpacaEasyToBorrow()) {
            log.warn("Asset {} is NOT easy to borrow on Alpaca - short positions may have high costs", ticker);
        }
    }
}