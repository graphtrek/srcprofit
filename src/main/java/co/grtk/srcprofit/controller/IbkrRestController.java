package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.FlexStatementResponse;
import co.grtk.srcprofit.service.IbkrService;
import co.grtk.srcprofit.service.OptionService;
import com.ctc.wstx.io.CharsetNames;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

@RestController
public class IbkrRestController {
    private static final Logger log = LoggerFactory.getLogger(IbkrRestController.class);

    private final IbkrService ibkrService;
    private final OptionService optionService;

    public IbkrRestController(IbkrService ibkrService, OptionService optionService) {
        this.ibkrService = ibkrService;
        this.optionService = optionService;
    }

    @GetMapping("/ibkrFlexStatement")
    public FlexStatementResponse getFlexStatement() {
        return ibkrService.getFlexStatement();
    }

    @GetMapping("/ibkrFlexQuery/{referenceCode}")
    public String getFlexQuery(@PathVariable String referenceCode) {
        String flexQuery = ibkrService.getFlexQuery(referenceCode);
        log.info("getFlexQuery referenceCode {} returned {}", referenceCode, flexQuery);
        return flexQuery;
    }

    @GetMapping("/ibkrFlexImport")
    public int ibkrFlexImport() throws Exception {
        FlexStatementResponse flexStatementResponse = ibkrService.getFlexStatement();
        Thread.sleep(2000);
        String flexQuery = ibkrService.getFlexQuery(flexStatementResponse.getReferenceCode());
        String userHome = System.getProperty("user.home");
        File file = new File(userHome + "/FLEX_QUERY_" + flexStatementResponse.getReferenceCode() + ".csv");
        FileUtils.write(file, flexQuery, CharsetNames.CS_UTF8);
        log.info("ibkrFlexImport file {} written", file.getAbsolutePath());
        return optionService.csvToOptions(file.toPath());
    }

}