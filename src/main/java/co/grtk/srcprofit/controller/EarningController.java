package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.EarningDto;
import co.grtk.srcprofit.service.EarningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class EarningController {
    private static final Logger log = LoggerFactory.getLogger(EarningController.class);
    private static final String EARNINGS_PAGE_PATH = "earnings_jte";
    private static final String MODEL_ATTRIBUTE_EARNINGS = "earnings";
    private final EarningService earningService;

    public EarningController(EarningService earningService) {
        this.earningService = earningService;
    }

    @GetMapping("/earnings")
    public String getNetAssetValues(Model model) {
        List<EarningDto> earningDtos = earningService.loadAllEarnings();
        model.addAttribute(MODEL_ATTRIBUTE_EARNINGS, earningDtos);
        return EARNINGS_PAGE_PATH;
    }
}