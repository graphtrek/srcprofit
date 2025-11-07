@import co.grtk.srcprofit.dto.PositionDto

@import java.util.List
@param co.grtk.srcprofit.dto.PositionDto positionDto
@param co.grtk.srcprofit.dto.DashboardDto dashboardDto
@param List<PositionDto> optionHistory
@param List<PositionDto> openOptions
@param String datesCsv
@param String navDatesCsv
@param String premiumsCsv
@param String totalsCsv
@param String cashCsv
@param String stockCsv
@param String optionsCsv

<div class="pagetitle">
    <h1>Dashboard</h1>
    <nav>
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="/">Home</a></li>
            <li class="breadcrumb-item active">Dashboard</li>
        </ol>
    </nav>
</div><!-- End Page Title -->

<section class="section dashboard">
    <div class="row">

        <!-- Left side columns -->
        <div class="col-lg-12">
            <div class="row">

                <!-- Unrealized P&L Card -->

                <div class="col-xxl-4 col-md-6">
                    <div class="card info-card sales-card">
                        <div class="card-body">
                            <h5 class="card-title">QQQ
                                @if (dashboardDto.QQQ.getChangePercent() > 0)
                                    <span class="badge fw-bold">${dashboardDto.QQQ.getPrice()} $</span>
                                    <span class="badge bg-success-light fw-bold">${dashboardDto.QQQ.getChangePercent()} %</span> /
                                    <span class="badge bg-success-light fw-bold">${dashboardDto.QQQ.getChange()} $</span>
                                @else
                                    <span class="badge fw-bold">${dashboardDto.QQQ.getPrice()} $</span>
                                    <span class="badge bg-danger-light fw-bold">${dashboardDto.QQQ.getChangePercent()} %</span> /
                                    <span class="badge bg-danger-light fw-bold">${dashboardDto.QQQ.getChange()} $</span>
                                @endif
                            </h5>

                            <div class="d-flex align-items-center">
                                <div class="zoom" style="width: 100%; height: 250px;">
                                    <!-- Feature flag: data-use-tradingview="false" to fallback to FinViz -->
                                    <div data-tradingview-symbol="QQQ" data-use-tradingview="true" style="width: 100%; height: 100%;">
                                        <!-- Fallback if TradingView disabled -->
                                        <a href="https://www.tradingview.com/chart/?symbol=QQQ" target="_blank">
                                            <img id="finvizQQQ" style="width: 100%;" src="https://charts2-node.finviz.com/chart.ashx?cs=l&t=QQQ&tf=d&s=linear&ct=candle_stick&tm=d&o[0][ot]=sma&o[0][op]=50&o[0][oc]=FF8F33C6&o[1][ot]=sma&o[1][op]=200&o[1][oc]=DCB3326D&o[2][ot]=sma&o[2][op]=20&o[2][oc]=DC32B363&o[3][ot]=patterns&o[3][op]=&o[3][oc]=000"  alt="FinViz Chart QQQ"/>
                                        </a>
                                    </div>
                                </div>
                            </div>
                        </div>

                    </div>
                </div>

                <!-- End Unrealized P&L -->

                <!-- Realized P&L Card -->

                <div class="col-xxl-4 col-md-6">
                    <div class="card info-card revenue-card">
                      <div class="card-body">
                            <h5 class="card-title">GDX
                                @if (dashboardDto.GDX.getChangePercent() > 0)
                                    <span class="badge fw-bold">${dashboardDto.GDX.getPrice()} $</span>
                                    <span class="badge bg-success-light fw-bold">${dashboardDto.GDX.getChangePercent()} %</span> /
                                    <span class="badge bg-success-light fw-bold">${dashboardDto.GDX.getChange()} $</span>
                                @else
                                    <span class="badge fw-bold">${dashboardDto.GDX.getPrice()} $</span>
                                    <span class="badge bg-danger-light fw-bold">${dashboardDto.GDX.getChangePercent()} %</span> /
                                    <span class="badge bg-danger-light fw-bold">${dashboardDto.GDX.getChange()} $</span>
                                @endif
                            </h5>

                          <div class="d-flex align-items-center">
                              <div class="zoom" style="width: 100%; height: 250px;">
                                  <!-- Feature flag: data-use-tradingview="false" to fallback to FinViz -->
                                  <div data-tradingview-symbol="GDX" data-use-tradingview="true" style="width: 100%; height: 100%;">
                                      <!-- Fallback if TradingView disabled -->
                                      <a href="https://www.tradingview.com/chart/?symbol=GDX" target="_blank">
                                        <img id="finvizGDX" style="width: 100%;" src="https://charts2-node.finviz.com/chart.ashx?cs=l&t=GDX&tf=d&s=linear&ct=candle_stick&tm=d&o[0][ot]=sma&o[0][op]=50&o[0][oc]=FF8F33C6&o[1][ot]=sma&o[1][op]=200&o[1][oc]=DCB3326D&o[2][ot]=sma&o[2][op]=20&o[2][oc]=DC32B363&o[3][ot]=patterns&o[3][op]=&o[3][oc]=000"  alt="FinViz Chart GDX"/>
                                      </a>
                                  </div>
                              </div>
                          </div>
                        </div>

                    </div>
                </div>

                <!-- End Realized P&L Card -->

                <!-- Positions Value Card -->


                <div class="col-xxl-4 col-xl-12">

                    <div class="card info-card customers-card">
                        <div class="card-body">
                            <h5 class="card-title">IBIT
                                @if (dashboardDto.IBIT.getChangePercent() > 0)
                                    <span class="badge fw-bold">${dashboardDto.IBIT.getPrice()} $</span>
                                    <span class="badge bg-success-light fw-bold">${dashboardDto.IBIT.getChangePercent()} %</span> /
                                    <span class="badge bg-success-light fw-bold">${dashboardDto.IBIT.getChange()} $</span>
                                @else
                                    <span class="badge fw-bold">${dashboardDto.IBIT.getPrice()} $</span>
                                    <span class="badge bg-danger-light fw-bold">${dashboardDto.IBIT.getChangePercent()} %</span> /
                                    <span class="badge bg-danger-light fw-bold">${dashboardDto.IBIT.getChange()} $</span>
                                @endif
                            </h5>

                            <div class="d-flex align-items-center">
                                <div class="zoom" style="width: 100%; height: 250px;">
                                    <!-- Feature flag: data-use-tradingview="false" to fallback to FinViz -->
                                    <div data-tradingview-symbol="IBIT" data-use-tradingview="true" style="width: 100%; height: 100%;">
                                        <!-- Fallback if TradingView disabled -->
                                        <a href="https://www.tradingview.com/chart/?symbol=IBIT" target="_blank">
                                            <img id="finvizIBIT" style="width: 100%;" src="https://charts2-node.finviz.com/chart.ashx?cs=l&t=IBIT&tf=d&s=linear&ct=candle_stick&tm=d&o[0][ot]=sma&o[0][op]=50&o[0][oc]=FF8F33C6&o[1][ot]=sma&o[1][op]=200&o[1][oc]=DCB3326D&o[2][ot]=sma&o[2][op]=20&o[2][oc]=DC32B363&o[3][ot]=patterns&o[3][op]=&o[3][oc]=000"  alt="FinViz Chart IBIT"/>
                                        </a>
                                    </div>
                                </div>
                            </div>

                        </div>
                    </div>

                </div>


                <!-- End Positions Value Card -->

                <!-- Reports -->
                <div id="chartCard" class="col-12">
                    <div class="card">

                        <div class="filter">
                            <a class="icon" href="#" data-bs-toggle="dropdown"><i class="bi bi-three-dots"></i></a>
                            <ul class="dropdown-menu dropdown-menu-end dropdown-menu-arrow">
                                <li class="dropdown-header text-start">
                                    <h6>Filter</h6>
                                </li>

                                <li>
                                    <a class="dropdown-item" href="#"
                                       hx-indicator="#spinner"
                                       hx-get="/dashboard/week"
                                       hx-trigger="click"
                                       hx-target="#main"
                                       hx-swap="innerHTML"
                                       hx-on::afterRequest="console.log('GET /dashboard/week');">This Week</a>
                                </li>
                                <li>
                                    <a class="dropdown-item" href="#"
                                       hx-indicator="#spinner"
                                       hx-get="/dashboard/month"
                                       hx-trigger="click"
                                       hx-target="#main"
                                       hx-swap="innerHTML"
                                       hx-on::afterRequest="console.log('GET /dashboard/month');">This Month</a>
                                </li>
                                <li>
                                    <a class="dropdown-item" href="#"
                                       hx-indicator="#spinner"
                                       hx-get="/dashboard/year"
                                       hx-trigger="click"
                                       hx-target="#main"
                                       hx-swap="innerHTML"
                                       hx-on::afterRequest="console.log('GET /dashboard/year');">This Year</a>
                                </li>
                                <li>
                                    <a class="dropdown-item" href="/dashboard/all"
                                       hx-indicator="#spinner"
                                       hx-get="/dashboard/all"
                                       hx-trigger="click"
                                       hx-target="#main"
                                       hx-swap="innerHTML"
                                       hx-on::afterRequest="console.log('GET /dashboard/all');">All</a>
                                </li>
                            </ul>
                        </div>

                        <div class="card-body">
                            <h5 class="card-title">Collected Premium <span class="fs-5">${positionDto.getCollectedPremium()} $</span></h5>

                            <!-- Premium Chart -->
                            <div id="reportsChart"></div>
                            <!-- End Premium Chart -->

                            <!-- Net Asset Values Chart -->
                            <div id="netAssetValuesChart"></div>
                            <!-- End Net Asset Values Chart -->

                        </div>

                    </div>
                </div>
                <!-- End Reports -->



            </div>
        </div><!-- End Left side columns -->


    </div>
