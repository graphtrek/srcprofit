package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.OpenPositionViewDto;
import co.grtk.srcprofit.dto.PositionDto;
import co.grtk.srcprofit.dto.StockPositionViewDto;
import co.grtk.srcprofit.service.NetAssetValueService;
import co.grtk.srcprofit.service.OpenPositionService;
import co.grtk.srcprofit.service.OptionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for the Open Positions view page.
 *
 * Provides a simplified view of open option positions with essential fields:
 * id, symbol, tradeDate, expirationDate, daysLeft, qty, strikePrice, underlyingPrice,
 * P&L, ROI, POP, and type.
 */
@Controller
public class OpenPositionsController {

    private final OpenPositionService openPositionService;
    private final NetAssetValueService netAssetValueService;
    private final OptionService optionService;
    public OpenPositionsController(OpenPositionService openPositionService,
                                   OptionService optionService,
                                   NetAssetValueService netAssetValueService) {
        this.openPositionService = openPositionService;
        this.netAssetValueService = netAssetValueService;
        this.optionService = optionService;
    }

    /**
     * Display the Open Positions page with all open option positions.
     *
     * @param model the model to add view attributes
     * @return the openpositions_jte view template name
     */
    @GetMapping("/openpositions")
    public String openPositions(Model model) {
        List<OpenPositionViewDto> openPositions = openPositionService.getAllOpenPositionViewDtos();
        model.addAttribute("openPositions", openPositions);

        List<StockPositionViewDto> stockPositions = openPositionService.getAllStockPositionViewDtos();
        model.addAttribute("stockPositions", stockPositions);

        // Get report date from latest NAV (same pattern as HomeController)
        // Falls back to today's date if no NAV exists
        LocalDate reportDate = LocalDate.now();
        var latestNav = netAssetValueService.loadLatestNetAssetValue();
        if (latestNav != null) {
            reportDate = latestNav.getReportDate();
        }
        model.addAttribute("reportDate", reportDate);

        // Calculate position summary (buy/sell obligations, premiums)
        PositionDto positionDto = new PositionDto();
        List<PositionDto> openOptions = openPositionService.getAllOpenOptionDtos(null);
        optionService.calculatePosition(positionDto, openOptions, List.of());

        // Get weekly positions (expiring within 7 days)
        List<PositionDto> weeklyOpenPositions = optionService.getWeeklySummaryOpenOptionDtos(openOptions);

        model.addAttribute("positionDto", positionDto);
        model.addAttribute("weeklyOpenPositions", weeklyOpenPositions);
        model.addAttribute("openOptions", openOptions);

        return "openpositions_jte";
    }
}
