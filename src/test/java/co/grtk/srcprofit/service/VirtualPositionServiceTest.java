package co.grtk.srcprofit.service;

import co.grtk.srcprofit.entity.InstrumentEntity;
import co.grtk.srcprofit.entity.OptionEntity;
import co.grtk.srcprofit.entity.OptionStatus;
import co.grtk.srcprofit.entity.OptionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for VirtualPositionService (session-scoped service).
 *
 * Tests verify:
 * - Virtual position storage and retrieval
 * - Ticker-based filtering
 * - Clear functionality
 * - Session isolation
 *
 * Note: Tests the service directly without full Spring context since VirtualPositionService
 * is a simple in-memory session-scoped bean with no database dependencies.
 */
@DisplayName("VirtualPositionService Tests")
class VirtualPositionServiceTest {

    private VirtualPositionService virtualPositionService;

    private OptionEntity testVirtualPosition;
    private InstrumentEntity testInstrument;

    @BeforeEach
    void setUp() {
        // Create new instance for each test (simulating session scope)
        virtualPositionService = new VirtualPositionService();

        // Create test instrument
        testInstrument = new InstrumentEntity();
        testInstrument.setTicker("SPY");

        // Create test virtual position
        testVirtualPosition = new OptionEntity();
        testVirtualPosition.setInstrument(testInstrument);
        testVirtualPosition.setTradeDate(LocalDate.now());
        testVirtualPosition.setExpirationDate(LocalDate.now().plusDays(30));
        testVirtualPosition.setTradePrice(5.0);
        testVirtualPosition.setPositionValue(100.0);
        testVirtualPosition.setMarketValue(50.0);
        testVirtualPosition.setQuantity(1);
        testVirtualPosition.setType(OptionType.PUT);
        testVirtualPosition.setStatus(OptionStatus.PENDING);
        testVirtualPosition.setId(null); // Virtual positions have no ID
    }

    @Test
    @DisplayName("Should store and retrieve virtual position")
    void testSetAndGetVirtualPosition() {
        // Given
        virtualPositionService.setVirtualPosition(testVirtualPosition);

        // When
        Optional<OptionEntity> retrieved = virtualPositionService.getVirtualPosition();

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isEqualTo(testVirtualPosition);
    }

    @Test
    @DisplayName("Should retrieve virtual position by matching ticker")
    void testGetVirtualPositionByTicker() {
        // Given
        virtualPositionService.setVirtualPosition(testVirtualPosition);

        // When
        Optional<OptionEntity> retrieved = virtualPositionService.getVirtualPosition("SPY");

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getInstrument().getTicker()).isEqualTo("SPY");
    }

    @Test
    @DisplayName("Should not retrieve virtual position with non-matching ticker")
    void testGetVirtualPositionByTickerNoMatch() {
        // Given
        virtualPositionService.setVirtualPosition(testVirtualPosition);

        // When
        Optional<OptionEntity> retrieved = virtualPositionService.getVirtualPosition("AAPL");

        // Then
        assertThat(retrieved).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when no virtual position exists")
    void testGetVirtualPositionWhenNone() {
        // When
        Optional<OptionEntity> retrieved = virtualPositionService.getVirtualPosition();

        // Then
        assertThat(retrieved).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when no virtual position matches ticker")
    void testGetVirtualPositionByTickerWhenNone() {
        // When
        Optional<OptionEntity> retrieved = virtualPositionService.getVirtualPosition("UNKNOWN");

        // Then
        assertThat(retrieved).isEmpty();
    }

    @Test
    @DisplayName("Should clear virtual position")
    void testClearVirtualPosition() {
        // Given
        virtualPositionService.setVirtualPosition(testVirtualPosition);
        assertThat(virtualPositionService.getVirtualPosition()).isPresent();

        // When
        virtualPositionService.clearVirtualPosition();

        // Then
        assertThat(virtualPositionService.getVirtualPosition()).isEmpty();
    }

    @Test
    @DisplayName("Should replace previous virtual position with new one")
    void testReplaceVirtualPosition() {
        // Given
        virtualPositionService.setVirtualPosition(testVirtualPosition);
        assertThat(virtualPositionService.getVirtualPosition()).isPresent();

        // Create new virtual position with different ticker
        OptionEntity newVirtual = new OptionEntity();
        InstrumentEntity newInstrument = new InstrumentEntity();
        newInstrument.setTicker("AAPL");
        newVirtual.setInstrument(newInstrument);
        newVirtual.setStatus(OptionStatus.PENDING);

        // When
        virtualPositionService.setVirtualPosition(newVirtual);

        // Then - old position should be gone, new one should be there
        assertThat(virtualPositionService.getVirtualPosition("SPY")).isEmpty();
        assertThat(virtualPositionService.getVirtualPosition("AAPL")).isPresent();
    }

    @Test
    @DisplayName("Should report has virtual position")
    void testHasVirtualPosition() {
        // Given
        virtualPositionService.setVirtualPosition(testVirtualPosition);

        // When
        boolean hasVirtual = virtualPositionService.hasVirtualPosition();

        // Then
        assertThat(hasVirtual).isTrue();
    }

    @Test
    @DisplayName("Should report no virtual position when none exists")
    void testHasVirtualPositionWhenNone() {
        // When
        boolean hasVirtual = virtualPositionService.hasVirtualPosition();

        // Then
        assertThat(hasVirtual).isFalse();
    }

    @Test
    @DisplayName("Should check for virtual position by ticker")
    void testHasVirtualPositionByTicker() {
        // Given
        virtualPositionService.setVirtualPosition(testVirtualPosition);

        // When/Then
        assertThat(virtualPositionService.hasVirtualPosition("SPY")).isTrue();
        assertThat(virtualPositionService.hasVirtualPosition("AAPL")).isFalse();
    }

    @Test
    @DisplayName("Should handle case-insensitive ticker matching")
    void testTickerMatchingCaseInsensitive() {
        // Given
        virtualPositionService.setVirtualPosition(testVirtualPosition);

        // When
        Optional<OptionEntity> retrievedLower = virtualPositionService.getVirtualPosition("spy");
        Optional<OptionEntity> retrievedMixed = virtualPositionService.getVirtualPosition("SpY");

        // Then
        assertThat(retrievedLower).isPresent();
        assertThat(retrievedMixed).isPresent();
    }

    @Test
    @DisplayName("Should handle null instrument gracefully")
    void testNullInstrument() {
        // Given - virtual position with null instrument
        OptionEntity virtualWithoutInstrument = new OptionEntity();
        virtualWithoutInstrument.setInstrument(null);
        virtualWithoutInstrument.setStatus(OptionStatus.PENDING);

        // When
        virtualPositionService.setVirtualPosition(virtualWithoutInstrument);
        Optional<OptionEntity> retrieved = virtualPositionService.getVirtualPosition("SPY");

        // Then - should not match any ticker
        assertThat(retrieved).isEmpty();
    }

    @Test
    @DisplayName("Should preserve virtual position data through retrieval")
    void testDataPreservation() {
        // Given
        testVirtualPosition.setQuantity(5);
        testVirtualPosition.setTradePrice(7.50);
        testVirtualPosition.setMarketValue(375.0);

        // When
        virtualPositionService.setVirtualPosition(testVirtualPosition);
        Optional<OptionEntity> retrieved = virtualPositionService.getVirtualPosition();

        // Then - all fields should be preserved
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getQuantity()).isEqualTo(5);
        assertThat(retrieved.get().getTradePrice()).isEqualTo(7.50);
        assertThat(retrieved.get().getMarketValue()).isEqualTo(375.0);
    }
}
