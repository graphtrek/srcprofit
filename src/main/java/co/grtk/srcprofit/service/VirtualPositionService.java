package co.grtk.srcprofit.service;

import co.grtk.srcprofit.entity.OptionEntity;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

/**
 * Session-scoped service for managing virtual (what-if) positions.
 * Virtual positions are temporary, in-memory positions used for scenario analysis
 * without persisting to the database.
 *
 * <p>Session scope ensures virtual positions persist across requests within the same
 * user session but are automatically cleaned up when the session expires.</p>
 */
@Service
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class VirtualPositionService {

    private OptionEntity virtualPosition;

    /**
     * Store a virtual position in the session.
     * Replaces any existing virtual position.
     *
     * @param position the virtual position to store (should have id=null and status=PENDING)
     */
    public void setVirtualPosition(OptionEntity position) {
        this.virtualPosition = position;
    }

    /**
     * Retrieve the current virtual position for a specific ticker.
     *
     * @param ticker the ticker symbol to match
     * @return Optional containing the virtual position if it exists and matches the ticker, empty otherwise
     */
    public Optional<OptionEntity> getVirtualPosition(String ticker) {
        if (virtualPosition != null &&
            virtualPosition.getInstrument() != null &&
            ticker.equalsIgnoreCase(virtualPosition.getInstrument().getTicker())) {
            return Optional.of(virtualPosition);
        }
        return Optional.empty();
    }

    /**
     * Retrieve the current virtual position regardless of ticker.
     *
     * @return Optional containing the virtual position if it exists, empty otherwise
     */
    public Optional<OptionEntity> getVirtualPosition() {
        return Optional.ofNullable(virtualPosition);
    }

    /**
     * Clear the virtual position from the session.
     */
    public void clearVirtualPosition() {
        this.virtualPosition = null;
    }

    /**
     * Check if a virtual position exists in the session.
     *
     * @return true if a virtual position is stored, false otherwise
     */
    public boolean hasVirtualPosition() {
        return virtualPosition != null;
    }

    /**
     * Check if a virtual position exists for a specific ticker.
     *
     * @param ticker the ticker symbol to check
     * @return true if a virtual position exists for the ticker, false otherwise
     */
    public boolean hasVirtualPosition(String ticker) {
        return getVirtualPosition(ticker).isPresent();
    }
}
