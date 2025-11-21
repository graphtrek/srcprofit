package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.FlexImportHistoryDto;
import co.grtk.srcprofit.service.FlexReportsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class FlexImportHistoryController {
    private static final Logger log = LoggerFactory.getLogger(FlexImportHistoryController.class);
    private final FlexReportsService flexReportsService;
    private final String FLEX_IMPORT_HISTORY_PAGE_PATH = "flex_import_history_jte";
    private final String MODEL_ATTRIBUTE_FLEX_IMPORTS = "flexImports";

    public FlexImportHistoryController(FlexReportsService flexReportsService) {
        this.flexReportsService = flexReportsService;
    }

    @GetMapping("/flexImportHistory")
    public String getFlexImportHistory(Model model) {
        log.debug("Loading FLEX import history");
        List<FlexImportHistoryDto> flexImports = flexReportsService.getAllImportHistory();
        log.debug("Loaded {} FLEX import records", flexImports.size());
        model.addAttribute(MODEL_ATTRIBUTE_FLEX_IMPORTS, flexImports);
        return FLEX_IMPORT_HISTORY_PAGE_PATH;
    }
}
