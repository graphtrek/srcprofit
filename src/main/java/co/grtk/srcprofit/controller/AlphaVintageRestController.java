package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.service.AlphaVintageService;
import co.grtk.srcprofit.service.EarningService;
import com.ctc.wstx.io.CharsetNames;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

@RestController
public class AlphaVintageRestController {
    private static final Logger log = LoggerFactory.getLogger(AlphaVintageRestController.class);
    private final String userHome = System.getProperty("user.home");
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
        File file = new File(userHome + "/EARNINGS_" + startTime + ".csv");
        FileUtils.write(file, csv, CharsetNames.CS_UTF8);
        int records = earningService.saveCSV(file.toPath());
        long elapsed = System.currentTimeMillis() - startTime;
        log.info("getEarnings file {} written elapsed:{}", file.getAbsolutePath(), elapsed);
        return records;
    }
}
