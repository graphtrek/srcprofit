package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.OpenPositionViewDto;
import co.grtk.srcprofit.service.OpenPositionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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

    public OpenPositionsController(OpenPositionService openPositionService) {
        this.openPositionService = openPositionService;
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
        return "openpositions_jte";
    }
}
