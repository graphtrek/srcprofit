package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.InstrumentDto;
import co.grtk.srcprofit.service.IbkrService;
import co.grtk.srcprofit.service.OptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;

@RestController
public class IbkrController {
    private static final Logger LOG = LoggerFactory.getLogger(IbkrController.class);

    private final IbkrService ibkrService;
    private final OptionService optionService;

    public IbkrController(IbkrService ibkrService, OptionService optionService) {
        this.ibkrService = ibkrService;
        this.optionService = optionService;
    }

    @GetMapping("/watchlist")
    public  List<InstrumentDto> watchlist() {
        List<InstrumentDto> instrumentDtoList = ibkrService.loadWatchList();
        if (instrumentDtoList.isEmpty()) {
            instrumentDtoList = ibkrService.refreshWatchlist();
        }
        return instrumentDtoList;
    }

    @GetMapping("/import")
    public void importWatchlist() {
        optionService.csvToOptions(new File("/Users/Imre/tmp/ITATAI_OPTIONS.csv").toPath());
    }

}