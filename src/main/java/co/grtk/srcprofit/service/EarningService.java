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

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.csv.CSVParser.parse;

@Service
public class EarningService {
    private static final Logger log = LoggerFactory.getLogger(EarningService.class);

    private final EarningRepository earningRepository;
    private final InstrumentRepository instrumentRepository;
    private final ObjectMapper objectMapper;

    public EarningService(EarningRepository earningRepository, InstrumentRepository instrumentRepository, ObjectMapper objectMapper) {
        this.earningRepository = earningRepository;
        this.instrumentRepository = instrumentRepository;
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

    @Transactional
    public int saveCSV(Path path) {
        int rowCount = 0;
        List<InstrumentEntity> instrumentEntities = instrumentRepository.findAll();
        try (CSVParser csvRecords = parse(path, StandardCharsets.UTF_8,
                CSVFormat.Builder.create()
                        .setHeader()                   // első sor fejléc
                        .setSkipHeaderRecord(true)    // ne olvassa be újra a fejlécet
                        .setIgnoreHeaderCase(true)    // fejlécmezők kis/nagybetű érzéketlenek
                        .setTrim(true)                // whitespace-ek levágása
                        .get())) {

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
            throw new RuntimeException("Fail to parse CSV file: " + e.getMessage(),e);
        }
    }
}