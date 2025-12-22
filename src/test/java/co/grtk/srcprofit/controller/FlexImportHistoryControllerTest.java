package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.FlexImportHistoryDto;
import co.grtk.srcprofit.service.FlexReportsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FlexImportHistoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FlexReportsService flexReportsService;

    @InjectMocks
    private FlexImportHistoryController flexImportHistoryController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(flexImportHistoryController).build();
    }

    @Test
    void testFlexImportHistoryEndpoint_ReturnsFlexImportHistoryPage() throws Exception {
        // Setup
        List<FlexImportHistoryDto> mockImports = new ArrayList<>();
        FlexImportHistoryDto dto1 = new FlexImportHistoryDto();
        dto1.setReferenceCode("REF001");
        dto1.setReportType("ActivityFlexStatement");
        dto1.setStatus("SUCCESS");
        mockImports.add(dto1);

        when(flexReportsService.getAllImportHistory()).thenReturn(mockImports);

        // Execute and verify
        mockMvc.perform(get("/flexImportHistory"))
                .andExpect(status().isOk())
                .andExpect(view().name("flex_import_history_jte"))
                .andExpect(model().attributeExists("flexImports"));

        // Verify service calls
        verify(flexReportsService, times(1)).getAllImportHistory();
    }

    @Test
    void testFlexImportHistoryEndpoint_WithEmptyList() throws Exception {
        // Setup - Empty import history
        List<FlexImportHistoryDto> emptyList = new ArrayList<>();

        when(flexReportsService.getAllImportHistory()).thenReturn(emptyList);

        // Execute and verify - should still return page successfully
        mockMvc.perform(get("/flexImportHistory"))
                .andExpect(status().isOk())
                .andExpect(view().name("flex_import_history_jte"))
                .andExpect(model().attributeExists("flexImports"))
                .andExpect(model().attribute("flexImports", emptyList));

        verify(flexReportsService, times(1)).getAllImportHistory();
    }

    @Test
    void testFlexImportHistoryPage_PopulatesModelAttributes() throws Exception {
        // Setup
        List<FlexImportHistoryDto> mockImports = new ArrayList<>();

        FlexImportHistoryDto dto1 = new FlexImportHistoryDto();
        dto1.setReferenceCode("REF001");
        dto1.setReportType("ActivityFlexStatement");
        dto1.setStatus("SUCCESS");
        dto1.setUpdatedAt(LocalDateTime.of(2025, 11, 21, 10, 0));
        dto1.setCsvRecordsCount(100);
        dto1.setCsvFailedRecordsCount(2);
        dto1.setCsvSkippedRecordsCount(3);
        dto1.setDataFixRecordsCount(5);
        mockImports.add(dto1);

        FlexImportHistoryDto dto2 = new FlexImportHistoryDto();
        dto2.setReferenceCode("REF002");
        dto2.setReportType("TradeConfirmFlexStatement");
        dto2.setStatus("FAILED");
        dto2.setUpdatedAt(LocalDateTime.of(2025, 11, 20, 15, 30));
        mockImports.add(dto2);

        when(flexReportsService.getAllImportHistory()).thenReturn(mockImports);

        // Execute and verify model attributes
        mockMvc.perform(get("/flexImportHistory"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("flexImports", mockImports))
                .andExpect(model().attributeExists("flexImports"));

        verify(flexReportsService, times(1)).getAllImportHistory();
    }
}
