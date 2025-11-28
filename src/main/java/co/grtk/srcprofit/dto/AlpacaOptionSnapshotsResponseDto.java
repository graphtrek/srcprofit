package co.grtk.srcprofit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * DTO for Alpaca option snapshots API response.
 *
 * Wraps the map of option snapshots returned by
 * /v1beta1/options/snapshots/{symbol} endpoint.
 *
 * Example:
 * {
 *   "snapshots": {
 *     "AAPL230120C00150000": { ... },
 *     "AAPL230120C00160000": { ... }
 *   }
 * }
 */
public class AlpacaOptionSnapshotsResponseDto {
    @JsonProperty("snapshots")
    public Map<String, AlpacaOptionSnapshotDto> snapshots;

    public AlpacaOptionSnapshotsResponseDto() {}

    public AlpacaOptionSnapshotsResponseDto(Map<String, AlpacaOptionSnapshotDto> snapshots) {
        this.snapshots = snapshots;
    }

    public Map<String, AlpacaOptionSnapshotDto> getSnapshots() {
        return snapshots;
    }

    public void setSnapshots(Map<String, AlpacaOptionSnapshotDto> snapshots) {
        this.snapshots = snapshots;
    }
}
