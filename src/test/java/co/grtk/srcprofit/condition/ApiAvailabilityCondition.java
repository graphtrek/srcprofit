package co.grtk.srcprofit.condition;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 condition for gracefully skipping tests when external APIs are unavailable.
 *
 * The test infrastructure requires these APIs to be configured:
 * - Alpaca API (via ALPACA_API_KEY environment variable)
 * - Alpha Vantage API (via ALPHA_VINTAGE_API_KEY environment variable)
 *
 * If either is missing, tests annotated with @EnabledIf(ApiAvailabilityCondition.class)
 * will be skipped with an informative message.
 */
public class ApiAvailabilityCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        String alpacaKey = System.getenv("ALPACA_API_KEY");
        String alphaVintageKey = System.getenv("ALPHA_VINTAGE_API_KEY");

        boolean alpacaAvailable = alpacaKey != null && !alpacaKey.trim().isEmpty();
        boolean alphaVintageAvailable = alphaVintageKey != null && !alphaVintageKey.trim().isEmpty();

        if (alpacaAvailable && alphaVintageAvailable) {
            return ConditionEvaluationResult.enabled("Both Alpaca and Alpha Vantage APIs are configured");
        }

        StringBuilder reason = new StringBuilder("Skipping test: Missing API configuration - ");
        if (!alpacaAvailable) {
            reason.append("ALPACA_API_KEY not set");
        }
        if (!alphaVintageAvailable) {
            if (!alpacaAvailable) {
                reason.append(", ");
            }
            reason.append("ALPHA_VINTAGE_API_KEY not set");
        }

        return ConditionEvaluationResult.disabled(reason.toString());
    }
}
