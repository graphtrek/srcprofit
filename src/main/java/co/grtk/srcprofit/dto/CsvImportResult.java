package co.grtk.srcprofit.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Result object for CSV import operations, providing detailed reporting of success/failure.
 *
 * Replaces simple int return value to enable:
 * - Tracking of successful, failed, and skipped records separately
 * - Error details for troubleshooting import failures
 * - User feedback on partial success scenarios
 */
public class CsvImportResult {

    private int totalRecords;
    private int successfulRecords;
    private int failedRecords;
    private int skippedRecords;  // Records filtered out (non-OPT, etc.)
    private List<CsvRecordError> errors;

    public CsvImportResult() {
        this.totalRecords = 0;
        this.successfulRecords = 0;
        this.failedRecords = 0;
        this.skippedRecords = 0;
        this.errors = new ArrayList<>();
    }

    /**
     * Detailed error information for a single CSV record failure.
     */
    public static class CsvRecordError {
        private int recordNumber;
        private String fieldName;
        private String fieldValue;
        private String errorMessage;
        private String exceptionType;

        public CsvRecordError(int recordNumber, String fieldName, String fieldValue,
                            String errorMessage, String exceptionType) {
            this.recordNumber = recordNumber;
            this.fieldName = fieldName;
            this.fieldValue = fieldValue;
            this.errorMessage = errorMessage;
            this.exceptionType = exceptionType;
        }

        public int getRecordNumber() {
            return recordNumber;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getFieldValue() {
            return fieldValue;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getExceptionType() {
            return exceptionType;
        }

        @Override
        public String toString() {
            return String.format("CSV Record #%d - %s parsing '%s' (value: '%s'): %s",
                    recordNumber, exceptionType, fieldName, fieldValue, errorMessage);
        }
    }

    // Getters and setters

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public int getSuccessfulRecords() {
        return successfulRecords;
    }

    public void setSuccessfulRecords(int successfulRecords) {
        this.successfulRecords = successfulRecords;
    }

    public int getFailedRecords() {
        return failedRecords;
    }

    public void setFailedRecords(int failedRecords) {
        this.failedRecords = failedRecords;
    }

    public int getSkippedRecords() {
        return skippedRecords;
    }

    public void setSkippedRecords(int skippedRecords) {
        this.skippedRecords = skippedRecords;
    }

    public List<CsvRecordError> getErrors() {
        return errors;
    }

    public void setErrors(List<CsvRecordError> errors) {
        this.errors = errors;
    }

    public void addError(CsvRecordError error) {
        this.errors.add(error);
    }

    /**
     * Increments successful record counter and returns this for method chaining.
     */
    public CsvImportResult incrementSuccessful() {
        this.successfulRecords++;
        return this;
    }

    /**
     * Increments failed record counter and returns this for method chaining.
     */
    public CsvImportResult incrementFailed() {
        this.failedRecords++;
        return this;
    }

    /**
     * Increments skipped record counter and returns this for method chaining.
     */
    public CsvImportResult incrementSkipped() {
        this.skippedRecords++;
        return this;
    }

    /**
     * Indicates whether import was completely successful (no failures).
     */
    public boolean isCompleteSuccess() {
        return failedRecords == 0;
    }

    /**
     * Indicates whether import salvaged any records (partial success).
     */
    public boolean isPartialSuccess() {
        return successfulRecords > 0 && failedRecords > 0;
    }

    /**
     * Indicates whether import completely failed (no successful records).
     */
    public boolean isCompleteFailure() {
        return successfulRecords == 0;
    }

    @Override
    public String toString() {
        return String.format(
                "CsvImportResult{total=%d, successful=%d, failed=%d, skipped=%d, errors=%d}",
                totalRecords, successfulRecords, failedRecords, skippedRecords, errors.size());
    }

    /**
     * Returns detailed summary for logging/reporting.
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("CSV Import Summary: %d total records\n", totalRecords));
        sb.append(String.format("  ✓ Successful: %d\n", successfulRecords));
        sb.append(String.format("  ✗ Failed: %d\n", failedRecords));
        sb.append(String.format("  ⊘ Skipped: %d\n", skippedRecords));

        if (!errors.isEmpty()) {
            sb.append("\nFailed Records:\n");
            for (CsvRecordError error : errors) {
                sb.append("  - ").append(error).append("\n");
            }
        }

        return sb.toString();
    }
}
