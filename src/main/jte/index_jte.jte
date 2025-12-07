
<!DOCTYPE html>
<html lang="en">

<head>
  <meta charset="utf-8">
  <meta content="width=device-width, initial-scale=1.0" name="viewport">

  <title>SrcProfit</title>
  <meta content="" name="description">
  <meta content="" name="keywords">

  <!-- Favicons -->
  <link href="assets/img/favicon.png" rel="icon">
  <link href="assets/img/apple-touch-icon.png" rel="apple-touch-icon">

  <!-- Google Fonts -->
  <link href="//fonts.gstatic.com" rel="preconnect">
  <link href="//fonts.googleapis.com/css?family=Open+Sans:300,300i,400,400i,600,600i,700,700i|Nunito:300,300i,400,400i,600,600i,700,700i|Poppins:300,300i,400,400i,500,500i,600,600i,700,700i" rel="stylesheet">

  <!-- Vendor CSS Files -->
  <link href="//cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/5.3.3/css/bootstrap.min.css" rel="stylesheet">
  <link href="//cdnjs.cloudflare.com/ajax/libs/bootstrap-icons/1.11.3/font/bootstrap-icons.min.css" rel="stylesheet" integrity="sha512-dPXYcDub/aeb08c63jRq/k6GaKccl256JQy/AnOq7CAnEZ9FzSL9wSbcZkMp4R26vBsMLFYH4kQ67/bbV8XaCQ==" crossorigin="anonymous" referrerpolicy="no-referrer" />
  <script src="//cdnjs.cloudflare.com/ajax/libs/boxicons/2.1.4/dist/boxicons.min.js" integrity="sha512-VTptlAlSWKaYE3DbrmwNYTzZg1zO6CtoGxplxlHxObgfLiCcRYDBqzTUWE/0ANUmyfYR7R227ZirV/I4rQsPNA==" crossorigin="anonymous" referrerpolicy="no-referrer"></script>

  <link href="//cdnjs.cloudflare.com/ajax/libs/remixicon/4.6.0/remixicon.min.css" rel="stylesheet" integrity="sha512-XcIsjKMcuVe0Ucj/xgIXQnytNwBttJbNjltBV18IOnru2lDPe9KRRyvCXw6Y5H415vbBLRm8+q6fmLUU7DfO6Q==" crossorigin="anonymous" referrerpolicy="no-referrer" />

  <link href="//cdn.datatables.net/2.3.2/css/dataTables.bootstrap5.css" rel="stylesheet">
  <link href="//cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/5.3.3/css/bootstrap.min.css" rel="stylesheet">
  <!-- Template Main CSS File -->
  <link href="assets/css/style.css" rel="stylesheet">
  <!-- DataTables Dark Mode Overrides -->
  <link href="assets/css/datatables-dark.css" rel="stylesheet">
  <script src="//unpkg.com/htmx.org@2.0.7"></script>
  <script src="//code.jquery.com/jquery-3.7.1.js"></script>
  <!-- TradingView Widgets: loaded dynamically per widget -->
</head>

