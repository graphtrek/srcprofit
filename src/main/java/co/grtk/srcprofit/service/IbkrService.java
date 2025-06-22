package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.IbkrWatchlistDto;
import co.grtk.srcprofit.dto.InstrumentDto;
import co.grtk.srcprofit.entity.InstrumentEntity;
import co.grtk.srcprofit.repository.InstrumentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class IbkrService {
    private static final Logger log = LoggerFactory.getLogger(IbkrService.class);

    private final RestClient ibkrRestClient;
    private final ObjectMapper objectMapper;
    private final InstrumentRepository instrumentRepository;
    private final OptionService optionService;

    public IbkrService(RestClient ibkrRestClient,
                       ObjectMapper objectMapper,
                       InstrumentRepository instrumentRepository,
                       OptionService optionService) {
        this.ibkrRestClient = ibkrRestClient;
        this.objectMapper = objectMapper;
        this.instrumentRepository = instrumentRepository;
        this.optionService = optionService;
    }

    public IbkrWatchlistDto getIbkrWatchlist() {
        IbkrWatchlistDto ibkrWatchlistDto = ibkrRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/iserver/watchlist")
                        .queryParam("id", 100)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<IbkrWatchlistDto>() {
                });
        log.info("getIbkrWatchlist /iserver/watchlist returned {}",
                (ibkrWatchlistDto != null) ? ibkrWatchlistDto.getName() : null);
        return ibkrWatchlistDto;
    }

    @Transactional
    public List<InstrumentDto> refreshWatchlist(IbkrWatchlistDto ibkrWatchlistDto) {
        Objects.requireNonNull(ibkrWatchlistDto).getInstruments().forEach(instrumentDto -> {
            InstrumentEntity instrumentEntity = objectMapper.convertValue(instrumentDto, InstrumentEntity.class);
            instrumentRepository.save(instrumentEntity);
        });
        return loadWatchList();
    }

    public List<InstrumentDto> loadWatchList() {
        List<InstrumentEntity> ibkrInstrumentEntities = instrumentRepository.findAll(Sort.by(Sort.Direction.ASC, "ticker"));
        return Optional.of(ibkrInstrumentEntities)
                .map(entities -> objectMapper.convertValue(entities, new TypeReference<List<InstrumentDto>>() {}))
                .orElse(Collections.emptyList());
    }

}