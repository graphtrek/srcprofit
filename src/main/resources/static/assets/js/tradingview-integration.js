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

    // AMEX (American Stock Exchange)
    'GDX': 'AMEX:GDX',
    'SLV': 'AMEX:SLV',
    'GLD': 'AMEX:GLD',

    // NYSE
    'SPY': 'NYSE:SPY',
    'IWM': 'NYSE:IWM',
    'DIA': 'NYSE:DIA',
  };

  // Return mapped value or default to NASDAQ (most common)
  return exchangeMap[ticker] || `NASDAQ:${ticker}`;
}

/**
 * Initialize TradingView Mini Chart widget for given symbol
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

  // Create widget container
  const widgetContainer = document.createElement('div');
  widgetContainer.className = 'tradingview-widget-container';

  // Create inner widget div
  const widgetDiv = document.createElement('div');
  widgetDiv.className = 'tradingview-widget-container__widget';
  widgetContainer.appendChild(widgetDiv);

  // Create configuration script with proper JSON format
  const configScript = document.createElement('script');
  configScript.type = 'text/javascript';
  configScript.src = 'https://s3.tradingview.com/external-embedding/embed-widget-mini-symbol-overview.js';
  configScript.async = true;

  // The configuration needs to be in a valid JSON format as text content
  configScript.textContent = `
  {
    "symbol": "${symbol}",
    "width": "100%",
    "height": "100%",
    "locale": "en",
    "dateRange": "12M",
    "colorTheme": "light",
    "isTransparent": false,
    "autosize": true,
    "largeChartUrl": ""
  }
  `;

  widgetContainer.appendChild(configScript);
  container.appendChild(widgetContainer);

  console.log('Initialized TradingView mini-chart for symbol:', symbol);
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
 * Initialize TradingView Advanced Chart widget for detailed technical analysis
 *
 * Used in Position Calculator modal with candlestick chart, Moving Average indicators (50, 100, 200) and Volume
 *
 * @param {HTMLElement} container - DOM element where widget should be rendered
 * @param {string} symbol - TradingView formatted symbol (e.g., "NASDAQ:AAPL")
 */
function initializeAdvancedChartWidget(container, symbol) {
  if (!container || !symbol) {
    console.warn('Missing container or symbol for Advanced Chart widget');
    return;
  }

  // Clear existing content
  container.innerHTML = '';

  // Create the main widget div that TradingView script will target
  const widgetId = 'tradingview_chart_widget_' + Date.now(); // Unique ID
  const widgetDiv = document.createElement('div');
  widgetDiv.id = widgetId;
  widgetDiv.className = 'tradingview-widget-container__widget';
  container.appendChild(widgetDiv);

  // Create configuration script for Advanced Chart
  const configScript = document.createElement('script');
  configScript.type = 'text/javascript';
  configScript.src = 'https://s3.tradingview.com/external-embedding/embed-widget-advanced-chart.js';
  configScript.async = true;

  // Advanced Chart configuration with candlestick, SMAs (50, 100, 200), and Volume
  const config = {
    "autosize": true,
    "symbol": symbol,
    "interval": "D",
    "timezone": "Etc/UTC",
    "theme": "light",
    "style": "1",
    "locale": "en",
    "toolbar_bg": "#f1f3f6",
    "enable_publishing": false,
    "allow_symbol_change": true,
    "chartType": "candlestick",
    "show_popup_button": true,
    "popup_width": "1000",
    "popup_height": "650",
    "studies": [
      "MA50@tv-basicstudies",
      "MA100@tv-basicstudies",
      "MA200@tv-basicstudies",
      "Volume@tv-basicstudies"
    ],
    "container_id": widgetId,
    "height": "600",
    "width": "100%"
  };

  configScript.textContent = JSON.stringify(config);

  container.appendChild(configScript);

  console.log('Initialized TradingView Advanced Chart for symbol:', symbol);
}

/**
 * Update Advanced Chart symbol dynamically
 *
 * Called when user changes ticker in position calculator
 *
 * @param {string} containerId - ID of Advanced Chart container
 * @param {string} ticker - New ticker symbol
 */
function updateAdvancedChartSymbol(containerId, ticker) {
  const container = document.getElementById(containerId);
  if (!container) {
    console.warn(`Container not found: ${containerId}`);
    return;
  }

  if (!ticker || ticker.trim() === '') {
    console.warn('Invalid ticker provided');
    return;
  }

  const useTradingView = container.getAttribute('data-use-tradingview') !== 'false';
  if (!useTradingView) {
    console.log('TradingView feature flag disabled, Advanced Chart update skipped');
    return;
  }

  const tvSymbol = convertToTradingViewSymbol(ticker);
  container.setAttribute('data-tradingview-symbol', ticker);
  initializeAdvancedChartWidget(container, tvSymbol);
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
