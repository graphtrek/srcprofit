/**
 * TradingView Widget Integration for SrcProfit
 *
 * Provides helper functions for initializing TradingView Symbol Overview widgets
 * with support for feature flag toggle (FinViz ↔ TradingView).
 *
 * Dynamically fetches exchange information from backend API instead of using
 * hardcoded mappings. Implements client-side caching to minimize API calls.
 *
 * Dependencies: TradingView external-embedding script (loaded in index_jte.jte)
 */

/**
 * Client-side cache for ticker -> exchange mappings
 * Persists for entire page session; cleared on page reload
 * @type {Map<string, string>}
 */
const exchangeCache = new Map();

/**
 * Map Alpaca exchange names to TradingView exchange codes
 * Handles differences in exchange naming conventions
 * @param {string} alpacaExchange - Exchange name from Alpaca API (e.g., "NASDAQ", "NYSEARCA")
 * @returns {string} TradingView exchange code or null if unmapped
 */
function mapAlpacaExchangeToTradingView(alpacaExchange) {
  if (!alpacaExchange) {
    return null;
  }

  // Map of Alpaca exchange names to TradingView exchange codes
  const exchangeMapping = {
    'NASDAQ': 'NASDAQ',
    'NYSE': 'NYSE',
    'ARCA': 'AMEX',      // NYSE Arca -> AMEX (TradingView convention)
    'NYSEARCA': 'AMEX',  // NYSEARCA -> AMEX
    'AMEX': 'AMEX',
    'CBOE': 'CBOE',
    'PINK': 'PINK',      // Pink Sheets / OTC Markets
  };

  return exchangeMapping[alpacaExchange] || null;
}

/**
 * Fetch instrument exchange data from backend API
 * Results are cached to avoid redundant API calls
 *
 * @param {string} ticker - Ticker symbol to look up
 * @returns {Promise<string|null>} TradingView exchange code or null if not found/error
 */
async function fetchInstrumentExchange(ticker) {
  // Check cache first
  if (exchangeCache.has(ticker)) {
    console.log(`Exchange cache hit for ${ticker}: ${exchangeCache.get(ticker)}`);
    return exchangeCache.get(ticker);
  }

  try {
    const response = await fetch(`/api/instruments/${encodeURIComponent(ticker)}`);

    if (!response.ok) {
      if (response.status === 404) {
        console.warn(`Instrument not found in database: ${ticker}`);
      } else {
        console.error(`Error fetching instrument ${ticker}: HTTP ${response.status}`);
      }
      return null;
    }

    const data = await response.json();
    const alpacaExchange = data.alpacaExchange;

    if (!alpacaExchange) {
      console.warn(`No exchange data for ticker ${ticker}`);
      return null;
    }

    const tvExchange = mapAlpacaExchangeToTradingView(alpacaExchange);
    if (!tvExchange) {
      console.warn(`Unmapped Alpaca exchange for ${ticker}: ${alpacaExchange}`);
      return null;
    }

    // Cache the result
    exchangeCache.set(ticker, tvExchange);
    console.log(`Cached exchange for ${ticker}: ${tvExchange}`);
    return tvExchange;

  } catch (error) {
    console.error(`Exception fetching instrument ${ticker}:`, error);
    return null;
  }
}

/**
 * Convert simple ticker to TradingView symbol format
 * Dynamically fetches exchange from backend, with fallback to NASDAQ
 * Examples: "AAPL" -> "NASDAQ:AAPL", "GDX" -> "AMEX:GDX"
 *
 * @param {string} ticker - Simple ticker symbol (e.g., "AAPL")
 * @returns {Promise<string>} TradingView formatted symbol (e.g., "NASDAQ:AAPL")
 */
