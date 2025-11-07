/**
 * TradingView Widget Integration for SrcProfit
 *
 * Provides helper functions for initializing TradingView Symbol Overview widgets
 * with support for feature flag toggle (FinViz ↔ TradingView).
 *
 * Dependencies: TradingView external-embedding script (loaded in index_jte.jte)
 */

/**
 * Convert simple ticker to TradingView symbol format
 * Examples: "AAPL" -> "NASDAQ:AAPL", "GDX" -> "NYSEARCA:GDX"
 *
 * @param {string} ticker - Simple ticker symbol (e.g., "AAPL")
 * @returns {string} TradingView formatted symbol (e.g., "NASDAQ:AAPL")
 */
function convertToTradingViewSymbol(ticker) {
  // Symbol to exchange mapping for common SrcProfit instruments
  const exchangeMap = {
    // NASDAQ symbols
    'QQQ': 'NASDAQ:QQQ',
    'AAPL': 'NASDAQ:AAPL',
    'MSFT': 'NASDAQ:MSFT',
    'GOOGL': 'NASDAQ:GOOGL',
    'AMZN': 'NASDAQ:AMZN',
    'TSLA': 'NASDAQ:TSLA',
    'NVDA': 'NASDAQ:NVDA',
    'IBIT': 'NASDAQ:IBIT',

    // NYSE ARCA (ETF)
    'GDX': 'NYSEARCA:GDX',
    'SLV': 'NYSEARCA:SLV',
    'GLD': 'NYSEARCA:GLD',

    // NYSE
    'SPY': 'NYSE:SPY',
    'IWM': 'NYSE:IWM',
    'DIA': 'NYSE:DIA',
  };

  // Return mapped value or default to NASDAQ (most common)
  return exchangeMap[ticker] || `NASDAQ:${ticker}`;
}

/**
 * Initialize TradingView Symbol Overview widget for given symbol
 *
 * @param {HTMLElement} container - DOM element where widget should be rendered
 * @param {string} symbol - TradingView formatted symbol (e.g., "NASDAQ:AAPL")
 */
function initializeTradingViewWidget(container, symbol) {
  if (!container || !symbol) {
    console.warn('Missing container or symbol for TradingView widget');
    return;
  }

  // Clear existing content
  container.innerHTML = '';

  // Create widget div with TradingView configuration
  const widgetDiv = document.createElement('div');
  widgetDiv.className = 'tradingview-widget-container';
  widgetDiv.innerHTML = `
    <div class="tradingview-widget-container__widget"></div>
    <script type="text/javascript" src="https://s3.tradingview.com/external-embedding/embed-widget-symbol-overview.js">
    {
      "symbols": [
        {
          "proName": "${symbol}",
          "title": "${symbol.split(':')[1]}"
        }
      ],
      "chartOnly": false,
      "width": "100%",
      "height": "100%",
      "locale": "en",
      "colorTheme": "light",
      "isTransparent": false,
      "autosize": true,
      "showVolume": false,
      "showMA": false,
      "hideDateRanges": false,
      "hideMarketStatus": false,
      "hideSymbolLogo": false,
      "scalePosition": "right",
      "scaleMode": "Normal",
      "fontFamily": "-apple-system, BlinkMacSystemFont, Trebuchet MS, Roboto, Ubuntu, sans-serif",
      "fontSize": "10",
      "noTimezoneName": false,
      "valuesTracking": "all",
      "changelogNewsNumberOfRows": 0,
      "disablePublishers": false
    }
    </script>
  `;

  container.appendChild(widgetDiv);

  // Trigger TradingView script loading if available
  if (typeof TradingView !== 'undefined' && TradingView.widget) {
    TradingView.widget(new Function('return ' + widgetDiv.querySelector('script').textContent)());
  }
}

/**
 * Initialize all TradingView widgets on page
 *
 * Called automatically on page load and after HTMX content swaps
 * Respects feature flag: data-use-tradingview="true|false"
 */
function initializeAllTradingViewWidgets() {
  const widgets = document.querySelectorAll('[data-tradingview-symbol]');

  widgets.forEach(function(container) {
    const useTradingView = container.getAttribute('data-use-tradingview') !== 'false';

    if (!useTradingView) {
      // Feature flag disabled - keep existing FinViz image
      console.log('TradingView feature flag disabled, using FinViz fallback');
      return;
    }

    const ticker = container.getAttribute('data-tradingview-symbol');
    if (!ticker) {
      console.warn('Missing data-tradingview-symbol attribute');
      return;
    }

    const tvSymbol = convertToTradingViewSymbol(ticker);
    initializeTradingViewWidget(container, tvSymbol);
  });
}

/**
 * Handle dynamic symbol updates in modals (e.g., Position Calculator)
 *
 * Called when user selects new ticker in position calculator
 *
 * @param {string} containerId - ID of widget container to update
 * @param {string} ticker - New ticker symbol
 */
function updateTradingViewSymbol(containerId, ticker) {
  const container = document.getElementById(containerId);
  if (!container) {
    console.warn(`Container not found: ${containerId}`);
    return;
  }

  const useTradingView = container.getAttribute('data-use-tradingview') !== 'false';
  if (!useTradingView) {
    // Feature flag disabled - update FinViz image instead
    console.log('TradingView feature flag disabled, updating FinViz image');
    return;
  }

  const tvSymbol = convertToTradingViewSymbol(ticker);
  initializeTradingViewWidget(container, tvSymbol);
}

/**
 * Reinitialize widgets after HTMX content swap
 *
 * HTMX loads new HTML content, but TradingView widgets need to be reinitialized
 * Hook this to HTMX afterSwap events:
 *
 * Example in template:
 * <div hx-swap="innerHTML" hx-on::afterSwap="initializeAllTradingViewWidgets()">
 */
document.addEventListener('htmx:afterSwap', function(event) {
  // Give TradingView time to load the script before initializing
  setTimeout(initializeAllTradingViewWidgets, 100);
});

/**
 * Initialize on page load
 */
document.addEventListener('DOMContentLoaded', function() {
  // Wait for TradingView library to load
  if (typeof TradingView !== 'undefined') {
    initializeAllTradingViewWidgets();
  } else {
    // Retry after TradingView library loads
    const checkInterval = setInterval(function() {
      if (typeof TradingView !== 'undefined') {
        clearInterval(checkInterval);
        initializeAllTradingViewWidgets();
      }
    }, 100);

    // Timeout after 5 seconds
    setTimeout(function() {
      clearInterval(checkInterval);
      console.warn('TradingView library failed to load');
    }, 5000);
  }
});

// Also initialize on window load (fallback)
window.addEventListener('load', function() {
  if (document.querySelectorAll('[data-tradingview-symbol]').length > 0 &&
      typeof TradingView === 'undefined') {
    console.warn('TradingView library not available, widgets may not render');
  }
});
