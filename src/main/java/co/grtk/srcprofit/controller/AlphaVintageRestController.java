package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.service.AlphaVintageService;
import co.grtk.srcprofit.service.EarningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AlphaVintageRestController {
    private static final Logger log = LoggerFactory.getLogger(AlphaVintageRestController.class);
    private final AlphaVintageService alphaVintageService;
    private final EarningService earningService;

    public AlphaVintageRestController(AlphaVintageService alphaVintageService, EarningService earningService) {
        this.alphaVintageService = alphaVintageService;
        this.earningService = earningService;
    }

    @GetMapping("/earningsCalendar")
    public Integer getEarnings() throws Exception {
        long startTime = System.currentTimeMillis();
        String csv = alphaVintageService.getEarningsCalendar();
        int records = earningService.saveCSV(csv);
        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Processed earnings calendar from API, records: {}, elapsed: {}ms", records, elapsed);
        return records;
    }
}
