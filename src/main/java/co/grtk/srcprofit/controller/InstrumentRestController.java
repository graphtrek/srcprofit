package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.InstrumentDto;
import co.grtk.srcprofit.service.InstrumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API endpoints for instrument data.
 * Used by TradingView integration to fetch dynamic exchange information.
 */
@RestController
@RequestMapping("/api/instruments")
public class InstrumentRestController {
    private static final Logger LOG = LoggerFactory.getLogger(InstrumentRestController.class);

    private final InstrumentService instrumentService;

    public InstrumentRestController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    /**
     * Get instrument by ticker symbol with exchange information.
     * Used by TradingView integration to dynamically map symbols to exchange-prefixed symbols.
     *
     * @param ticker The ticker symbol (e.g., "AAPL", "SPY", "GDX")
     * @return InstrumentDto with alpaca_exchange field, or 404 if not found
     */
    @GetMapping(value = "/{ticker}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InstrumentDto> getInstrumentByTicker(@PathVariable String ticker) {
        try {
            InstrumentDto instrument = instrumentService.loadInstrumentByTicker(ticker);
            if (instrument == null) {
                LOG.warn("Instrument not found for ticker: {}", ticker);
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(instrument);
        } catch (Exception e) {
            LOG.error("Error fetching instrument for ticker: {}", ticker, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
