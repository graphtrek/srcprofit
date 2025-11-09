package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.EarningDto;
import co.grtk.srcprofit.entity.EarningEntity;
import co.grtk.srcprofit.entity.InstrumentEntity;
import co.grtk.srcprofit.repository.EarningRepository;
import co.grtk.srcprofit.repository.InstrumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EarningService, specifically the refreshEarningsDataForAllInstruments() method.
 *
 * Covers:
 * - Fetching earnings from Alpha Vantage API
 * - Creating new EarningEntity records
 * - Deduplication of existing records
 * - Updating InstrumentEntity earnings dates
 * - Per-symbol error handling
 * - Batch processing and statistics
 */
@ExtendWith(MockitoExtension.class)
class EarningServiceTest {

    @Mock
    private EarningRepository earningRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private AlphaVintageService alphaVintageService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EarningService earningService;

    private InstrumentEntity testInstrument;
    private List<InstrumentEntity> allInstruments;

    @BeforeEach
    void setUp() {
        // Setup test instrument
        testInstrument = new InstrumentEntity();
        testInstrument.setId(1L);
        testInstrument.setTicker("AAPL");

        allInstruments = new ArrayList<>();
        allInstruments.add(testInstrument);

        reset(earningRepository, instrumentRepository, alphaVintageService, objectMapper);
    }

    @Nested
    class RefreshEarningsDataTests {

        /**
         * Test 1: Happy path - Successfully refreshes earnings for all instruments
         */
        @Test
        void testRefreshEarningsData_withValidInput_shouldSucceed() {
            // Arrange
            EarningDto earning1 = createEarningDto("AAPL", "2025-01-15", "2024-12-31");
            EarningDto earning2 = createEarningDto("AAPL", "2025-04-15", "2025-03-31");
            List<EarningDto> earningsFromApi = List.of(earning1, earning2);

            when(alphaVintageService.fetchEarningsCalendar()).thenReturn(earningsFromApi);
            when(instrumentRepository.findAll()).thenReturn(allInstruments);
            when(earningRepository.findBySymbolAndReportDateAndFiscalDateEnding(
                    "AAPL",
                    LocalDate.parse("2025-01-15"),
                    LocalDate.parse("2024-12-31")
            )).thenReturn(null); // New record

            when(earningRepository.findBySymbolAndReportDateAndFiscalDateEnding(
                    "AAPL",
                    LocalDate.parse("2025-04-15"),
                    LocalDate.parse("2025-03-31")
            )).thenReturn(null); // New record

            // Act
            String result = earningService.refreshEarningsDataForAllInstruments();

            // Assert
            assertThat(result).contains("/2/"); // 1 processed symbol, 2 new records, 0 failures
            verify(alphaVintageService, times(1)).fetchEarningsCalendar();
            verify(earningRepository, times(2)).save(any(EarningEntity.class));
        }

        /**
         * Test 2: No earnings data from API
         */
        @Test
        void testRefreshEarningsData_withEmptyApiResponse_shouldReturnZeros() {
            // Arrange
            when(alphaVintageService.fetchEarningsCalendar()).thenReturn(new ArrayList<>());

            // Act
            String result = earningService.refreshEarningsDataForAllInstruments();

            // Assert
            assertThat(result).isEqualTo("0/0/0");
            verify(instrumentRepository, never()).findAll();
        }

        /**
         * Test 3: Deduplication - Existing earnings should not be re-saved
         */
        @Test
        void testRefreshEarningsData_withDuplicateEarnings_shouldNotCreateDuplicates() {
            // Arrange
            EarningDto earning = createEarningDto("AAPL", "2025-01-15", "2024-12-31");
            List<EarningDto> earningsFromApi = List.of(earning);

            when(alphaVintageService.fetchEarningsCalendar()).thenReturn(earningsFromApi);
            when(instrumentRepository.findAll()).thenReturn(allInstruments);

            // Mock existing earnings record (deduplication check)
            EarningEntity existingEarning = new EarningEntity();
            when(earningRepository.findBySymbolAndReportDateAndFiscalDateEnding(
                    "AAPL",
                    LocalDate.parse("2025-01-15"),
                    LocalDate.parse("2024-12-31")
            )).thenReturn(existingEarning);

            // Act
            String result = earningService.refreshEarningsDataForAllInstruments();

            // Assert
            assertThat(result).contains("/0/"); // 1 processed, 0 new records
            verify(earningRepository, never()).save(any(EarningEntity.class)); // No save on duplicate
        }

