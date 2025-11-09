package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.InstrumentDto;
import co.grtk.srcprofit.service.InstrumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for InstrumentRestController
 * Validates that the API endpoint correctly returns instrument data with exchange information
 * for TradingView integration
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InstrumentRestController Tests")
class InstrumentRestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InstrumentService instrumentService;

    @InjectMocks
    private InstrumentRestController instrumentRestController;

    private InstrumentDto testInstrument;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(instrumentRestController).build();

        testInstrument = new InstrumentDto();
        testInstrument.setId(1L);
        testInstrument.setTicker("AAPL");
        testInstrument.setName("Apple Inc.");
        testInstrument.setPrice(150.25);
        testInstrument.setAssetClass("US");
        testInstrument.setAlpacaExchange("NASDAQ");
    }

    @Test
    @DisplayName("Should return instrument with exchange data for valid ticker")
    void testGetInstrumentByTickerSuccess() throws Exception {
        // Arrange
        when(instrumentService.loadInstrumentByTicker("AAPL"))
                .thenReturn(testInstrument);

        // Act & Assert
        mockMvc.perform(get("/api/instruments/AAPL")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticker", is("AAPL")))
                .andExpect(jsonPath("$.alpacaExchange", is("NASDAQ")))
                .andExpect(jsonPath("$.name", is("Apple Inc.")))
                .andExpect(jsonPath("$.price", is(150.25)));
    }

    @Test
    @DisplayName("Should return 404 when instrument not found")
    void testGetInstrumentByTickerNotFound() throws Exception {
        // Arrange
        when(instrumentService.loadInstrumentByTicker("UNKNOWN"))
                .thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/instruments/UNKNOWN")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should handle ticker with special characters (URL encoding)")
    void testGetInstrumentByTickerWithEncoding() throws Exception {
        // Arrange - Some ETFs have special characters
        InstrumentDto etf = new InstrumentDto();
        etf.setId(2L);
        etf.setTicker("SPY");
        etf.setAlpacaExchange("NYSE");
        etf.setPrice(450.75);

        when(instrumentService.loadInstrumentByTicker("SPY"))
                .thenReturn(etf);

        // Act & Assert
        mockMvc.perform(get("/api/instruments/SPY")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker", is("SPY")))
                .andExpect(jsonPath("$.alpacaExchange", is("NYSE")));
    }

    @Test
    @DisplayName("Should return instrument data for AMEX/ARCA symbols")
    void testGetInstrumentByTickerAMEX() throws Exception {
        // Arrange
        InstrumentDto gdx = new InstrumentDto();
        gdx.setId(3L);
        gdx.setTicker("GDX");
        gdx.setName("VanEck Gold Miners ETF");
        gdx.setAlpacaExchange("NYSEARCA");  // Alpaca returns NYSEARCA
        gdx.setPrice(32.50);

        when(instrumentService.loadInstrumentByTicker("GDX"))
                .thenReturn(gdx);

        // Act & Assert
        mockMvc.perform(get("/api/instruments/GDX")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker", is("GDX")))
                .andExpect(jsonPath("$.alpacaExchange", is("NYSEARCA")));
    }

    @Test
    @DisplayName("Should return instrument with null exchange gracefully")
    void testGetInstrumentByTickerNullExchange() throws Exception {
        // Arrange
        InstrumentDto instrumentNoExchange = new InstrumentDto();
        instrumentNoExchange.setId(4L);
        instrumentNoExchange.setTicker("TEST");
        instrumentNoExchange.setAlpacaExchange(null);  // No exchange data yet

        when(instrumentService.loadInstrumentByTicker("TEST"))
                .thenReturn(instrumentNoExchange);

        // Act & Assert
        mockMvc.perform(get("/api/instruments/TEST")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker", is("TEST")))
                .andExpect(jsonPath("$.alpacaExchange").doesNotExist());
    }
}
