/**
 * Dark Mode Toggle
 * Manages dark/light theme switching with localStorage persistence
 * Supports both custom data-theme and Bootstrap's data-bs-theme
 */

(function() {
  const THEME_KEY = 'srcprofit-theme';
  const DARK_THEME = 'dark';
  const LIGHT_THEME = 'light';

  /**
   * Get current theme from localStorage
   * Default to LIGHT theme (never use system preference)
   */
  function getCurrentTheme() {
    // Check localStorage first - only source of truth for stored preference
    const stored = localStorage.getItem(THEME_KEY);
    if (stored) {
      return stored;
    }

    // Default to light theme - no system preference auto-detection
    return LIGHT_THEME;
  }

  /**
   * Apply theme to document
   */
  function applyTheme(theme) {
    const html = document.documentElement;

    if (theme === DARK_THEME) {
      // For custom dark mode
      html.setAttribute('data-theme', DARK_THEME);
      // For Bootstrap 5.3+ DataTables dark mode
      html.setAttribute('data-bs-theme', DARK_THEME);
      // For DataTables default dark mode
      html.classList.add('dark');
    } else {
      // For custom dark mode
      html.removeAttribute('data-theme');
      // For Bootstrap 5.3+ DataTables dark mode
      html.removeAttribute('data-bs-theme');
      // For DataTables default dark mode
      html.classList.remove('dark');
    }

    localStorage.setItem(THEME_KEY, theme);
    updateThemeIcon(theme);
    updateTradingViewTheme(theme);
    updateApexChartsTheme(theme);
  }

  /**
   * Update icon based on theme
   */
  function updateThemeIcon(theme) {
    const toggle = document.getElementById('theme-toggle');
    if (!toggle) return;

    const icon = toggle.querySelector('i');
    if (theme === DARK_THEME) {
      icon.classList.remove('bi-moon');
      icon.classList.add('bi-sun');
    } else {
      icon.classList.remove('bi-sun');
      icon.classList.add('bi-moon');
    }
  }

  /**
   * Update TradingView widgets to match theme
   */
  function updateTradingViewTheme(theme) {
    // TradingView mini widget containers use data-theme attribute
    const tvWidgets = document.querySelectorAll('[data-tradingview-symbol]');

    tvWidgets.forEach(widget => {
      // Set the theme attribute for TradingView mini widgets
      if (theme === DARK_THEME) {
        widget.setAttribute('data-tradingview-theme', 'dark');
      } else {
        widget.setAttribute('data-tradingview-theme', 'light');
      }
    });

    // Also update TradingView Advanced Charts (if function exists and chart is visible)
    if (typeof updateAdvancedChartTheme === 'function') {
      updateAdvancedChartTheme(theme === DARK_THEME ? 'dark' : 'light');
    }
  }

  /**
   * Update ApexCharts to match theme
   */
  function updateApexChartsTheme(theme) {
    if (typeof ApexCharts === 'undefined') return;

    // Get all ApexCharts instances
    const charts = document.querySelectorAll('[data-apexcharts], .apexcharts-canvas');

    charts.forEach(chartEl => {
      // Find the ApexCharts instance for this element
      const chart = window.ApexCharts?.getChartByID?.(chartEl.id);
      if (!chart) return;

      // Dark mode color palette - better visibility on dark backgrounds
      const darkColors = theme === DARK_THEME ? {
        colors: ['#6ea896', '#ffb74d', '#6ea8fe', '#9ec5fe', '#ffd966', '#6edff6'],
        foreColor: '#f1f3f5'
      } : {
        colors: ['#2eca6a', '#ff771d', '#4154f1', '#717ff5', '#ffc107', '#17a2b8'],
        foreColor: '#444444'
      };

      const options = {
        theme: theme === DARK_THEME ? 'dark' : 'light',
        chart: {
          foreColor: darkColors.foreColor
        },
        colors: darkColors.colors,
        plotOptions: {
          radialBar: {
            track: {
              background: theme === DARK_THEME ? '#2d3238' : '#f6f9ff'
            }
          }
        },
        stroke: {
          colors: [darkColors.foreColor]
        }
      };

      chart.updateOptions(options, false);
    });
  }

  /**
   * Toggle between light and dark theme
   */
  function toggleTheme() {
    const current = document.documentElement.getAttribute('data-theme') || LIGHT_THEME;
    const next = current === DARK_THEME ? LIGHT_THEME : DARK_THEME;
    applyTheme(next);
  }

  /**
   * Initialize dark mode
   */
  function init() {
    // Apply stored or system theme on page load
    const theme = getCurrentTheme();
    applyTheme(theme);

    // Setup toggle button
    const toggle = document.getElementById('theme-toggle');
    if (toggle) {
      toggle.addEventListener('click', function(e) {
        e.preventDefault();
        toggleTheme();
      });
    }

    // Listen for system theme changes
    if (window.matchMedia) {
      window.matchMedia('(prefers-color-scheme: dark)').addListener(function(e) {
        if (!localStorage.getItem(THEME_KEY)) {
          const theme = e.matches ? DARK_THEME : LIGHT_THEME;
          applyTheme(theme);
        }
      });
    }
  }

  // Initialize when DOM is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