        /**
         * Test 4: Instrument earnings date update - Future earnings only
         */
        @Test
        void testRefreshEarningsData_shouldUpdateInstrumentEarningDate() {
            // Arrange
            LocalDate futureDate = LocalDate.now().plusDays(30);
            EarningDto futureEarning = new EarningDto();
            futureEarning.setSymbol("AAPL");
            futureEarning.setName("Apple Inc");
            futureEarning.setReportDate(futureDate);
            futureEarning.setFiscalDateEnding(LocalDate.of(2025, 3, 31));
            futureEarning.setEstimate("1.50");
            futureEarning.setCurrency("USD");

            when(alphaVintageService.fetchEarningsCalendar()).thenReturn(List.of(futureEarning));
            when(instrumentRepository.findAll()).thenReturn(allInstruments);
            when(earningRepository.findBySymbolAndReportDateAndFiscalDateEnding(
                    "AAPL", futureDate, LocalDate.of(2025, 3, 31)
            )).thenReturn(null); // New record

            // Act
            earningService.refreshEarningsDataForAllInstruments();

            // Assert
            verify(instrumentRepository, times(1)).save(testInstrument); // Save once to update instrument
            verify(earningRepository, times(1)).save(any(EarningEntity.class)); // Save earning
            assertThat(testInstrument.getEarningDate()).isEqualTo(futureDate);
        }

        /**
         * Test 5: Symbol not in database - Should skip gracefully
         */
        @Test
        void testRefreshEarningsData_withUnknownSymbol_shouldSkip() {
            // Arrange
            EarningDto earning = createEarningDto("UNKNOWN", "2025-01-15", "2024-12-31");
            when(alphaVintageService.fetchEarningsCalendar()).thenReturn(List.of(earning));
            when(instrumentRepository.findAll()).thenReturn(allInstruments); // Only AAPL

            // Act
            String result = earningService.refreshEarningsDataForAllInstruments();

            // Assert
            assertThat(result).isEqualTo("0/0/0"); // No instruments processed
            verify(earningRepository, never()).findBySymbolAndReportDateAndFiscalDateEnding(any(), any(), any());
        }

        /**
         * Test 6: Multiple symbols - Mixed success and failures
         */
        @Test
        void testRefreshEarningsData_withMultipleSymbols_shouldProcessAll() {
            // Arrange
            InstrumentEntity googleInstrument = new InstrumentEntity();
            googleInstrument.setId(2L);
            googleInstrument.setTicker("GOOGL");

            allInstruments.add(googleInstrument);

            EarningDto appleEarning = createEarningDto("AAPL", "2025-01-15", "2024-12-31");
            EarningDto googleEarning = createEarningDto("GOOGL", "2025-01-29", "2024-12-31");
            List<EarningDto> earningsFromApi = List.of(appleEarning, googleEarning);

            when(alphaVintageService.fetchEarningsCalendar()).thenReturn(earningsFromApi);
            when(instrumentRepository.findAll()).thenReturn(allInstruments);
            when(earningRepository.findBySymbolAndReportDateAndFiscalDateEnding(anyString(), any(), any()))
                    .thenReturn(null); // All new

            // Act
            String result = earningService.refreshEarningsDataForAllInstruments();

            // Assert
            assertThat(result).contains("2/"); // 2 symbols processed
            verify(earningRepository, times(2)).save(any(EarningEntity.class)); // 2 new records
        }

        /**
         * Test 7: API failure - Should return error count
         */
        @Test
        void testRefreshEarningsData_whenApiThrows_shouldReturnEmptyList() {
            // Arrange
            when(alphaVintageService.fetchEarningsCalendar())
                    .thenThrow(new RuntimeException("API Error"));

            // Act & Assert
            assertThatThrownBy(() -> earningService.refreshEarningsDataForAllInstruments())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("API Error");
        }

        /**
         * Test 8: Earnings with past dates - Should be ignored for instrument update
         */
        @Test
        void testRefreshEarningsData_withPastEarnings_shouldNotUpdateInstrumentDate() {
            // Arrange
            LocalDate pastDate = LocalDate.now().minusDays(30);
            EarningDto pastEarning = new EarningDto();
            pastEarning.setSymbol("AAPL");
            pastEarning.setName("Apple Inc");
            pastEarning.setReportDate(pastDate);
            pastEarning.setFiscalDateEnding(LocalDate.of(2024, 12, 31));
            pastEarning.setEstimate("1.30");
            pastEarning.setCurrency("USD");

            when(alphaVintageService.fetchEarningsCalendar()).thenReturn(List.of(pastEarning));
            when(instrumentRepository.findAll()).thenReturn(allInstruments);
            when(earningRepository.findBySymbolAndReportDateAndFiscalDateEnding(
                    "AAPL", pastDate, LocalDate.of(2024, 12, 31)
            )).thenReturn(null); // New record

            // Act
            earningService.refreshEarningsDataForAllInstruments();

            // Assert - Instrument should be saved for the new earning but earnings date not updated
            verify(earningRepository, times(1)).save(any(EarningEntity.class)); // Save earning
            assertThat(testInstrument.getEarningDate()).isNull(); // Not updated for past date
        }