</section>

<script>
    $( document ).ready(function() {
        console.log( "ready!" );

        $('.currency-usd').each(function () {
            var monetary_value = $(this).text();
            var i = new Intl.NumberFormat('en-US', {
                style: 'currency',
                currency: 'USD',
                maximumFractionDigits: 0
            }).format(monetary_value);
            $(this).text(i);
        });

        const dailyPremium = [$unsafe{premiumsCsv}];
        const dailyTotals = [$unsafe{totalsCsv}];
        const dailyCash = [$unsafe{cashCsv}];
        const dailyStock = [$unsafe{stockCsv}];
        const dailyOptions = [$unsafe{optionsCsv}];
        const dates = [$unsafe{datesCsv}];
        const navDates = [$unsafe{navDatesCsv}];


        function chart() {

            new ApexCharts(document.querySelector("#reportsChart"), {
                series: [
                    {
                        name: 'Daily Premium',
                        data: dailyPremium
                    }
                ],
                chart: {
                    height: 250,
                    type: 'area',
                    toolbar: {
                        show: false
                    },
                },
                markers: {
                    size: 4
                },
                colors: [ '#2eca6a','#ff771d','#4154f1'],
                fill: {
                    type: "gradient",
                    gradient: {
                        shadeIntensity: 1,
                        opacityFrom: 0.3,
                        opacityTo: 0.4,
                        stops: [0, 90, 100]
                    }
                },
                dataLabels: {
                    enabled: false
                },
                stroke: {
                    curve: 'smooth',
                    width: 2
                },
                xaxis: {
                    labels: {format: 'yy-MM-dd'},
                    type: 'date',
                    categories: dates
                },
                tooltip: {
                    x: {
                        format: 'yy-MM-dd'
                    },
                }
            }).render();

            new ApexCharts(document.querySelector("#netAssetValuesChart"), {
                series: [
                    {
                        name: 'Total',
                        data: dailyTotals
                    },
                    {
                        name: 'Cash',
                        data: dailyCash
                    },
                    {
                        name: 'Stock',
                        data: dailyStock
                    },
                    {
                        name: 'Options',
                        data: dailyOptions
                    }
                ],
                chart: {
                    height: 450,
                    type: 'area',
                    toolbar: {
                        show: false
                    },
                },
                markers: {
                    size: 4
                },
                colors: [ '#2eca6a','#ff771d','#4154f1','#f14161'],
                fill: {
                    type: "gradient",
                    gradient: {
                        shadeIntensity: 1,
                        opacityFrom: 0.3,
                        opacityTo: 0.4,
                        stops: [0, 90, 100]
                    }
                },
                dataLabels: {
                    enabled: false
                },
                stroke: {
                    curve: 'smooth',
                    width: 2
                },
                xaxis: {
                    labels: {format: 'yy-MM-dd'},
                    type: 'date',
                    categories: navDates
                },
                tooltip: {
                    x: {
                        format: 'yy-MM-dd'
                    },
                }
            }).render();

        }



        $("#chartCard").show();
        chart();
        totalSeconds = 0;
    });
</script>


