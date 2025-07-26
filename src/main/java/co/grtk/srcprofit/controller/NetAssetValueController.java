package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.NetAssetValueDto;
import co.grtk.srcprofit.service.NetAssetValueService;
import co.grtk.srcprofit.service.OptionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static co.grtk.srcprofit.mapper.MapperUtils.round2Digits;

@Controller
public class NetAssetValueController {
    private final NetAssetValueService netAssetValueService;
    private final OptionService optionService;
    private final String NET_ASSET_VALUE_PAGE_PATH = "net_asset_values_jte";
    private final String MODEL_ATTRIBUTE_NET_ASSET_VALUES = "netAssetValues";

    public NetAssetValueController(NetAssetValueService netAssetValueService, OptionService optionService) {
        this.netAssetValueService = netAssetValueService;
        this.optionService = optionService;
    }

    @GetMapping("/netAssetValues")
    public String getNetAssetValues(Model model) {
        List<NetAssetValueDto> netAssetValues = netAssetValueService.loadAllNetAssetValues();
        Map<LocalDate, BigDecimal> dailyPremiums = optionService.getDailyPremium();
        int counter = 0;
        double cash = 0;
        for (NetAssetValueDto netAssetValue : netAssetValues) {
            BigDecimal premium = dailyPremiums.get(netAssetValue.getReportDate());
            counter++;
            cash += netAssetValue.getCash();
            netAssetValue.setAverageCash(round2Digits(cash / counter));
            if (premium != null) {
                netAssetValue.setDailyPremium(round2Digits(premium.doubleValue()));
                netAssetValue.setRoi(round2Digits((premium.doubleValue() / netAssetValue.getAverageCash()) * 100));
            }
        }
        model.addAttribute(MODEL_ATTRIBUTE_NET_ASSET_VALUES, netAssetValues);
        return NET_ASSET_VALUE_PAGE_PATH;
    }
}