        /**
         * Test 9: Multiple earnings for same symbol - Should use earliest future date
         */
        @Test
        void testRefreshEarningsData_withMultipleEarningDates_shouldUseEarliest() {
            // Arrange
            LocalDate date1 = LocalDate.now().plusDays(30);
            LocalDate date2 = LocalDate.now().plusDays(60);
            LocalDate date3 = LocalDate.now().plusDays(45);

            EarningDto earning1 = createEarningDto("AAPL", date1.toString(), "2024-12-31");
            EarningDto earning2 = createEarningDto("AAPL", date2.toString(), "2025-03-31");
            EarningDto earning3 = createEarningDto("AAPL", date3.toString(), "2025-06-30");

            List<EarningDto> earningsFromApi = List.of(earning1, earning2, earning3);

            when(alphaVintageService.fetchEarningsCalendar()).thenReturn(earningsFromApi);
            when(instrumentRepository.findAll()).thenReturn(allInstruments);
            when(earningRepository.findBySymbolAndReportDateAndFiscalDateEnding(anyString(), any(), any()))
                    .thenReturn(null); // All new

            // Act
            earningService.refreshEarningsDataForAllInstruments();

            // Assert - Should use earliest date (date1)
            assertThat(testInstrument.getEarningDate()).isEqualTo(date1);
        }

        /**
         * Test 10: Empty instruments list - Should handle gracefully
         */
        @Test
        void testRefreshEarningsData_withNoInstruments_shouldReturnZeros() {
            // Arrange
            EarningDto earning = createEarningDto("AAPL", "2025-01-15", "2024-12-31");
            when(alphaVintageService.fetchEarningsCalendar()).thenReturn(List.of(earning));
            when(instrumentRepository.findAll()).thenReturn(new ArrayList<>()); // Empty

            // Act
            String result = earningService.refreshEarningsDataForAllInstruments();

            // Assert
            assertThat(result).isEqualTo("0/0/0");
        }

        /**
         * Test 11: Result format - Should return "processed/newRecords/failures"
         */
        @Test
        void testRefreshEarningsData_resultFormat_shouldMatchExpectedFormat() {
            // Arrange
            EarningDto earning = createEarningDto("AAPL", "2025-01-15", "2024-12-31");
            when(alphaVintageService.fetchEarningsCalendar()).thenReturn(List.of(earning));
            when(instrumentRepository.findAll()).thenReturn(allInstruments);
            when(earningRepository.findBySymbolAndReportDateAndFiscalDateEnding(anyString(), any(), any()))
                    .thenReturn(null); // New

            // Act
            String result = earningService.refreshEarningsDataForAllInstruments();

            // Assert
            assertThat(result).matches("\\d+/\\d+/\\d+"); // Format: X/Y/Z
            String[] parts = result.split("/");
            assertThat(parts).hasSize(3);
            assertThat(Integer.parseInt(parts[0])).isGreaterThan(0); // processed count
            assertThat(Integer.parseInt(parts[1])).isGreaterThan(0); // new records
            assertThat(Integer.parseInt(parts[2])).isEqualTo(0); // failures
        }

        /**
         * Test 12: Idempotency - Multiple runs should be safe
         */
        @Test
        void testRefreshEarningsData_isIdempotent_shouldProduceConsistentResults() {
            // Arrange
            EarningDto earning = createEarningDto("AAPL", "2025-01-15", "2024-12-31");
            EarningEntity existingEarning = new EarningEntity();

            when(alphaVintageService.fetchEarningsCalendar())
                    .thenReturn(List.of(earning))
                    .thenReturn(List.of(earning));

            when(instrumentRepository.findAll())
                    .thenReturn(allInstruments)
                    .thenReturn(allInstruments);

            when(earningRepository.findBySymbolAndReportDateAndFiscalDateEnding(
                    "AAPL",
                    LocalDate.parse("2025-01-15"),
                    LocalDate.parse("2024-12-31")
            )).thenReturn(null).thenReturn(existingEarning); // First run: new, Second run: exists

            // Act - Run twice
            String result1 = earningService.refreshEarningsDataForAllInstruments();
            String result2 = earningService.refreshEarningsDataForAllInstruments();

            // Assert
            assertThat(result1).contains("/1/"); // 1 new record
            assertThat(result2).contains("/0/"); // 0 new records (already exists)
        }
    }

    /**
     * Helper method to create test EarningDto
     */
    private EarningDto createEarningDto(String symbol, String reportDate, String fiscalDateEnding) {
        EarningDto earning = new EarningDto();
        earning.setSymbol(symbol);
        earning.setName("Test Company");
        earning.setReportDate(LocalDate.parse(reportDate));
        earning.setFiscalDateEnding(LocalDate.parse(fiscalDateEnding));
        earning.setEstimate("1.50");
        earning.setCurrency("USD");
        return earning;
    }
}
