package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.NetAssetValueDto;
import co.grtk.srcprofit.dto.PositionDto;
import co.grtk.srcprofit.service.AlpacaService;
import co.grtk.srcprofit.service.InstrumentService;
import co.grtk.srcprofit.service.NetAssetValueService;
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
public class TradeLogController {

    private static final Logger log = LoggerFactory.getLogger(TradeLogController.class);
    private static final String POSITIONS_PAGE_PATH = "positions_jte";
    private static final String MODEL_ATTRIBUTE_DTO = "positionDto";
    private static final String MODEL_ATTRIBUTE_WEEKLY_OPTION_OPEN = "weeklyOpenPositions";
    private static final String MODEL_ATTRIBUTE_OPTION_OPEN = "openOptions";
    private static final String MODEL_ATTRIBUTE_OPTION_HISTORY = "optionHistory";

    private final OptionService optionService;
    private final InstrumentService instrumentService;
    private final AlpacaService alpacaService;
    private final NetAssetValueService netAssetValueService;

    public TradeLogController(
            OptionService optionService,
            InstrumentService instrumentService,
            AlpacaService alpacaService,
            NetAssetValueService netAssetValueService) {
        this.optionService = optionService;
        this.instrumentService = instrumentService;
        this.alpacaService = alpacaService;
        this.netAssetValueService = netAssetValueService;
    }

    @GetMapping("/positions")
    public String positions(Model model) {
        PositionDto positionDto = new PositionDto();
        fillPositionsPage(positionDto, model);
        return POSITIONS_PAGE_PATH;
    }

    @PostMapping(value = "/getPositionsFromDate", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String getPositionsFromDate(@RequestBody MultiValueMap<String, String> formData, Model model) {
        log.info("getPositionsFromDate formData {}", formData);
        LocalDate fromDate = toLocalDate(formData.getFirst("fromDate"));
        PositionDto positionDto = new PositionDto();
        positionDto.setPositionsFromDate(fromDate);
        fillPositionsPage(positionDto, model);
        return POSITIONS_PAGE_PATH;
    }

    private void fillPositionsPage(PositionDto positionDto, Model model) {
        List<PositionDto> openOptions = optionService.getAllOpenPositions(positionDto.getPositionsFromDate());
        model.addAttribute(MODEL_ATTRIBUTE_OPTION_OPEN, openOptions);
        List<PositionDto> optionHistory = optionService.getAllClosedOptions(positionDto.getPositionsFromDate());
        model.addAttribute(MODEL_ATTRIBUTE_OPTION_HISTORY, optionHistory);
        optionService.calculatePosition(positionDto, openOptions, optionHistory);
        NetAssetValueDto netAssetValueDto = netAssetValueService.loadLatestNetAssetValue();
        if(netAssetValueDto == null)
            netAssetValueDto = new NetAssetValueDto();
        positionDto.setCash(netAssetValueDto.getCash());
        positionDto.setStock(netAssetValueDto.getStock());
        model.addAttribute(MODEL_ATTRIBUTE_DTO, positionDto);
        List<PositionDto> weeklyOpenPositions = optionService.getWeeklyOpenPositions(openOptions);
        model.addAttribute(MODEL_ATTRIBUTE_WEEKLY_OPTION_OPEN, weeklyOpenPositions);
    }
}
