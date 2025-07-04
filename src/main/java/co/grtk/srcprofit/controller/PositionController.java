package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.InstrumentDto;
import co.grtk.srcprofit.dto.PositionDto;
import co.grtk.srcprofit.mapper.PositionMapper;
import co.grtk.srcprofit.service.AlpacaService;
import co.grtk.srcprofit.service.InstrumentService;
import co.grtk.srcprofit.service.OptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static co.grtk.srcprofit.mapper.PositionMapper.round2Digits;

@Controller
public class PositionController {

    private static final Logger log = LoggerFactory.getLogger(PositionController.class);
    private static final String POSITION_FORM_PATH = "position-form";
    private static final String MODEL_ATTRIBUTE_DTO = "positionDto";
    private static final String MODEL_ATTRIBUTE_OPTION_OPEN = "openOptions";
    private static final String MODEL_ATTRIBUTE_OPTION_HISTORY = "optionHistory";
    private static final String MODEL_ATTRIBUTE_SUCCESS = "success";
    private static final String POSITIONS_PAGE_PATH = "positions";

    private final OptionService optionService;
    private final InstrumentService instrumentService;
    private final AlpacaService alpacaService;

    public PositionController(OptionService optionService, InstrumentService instrumentService, AlpacaService alpacaService) {
        this.optionService = optionService;
        this.instrumentService = instrumentService;
        this.alpacaService = alpacaService;
    }

    @GetMapping("/calculatePosition")
    public String getPositionForm(Model model) {
        model.addAttribute(MODEL_ATTRIBUTE_SUCCESS, null);
        PositionDto positionDto = new PositionDto();
        positionDto.setTradeDate(LocalDate.now());
        model.addAttribute(MODEL_ATTRIBUTE_DTO, positionDto);
        return POSITION_FORM_PATH;
    }

    @PostMapping(path = "/calculatePosition", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String calculatePosition(@RequestBody MultiValueMap<String, String> formData, Model model) {
        log.info("calculatePosition formData {}", formData);
        PositionDto positionDto = PositionMapper.mapFromData(formData);
        getMarketValue(positionDto);
        fillPositionForm(positionDto, model);
        return POSITION_FORM_PATH;
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
        LocalDate fromDate = PositionMapper.toLocalDate(formData.getFirst("fromDate"));
        PositionDto positionDto = new PositionDto();
        positionDto.setPositionsFromDate(fromDate);
        fillPositionsPage(positionDto, model);
        return POSITIONS_PAGE_PATH;
    }

    @GetMapping(path = "/getPosition/{ticker}")
    public String getPosition(@PathVariable String ticker, Model model) {
        log.info("getPosition ticker {}", ticker);
        PositionDto positionDto = new PositionDto();
        positionDto.setTicker(ticker);
        getMarketValue(positionDto);
        fillPositionForm(positionDto, model);
        return POSITION_FORM_PATH;
    }

    private void fillPositionsPage(PositionDto positionDto, Model model) {
        List<PositionDto> openOptions = optionService.getAllOpenOptions(positionDto.getPositionsFromDate());
        model.addAttribute(MODEL_ATTRIBUTE_OPTION_OPEN, openOptions);
        List<PositionDto> optionHistory = optionService.getAllClosedOptions(positionDto.getPositionsFromDate());
        model.addAttribute(MODEL_ATTRIBUTE_OPTION_HISTORY, optionHistory);
        optionService.calculatePosition(positionDto, openOptions, optionHistory);
        model.addAttribute(MODEL_ATTRIBUTE_DTO, positionDto);
    }

    private void fillPositionForm(PositionDto positionDto, Model model) {
        List<PositionDto> optionHistory = optionService.getClosedOptionsByTicker(positionDto.getTicker());
        model.addAttribute(MODEL_ATTRIBUTE_OPTION_HISTORY, optionHistory);

        List<PositionDto> openOptions = optionService.getOpenOptionsByTicker(positionDto.getTicker());
        model.addAttribute(MODEL_ATTRIBUTE_OPTION_OPEN, openOptions);
        if (openOptions.isEmpty() && positionDto.getTicker() != null && positionDto.getMarketValue() == null) {
            InstrumentDto instrumentDto = instrumentService.loadInstrumentByTicker(positionDto.getTicker());
            Optional.ofNullable(instrumentDto).ifPresent(instrumentDto1 ->
                    positionDto.setMarketValue(instrumentDto1.getPrice() != null ? instrumentDto1.getPrice() * 100 : 0));
        }
        optionService.calculatePosition(positionDto, openOptions, optionHistory);
        model.addAttribute(MODEL_ATTRIBUTE_DTO, positionDto);
    }

    private void getMarketValue(PositionDto positionDto) {
        Optional.ofNullable(alpacaService.getMarketDataSnapshot(positionDto.getTicker()))
                .map(data -> {
                    instrumentService.saveAlpacaQuotes(data);
                    return data.getQuotes();
                })
                .map(quotes -> quotes.get(positionDto.getTicker()))
                .map(alpacaSingleAssetDto -> alpacaSingleAssetDto.getLatestTrade().getPrice())
                .ifPresent(price -> positionDto.setMarketValue(round2Digits(price * 100)));
    }
}