async function convertToTradingViewSymbol(ticker) {
  if (!ticker || typeof ticker !== 'string') {
    console.warn('Invalid ticker provided');
    return null;
  }

  // Fetch exchange from backend API
  const exchange = await fetchInstrumentExchange(ticker);

  // Fallback to NASDAQ if exchange not found (covers unknown tickers)
  if (!exchange) {
    console.warn(`Falling back to NASDAQ for ticker: ${ticker}`);
    return `NASDAQ:${ticker}`;
  }

  return `${exchange}:${ticker}`;
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

  // Get current theme from document (respects dark mode toggle)
  const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';

  // The configuration needs to be in a valid JSON format as text content
  configScript.textContent = `
  {
    "symbol": "${symbol}",
    "width": "100%",
    "height": "100%",
    "locale": "en",
    "dateRange": "12M",
    "colorTheme": "${currentTheme}",
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
 * Skips Advanced Chart widgets (those with data-widget-type="advanced")
 * Handles async exchange conversion
 */
async function initializeAllTradingViewWidgets() {
  const widgets = document.querySelectorAll('[data-tradingview-symbol]');

  for (const container of widgets) {
    // Skip Advanced Chart widgets - they are initialized manually
    const widgetType = container.getAttribute('data-widget-type');
    if (widgetType === 'advanced') {
      console.log('Skipping Advanced Chart widget - initialized manually');
      continue;
    }

    const useTradingView = container.getAttribute('data-use-tradingview') !== 'false';

    if (!useTradingView) {
      // Feature flag disabled - keep existing FinViz image
      console.log('TradingView feature flag disabled, using FinViz fallback');
      continue;
    }

    const ticker = container.getAttribute('data-tradingview-symbol');
    if (!ticker) {
      console.warn('Missing data-tradingview-symbol attribute');
      continue;
    }

    const tvSymbol = await convertToTradingViewSymbol(ticker);
    if (tvSymbol) {
      initializeTradingViewWidget(container, tvSymbol);
    } else {
      console.error(`Failed to convert ticker ${ticker} to TradingView symbol`);
    }
  }
}

/**
 * Handle dynamic symbol updates in modals (e.g., Position Calculator)
 *
 * Called when user selects new ticker in position calculator
 * Handles async exchange conversion (internally)
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

  // Handle async conversion internally without exposing Promise to caller
  convertToTradingViewSymbol(ticker).then(tvSymbol => {
    if (tvSymbol) {
      initializeTradingViewWidget(container, tvSymbol);
    } else {
      console.error(`Failed to convert ticker ${ticker} to TradingView symbol`);
    }
  }).catch(error => {
    console.error(`Error updating TradingView symbol for ${ticker}:`, error);
  });
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

  // Create the proper TradingView widget structure
  const widgetContainer = document.createElement('div');
  widgetContainer.className = 'tradingview-widget-container';
  widgetContainer.style.height = '100%';
  widgetContainer.style.width = '100%';

  // Create the widget div
  const widgetDiv = document.createElement('div');
  widgetDiv.className = 'tradingview-widget-container__widget';
  widgetDiv.style.height = 'calc(100% - 32px)';
  widgetDiv.style.width = '100%';
  widgetContainer.appendChild(widgetDiv);

  // Create configuration script for Advanced Chart
  const configScript = document.createElement('script');
  configScript.type = 'text/javascript';
  configScript.src = 'https://s3.tradingview.com/external-embedding/embed-widget-advanced-chart.js';
  configScript.async = true;

  // Get current theme from document (respects dark mode toggle)
  const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';

  // Advanced Chart configuration with candlestick, SMAs (50, 100, 200), and Volume
  const config = {
    "autosize": true,
    "symbol": symbol,
    "interval": "D",
    "timezone": "Etc/UTC",
    "theme": currentTheme,
    "style": "1",
    "locale": "en",
    "enable_publishing": false,
    "allow_symbol_change": true,
    "studies": [
      {
        "id": "MASimple@tv-basicstudies",
        "inputs": {
          "length": 50
        }
      },
      {
        "id": "MASimple@tv-basicstudies",
        "inputs": {
          "length": 100
        }
      },
      {
        "id": "MASimple@tv-basicstudies",
        "inputs": {
          "length": 200
        }
      },
      "Volume@tv-basicstudies"
    ],
    "hide_side_toolbar": false,
    "withdateranges": true,
    "hide_top_toolbar": false,
    "save_image": false,
    "support_host": "https://www.tradingview.com"
  };

  configScript.textContent = JSON.stringify(config);
  widgetContainer.appendChild(configScript);

  // Add copyright div
  const copyrightDiv = document.createElement('div');
  copyrightDiv.className = 'tradingview-widget-copyright';
  copyrightDiv.innerHTML = '<a href="https://www.tradingview.com/" rel="noopener nofollow" target="_blank"><span class="blue-text">Track all markets on TradingView</span></a>';
  widgetContainer.appendChild(copyrightDiv);

  container.appendChild(widgetContainer);

  console.log('Initialized TradingView Advanced Chart for symbol:', symbol);
}

/**
 * Update Advanced Chart symbol dynamically
 *
 * Called when user changes ticker in position calculator
 * Handles async exchange conversion (internally)
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

  // Handle async conversion internally without exposing Promise to caller
  convertToTradingViewSymbol(ticker).then(tvSymbol => {
    if (tvSymbol) {
      container.setAttribute('data-tradingview-symbol', ticker);
      initializeAdvancedChartWidget(container, tvSymbol);
    } else {
      console.error(`Failed to convert ticker ${ticker} to TradingView symbol`);
    }
  }).catch(error => {
    console.error(`Error updating Advanced Chart symbol for ${ticker}:`, error);
  });
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

/**
 * Update all TradingView charts theme when dark mode is toggled
 * TradingView widgets cannot be updated dynamically after creation,
 * so we need to rebuild them when the theme changes
 *
 * @param {string} theme - 'dark' or 'light'
 */
function updateAdvancedChartTheme(theme) {
  console.log(`Updating TradingView widgets to theme: ${theme}`);

  // Update all mini charts on the dashboard
  const miniChartWidgets = document.querySelectorAll('[data-tradingview-symbol]:not([data-widget-type="advanced"])');
  miniChartWidgets.forEach(container => {
    const ticker = container.getAttribute('data-tradingview-symbol');
    if (ticker) {
      // Convert ticker and rebuild mini chart with new theme
      convertToTradingViewSymbol(ticker).then(tvSymbol => {
        if (tvSymbol) {
          initializeTradingViewWidget(container, tvSymbol);
        }
      }).catch(error => {
        console.error(`Error updating mini chart for ${ticker}:`, error);
      });
    }
  });

  // Update Advanced Chart in modal (if present)
  const advancedChartContainer = document.getElementById('tradingview_advanced_chart');
  if (advancedChartContainer) {
    const ticker = advancedChartContainer.getAttribute('data-tradingview-symbol');
    if (ticker) {
      // Use updateAdvancedChartSymbol which handles ticker-to-TradingView symbol conversion
      updateAdvancedChartSymbol('tradingview_advanced_chart', ticker);
    }
  }
}
