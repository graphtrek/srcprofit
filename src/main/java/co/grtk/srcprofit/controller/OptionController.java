package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.OptionDto;
import co.grtk.srcprofit.mapper.OptionMapper;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class OptionController {

    private static final Logger log = LoggerFactory.getLogger(OptionController.class);
    private static final String OPTION_FORM_PATH = "option-form";
    private static final String MODEL_ATTRIBUTE_DTO = "optionDto";
    private static final String MODEL_ATTRIBUTE_SUCCESS = "success";
    private static final String MODEL_ATTRIBUTE_ERROR = "error";

    private final OptionService optionService;

    public OptionController(OptionService optionService) {
        this.optionService = optionService;
    }

    @GetMapping("/option")
    public String getOptionForm(Model model) {
        model.addAttribute(MODEL_ATTRIBUTE_DTO, new OptionDto());
        model.addAttribute(MODEL_ATTRIBUTE_SUCCESS, null);
        return OPTION_FORM_PATH;
    }

    @PostMapping(path="/copy-option", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String copyOption(@RequestBody MultiValueMap<String, String> formData, Model model) {
        log.info("copyOption formData {}", formData);

        OptionDto optionDto = OptionMapper.mapFromData(formData);

        model.addAttribute(MODEL_ATTRIBUTE_DTO, optionDto);
        model.addAttribute(MODEL_ATTRIBUTE_SUCCESS, MODEL_ATTRIBUTE_SUCCESS);
        log.info("Copied option {}", optionDto);
        return OPTION_FORM_PATH;
    }

    @PostMapping(path="/save-option", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String saveOption(@RequestBody MultiValueMap<String, String> formData, Model model) {
        log.info("saveOption formData {}", formData);
        OptionDto optionDto = OptionMapper.mapFromData(formData);
        try {
            optionDto = optionService.saveOption(optionDto);
            model.addAttribute(MODEL_ATTRIBUTE_DTO, optionDto);
            model.addAttribute(MODEL_ATTRIBUTE_SUCCESS, MODEL_ATTRIBUTE_SUCCESS);
            log.info("Saved option {}", optionDto);
        } catch (Exception e) {
            log.error("Error saving option", e);
            model.addAttribute(MODEL_ATTRIBUTE_DTO, optionDto);
            model.addAttribute(MODEL_ATTRIBUTE_ERROR, e.getMessage());
        }
        return OPTION_FORM_PATH;
    }

    @PutMapping(path="/update-option/{id}")
    public String updateOption(@PathVariable Integer id, Model model) {
        log.info("updateOption formUpdate {}", id);
        OptionDto optionDto = new OptionDto();
        model.addAttribute(MODEL_ATTRIBUTE_DTO, optionDto);
        model.addAttribute(MODEL_ATTRIBUTE_SUCCESS, MODEL_ATTRIBUTE_SUCCESS);
        log.info("Updated option {}", optionDto);
        return OPTION_FORM_PATH;
    }
}