package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.ChartDataDto;
import co.grtk.srcprofit.dto.NetAssetValueDto;
import co.grtk.srcprofit.dto.PositionDto;
import co.grtk.srcprofit.entity.NetAssetValueEntity;
import co.grtk.srcprofit.mapper.Interval;
import co.grtk.srcprofit.repository.NetAssetValueRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static co.grtk.srcprofit.mapper.MapperUtils.parseDouble;
import static co.grtk.srcprofit.mapper.MapperUtils.round2Digits;
import static org.apache.commons.csv.CSVParser.parse;

@Service
public class NetAssetValueService {
    private final NetAssetValueRepository netAssetValueRepository;
    private final ObjectMapper objectMapper;

    public NetAssetValueService(NetAssetValueRepository netAssetValueRepository, ObjectMapper objectMapper) {
        this.netAssetValueRepository = netAssetValueRepository;
        this.objectMapper = objectMapper;
    }

    public NetAssetValueDto loadLatestNetAssetValue() {
        NetAssetValueEntity netAssetValueEntity = netAssetValueRepository.findTopByOrderByReportDateDesc();
        if (netAssetValueEntity == null)
            return null;
        return objectMapper.convertValue(netAssetValueEntity, NetAssetValueDto.class);
    }

    public List<NetAssetValueDto> loadAllNetAssetValues() {
        List<NetAssetValueDto> navList = new java.util.ArrayList<>(netAssetValueRepository
                .findAll()
                .stream()
                .map(nav -> objectMapper.convertValue(nav, NetAssetValueDto.class))
                .toList());
        navList.sort(Comparator.comparing(NetAssetValueDto::getReportDate));
        return navList;
    }

    public void getDailyNav(ChartDataDto chartDataDto) {
        if (Interval.ALL.equals(chartDataDto.getInterval())) {
            List<NetAssetValueEntity> navs =  netAssetValueRepository.findAll();
            chartDataDto.setDailyTotal(getDailyTotal(navs));
            chartDataDto.setDailyCash(getDailyCash(navs));
            chartDataDto.setDailyStock(getDailyStock(navs));
            chartDataDto.setDailyOptions(getDailyOptions(navs));
        } else {
            List<NetAssetValueEntity> navs =
                    netAssetValueRepository.findBetweenDates(chartDataDto.getStartDate(), chartDataDto.getEndDate());
            chartDataDto.setDailyTotal(getDailyTotal(navs));
            chartDataDto.setDailyCash(getDailyCash(navs));
            chartDataDto.setDailyStock(getDailyStock(navs));
            chartDataDto.setDailyOptions(getDailyOptions(navs));
        }
    }

    private Map<LocalDate, BigDecimal> getDailyTotal(List<NetAssetValueEntity> navs) {
        return navs.stream()
                .collect(Collectors.groupingBy(
                        NetAssetValueEntity::getReportDate,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                nav -> BigDecimal.valueOf(nav.getTotal()),
                                BigDecimal::add
                        )
                ));
    }

    private Map<LocalDate, BigDecimal> getDailyCash(List<NetAssetValueEntity> navs) {
        return navs.stream()
                .collect(Collectors.groupingBy(
                        NetAssetValueEntity::getReportDate,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                nav -> BigDecimal.valueOf(nav.getCash()),
                                BigDecimal::add
                        )
                ));
    }


    private Map<LocalDate, BigDecimal> getDailyStock(List<NetAssetValueEntity> navs) {
        return navs.stream()
                .collect(Collectors.groupingBy(
                        NetAssetValueEntity::getReportDate,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                nav -> BigDecimal.valueOf(nav.getStock()),
                                BigDecimal::add
                        )
                ));
    }

    private Map<LocalDate, BigDecimal> getDailyOptions(List<NetAssetValueEntity> navs) {
        return navs.stream()
                .collect(Collectors.groupingBy(
                        NetAssetValueEntity::getReportDate,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                nav -> BigDecimal.valueOf(nav.getOptions()),
                                BigDecimal::add
                        )
                ));
    }

    @Transactional
    public int saveCSV(Path path) throws IOException {
        int rowCount = 0;
        try (CSVParser csvRecords = parse(path, StandardCharsets.UTF_8,
                CSVFormat.Builder.create()
                        .setHeader()                   // első sor fejléc
                        .setSkipHeaderRecord(true)    // ne olvassa be újra a fejlécet
                        .setIgnoreHeaderCase(true)    // fejlécmezők kis/nagybetű érzéketlenek
                        .setTrim(true)                // whitespace-ek levágása
                        .get())) {

            for (CSVRecord csvRecord : csvRecords) {
                String account = csvRecord.get("ClientAccountID");
                LocalDate reportDate = LocalDate.parse(csvRecord.get("reportDate"));
                Double total = 0.0;
                if(csvRecords.getHeaderMap().get("total") != null)
                    total = parseDouble(csvRecord.get("total"),0.0);

                NetAssetValueEntity netAssetValueEntity = netAssetValueRepository.findByReportDate(reportDate);
                if (netAssetValueEntity == null) {
                    Double cash = Double.parseDouble(csvRecord.get("cash"));
                    Double stock = Double.parseDouble(csvRecord.get("stock"));
                    Double options = Double.parseDouble(csvRecord.get("options"));
                    Double dividendAccruals = Double.parseDouble(csvRecord.get("dividendAccruals"));
                    Double interestAccruals = Double.parseDouble(csvRecord.get("interestAccruals"));

                    netAssetValueEntity = new NetAssetValueEntity();
                    netAssetValueEntity.setAccount(account);
                    netAssetValueEntity.setReportDate(reportDate);
                    netAssetValueEntity.setCash(round2Digits(cash));
                    netAssetValueEntity.setStock(round2Digits(stock));
                    netAssetValueEntity.setOptions(round2Digits(options));
                    netAssetValueEntity.setDividendAccruals(round2Digits(dividendAccruals));
                    netAssetValueEntity.setInterestAccruals(round2Digits(interestAccruals));
                    netAssetValueEntity.setTotal(round2Digits(total));
                    netAssetValueRepository.save(netAssetValueEntity);
                    rowCount++;
                }
            }
        }
        return rowCount;
    }
}
