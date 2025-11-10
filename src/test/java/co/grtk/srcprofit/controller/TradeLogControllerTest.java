package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.NetAssetValueDto;
import co.grtk.srcprofit.dto.PositionDto;
import co.grtk.srcprofit.service.NetAssetValueService;
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
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TradeLogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OptionService optionService;

    @Mock
    private NetAssetValueService netAssetValueService;

    @InjectMocks
    private TradeLogController tradeLogController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tradeLogController).build();
    }

    @Test
    void testTradeLogControllerExists() {
        assertNotNull(tradeLogController);
        assertTrue(TradeLogController.class.isAnnotationPresent(org.springframework.stereotype.Controller.class));
    }

    @Test
    void testTradelogEndpoint_ReturnsTradeLogPage() throws Exception {
        // Setup
        List<PositionDto> mockOpenPositions = new ArrayList<>();
        NetAssetValueDto mockNav = new NetAssetValueDto();
        mockNav.setCash(10000.0);
        mockNav.setStock(5000.0);

        when(optionService.getAllOpenPositions(null)).thenReturn(mockOpenPositions);
        when(netAssetValueService.loadLatestNetAssetValue()).thenReturn(mockNav);
        when(optionService.getWeeklyOpenPositions(mockOpenPositions)).thenReturn(mockOpenPositions);
        doNothing().when(optionService).calculatePosition(any(PositionDto.class), anyList(), anyList());

        // Execute and verify
        mockMvc.perform(get("/tradelog"))
                .andExpect(status().isOk())
                .andExpect(view().name("tradelog_jte"))
                .andExpect(model().attributeExists("positionDto", "openOptions", "weeklyOpenPositions"));

        // Verify service calls
        verify(optionService).getAllOpenPositions(null);
        // Should NOT call getAllClosedOptions - closed positions moved to TradeHistory
        verify(optionService, never()).getAllClosedOptions(any());
        verify(netAssetValueService).loadLatestNetAssetValue();
        verify(optionService).getWeeklyOpenPositions(mockOpenPositions);
    }

    @Test
    void testTradelogEndpoint_WithNullNetAssetValue() throws Exception {
        // Setup - NetAssetValue returns null
        List<PositionDto> mockOpenPositions = new ArrayList<>();

        when(optionService.getAllOpenPositions(null)).thenReturn(mockOpenPositions);
        when(netAssetValueService.loadLatestNetAssetValue()).thenReturn(null);
        when(optionService.getWeeklyOpenPositions(mockOpenPositions)).thenReturn(mockOpenPositions);
        doNothing().when(optionService).calculatePosition(any(PositionDto.class), anyList(), anyList());

        // Execute and verify - should create default NetAssetValueDto
        mockMvc.perform(get("/tradelog"))
                .andExpect(status().isOk())
                .andExpect(view().name("tradelog_jte"))
                .andExpect(model().attributeExists("positionDto"));

        verify(netAssetValueService).loadLatestNetAssetValue();
    }

    @Test
    void testTradelogFromDateEndpoint_WithValidDate() throws Exception {
        // Setup
        LocalDate fromDate = LocalDate.of(2025, 1, 1);
        List<PositionDto> mockOpenPositions = new ArrayList<>();
        NetAssetValueDto mockNav = new NetAssetValueDto();
        mockNav.setCash(10000.0);
        mockNav.setStock(5000.0);

        when(optionService.getAllOpenPositions(fromDate)).thenReturn(mockOpenPositions);
        when(netAssetValueService.loadLatestNetAssetValue()).thenReturn(mockNav);
        when(optionService.getWeeklyOpenPositions(mockOpenPositions)).thenReturn(mockOpenPositions);
        doNothing().when(optionService).calculatePosition(any(PositionDto.class), anyList(), anyList());

        // Execute and verify
        mockMvc.perform(post("/tradelogFromDate")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("fromDate", "2025-01-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("tradelog_jte"))
                .andExpect(model().attributeExists("positionDto", "openOptions"));

        // Verify service calls with correct date
        verify(optionService).getAllOpenPositions(fromDate);
        // Should NOT call getAllClosedOptions - closed positions moved to TradeHistory
        verify(optionService, never()).getAllClosedOptions(any());
    }

    @Test
    void testTradelogPage_PopulatesModelAttributes() throws Exception {
        // Setup
        List<PositionDto> mockOpenPositions = new ArrayList<>();
        PositionDto openPos1 = new PositionDto();
        openPos1.setTicker("AAPL");
        mockOpenPositions.add(openPos1);

        NetAssetValueDto mockNav = new NetAssetValueDto();
        mockNav.setCash(25000.0);
        mockNav.setStock(50000.0);

        when(optionService.getAllOpenPositions(null)).thenReturn(mockOpenPositions);
        when(netAssetValueService.loadLatestNetAssetValue()).thenReturn(mockNav);
        when(optionService.getWeeklyOpenPositions(mockOpenPositions)).thenReturn(new ArrayList<>());
        doNothing().when(optionService).calculatePosition(any(PositionDto.class), anyList(), anyList());

        // Execute and verify model attributes
        mockMvc.perform(get("/tradelog"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("openOptions", mockOpenPositions))
                .andExpect(model().attributeExists("positionDto", "weeklyOpenPositions"));

        // Verify that closed positions are NOT included
        verify(optionService, never()).getAllClosedOptions(any());
    }

    @Test
    void testTradelogEndpoint_CallsServiceMethods() throws Exception {
        // Setup
        when(optionService.getAllOpenPositions(null)).thenReturn(new ArrayList<>());
        when(netAssetValueService.loadLatestNetAssetValue()).thenReturn(null);
        when(optionService.getWeeklyOpenPositions(new ArrayList<>())).thenReturn(new ArrayList<>());
        doNothing().when(optionService).calculatePosition(any(PositionDto.class), anyList(), anyList());

        // Execute
        mockMvc.perform(get("/tradelog"));

        // Verify service methods were called exactly once
        verify(optionService, times(1)).getAllOpenPositions(null);
        // Should NOT call getAllClosedOptions - closed positions moved to TradeHistory
        verify(optionService, never()).getAllClosedOptions(any());
        verify(optionService, times(1)).calculatePosition(any(PositionDto.class), anyList(), anyList());
        verify(netAssetValueService, times(1)).loadLatestNetAssetValue();
        verify(optionService, times(1)).getWeeklyOpenPositions(anyList());
    }

    @Test
    void testConstructor_InjectsAllServices() {
        assertNotNull(tradeLogController);
        // Test that controller was properly constructed with mocked services
        // This is implicitly tested by the fact that all the other tests work
    }
}
