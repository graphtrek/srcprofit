package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.entity.OptionSnapshotEntity;
import co.grtk.srcprofit.service.OptionSnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for option snapshots endpoints.
 *
 * Provides endpoints to:
 * - Trigger refresh of option snapshots
 * - Query snapshots by instrument ticker
 * - Clean up expired snapshots
 */
@RestController
@RequestMapping("/api/option-snapshots")
public class OptionSnapshotRestController {
    private static final Logger log = LoggerFactory.getLogger(OptionSnapshotRestController.class);

    private final OptionSnapshotService optionSnapshotService;

    public OptionSnapshotRestController(OptionSnapshotService optionSnapshotService) {
        this.optionSnapshotService = optionSnapshotService;
    }

    /**
     * Trigger refresh of option snapshots for all eligible instruments.
     *
     * Eligible instruments: price < $100
     *
     * @return JSON response with success status and count of snapshots saved
     *         Example: {
     *           "success": true,
     *           "snapshotsSaved": 250,
     *           "message": "Refreshed 250 option snapshots"
     *         }
     */
    @PostMapping(value = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> refreshOptionSnapshots() {
        try {
            log.info("OptionSnapshotRestController: POST /api/option-snapshots/refresh - Starting refresh");

            int totalSaved = optionSnapshotService.refreshOptionSnapshots();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("snapshotsSaved", totalSaved);
            response.put("message", String.format("Refreshed %d option snapshots", totalSaved));

            log.info("OptionSnapshotRestController: POST /api/option-snapshots/refresh - Success, saved {} snapshots",
                    totalSaved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("OptionSnapshotRestController: POST /api/option-snapshots/refresh - Error: {}",
                    e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get all option snapshots for a specific instrument ticker.
     *
     * @param ticker The instrument ticker (e.g., "AAPL")
     * @return List of OptionSnapshotEntity objects for the ticker
     *         Returns empty list if ticker not found
     *
     * Example response:
     * [
     *   {
     *     "id": 1,
     *     "symbol": "AAPL230120C00150000",
     *     "optionType": "call",
     *     "strikePrice": 150.00,
     *     "expirationDate": "2023-01-20",
     *     "lastTradePrice": 2.50,
     *     "bidPrice": 2.45,
     *     "askPrice": 2.55,
     *     "delta": 0.45,
     *     "gamma": 0.03,
     *     ...
     *   }
     * ]
     */
    @GetMapping(value = "/{ticker}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<OptionSnapshotEntity>> getSnapshotsByTicker(
            @PathVariable String ticker) {
        try {
            log.debug("OptionSnapshotRestController: GET /api/option-snapshots/{} - Fetching snapshots", ticker);

            List<OptionSnapshotEntity> snapshots = optionSnapshotService.getSnapshotsForInstrument(ticker);

            log.info("OptionSnapshotRestController: GET /api/option-snapshots/{} - Found {} snapshots",
                    ticker, snapshots.size());
            return ResponseEntity.ok(snapshots);
        } catch (Exception e) {
            log.error("OptionSnapshotRestController: GET /api/option-snapshots/{} - Error: {}",
                    ticker, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete all expired option snapshots.
     *
     * Removes snapshots with expiration_date < today.
     *
     * @return JSON response with success status and count of snapshots deleted
     *         Example: {
     *           "success": true,
     *           "deletedCount": 5,
     *           "message": "Deleted 5 expired snapshots"
     *         }
     */
    @DeleteMapping(value = "/expired", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> deleteExpiredSnapshots() {
        try {
            log.info("OptionSnapshotRestController: DELETE /api/option-snapshots/expired - Starting cleanup");

            int deletedCount = optionSnapshotService.deleteExpiredSnapshots();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("deletedCount", deletedCount);
            response.put("message", String.format("Deleted %d expired snapshots", deletedCount));

            log.info("OptionSnapshotRestController: DELETE /api/option-snapshots/expired - Deleted {} snapshots",
                    deletedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("OptionSnapshotRestController: DELETE /api/option-snapshots/expired - Error: {}",
                    e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
