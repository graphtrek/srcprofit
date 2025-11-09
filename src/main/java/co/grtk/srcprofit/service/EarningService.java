package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.EarningDto;
import co.grtk.srcprofit.entity.EarningEntity;
import co.grtk.srcprofit.entity.InstrumentEntity;
import co.grtk.srcprofit.repository.EarningRepository;
import co.grtk.srcprofit.repository.InstrumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.csv.CSVParser.parse;

@Service
public class EarningService {
    private static final Logger log = LoggerFactory.getLogger(EarningService.class);

    private final EarningRepository earningRepository;
    private final InstrumentRepository instrumentRepository;
    private final AlphaVintageService alphaVintageService;
    private final ObjectMapper objectMapper;

    public EarningService(EarningRepository earningRepository, InstrumentRepository instrumentRepository,
                         AlphaVintageService alphaVintageService, ObjectMapper objectMapper) {
        this.earningRepository = earningRepository;
        this.instrumentRepository = instrumentRepository;
        this.alphaVintageService = alphaVintageService;
        this.objectMapper = objectMapper;
    }

    public List<EarningDto> loadAllEarnings() {
        return earningRepository
                .findAll(Sort.by(Sort.Direction.ASC, "symbol"))
                .stream()
                .map(nav -> objectMapper.convertValue(nav, EarningDto.class))
                .toList();
    }

    public List<EarningDto> findBySymbol(String symbol) {
        return earningRepository
                .findAllBySymbol(symbol)
                .stream()
                .map(nav -> objectMapper.convertValue(nav, EarningDto.class))
                .toList();
    }

    /**
     * Parse and save earnings data from CSV string.
     *
     * CSV Format (pipe-delimited):
     * symbol|name|reportDate|fiscalDateEnding|estimate|currency
     * AAPL|Apple Inc|2024-01-30|2023-12-31|1.25|USD
     *
     * @param csvString The CSV data as a string (pipe-delimited, with header row)
     * @return Number of new records created
     */
    @Transactional
    public int saveCSV(String csvString) {
        int rowCount = 0;
        List<InstrumentEntity> instrumentEntities = instrumentRepository.findAll();
        try (CSVParser csvRecords = CSVFormat.Builder.create()
                .setDelimiter(',')                 // pipe-delimited format
                .setHeader()                       // first row is header
                .setSkipHeaderRecord(true)         // skip header record
                .setIgnoreHeaderCase(true)         // case-insensitive header matching
                .setTrim(true)                     // trim whitespace
                .get()
                .parse(new StringReader(csvString))) {

            for (CSVRecord csvRecord : csvRecords) {
                String symbol = csvRecord.get("symbol");
                String name = csvRecord.get("name");
                LocalDate reportDate = LocalDate.parse(csvRecord.get("reportDate"));
                LocalDate fiscalDateEnding = LocalDate.parse(csvRecord.get("fiscalDateEnding"));
                String estimate = csvRecord.get("estimate");
                String currency = csvRecord.get("currency");
                Optional<InstrumentEntity> optionalInstrumentEntity =
                        instrumentEntities.stream()
                                .filter(instrumentEntity -> instrumentEntity.getTicker().equals(symbol))
                                .findFirst();

                if(optionalInstrumentEntity.isPresent()) {

                    EarningEntity earningEntity = earningRepository
                            .findBySymbolAndReportDateAndFiscalDateEnding(symbol, reportDate, fiscalDateEnding);
                    if(earningEntity == null) {
                        rowCount++;
                        earningEntity = new EarningEntity();
                        earningEntity.setSymbol(symbol);
                        earningEntity.setName(name);
                        earningEntity.setReportDate(reportDate);
                        earningEntity.setFiscalDateEnding(fiscalDateEnding);
                        earningEntity.setEstimate(estimate);
                        earningEntity.setCurrency(currency);
                    }

                    InstrumentEntity instrumentEntity = optionalInstrumentEntity.get();

                    if(instrumentEntity.getEarningDate() == null ||
                            instrumentEntity.getEarningDate().isBefore(LocalDate.now()) ||
                            instrumentEntity.getEarningDate().isAfter(reportDate)) {
                        instrumentEntity.setEarningDate(reportDate);
                        instrumentRepository.save(instrumentEntity);
                    }

                    earningRepository.save(earningEntity);
                    log.info("rowCount:{} symbol:{} reportDate:{} estimate:{}",rowCount, symbol, reportDate, estimate);
                }
            }
            return rowCount;

        } catch (Exception e) {
            throw new RuntimeException("Fail to parse CSV string: " + e.getMessage(),e);
        }
    }

    /**
     * Scheduled job orchestrator: Refresh earnings calendar data for all instruments.
     *
     * Fetches earnings data from Alpha Vantage API (CSV format) and persists to database
     * using the saveCSV() method. Implements error handling so that API failures don't
     * prevent the application from continuing.
     *
     * @return Summary string with refresh statistics: "{rowsProcessed}/0/0" on success
     *         or "0/0/1" on API error
     */
    @Transactional
    public String refreshEarningsDataForAllInstruments() {
        log.debug("Starting earnings calendar refresh for all instruments");

        try {
            // Fetch earnings data from Alpha Vantage API (returns CSV string)
            String csvResponse = alphaVintageService.getEarningsCalendar();

            if (csvResponse == null || csvResponse.trim().isEmpty()) {
                log.warn("No earnings data returned from Alpha Vantage API");
                return "0/0/1";
            }

            // Parse CSV string and save to database
            int newRecordsCount = saveCSV(csvResponse);

            log.info("Completed earnings refresh: newRecords={}", newRecordsCount);
            return newRecordsCount + "/0/0";

        } catch (Exception e) {
            log.error("Failed to refresh earnings data: {}", e.getMessage(), e);
            return "0/0/1";
        }
    }
}