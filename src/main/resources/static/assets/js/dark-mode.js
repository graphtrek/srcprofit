/**
 * Dark Mode Toggle
 * Manages dark/light theme switching with localStorage persistence
 */

(function() {
  const THEME_KEY = 'srcprofit-theme';
  const DARK_THEME = 'dark';
  const LIGHT_THEME = 'light';

  /**
   * Get current theme from localStorage or system preference
   */
  function getCurrentTheme() {
    // Check localStorage first
    const stored = localStorage.getItem(THEME_KEY);
    if (stored) {
      return stored;
    }

    // Check system preference
    if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
      return DARK_THEME;
    }

    return LIGHT_THEME;
  }

  /**
   * Apply theme to document
   */
  function applyTheme(theme) {
    const html = document.documentElement;

    if (theme === DARK_THEME) {
      html.setAttribute('data-theme', DARK_THEME);
    } else {
      html.removeAttribute('data-theme');
    }

    localStorage.setItem(THEME_KEY, theme);
    updateThemeIcon(theme);
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
