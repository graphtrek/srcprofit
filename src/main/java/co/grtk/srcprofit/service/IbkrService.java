package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.IbkrInstrumentDto;
import co.grtk.srcprofit.dto.IbkrWatchlistDto;
import co.grtk.srcprofit.entity.IbkrInstrumentEntity;
import co.grtk.srcprofit.repository.IbkrInstrumentRepository;
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
    private final IbkrInstrumentRepository ibkrInstrumentRepository;

    public IbkrService(RestClient ibkrRestClient, ObjectMapper objectMapper, IbkrInstrumentRepository ibkrInstrumentRepository) {
        this.ibkrRestClient = ibkrRestClient;
        this.objectMapper = objectMapper;
        this.ibkrInstrumentRepository = ibkrInstrumentRepository;
    }

    @Transactional
    public List<IbkrInstrumentDto> refreshWatchlist() {

        IbkrWatchlistDto ibkrWatchlistDto =ibkrRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/iserver/watchlist")
                        .queryParam("id", 100)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<IbkrWatchlistDto>() {
                });


        log.info("refreshWatchlist /iserver/watchlist returned {}",(ibkrWatchlistDto != null) ? ibkrWatchlistDto.getName() : null);

        Objects.requireNonNull(ibkrWatchlistDto).getInstruments().forEach(instrument -> {
            IbkrInstrumentEntity ibkrInstrumentEntity = objectMapper.convertValue(instrument, IbkrInstrumentEntity.class);
            ibkrInstrumentRepository.save(ibkrInstrumentEntity);
        });

        return loadWatchList();
    }

    public List<IbkrInstrumentDto> loadWatchList() {
        List<IbkrInstrumentEntity> ibkrInstrumentEntities = ibkrInstrumentRepository.findAll(Sort.by(Sort.Direction.ASC, "ticker"));
        return Optional.of(ibkrInstrumentEntities)
                .map(entities -> objectMapper.convertValue(entities, new TypeReference<List<IbkrInstrumentDto>>() {}))
                .orElse(Collections.emptyList());
    }
}