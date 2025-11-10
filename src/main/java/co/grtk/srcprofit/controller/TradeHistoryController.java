package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.PositionDto;
import co.grtk.srcprofit.service.OptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;
import java.util.List;

import static co.grtk.srcprofit.mapper.MapperUtils.toLocalDate;

@Controller
public class TradeHistoryController {

    private static final Logger log = LoggerFactory.getLogger(TradeHistoryController.class);
    private static final String TRADE_HISTORY_PAGE_PATH = "trade_history_jte";
    private static final String MODEL_ATTRIBUTE_CLOSED_POSITIONS = "closedPositions";

    private final OptionService optionService;

    public TradeHistoryController(OptionService optionService) {
        this.optionService = optionService;
    }

    @GetMapping("/tradehistory")
    public String tradehistory(Model model) {
        fillTradeHistoryPage(null, model);
        return TRADE_HISTORY_PAGE_PATH;
    }

    @PostMapping(value = "/tradehistoryFromDate", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String tradehistoryFromDate(@RequestBody MultiValueMap<String, String> formData, Model model) {
        log.info("tradehistoryFromDate formData {}", formData);
        LocalDate fromDate = toLocalDate(formData.getFirst("tradeDate"));
        fillTradeHistoryPage(fromDate, model);
        return TRADE_HISTORY_PAGE_PATH;
    }

    private void fillTradeHistoryPage(LocalDate fromDate, Model model) {
        List<PositionDto> closedPositions = optionService.getAllClosedOptions(fromDate);
        model.addAttribute(MODEL_ATTRIBUTE_CLOSED_POSITIONS, closedPositions);
    }
}
