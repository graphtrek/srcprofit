package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.PositionDto;
import co.grtk.srcprofit.mapper.PositionMapper;
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

    public PositionController(OptionService optionService) {
        this.optionService = optionService;
    }

    @GetMapping("/positionForm")
    public String getOptionForm(Model model) {
        model.addAttribute(MODEL_ATTRIBUTE_DTO, new PositionDto());
        model.addAttribute(MODEL_ATTRIBUTE_SUCCESS, null);
        return POSITION_FORM_PATH;
    }

    @PostMapping(path="/calculatePosition", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String calculatePosition(@RequestBody MultiValueMap<String, String> formData, Model model) {
        log.info("calculatePosition formData {}", formData);
        PositionDto positionDto = PositionMapper.mapFromData(formData);
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

    @GetMapping(path="/getPosition/{ticker}")
    public String getPosition(@PathVariable String ticker, Model model) {
        log.info("getPosition ticker {}", ticker);
        PositionDto positionDto = new PositionDto();
        positionDto.setTicker(ticker);
        fillPositionForm(positionDto, model);
        return POSITION_FORM_PATH;
    }

    private void fillPositionsPage(PositionDto positionDto, Model model) {
        List<PositionDto> openOptions = optionService.getAllOpenOptions(positionDto.getPositionsFromDate());
        model.addAttribute("openOptions", openOptions);
        List<PositionDto> optionHistory = optionService.getAllClosedOptions(positionDto.getPositionsFromDate());
        model.addAttribute("optionHistory", optionHistory);
        optionService.calculatePosition(positionDto,openOptions,optionHistory);
        model.addAttribute(MODEL_ATTRIBUTE_DTO, positionDto);
    }

    private void fillPositionForm(PositionDto positionDto, Model model) {
        List<PositionDto> optionHistory = optionService.getClosedOptionsByTicker(positionDto.getTicker());
        model.addAttribute(MODEL_ATTRIBUTE_OPTION_HISTORY, optionHistory);

        List<PositionDto> openOptions = optionService.getOpenOptionsByTicker(positionDto.getTicker());
        model.addAttribute(MODEL_ATTRIBUTE_OPTION_OPEN, openOptions);

        optionService.calculatePosition(positionDto,openOptions,optionHistory);
        model.addAttribute(MODEL_ATTRIBUTE_DTO, positionDto);
    }
}