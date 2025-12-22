package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.PositionDto;
import co.grtk.srcprofit.service.OptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TradeHistoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OptionService optionService;

    @InjectMocks
    private TradeHistoryController tradeHistoryController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tradeHistoryController).build();
    }

    @Test
    void testTradehistoryEndpoint_ReturnsTradeHistoryPage() throws Exception {
        // Setup
        List<PositionDto> mockClosedPositions = new ArrayList<>();

        when(optionService.getAllClosedOptions(null)).thenReturn(mockClosedPositions);

        // Execute and verify
        mockMvc.perform(get("/tradehistory"))
                .andExpect(status().isOk())
                .andExpect(view().name("trade_history_jte"))
                .andExpect(model().attributeExists("closedPositions"));

        // Verify service calls
        verify(optionService).getAllClosedOptions(null);
    }

    @Test
    void testTradehistoryFromDateEndpoint_WithValidDate() throws Exception {
        // Setup
        LocalDate filterDate = LocalDate.of(2025, 1, 1);
        List<PositionDto> mockClosedPositions = new ArrayList<>();

        when(optionService.getAllClosedOptions(filterDate)).thenReturn(mockClosedPositions);

        // Execute and verify
        mockMvc.perform(post("/tradehistoryFromDate")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tradeDate", "2025-01-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("trade_history_jte"))
                .andExpect(model().attributeExists("closedPositions"));

        // Verify service calls with correct date
        verify(optionService).getAllClosedOptions(filterDate);
    }

    @Test
    void testTradehistoryFromDateEndpoint_FiltersClosedPositionsByDate() throws Exception {
        // Setup
        LocalDate filterDate = LocalDate.of(2025, 6, 1);
        List<PositionDto> mockFilteredClosedPositions = new ArrayList<>();
        PositionDto closedPos = new PositionDto();
        closedPos.setTicker("MSFT");
        closedPos.setTradeDate(LocalDate.of(2025, 5, 15));
        mockFilteredClosedPositions.add(closedPos);

        when(optionService.getAllClosedOptions(filterDate)).thenReturn(mockFilteredClosedPositions);

        // Execute and verify
        mockMvc.perform(post("/tradehistoryFromDate")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tradeDate", "2025-06-01"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("closedPositions", mockFilteredClosedPositions));

        verify(optionService).getAllClosedOptions(filterDate);
    }

    @Test
    void testTradehistoryEndpoint_HandlesEmptyClosedPositionsList() throws Exception {
        // Setup - empty list
        List<PositionDto> emptyClosedPositions = new ArrayList<>();

        when(optionService.getAllClosedOptions(null)).thenReturn(emptyClosedPositions);

        // Execute and verify
        mockMvc.perform(get("/tradehistory"))
                .andExpect(status().isOk())
                .andExpect(view().name("trade_history_jte"))
                .andExpect(model().attribute("closedPositions", emptyClosedPositions));

        assertTrue(emptyClosedPositions.isEmpty());
    }

    @Test
    void testTradehistoryFromDateEndpoint_HandlesInvalidDateFormat() throws Exception {
        // Setup - invalid date format should result in null parsing
        when(optionService.getAllClosedOptions(null)).thenReturn(new ArrayList<>());

        // Execute - invalid date should be handled gracefully
        mockMvc.perform(post("/tradehistoryFromDate")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tradeDate", "invalid-date"))
                .andExpect(status().isOk());

        // Verify service was still called (likely with null value)
        verify(optionService).getAllClosedOptions(any());
    }
}
