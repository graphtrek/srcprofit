package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.AlpacaMarketDataDto;
import co.grtk.srcprofit.dto.AlpacaSingleAssetDto;
import co.grtk.srcprofit.dto.IbkrMarketDataDto;
import co.grtk.srcprofit.dto.InstrumentDto;
import co.grtk.srcprofit.entity.InstrumentEntity;
import co.grtk.srcprofit.mapper.PositionMapper;
import co.grtk.srcprofit.repository.InstrumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InstrumentService {
    private final InstrumentRepository instrumentRepository;
    private final ObjectMapper objectMapper;
    Logger log = LoggerFactory.getLogger(InstrumentService.class);

    public InstrumentService(InstrumentRepository instrumentRepository, ObjectMapper objectMapper) {
        this.instrumentRepository = instrumentRepository;
        this.objectMapper = objectMapper;
    }

    private static InstrumentEntity getInstrumentEntity(Map.Entry<String, AlpacaSingleAssetDto> alpacaMarketDataDtoEntry, Optional<InstrumentEntity> result) {
        InstrumentEntity instrumentEntity;
        if (result.isPresent()) {
            instrumentEntity = result.get();
            instrumentEntity.setPrice(alpacaMarketDataDtoEntry.getValue().getLatestTrade().getPrice());
        } else {
            instrumentEntity = new InstrumentEntity();
            instrumentEntity.setTicker(alpacaMarketDataDtoEntry.getKey());
        }
        instrumentEntity.setPrice(alpacaMarketDataDtoEntry.getValue().getLatestTrade().getPrice());
        return instrumentEntity;
    }

    public List<InstrumentDto> loadAllInstruments() {
        List<InstrumentEntity> ibkrInstrumentEntities = instrumentRepository.findAllInstrument();
        return ibkrInstrumentEntities.stream()
                .map(entity -> objectMapper.convertValue(entity, InstrumentDto.class)).toList();
    }

    public InstrumentDto loadInstrumentByTicker(String ticker) {
        InstrumentEntity instrumentEntity = instrumentRepository.findByTicker(ticker);
        return objectMapper.convertValue(instrumentEntity, InstrumentDto.class);
    }

    @Transactional
    public void refreshWatchlist(List<InstrumentDto> instruments) {
        for (InstrumentDto instrumentDto : instruments) {
            if (instrumentDto.getTicker() == null || instrumentDto.getConid() == null) {
                log.warn("Null instrument in watchlist returned {}", instrumentDto);
                continue;
            }
            InstrumentEntity instrumentEntity = instrumentRepository.findByTicker(instrumentDto.getTicker());
            if (instrumentEntity != null) {
                instrumentEntity.setConid(instrumentDto.getConid());
                instrumentEntity.setName(instrumentDto.getName());
            } else {
                instrumentEntity = objectMapper.convertValue(instrumentDto, InstrumentEntity.class);
            }
            instrumentRepository.save(instrumentEntity);
        }
    }

    @Transactional
    public void saveIbkrMarketData(List<IbkrMarketDataDto> ibkrMarketDataDtoList) {
        List<InstrumentEntity> ibkrInstrumentEntities = instrumentRepository.findAllInstrument();
        for (InstrumentEntity instrumentEntity : ibkrInstrumentEntities) {
            Optional<IbkrMarketDataDto> result = ibkrMarketDataDtoList.stream()
                    .filter(dto -> dto.getConid().equals(instrumentEntity.getConid()))
                    .findFirst();
            if (result.isPresent()) {
                IbkrMarketDataDto ibkrMarketDataDto = result.get();
                instrumentEntity.setPrice(PositionMapper.parseDouble(ibkrMarketDataDto.getPriceStr(), instrumentEntity.getPrice()));
                instrumentEntity.setUpdated(ibkrMarketDataDto.getUpdated());
                instrumentEntity.setName(ibkrMarketDataDto.getCompanyName());
                if (ibkrMarketDataDto.getChange() != null)
                    instrumentEntity.setChange(ibkrMarketDataDto.getChange());
                if (ibkrMarketDataDto.getChangePercent() != null)
                    instrumentEntity.setChangePercent(ibkrMarketDataDto.getChangePercent());
                instrumentRepository.save(instrumentEntity);
            }
        }
    }

    @Transactional
    public void saveAlpacaMarketData(AlpacaMarketDataDto alpacaMarketDataDto) {
        List<InstrumentEntity> instrumentEntities = instrumentRepository.findAllInstrument();
        for (InstrumentEntity instrumentEntity : instrumentEntities) {
            Optional<Map.Entry<String, AlpacaSingleAssetDto>> result = alpacaMarketDataDto.getQuotes().entrySet().stream()
                    .filter(dto -> dto.getKey().equals(instrumentEntity.getTicker()))
                    .findFirst();
            if (result.isPresent()) {
                Map.Entry<String, AlpacaSingleAssetDto> alpacaSingleAssetDtoEntry = result.get();
                instrumentEntity.setPrice(alpacaSingleAssetDtoEntry.getValue().getLatestTrade().getPrice());
                instrumentRepository.save(instrumentEntity);
            }
        }
    }

    @Transactional
    public void saveAlpacaQuotes(AlpacaMarketDataDto alpacaMarketDataDto) {
        List<InstrumentEntity> ibkrInstrumentEntities = instrumentRepository.findAllInstrument();
        for (Map.Entry<String, AlpacaSingleAssetDto> alpacaMarketDataDtoEntry : alpacaMarketDataDto.getQuotes().entrySet()) {
            Optional<InstrumentEntity> result = ibkrInstrumentEntities.stream()
                    .filter(instrumentEntity -> alpacaMarketDataDtoEntry.getKey().equals(instrumentEntity.getTicker()))
                    .findFirst();
            InstrumentEntity instrumentEntity = getInstrumentEntity(alpacaMarketDataDtoEntry, result);
            instrumentRepository.save(instrumentEntity);
        }
    }

    public String buildTickerCsv(List<InstrumentDto> instruments) {
        return instruments.stream()
                .map(dto -> String.valueOf(dto.getTicker()))
                .collect(Collectors.joining(","));
    }

    public String buildConidCsv(List<InstrumentDto> instruments) {
        return instruments.stream()
                .map(dto -> String.valueOf(dto.getConid()))
                .collect(Collectors.joining(","));
    }

}
