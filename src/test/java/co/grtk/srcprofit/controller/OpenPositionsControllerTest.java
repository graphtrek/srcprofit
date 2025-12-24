package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.NetAssetValueDto;
import co.grtk.srcprofit.dto.OpenPositionViewDto;
import co.grtk.srcprofit.dto.StockPositionViewDto;
import co.grtk.srcprofit.service.NetAssetValueService;
import co.grtk.srcprofit.service.OpenPositionService;
import co.grtk.srcprofit.service.OptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OpenPositionsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OpenPositionService openPositionService;

    @Mock
    private NetAssetValueService netAssetValueService;

    @Mock
    private OptionService optionService;

    @InjectMocks
    private OpenPositionsController openPositionsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(openPositionsController).build();

        // Setup default NAV with report date (lenient to avoid unnecessary stubbing errors)
        NetAssetValueDto mockNav = new NetAssetValueDto();
        mockNav.setReportDate(LocalDate.of(2025, 12, 22));
        lenient().when(netAssetValueService.loadLatestNetAssetValue()).thenReturn(mockNav);

        // Setup default option service calls (lenient to avoid unnecessary stubbing errors)
        lenient().when(openPositionService.getAllOpenOptionDtos(null)).thenReturn(new ArrayList<>());
        lenient().when(optionService.getWeeklySummaryOpenOptionDtos(any())).thenReturn(new ArrayList<>());
    }

    @Test
    void testOpenPositionsEndpoint_ReturnsCorrectView() throws Exception {
        // Setup - empty list for initial test
        List<OpenPositionViewDto> mockPositions = new ArrayList<>();

        when(openPositionService.getAllOpenPositionViewDtos()).thenReturn(mockPositions);

        // Execute and verify
        mockMvc.perform(get("/openpositions"))
                .andExpect(status().isOk())
                .andExpect(view().name("openpositions_jte"))
                .andExpect(model().attributeExists("openPositions"))
                .andExpect(model().attributeExists("reportDate"));

        // Verify service was called
        verify(openPositionService).getAllOpenPositionViewDtos();
    }

    @Test
    void testOpenPositionsEndpoint_PopulatesModelAttribute() throws Exception {
        // Setup - create sample positions
        List<OpenPositionViewDto> mockPositions = new ArrayList<>();
        mockPositions.add(new OpenPositionViewDto(
                1L,
                "SPY",
                LocalDate.of(2025, 12, 1),
                LocalDate.of(2025, 12, 20),
                10,
                -5,
                420.0,
                415.0,
                -125.0,
                -130.0,      // calculatedPnl
                15,
                75,
                "PUT",
                500.0        // costBasisMoney
        ));
        mockPositions.add(new OpenPositionViewDto(
                2L,
                "AAPL",
                LocalDate.of(2025, 12, 1),
                LocalDate.of(2025, 12, 27),
                17,
                10,
                210.0,
                208.0,
                250.0,
                255.0,       // calculatedPnl
                22,
                80,
                "CALL",
                2100.0       // costBasisMoney
        ));

        when(openPositionService.getAllOpenPositionViewDtos()).thenReturn(mockPositions);

        // Execute and verify model attribute contains the positions
        mockMvc.perform(get("/openpositions"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("openPositions", mockPositions));

        // Verify service call
        verify(openPositionService, times(1)).getAllOpenPositionViewDtos();
    }

    @Test
    void testOpenPositionsEndpoint_WithEmptyList() throws Exception {
        // Setup - service returns empty list
        when(openPositionService.getAllOpenPositionViewDtos()).thenReturn(new ArrayList<>());

        // Execute and verify
        mockMvc.perform(get("/openpositions"))
                .andExpect(status().isOk())
                .andExpect(view().name("openpositions_jte"))
                .andExpect(model().attributeExists("openPositions"));

        // Verify service was called
        verify(openPositionService).getAllOpenPositionViewDtos();
    }

    @Test
    void testOpenPositionsEndpoint_LoadsStockData() throws Exception {
        // Setup
        List<OpenPositionViewDto> mockOptions = new ArrayList<>();
        List<StockPositionViewDto> mockStocks = new ArrayList<>();
        mockStocks.add(new StockPositionViewDto(
                1L, "AAPL", LocalDate.of(2025, 1, 15), 100,
                15000.0, 155.50, 15550.0, 550.0, 5.2
        ));

        when(openPositionService.getAllOpenPositionViewDtos()).thenReturn(mockOptions);
        when(openPositionService.getAllStockPositionViewDtos()).thenReturn(mockStocks);

        // Execute and verify
        mockMvc.perform(get("/openpositions"))
                .andExpect(status().isOk())
                .andExpect(view().name("openpositions_jte"))
                .andExpect(model().attributeExists("stockPositions"))
                .andExpect(model().attribute("stockPositions", mockStocks));

        // Verify service calls
        verify(openPositionService).getAllStockPositionViewDtos();
    }

    @Test
    void testOpenPositionsEndpoint_WithEmptyStockList() throws Exception {
        // Setup
        when(openPositionService.getAllOpenPositionViewDtos()).thenReturn(new ArrayList<>());
        when(openPositionService.getAllStockPositionViewDtos()).thenReturn(new ArrayList<>());

        // Execute and verify
        mockMvc.perform(get("/openpositions"))
                .andExpect(status().isOk())
                .andExpect(view().name("openpositions_jte"))
                .andExpect(model().attributeExists("stockPositions"));

        // Verify service was called
        verify(openPositionService).getAllStockPositionViewDtos();
    }

    @Test
    void testStockPositionViewDto_HandlesNullFields() {
        // Verify that the DTO can handle null P&L and percentOfNAV
        StockPositionViewDto dto = new StockPositionViewDto(
                1L, "TSLA", LocalDate.of(2025, 3, 1), 10,
                2500.0, 250.0, 2500.0, null, null
        );

        assertNotNull(dto);
        assertNull(dto.pnl());
        assertNull(dto.percentOfNAV());
    }

    @Test
    void testOpenPositionsEndpoint_LoadsBothOptionsAndStocks() throws Exception {
        // Setup - both options and stocks
        List<OpenPositionViewDto> mockOptions = new ArrayList<>();
        mockOptions.add(new OpenPositionViewDto(
                1L, "SPY", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1),
                15, 10, 450.0, 451.0, 100.0, 150.0, 5, 50, "PUT", 4500.0
        ));

        List<StockPositionViewDto> mockStocks = new ArrayList<>();
        mockStocks.add(new StockPositionViewDto(
                2L, "AAPL", LocalDate.of(2025, 1, 15), 100,
                15000.0, 155.50, 15550.0, 550.0, 5.2
        ));

        when(openPositionService.getAllOpenPositionViewDtos()).thenReturn(mockOptions);
        when(openPositionService.getAllStockPositionViewDtos()).thenReturn(mockStocks);

        // Execute and verify
        mockMvc.perform(get("/openpositions"))
                .andExpect(status().isOk())
                .andExpect(view().name("openpositions_jte"))
                .andExpect(model().attributeExists("openPositions", "stockPositions", "reportDate"))
                .andExpect(model().attribute("openPositions", mockOptions))
                .andExpect(model().attribute("stockPositions", mockStocks));

        // Verify both services were called
        verify(openPositionService).getAllOpenPositionViewDtos();
        verify(openPositionService).getAllStockPositionViewDtos();
    }
}
