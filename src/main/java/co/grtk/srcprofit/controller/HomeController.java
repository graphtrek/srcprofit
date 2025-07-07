package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.PositionDto;
import co.grtk.srcprofit.service.OptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);
    private static final String INDEX_PAGE_PATH = "index";
    private static final String DASHBOARD_PAGE_PATH = "dashboard";
    private static final String IBKR_LOGIN_PAGE_PATH = "ibkr-login";
    private static final String MODEL_ATTRIBUTE_DTO = "positionDto";
    private static final String MODEL_ATTRIBUTE_DAILY_PREMIUM_DATES = "datesCsv";
    private static final String MODEL_ATTRIBUTE_DAILY_PREMIUM_VALUES = "premiumsCsv";

    private final OptionService optionService;

    public HomeController(OptionService optionService) {
        this.optionService = optionService;
    }

    @GetMapping("/")
    public String home(Model model) {
        return INDEX_PAGE_PATH;
    }

    @GetMapping("/index")
    public String index(Model model) {
        return INDEX_PAGE_PATH;
    }

    @GetMapping("/ibkrLogin")
    public String ibkrLogin(Model model) {
        return IBKR_LOGIN_PAGE_PATH;
    }

    @GetMapping("/dashboard")
    public String positions(Model model) {
        Map<LocalDate, BigDecimal> dailyPremium = optionService.getDailyPremium();

        String datesCsv = dailyPremium.keySet().stream()
                .sorted()
                .map(date -> "\"" + date.toString() + "\"")
                .collect(Collectors.joining(","));
        log.info("datesCsv {}", datesCsv);
        model.addAttribute(MODEL_ATTRIBUTE_DAILY_PREMIUM_DATES, datesCsv);

        String premiumsCsv = dailyPremium.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getValue().toString())
                .collect(Collectors.joining(","));
        log.info("premiumsCsv {}", premiumsCsv);
        model.addAttribute(MODEL_ATTRIBUTE_DAILY_PREMIUM_VALUES, premiumsCsv);

        PositionDto positionDto = new PositionDto();
        model.addAttribute(MODEL_ATTRIBUTE_DTO, positionDto);
        return DASHBOARD_PAGE_PATH;
    }
}
