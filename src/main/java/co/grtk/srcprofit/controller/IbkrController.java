package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.IbkrInstrumentDto;
import co.grtk.srcprofit.service.IbkrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class IbkrController {
    private static final Logger LOG = LoggerFactory.getLogger(IbkrController.class);

    private final IbkrService ibkrService;

    public IbkrController(IbkrService ibkrService) {
        this.ibkrService = ibkrService;
    }

    @GetMapping("/watchlist")
    public  List<IbkrInstrumentDto> watchlist() {
        List<IbkrInstrumentDto> ibkrInstrumentDtoList = ibkrService.loadWatchList();
        if (ibkrInstrumentDtoList.isEmpty()) {
            ibkrInstrumentDtoList = ibkrService.refreshWatchlist();
        }
        return ibkrInstrumentDtoList;
    }


}
