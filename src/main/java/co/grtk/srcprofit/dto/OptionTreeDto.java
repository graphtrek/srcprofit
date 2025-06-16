package co.grtk.srcprofit.dto;

import java.util.List;

public class OptionTreeDto {
    private Long id;
    private String symbol;
    private List<OptionTreeDto> children;

    // Constructor
    public OptionTreeDto(Long id, String symbol, List<OptionTreeDto> children) {
        this.id = id;
        this.symbol = symbol;
        this.children = children;
    }

}