<body>

  <!-- ======= Header ======= -->
  <header id="header" class="header fixed-top d-flex align-items-center">

    <div class="d-flex align-items-center justify-content-between">
      <a href="/" class="logo d-flex align-items-center">
        <img src="assets/img/logo.png" alt="">
        <span class="d-none d-lg-block">
            SrcProfit
            <label id="minutes">00</label>:<label id="seconds">00</label>
        </span>
      </a>
      <i class="bi bi-list toggle-sidebar-btn"></i>
    </div><!-- End Logo -->

    <div class="search-bar">
      <form class="search-form d-flex align-items-center"
            hx-post="/tradelogFromDate"
            hx-trigger="submit"
            hx-target="#main"
            hx-swap="innerHTML"
            hx-on::afterRequest="console.log('POST /tradelogFromDate');">
        <input type="date" name="fromDate" placeholder="Search" title="Enter search keyword">
        <button type="submit" title="Search"><i class="bi bi-search"></i></button>
      </form>
    </div>

    <nav class="header-nav ms-auto">
      <ul class="d-flex align-items-center">

        <li class="nav-item d-block d-lg-none">
          <a class="nav-link nav-icon search-bar-toggle " href="#">
            <i class="bi bi-search"></i>
          </a>
        </li><!-- End Search Icon-->

        <li class="nav-item">
          <a class="nav-link nav-icon" href="#" id="theme-toggle" title="Toggle Dark Mode">
            <i class="bi bi-moon"></i>
          </a>
        </li><!-- End Dark Mode Toggle -->

      </ul>
    </nav><!-- End Icons Navigation -->

  </header><!-- End Header -->

  <!-- ======= Sidebar ======= -->
  <aside id="sidebar" class="sidebar">

    <ul class="sidebar-nav" id="sidebar-nav">

        <li class="nav-item">
            <a class="nav-link " href="#"
               hx-indicator="#spinner"
               hx-get="/dashboard/all"
               hx-trigger="click"
               hx-target="#main"
               hx-swap="innerHTML"
               hx-on::afterRequest="console.log('GET /dashboard');">
              <i class="bi bi-grid"></i>
              <span>Dashboard</span>
            </a>
        </li><!-- End Dashboard Nav -->
        <li class="nav-item">
            <a class="nav-link collapsed" href="#"
               hx-indicator="#spinner"
               hx-get="/openpositions"
               hx-trigger="click"
               hx-target="#main"
               hx-swap="innerHTML"
               hx-on::afterRequest="console.log('GET /openpositions')">
              <i class="bi bi-folder2-open"></i>
              <span>Open Positions</span>
            </a>
        </li><!-- End Open Positions -->
        <li class="nav-item">
            <a class="nav-link collapsed" href="#"
               hx-indicator="#spinner"
               hx-get="/tradelog"
               hx-trigger="click"
               hx-target="#main"
               hx-swap="innerHTML"
               hx-on::afterRequest="console.log('GET /tradelog')">
              <i class="bi bi-journal-text"></i>
              <span>Trade Log</span>
            </a>
        </li><!-- End Trade Log -->
        <li class="nav-item">
            <a class="nav-link collapsed" href="#"
               hx-indicator="#spinner"
               hx-get="/tradehistory"
               hx-trigger="click"
               hx-target="#main"
               hx-swap="innerHTML"
               hx-on::afterRequest="console.log('GET /tradehistory')">
              <i class="bi bi-archive"></i>
              <span>Trade History</span>
            </a>
        </li><!-- End Trade History -->
        <li class="nav-item">
            <a class="nav-link collapsed" href="#"
               hx-indicator="#spinner"
               hx-get="/calculatePosition"
               hx-trigger="click"
               hx-target="#main"
               hx-swap="innerHTML"
               hx-on::afterRequest="console.log('GET /calculatePosition');">
              <i class="bi bi-calculator"></i>
              <span>Position Calculator</span>
            </a>
        </li><!-- End PositionForm -->
        <li class="nav-item">
            <a class="nav-link collapsed" href="#"
               hx-indicator="#spinner"
               hx-get="/instruments"
               hx-trigger="click"
               hx-target="#main"
               hx-swap="innerHTML"
               hx-on::afterRequest="console.log('GET /instruments')">
              <i class="bi bi-collection"></i>
              <span>Instruments</span>
            </a>
        </li><!-- End Instruments -->
        <li class="nav-item">
            <a class="nav-link collapsed" href="#"
               hx-indicator="#spinner"
               hx-get="/netAssetValues"
               hx-trigger="click"
               hx-target="#main"
               hx-swap="innerHTML"
               hx-on::afterRequest="console.log('GET /netAssetValues')">
              <i class="bi bi-pie-chart"></i>
              <span>Net Asset Value</span>
            </a>
        </li><!-- End Net Asset Values -->
        <li class="nav-item">
            <a class="nav-link collapsed" href="#"
               hx-indicator="#spinner"
               hx-get="/earnings"
               hx-trigger="click"
               hx-target="#main"
               hx-swap="innerHTML"
               hx-on::afterRequest="console.log('GET /earnings')">
                <i class="bi bi-calendar"></i>
                <span>Earnings</span>
            </a>
        </li><!-- End Earnings -->
        <li class="nav-item">
            <a class="nav-link collapsed" href="#"
               hx-indicator="#spinner"
               hx-get="/flexImportHistory"
               hx-trigger="click"
               hx-target="#main"
               hx-swap="innerHTML"
               hx-on::afterRequest="console.log('GET /flexImportHistory')">
                <i class="bi bi-clock-history"></i>
                <span>Import History</span>
            </a>
        </li><!-- End FLEX Import History -->

        <%-- COMMENTED OUT: Admin Data section
        <li class="nav-item">
            <a class="nav-link collapsed" data-bs-target="#admin-data" data-bs-toggle="collapse" href="#" aria-expanded="false">
              <i class="bi bi-bar-chart"></i><span>Admin Data</span><i class="bi bi-chevron-down ms-auto"></i>
            </a>
            <ul id="admin-data" class="nav-content collapse" data-bs-parent="#sidebar-nav" style="">
                <li class="nav-item">
                <a class="nav-link collapsed" href="#"
                   hx-indicator="#spinner"
                   hx-get="/earningsCalendar"
                   hx-trigger="click"
                   hx-target="#earningsCalendarImport"
                   hx-swap="innerHTML"
                   hx-on::afterRequest="console.log('GET /earningsCalendar')">
                    <i class="bi bi-card-list"></i>
                    <span>Earnings Calendar</span>
                    &nbsp;
                    <span id="earningsCalendarImport"></span>
                </a>
            </li><!-- End Alpha Vintage Earnings Calendar -->
                <li class="nav-item">
                    <a class="nav-link collapsed" href="#"
                       hx-indicator="#spinner"
                       hx-get="/ibkrFlexTradesImport"
                       hx-trigger="click"
                       hx-target="#ibkrFlexTradesImport"
                       hx-swap="innerHTML"
                       hx-on::afterRequest="console.log('GET /ibkrFlexTradesImport')">
                        <i class="bi bi-card-list"></i>
                        <span>Ibkr Flex Trades</span>
                        &nbsp;
                        <span id="ibkrFlexTradesImport"></span>
                    </a>
                </li><!-- End Ibkr Flex Trades Import -->
                <li class="nav-item">
                <a class="nav-link collapsed" href="#"
                   hx-indicator="#spinner"
                   hx-get="/ibkrFlexNetAssetValueImport"
                   hx-trigger="click"
                   hx-target="#ibkrFlexNetAssetValueImport"
                   hx-swap="innerHTML"
                   hx-on::afterRequest="console.log('GET /ibkrFlexNetAssetValueImport')">
                  <i class="bi bi-card-list"></i>
                  <span>Ibkr Flex Net Asset</span>
                  &nbsp;
                  <span id="ibkrFlexNetAssetValueImport"></span>
                </a>
              </li><!-- End Ibkr Flex Net Asset Value Import -->
            </ul>
        </li><!-- End Admin -->
        --%>


        <%-- COMMENTED OUT: Admin menu section
        <li class="nav-item">
            <a class="nav-link collapsed" data-bs-target="#admin-nav" data-bs-toggle="collapse" href="#" aria-expanded="false">
                <i class="bi bi-bar-chart"></i><span>Admin</span><i class="bi bi-chevron-down ms-auto"></i>
            </a>
            <ul id="admin-nav" class="nav-content collapse" data-bs-parent="#sidebar-nav" style="">
                <li class="nav-item">
                    <a class="nav-link collapsed" href="#"
                       hx-indicator="#spinner"
                       hx-get="/alpacaMarketData"
                       hx-trigger="click"
                       hx-target="#main"
                       hx-swap="innerHTML"
                       hx-on::afterRequest="console.log('GET /alpacaMarketData')">
                        <i class="bi bi-card-list"></i>
                        <span>Alpaca Market Data</span>
                    </a>
                </li><!-- End Alpaca Market Data -->
                <li class="nav-item">
                    <a class="nav-link collapsed" href="#"
                       hx-indicator="#spinner"
                       hx-get="/ibkrMarketData"
                       hx-trigger="click"
                       hx-target="#main"
                       hx-swap="innerHTML"
                       hx-on::afterRequest="console.log('GET /ibkrMarketData')">
                        <i class="bi bi-card-list"></i>
                        <span>Ibkr Market Data</span>
                    </a>
                </li><!-- End Ibkr Market Data -->
                <li class="nav-item">
                    <a class="nav-link collapsed" href="#"
                       hx-indicator="#spinner"
                       hx-get="/ibkrWatchlist"
                       hx-trigger="click"
                       hx-target="#main"
                       hx-swap="innerHTML"
                       hx-on::afterRequest="console.log('GET /ibkrWatchlist')">
                        <i class="bi bi-card-list"></i>
                        <span>Ibkr Watchlist</span>
                    </a>
                </li><!-- End Ibkr Watchlist -->
                <li class="nav-item">
                    <a class="nav-link collapsed" href="/alpacaOptionsQuotes" target="_blank">
                        <i class="bi bi-card-list"></i>
                        <span>Alpaca Options Quotes</span>
                    </a>
                </li><!-- End Alpaca Options Quotes -->
                <li class="nav-item">
                    <a class="nav-link collapsed" href="/ibkrLatestTrades" target="_blank">
                        <i class="bi bi-card-list"></i>
                        <span>Ibkr Latest Trades</span>
                    </a>
                </li><!-- End Ibkr LatestTrades -->
                <li class="nav-item">
                    <a class="nav-link collapsed" href="https://localhost:5055/sso/Login" target="_blank">
                        <i class="bi bi-card-list"></i>
                        <span>Ibkr Login</span>
                    </a>
                </li><!-- End Ibkr Login Page Nav -->
            </ul>
        </li><!-- End Admin -->
        --%>

        <li class="nav-item htmx-indicator" id="spinner">
        <div class="spinner-border" role="status">
          <span class="visually-hidden">Loading...</span>
        </div>
        <div class="spinner-border text-secondary" role="status">
          <span class="visually-hidden">Loading...</span>
        </div>
        <div class="spinner-border text-success" role="status">
          <span class="visually-hidden">Loading...</span>
        </div>
        <div class="spinner-border text-danger" role="status">
          <span class="visually-hidden">Loading...</span>
        </div>
      </li><!-- End Hidden Spinner Page Nav -->

    </ul>

  </aside><!-- End Sidebar-->

  <main id="main" class="main"
        hx-get="/dashboard/all"
        hx-trigger="load delay:0s"
        hx-swap="innerHTML"
        hx-on::afterRequest="console.log('GET /dashboard/all');">

  </main><!-- End #main -->



  <!-- ======= Footer ======= -->
  <footer id="footer" class="footer">
    <div class="copyright">
      &copy; Copyright <strong><span>NiceAdmin</span></strong>. All Rights Reserved
    </div>
    <div class="credits">
      <!-- All the links in the footer should remain intact. -->
      <!-- You can delete the links only if you purchased the pro version. -->
      <!-- Licensing information: https://bootstrapmade.com/license/ -->
      <!-- Purchase the pro version with working PHP/AJAX contact form: https://bootstrapmade.com/nice-admin-bootstrap-admin-html-template/ -->
      Designed by <a href="https://bootstrapmade.com/">BootstrapMade</a>
    </div>
  </footer><!-- End Footer -->

  <a href="#" class="back-to-top d-flex align-items-center justify-content-center"><i class="bi bi-arrow-up-short"></i></a>

  <!-- Vendor JS Files -->
  <script src="//cdnjs.cloudflare.com/ajax/libs/apexcharts/4.5.0/apexcharts.min.js" integrity="sha512-yMnvLee1a5S9nemgCoMth5YvOchnQMFMOSao/bH6SLAXZnauuHs1gd92DnE9+sVQ5aglei3LZDelg8LauSlWkw==" crossorigin="anonymous" referrerpolicy="no-referrer"></script>
  <script src="//cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/5.3.3/js/bootstrap.bundle.min.js"></script>
  <script src="//cdn.datatables.net/2.3.2/js/dataTables.js"></script>
  <script src="//cdn.datatables.net/2.3.2/js/dataTables.bootstrap5.js"></script>

  <!-- Template Main JS File -->
  <script src="assets/js/main.js"></script>
  <!-- Dark Mode Toggle -->
  <script src="assets/js/dark-mode.js"></script>
  <!-- TradingView Integration -->
  <script src="assets/js/tradingview-integration.js"></script>
  <script>
      $( document ).ready(function() {
          console.log( "ready!" );
          totalSeconds = 0;
      });
  </script>
</body>

</html>