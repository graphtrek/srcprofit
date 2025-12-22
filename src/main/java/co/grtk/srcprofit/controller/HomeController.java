package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.ChartDataDto;
import co.grtk.srcprofit.dto.DashboardDto;
import co.grtk.srcprofit.dto.InstrumentDto;
import co.grtk.srcprofit.dto.PositionDto;
import co.grtk.srcprofit.mapper.Interval;
import co.grtk.srcprofit.service.InstrumentService;
import co.grtk.srcprofit.service.NetAssetValueService;
import co.grtk.srcprofit.service.OpenPositionService;
import co.grtk.srcprofit.service.OptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Arrays;
import java.util.List;

@Controller
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);
    private static final String INDEX_PAGE_PATH = "index_jte";
    private static final String DASHBOARD_PAGE_PATH = "dashboard_jte";
    private static final String IBKR_LOGIN_PAGE_PATH = "ibkr-login";
    private static final String MODEL_ATTRIBUTE_POSITION_DTO = "positionDto";
    private static final String MODEL_ATTRIBUTE_DASHBOARD_DTO = "dashboardDto";
    private static final String MODEL_ATTRIBUTE_DAILY_PREMIUM_DATES = "datesCsv";
    private static final String MODEL_ATTRIBUTE_DAILY_NAV_DATES = "navDatesCsv";
    private static final String MODEL_ATTRIBUTE_DAILY_PREMIUM_VALUES = "premiumsCsv";
    private static final String MODEL_ATTRIBUTE_DAILY_TOTAL_VALUES = "totalsCsv";
    private static final String MODEL_ATTRIBUTE_DAILY_CASH_VALUES = "cashCsv";
    private static final String MODEL_ATTRIBUTE_DAILY_STOCK_VALUES = "stockCsv";
    private static final String MODEL_ATTRIBUTE_DAILY_OPTION_VALUES = "optionsCsv";


    private final OptionService optionService;
    private final InstrumentService instrumentService;
    private final NetAssetValueService netAssetValueService;
    private final OpenPositionService openPositionService;

    public HomeController(OptionService optionService, InstrumentService instrumentService, NetAssetValueService netAssetValueService, OpenPositionService openPositionService) {
        this.optionService = optionService;
        this.instrumentService = instrumentService;
        this.netAssetValueService = netAssetValueService;
        this.openPositionService = openPositionService;
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

    @GetMapping("/dashboard/{interval}")
    public String positions(@PathVariable(required = false) String interval, Model model) {
        log.info("positions interval {}", interval);

        ChartDataDto chartDataDto =  new ChartDataDto(Interval.fromString(interval));
        optionService.getDailyPremium(chartDataDto);
        model.addAttribute(MODEL_ATTRIBUTE_DAILY_PREMIUM_DATES, chartDataDto.getDatesCsv());
        model.addAttribute(MODEL_ATTRIBUTE_DAILY_PREMIUM_VALUES, chartDataDto.getDailyPremiumCsv());

        netAssetValueService.getDailyNav(chartDataDto);
        model.addAttribute(MODEL_ATTRIBUTE_DAILY_NAV_DATES, chartDataDto.getNavDatesCsv());
        model.addAttribute(MODEL_ATTRIBUTE_DAILY_TOTAL_VALUES, chartDataDto.getDailyTotalCsv());
        model.addAttribute(MODEL_ATTRIBUTE_DAILY_CASH_VALUES, chartDataDto.getDailyCashCsv());
        model.addAttribute(MODEL_ATTRIBUTE_DAILY_STOCK_VALUES, chartDataDto.getDailyStockCsv());
        model.addAttribute(MODEL_ATTRIBUTE_DAILY_OPTION_VALUES, chartDataDto.getDailyOptionsCsv());

        List<InstrumentDto> instruments = instrumentService.findByTickers( Arrays.asList("QQQ", "GDX", "IBIT"));
        DashboardDto dashboardDto = new DashboardDto();
        for (InstrumentDto instrumentDto : instruments) {
            if ("QQQ".equals(instrumentDto.getTicker()))
                dashboardDto.QQQ = instrumentDto;
            else if ("GDX".equals(instrumentDto.getTicker()))
                dashboardDto.GDX = instrumentDto;
            else if("IBIT".equals(instrumentDto.getTicker()))
                dashboardDto.IBIT = instrumentDto;
        }
        model.addAttribute(MODEL_ATTRIBUTE_DASHBOARD_DTO, dashboardDto);

        // Get open positions and calculate aggregated metrics
        // ISSUE-051: Use OpenPositionService for authoritative IBKR snapshot data instead of OptionService trading history
        List<PositionDto> openOptions = optionService.getAllOpenOptionDtos(null);

        // Calculate position summary (buy/sell obligations, premiums)
        PositionDto positionDto = new PositionDto();

        optionService.calculatePosition(positionDto, openOptions, List.of());
        // Get weekly positions (expiring within 7 days)
        List<PositionDto> weeklyOpenPositions = optionService.getWeeklySummaryOpenOptionDtos(openOptions);

        // Load latest NAV for cash and stock values
        var latestNav = netAssetValueService.loadLatestNetAssetValue();
        if (latestNav != null) {
            positionDto.setCash(latestNav.getCash());
            positionDto.setStock(latestNav.getStock());
        }

        // Set latest daily NAV values for dashboard display
        positionDto.setCollectedPremium(chartDataDto.getLastDailyPremium());
        positionDto.setTotal(chartDataDto.getLastDailyTotal());
        positionDto.setOptions(chartDataDto.getLastDailyOptions());

        model.addAttribute(MODEL_ATTRIBUTE_POSITION_DTO, positionDto);
        model.addAttribute("weeklyOpenPositions", weeklyOpenPositions);
        model.addAttribute("openOptions", openOptions);
        return DASHBOARD_PAGE_PATH;
    }
}
