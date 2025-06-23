package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.InstrumentDto;
import co.grtk.srcprofit.dto.MarketDataDto;
import co.grtk.srcprofit.entity.InstrumentEntity;
import co.grtk.srcprofit.mapper.PositionMapper;
import co.grtk.srcprofit.repository.InstrumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InstrumentService {
    private final InstrumentRepository instrumentRepository;
    private final ObjectMapper objectMapper;
    Logger log = LoggerFactory.getLogger(InstrumentService.class);

    public InstrumentService(InstrumentRepository instrumentRepository, ObjectMapper objectMapper) {
        this.instrumentRepository = instrumentRepository;
        this.objectMapper = objectMapper;
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
            if(instrumentDto.getTicker()==null || instrumentDto.getConid() == null) {
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
    public void saveMarketData(List<MarketDataDto> marketDataDtoList) {
        List<InstrumentEntity> ibkrInstrumentEntities = instrumentRepository.findAllInstrument();
        for ( InstrumentEntity instrumentEntity : ibkrInstrumentEntities) {
            Optional<MarketDataDto> result = marketDataDtoList.stream()
                    .filter(dto -> dto.getConid().equals(instrumentEntity.getConid()))
                    .findFirst();
            if (result.isPresent()) {
                MarketDataDto marketDataDto = result.get();
                instrumentEntity.setPrice(PositionMapper.parseDouble(marketDataDto.getPriceStr(), instrumentEntity.getPrice()));
                instrumentEntity.setUpdated(marketDataDto.getUpdated());
                instrumentEntity.setName(marketDataDto.getCompanyName());
                if(marketDataDto.getChange()!=null)
                    instrumentEntity.setChange(marketDataDto.getChange());
                if(marketDataDto.getChangePercent()!=null)
                    instrumentEntity.setChangePercent(marketDataDto.getChangePercent());
                instrumentRepository.save(instrumentEntity);
            }
        }
    }

}
