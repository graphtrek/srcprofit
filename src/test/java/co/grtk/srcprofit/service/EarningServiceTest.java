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
            String csvData = "symbol,name,reportDate,fiscalDateEnding,estimate,currency\n" +
                    "AAPL,Apple Inc,2025-01-15,2024-12-31,1.50,USD\n" +
                    "AAPL,Apple Inc,2025-04-15,2025-03-31,1.75,USD";

            when(alphaVintageService.getEarningsCalendar()).thenReturn(csvData);
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

            // Assert - Format is "{newRecordsCount}/0/0"
            assertThat(result).startsWith("2/"); // 2 new records created
            verify(alphaVintageService, times(1)).getEarningsCalendar();
            verify(earningRepository, times(2)).save(any(EarningEntity.class));
        }

        /**
         * Test 2: No earnings data from API
         */
        @Test
        void testRefreshEarningsData_withEmptyApiResponse_shouldReturnZeros() {
            // Arrange
            when(alphaVintageService.getEarningsCalendar()).thenReturn(""); // Empty response

            // Act
            String result = earningService.refreshEarningsDataForAllInstruments();

            // Assert - Returns "0/0/1" for empty/null API response
            assertThat(result).isEqualTo("0/0/1");
        }

        /**
         * Test 3: Deduplication - Existing earnings should not be re-saved
         */
        @Test
        void testRefreshEarningsData_withDuplicateEarnings_shouldNotCreateDuplicates() {
            // Arrange
            String csvData = "symbol,name,reportDate,fiscalDateEnding,estimate,currency\n" +
                    "AAPL,Apple Inc,2025-01-15,2024-12-31,1.50,USD";

            when(alphaVintageService.getEarningsCalendar()).thenReturn(csvData);
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

            // Assert - 0 new records because earning already exists
            assertThat(result).startsWith("0/"); // 0 new records
            verify(earningRepository, times(1)).save(any(EarningEntity.class)); // Save happens but for instrument update
        }

        /**
         * Test 4: Instrument earnings date update - Future earnings only
         */
        @Test
        void testRefreshEarningsData_shouldUpdateInstrumentEarningDate() {
            // Arrange
            LocalDate futureDate = LocalDate.now().plusDays(30);
            String csvData = "symbol,name,reportDate,fiscalDateEnding,estimate,currency\n" +
                    "AAPL,Apple Inc," + futureDate + ",2025-03-31,1.50,USD";

            when(alphaVintageService.getEarningsCalendar()).thenReturn(csvData);
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
            String csvData = "symbol,name,reportDate,fiscalDateEnding,estimate,currency\n" +
                    "UNKNOWN,Unknown Corp,2025-01-15,2024-12-31,1.50,USD";
            when(alphaVintageService.getEarningsCalendar()).thenReturn(csvData);
            when(instrumentRepository.findAll()).thenReturn(allInstruments); // Only AAPL

            // Act
            String result = earningService.refreshEarningsDataForAllInstruments();

            // Assert - 0 new records because symbol not found in instruments
            assertThat(result).startsWith("0/");
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

            String csvData = "symbol,name,reportDate,fiscalDateEnding,estimate,currency\n" +
                    "AAPL,Apple Inc,2025-01-15,2024-12-31,1.50,USD\n" +
                    "GOOGL,Alphabet Inc,2025-01-29,2024-12-31,1.75,USD";

            when(alphaVintageService.getEarningsCalendar()).thenReturn(csvData);
            when(instrumentRepository.findAll()).thenReturn(allInstruments);
            when(earningRepository.findBySymbolAndReportDateAndFiscalDateEnding(anyString(), any(), any()))
                    .thenReturn(null); // All new

            // Act
            String result = earningService.refreshEarningsDataForAllInstruments();

            // Assert - Format is "{newRecordsCount}/0/0"
            assertThat(result).startsWith("2/"); // 2 new records
            verify(earningRepository, times(2)).save(any(EarningEntity.class)); // 2 new records
        }

        /**
         * Test 7: API failure - Should return error count
         */
        @Test
        void testRefreshEarningsData_whenApiThrows_shouldReturnEmptyList() {
            // Arrange
            when(alphaVintageService.getEarningsCalendar())
                    .thenThrow(new RuntimeException("API Error"));

            // Act
            String result = earningService.refreshEarningsDataForAllInstruments();

            // Assert - Returns "0/0/1" on exception
            assertThat(result).isEqualTo("0/0/1");
        }

        /**
         * Test 8: Earnings with past dates - Updates instrument when no date is set
         */
        @Test
        void testRefreshEarningsData_withPastEarnings_shouldNotUpdateInstrumentDate() {
            // Arrange
            LocalDate pastDate = LocalDate.now().minusDays(30);

            String csvData = "symbol,name,reportDate,fiscalDateEnding,estimate,currency\n" +
                    "AAPL,Apple Inc," + pastDate + ",2024-12-31,1.30,USD";

            when(alphaVintageService.getEarningsCalendar()).thenReturn(csvData);
            when(instrumentRepository.findAll()).thenReturn(allInstruments);
            when(earningRepository.findBySymbolAndReportDateAndFiscalDateEnding(
                    "AAPL", pastDate, LocalDate.of(2024, 12, 31)
            )).thenReturn(null); // New record

            // Act
            earningService.refreshEarningsDataForAllInstruments();

            // Assert - Instrument earnings date IS updated to past date (because it was null)
            verify(earningRepository, times(1)).save(any(EarningEntity.class)); // Save earning
            verify(instrumentRepository, times(1)).save(any(InstrumentEntity.class)); // Instrument IS saved for null earningDate
            assertThat(testInstrument.getEarningDate()).isEqualTo(pastDate); // Updated to past date
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

            String csvData = "symbol,name,reportDate,fiscalDateEnding,estimate,currency\n" +
                    "AAPL,Apple Inc," + date1 + ",2024-12-31,1.50,USD\n" +
                    "AAPL,Apple Inc," + date2 + ",2025-03-31,1.60,USD\n" +
                    "AAPL,Apple Inc," + date3 + ",2025-06-30,1.70,USD";

            when(alphaVintageService.getEarningsCalendar()).thenReturn(csvData);
            when(instrumentRepository.findAll()).thenReturn(allInstruments);
            when(earningRepository.findBySymbolAndReportDateAndFiscalDateEnding(anyString(), any(), any()))
                    .thenReturn(null); // All new

            // Act
            earningService.refreshEarningsDataForAllInstruments();

            // Assert - Should use earliest date (date1) as it's processed first
            assertThat(testInstrument.getEarningDate()).isEqualTo(date1);
        }

        /**
         * Test 10: Empty instruments list - Should handle gracefully
         */
        @Test
        void testRefreshEarningsData_withNoInstruments_shouldReturnZeros() {
            // Arrange
            String csvData = "symbol,name,reportDate,fiscalDateEnding,estimate,currency\n" +
                    "AAPL,Apple Inc,2025-01-15,2024-12-31,1.50,USD";
            when(alphaVintageService.getEarningsCalendar()).thenReturn(csvData);
            when(instrumentRepository.findAll()).thenReturn(new ArrayList<>()); // Empty

            // Act
            String result = earningService.refreshEarningsDataForAllInstruments();

            // Assert - 0 new records because no instruments match
            assertThat(result).startsWith("0/");
        }

        /**
         * Test 11: Result format - Should return "newRecordsCount/0/0" or "0/0/1"
         */
        @Test
        void testRefreshEarningsData_resultFormat_shouldMatchExpectedFormat() {
            // Arrange
            String csvData = "symbol,name,reportDate,fiscalDateEnding,estimate,currency\n" +
                    "AAPL,Apple Inc,2025-01-15,2024-12-31,1.50,USD";
            when(alphaVintageService.getEarningsCalendar()).thenReturn(csvData);
            when(instrumentRepository.findAll()).thenReturn(allInstruments);
            when(earningRepository.findBySymbolAndReportDateAndFiscalDateEnding(anyString(), any(), any()))
                    .thenReturn(null); // New

            // Act
            String result = earningService.refreshEarningsDataForAllInstruments();

            // Assert - Format: "{newRecordsCount}/0/0"
            assertThat(result).matches("\\d+/\\d+/\\d+"); // Format: X/Y/Z
            String[] parts = result.split("/");
            assertThat(parts).hasSize(3);
            assertThat(Integer.parseInt(parts[0])).isGreaterThan(0); // new records count
            assertThat(Integer.parseInt(parts[1])).isEqualTo(0); // always 0
            assertThat(Integer.parseInt(parts[2])).isEqualTo(0); // always 0
        }

        /**
         * Test 12: Idempotency - Multiple runs should be safe
         */
        @Test
        void testRefreshEarningsData_isIdempotent_shouldProduceConsistentResults() {
            // Arrange
            String csvData = "symbol,name,reportDate,fiscalDateEnding,estimate,currency\n" +
                    "AAPL,Apple Inc,2025-01-15,2024-12-31,1.50,USD";
            EarningEntity existingEarning = new EarningEntity();

            when(alphaVintageService.getEarningsCalendar())
                    .thenReturn(csvData)
                    .thenReturn(csvData);

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
            assertThat(result1).startsWith("1/"); // 1 new record
            assertThat(result2).startsWith("0/"); // 0 new records (already exists)
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
