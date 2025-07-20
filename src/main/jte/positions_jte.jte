@import co.grtk.srcprofit.dto.PositionDto
@import co.grtk.srcprofit.entity.OptionStatus

@import java.util.List
@param co.grtk.srcprofit.dto.PositionDto positionDto
@param List<PositionDto> optionHistory
@param List<PositionDto> openOptions

<div class="pagetitle">
    <h1>Positions</h1>
    <nav>
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="/">Home</a></li>
            <li class="breadcrumb-item active">Positions</li>
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
                            <h5 class="card-title">Market Value</h5>

                            <div class="d-flex align-items-center">
                                <div class="card-icon rounded-circle d-flex align-items-center justify-content-center">
                                    <i class="bi bi-cart"></i>
                                </div>
                                <div class="ps-3">
                                    <h6 class="currency-usd">${positionDto.getMarketValue()}</h6>
                                    @if(positionDto.getMarketVsPositionsPercentage() > 0)
                                    <span class="text-success small pt-1 fw-bold">${positionDto.getMarketVsPositionsPercentage()} %</span>
                                    <span class="text-muted small pt-2 ps-1">Increase</span>
                                    @else
                                        <span class="text-success small pt-1 fw-bold">${positionDto.getMarketVsPositionsPercentage()} %</span>
                                        <span class="text-muted small pt-2 ps-1">Decrease</span>
                                    @endif
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
                            <h5 class="card-title">Collected Premium</h5>

                            <div class="d-flex align-items-center">
                                <div class="card-icon rounded-circle d-flex align-items-center justify-content-center">
                                    <i class="bi bi-currency-dollar"></i>
                                </div>
                                <div class="ps-3">
                                    <h6 class="currency-usd">${positionDto.getCollectedPremium()}</h6>
                                    <span class="text-danger small pt-1 fw-bold currency-usd">${positionDto.getUnRealizedProfitOrLoss()}</span>
                                    <span class="text-muted small pt-2 ps-1">Open</span>
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
                            <h5 class="card-title">Positions Value</h5>

                            <div class="d-flex align-items-center">
                                <div class="card-icon rounded-circle d-flex align-items-center justify-content-center">
                                    <i class="bi bi-people"></i>
                                </div>
                                <div class="ps-3">
                                    <h6 class="currency-usd">${positionDto.getPositionValue()}</h6>
                                    <span class="text-success small pt-1 fw-bold currency-usd">${positionDto.getCoveredPositionValue()}</span>
                                    <span class="text-muted small pt-2 ps-1">Covered</span>
                                </div>
                            </div>

                        </div>
                    </div>

                </div>
                <!-- End Positions Value Card -->


                <!-- DataTable Open Positions-->
                <div class="col-12">
                    <div class="card recent-sales overflow-auto">

                        <div class="filter">
                            <a class="icon" href="#" data-bs-toggle="dropdown"><i class="bi bi-three-dots"></i></a>
                            <ul class="dropdown-menu dropdown-menu-end dropdown-menu-arrow">
                                <li class="dropdown-header text-start">
                                    <h6>Filter</h6>
                                </li>

                                <li><a class="dropdown-item" href="#">Today</a></li>
                                <li><a class="dropdown-item" href="#">This Month</a></li>
                                <li><a class="dropdown-item" href="#">This Year</a></li>
                            </ul>
                        </div>

                        <div class="card-body">
                            <h5 class="card-title">Open Positions <span></span></h5>

                            <table id="datatableOpenPositions" class="table table-striped">
                                <thead>
                                <tr>
                                    <th scope="col">#</th>
                                    <th scope="col">Code</th>
                                    <th scope="col">Symbol</th>
                                    <th scope="col">TradeDate</th>
                                    <th scope="col">Expiration</th>
                                    <th scope="col">DaysLeft</th>
                                    <th scope="col">Value</th>
                                    <th scope="col">MrktValue</th>
                                    <th scope="col">Price</th>
                                    <th scope="col">Days</th>
                                    <th scope="col">Type</th>
                                    <th scope="col">Status</th>
                                </tr>
                                </thead>
                                <tbody>
                                @for(var option : openOptions )
                                    <tr style="cursor: pointer;">
                                        @if(option.getMarketValue() < option.getPositionValue())
                                            <td style="background-color:rgb(220 53 69)">${option.getId()}</td>
                                        @else
                                            <td>${option.getId()}</td>
                                        @endif
                                        <td style="text-wrap: nowrap;">${option.getCode()}</td>
                                        <td class="text-uppercase">${option.getTicker()}</td>
                                        <td>${option.getTradeDateString()}</td>
                                        <td>${option.getExpirationDateString()}</td>
                                        <td>${option.getDaysLeft()}</td>

                                        @if(option.getTradePrice() > 0)
                                            <td><b>${option.getPositionValue()}</b></td>
                                            <td><b>${option.getMarketValue()}</b></td>
                                            <td><b>${option.getTradePrice()}</b></td>
                                        @else
                                            <td>${option.getPositionValue()}</td>
                                            <td>${option.getMarketValue()}</td>
                                            <td>${option.getTradePrice()}</td>
                                        @endif

                                        <td>${option.getDaysBetween()}</td>
                                        <td>${option.getType()}</td>
                                        @if(OptionStatus.PENDING.equals(option.getStatus()))
                                            <td class="badge bg-warning">${option.getStatus()}</td>
                                        @elseif(OptionStatus.CLOSED.equals(option.getStatus()))
                                            <td class="badge bg-success">${option.getStatus()}</td>
                                        @else
                                            <td class="badge bg-danger">${option.getStatus()}</td>
                                        @endif
                                    </tr>
                                @endfor
                                </tbody>
                            </table>

                        </div>

                    </div>
                </div>
                <!-- End DataTable Open Positions -->

                <!-- DataTable Closed Positions-->
                <div class="col-12">
                    <div class="card recent-sales overflow-auto">

                        <div class="filter">
                            <a class="icon" href="#" data-bs-toggle="dropdown"><i class="bi bi-three-dots"></i></a>
                            <ul class="dropdown-menu dropdown-menu-end dropdown-menu-arrow">
                                <li class="dropdown-header text-start">
                                    <h6>Filter</h6>
                                </li>

                                <li><a class="dropdown-item" href="#">Today</a></li>
                                <li><a class="dropdown-item" href="#">This Month</a></li>
                                <li><a class="dropdown-item" href="#">This Year</a></li>
                            </ul>
                        </div>

                        <div class="card-body">
                            <h5 class="card-title">Closed Positions <span></span></h5>

                            <table id="datatableClosedPositions" class="table table-striped">
                                <thead>
                                <tr>
                                    <th scope="col">#</th>
                                    <th scope="col">Code</th>
                                    <th scope="col">Symbol</th>
                                    <th scope="col">TradeDate</th>
                                    <th scope="col">Expiration</th>
                                    <th scope="col">DaysLeft</th>
                                    <th scope="col">Value</th>
                                    <th scope="col">MrktValue</th>
                                    <th scope="col">Price</th>
                                    <th scope="col">Days</th>
                                    <th scope="col">Type</th>
                                    <th scope="col">Status</th>
                                </tr>
                                </thead>
                                <tbody>
                                @for(var option : optionHistory )
                                    <tr style="cursor: pointer;">
                                        <td style="color:${option.getColor()};">${option.getId()}</td>
                                        <td style="color:${option.getColor()};text-wrap: nowrap;">${option.getCode()}</td>
                                        <td class="text-uppercase">${option.getTicker()}</td>
                                        <td>${option.getTradeDateString()}</td>
                                        <td>${option.getExpirationDateString()}</td>
                                        <td>${option.getDaysLeft()}</td>

                                        @if(option.getTradePrice() > 0)
                                            <td><b>${option.getPositionValue()}</b></td>
                                            <td><b>${option.getMarketValue()}</b></td>
                                            <td><b>${option.getTradePrice()}</b></td>
                                        @else
                                            <td>${option.getPositionValue()}</td>
                                            <td>${option.getMarketValue()}</td>
                                            <td>${option.getTradePrice()}</td>
                                        @endif

                                        <td>${option.getDaysBetween()}</td>
                                        <td>${option.getType()}</td>
                                        @if(OptionStatus.PENDING.equals(option.getStatus()))
                                            <td class="badge bg-warning">${option.getStatus()}</td>
                                        @elseif(OptionStatus.CLOSED.equals(option.getStatus()))
                                            <td class="badge bg-success">${option.getStatus()}</td>
                                        @else
                                            <td class="badge bg-danger">${option.getStatus()}</td>
                                        @endif
                                    </tr>
                                @endfor
                                </tbody>
                            </table>

                        </div>

                    </div>
                </div>
                <!-- End  DataTable Closed Positions -->


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

        function datatable() {
            console.log('datatable init');
            const tableOpen = new DataTable('#datatableOpenPositions', {
                perPageSelect: [5, 10, 15, ["All", -1]],
                columnDefs: [{target: 1, visible: false, searchable: false}], // code hidden
                order: [[4, 'asc'],[1, 'asc']] // daysLeft, code
            });

            $('#datatableOpenPositions tbody').on('click', 'tr', function() {
                const ticker = tableOpen.row(this).data()[2];
                console.log('GET  /getPosition/' + ticker);
                htmx.ajax('GET', '/getPosition/' + ticker, '#main');
            });

            const tableClosed = new DataTable('#datatableClosedPositions', {
                perPageSelect: [5, 10, 15, ["All", -1]],
                columnDefs: [{target: 1, visible: false, searchable: false}], // code hidden
                order: [[1, 'asc'],[3, 'desc']] // code, TradeDate
            });

            $('#datatableClosedPositions tbody').on('click', 'tr', function() {
                const ticker = tableClosed.row(this).data()[2];
                console.log('GET  /getPosition/' + ticker);
                htmx.ajax('GET', '/getPosition/' + ticker, '#main');
            });
        }
        datatable();
    });
</script>