package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.OpenPositionViewDto;
import co.grtk.srcprofit.service.OpenPositionService;
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

    @InjectMocks
    private OpenPositionsController openPositionsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(openPositionsController).build();
    }

    @Test
    void testOpenPositionsControllerExists() {
        assertNotNull(openPositionsController);
        assertTrue(OpenPositionsController.class.isAnnotationPresent(org.springframework.stereotype.Controller.class));
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
                .andExpect(model().attributeExists("openPositions"));

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
                15,
                75,
                "PUT"
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
                22,
                80,
                "CALL"
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
    void testOpenPositionViewDto_HasAllRequiredFields() {
        // Verify that the DTO record can be constructed with all fields
        OpenPositionViewDto dto = new OpenPositionViewDto(
                123L,
                "SPY",
                LocalDate.of(2025, 12, 1),
                LocalDate.of(2025, 12, 20),
                10,
                -5,
                420.0,
                415.0,
                -125.0,
                15,
                75,
                "PUT"
        );

        // Assert all fields are accessible
        assertEquals(123L, dto.id());
        assertEquals("SPY", dto.symbol());
        assertEquals(LocalDate.of(2025, 12, 1), dto.tradeDate());
        assertEquals(LocalDate.of(2025, 12, 20), dto.expirationDate());
        assertEquals(10, dto.daysLeft());
        assertEquals(-5, dto.qty());
        assertEquals(420.0, dto.strikePrice());
        assertEquals(415.0, dto.underlyingPrice());
        assertEquals(-125.0, dto.pnl());
        assertEquals(15, dto.roi());
        assertEquals(75, dto.pop());
        assertEquals("PUT", dto.type());
    }

    @Test
    void testConstructor_InjectsOpenPositionService() {
        assertNotNull(openPositionsController);
        // The fact that all tests pass proves the service was properly injected
    }
}
