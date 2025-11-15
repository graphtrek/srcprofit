@import co.grtk.srcprofit.dto.PositionDto
@import co.grtk.srcprofit.entity.OptionStatus

@import java.util.List
@param co.grtk.srcprofit.dto.PositionDto positionDto
@param List<PositionDto> openOptions
@param List<PositionDto> weeklyOpenPositions

<div class="pagetitle">
    <h1>Trade Log</h1>
    <nav>
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="/">Home</a></li>
            <li class="breadcrumb-item active">Trade Log</li>
        </ol>
    </nav>
</div><!-- End Page Title -->

<section class="section dashboard">
    <div class="row">

        <!-- Left side columns -->
        <div class="col-lg-12">
            <div class="row">

                <!-- Buy Obligation Market Value Card -->

                <div class="col-xxl-4 col-md-6">
                    <div class="card info-card sales-card">
                        <div class="card-body">
                            <h5 class="card-title">Buy Obligation Market Value</h5>

                            <div class="d-flex align-items-center">
                                <div class="card-icon rounded-circle d-flex align-items-center justify-content-center">
                                    <i class="bi bi-cart"></i>
                                </div>
                                <div class="ps-3">
                                    <h6 class="currency-usd">${positionDto.getMarketValue()}</h6>
                                    @if(positionDto.getMarketVsPositionsPercentage() > 0)
                                        <span class="text-success small pt-1 fw-bold">+ ${positionDto.getMarketVsPositionsPercentage()} %</span> /
                                        <span class="text-success small pt-1 fw-bold currency-usd">${positionDto.getMarketValue() - positionDto.getPositionValue()}</span>
                                        <span class="text-muted small pt-2 ps-1">Covered</span>
                                    @else
                                        <span class="text-danger small pt-1 fw-bold">${positionDto.getMarketVsPositionsPercentage()} %</span> /
                                        <span class="text-danger small pt-1 fw-bold currency-usd">${positionDto.getPositionValue() - positionDto.getMarketValue()}</span>
                                        <span class="text-muted small pt-2 ps-1">Covered</span>
                                    @endif
                                 </div>
                            </div>
                        </div>

                    </div>
                </div>

                <!-- EndBuy Obligation Market Value -->

                <!-- Buy Obligation Card -->

                <div class="col-xxl-4 col-xl-12">

                    <div class="card info-card customers-card">
                        <div class="card-body">
                            <h5 class="card-title">Buy Obligation</h5>

                            <div class="d-flex align-items-center">
                                <div class="card-icon rounded-circle d-flex align-items-center justify-content-center">
                                    <i class="bi bi-people"></i>
                                </div>
                                <div class="ps-3">
                                    <h6 class="currency-usd">${positionDto.getPositionValue()}</h6>
                                    <span class="text-success small pt-1 fw-bold currency-usd">${positionDto.getCash()}</span>
                                    <span class="text-muted small pt-2 ps-1">Cash</span>
                                    <span class="text-success small pt-1 fw-bold currency-usd">${positionDto.getStock()}</span>
                                    <span class="text-muted small pt-2 ps-1">Stock</span>
                                </div>
                            </div>

                        </div>
                    </div>

                </div>
                <!-- End Buy Obligation Card -->

                <!-- PUT Premium Card -->

                <div class="col-xxl-4 col-md-6">
                    <div class="card info-card revenue-card">
                        <div class="card-body">
                            <h5 class="card-title">PUT Premium</h5>

                            <div class="d-flex align-items-center">
                                <div class="card-icon rounded-circle d-flex align-items-center justify-content-center">
                                    <i class="bi bi-currency-dollar"></i>
                                </div>
                                <div class="ps-3">
                                    <h6 class="currency-usd">${positionDto.getPut()}</h6>
                                    <span class="text-muted small pt-2 ps-1">PUT</span>
                                    <span class="text-danger small pt-1 fw-bold currency-usd">${positionDto.getPut()}</span>
                                    <span class="text-muted small pt-2 ps-1">/</span>
                                    <span class="text-danger small pt-1 fw-bold currency-usd">${positionDto.getPutMarketPrice()}</span>
                                    <span class="text-muted small pt-2 ps-1">/</span>
                                    @if(positionDto.getPutMarketPrice() > positionDto.getPut())
                                        <span class="text-danger small pt-1 fw-bold currency-usd">- ${positionDto.getPutMarketPrice()}</span>
                                    @else
                                        <span class="text-success small pt-1 fw-bold currency-usd">${positionDto.getProfitOrLoss()}</span>
                                    @endif
                                </div>
                            </div>
                        </div>

                    </div>
                </div>
                <!-- End PUT Premium Card -->

                <!-- Sell Obligation Market Value Card (CALL) -->
                <div class="col-xxl-4 col-md-6">
                    <div class="card info-card sales-card">
                        <div class="card-body">
                            <h5 class="card-title">Sell Obligation Market Value</h5>

                            <div class="d-flex align-items-center">
                                <div class="card-icon rounded-circle d-flex align-items-center justify-content-center">
                                    <i class="bi bi-box-arrow-up"></i>
                                </div>
                                <div class="ps-3">
                                    <h6 class="currency-usd">${positionDto.getCallObligationMarketValue()}</h6>
                                    @if(positionDto.getCallMarketVsObligationsPercentage() > 0)
                                        <span class="text-success small pt-1 fw-bold">+ ${positionDto.getCallMarketVsObligationsPercentage()} %</span> /
                                        <span class="text-success small pt-1 fw-bold currency-usd">${positionDto.getCallObligationMarketValue() - positionDto.getCallObligationValue()}</span>
                                        <span class="text-muted small pt-2 ps-1">Covered</span>
                                    @else
                                        <span class="text-danger small pt-1 fw-bold">${positionDto.getCallMarketVsObligationsPercentage()} %</span> /
                                        <span class="text-danger small pt-1 fw-bold currency-usd">${positionDto.getCallObligationValue() - positionDto.getCallObligationMarketValue()}</span>
                                        <span class="text-muted small pt-2 ps-1">Covered</span>
                                    @endif
                                </div>
                            </div>
                        </div>

                    </div>
                </div>
                <!-- End Sell Obligation Market Value Card -->

                <!-- Sell Obligation Card (CALL) -->
                <div class="col-xxl-4 col-xl-12">
                    <div class="card info-card customers-card">
                        <div class="card-body">
                            <h5 class="card-title">Sell Obligation</h5>

                            <div class="d-flex align-items-center">
                                <div class="card-icon rounded-circle d-flex align-items-center justify-content-center">
                                    <i class="bi bi-people"></i>
                                </div>
                                <div class="ps-3">
                                    <h6 class="currency-usd">${positionDto.getCallObligationValue()}</h6>
                                    @if(positionDto.getStock() != null && positionDto.getCallObligationValue() != null && positionDto.getStock() >= positionDto.getCallObligationValue())
                                        <span class="text-success small pt-1 fw-bold">Covered</span>
                                    @else
                                        <span class="text-danger small pt-1 fw-bold">Not Covered</span>
                                    @endif
                                    <span class="text-muted small pt-2 ps-1">/</span>
                                    <span class="text-success small pt-1 fw-bold currency-usd">${positionDto.getStock()}</span>
                                    <span class="text-muted small pt-2 ps-1">Stock</span>
                                </div>
                            </div>

                        </div>
                    </div>

                </div>
                <!-- End Sell Obligation Card -->

                <!-- CALL Premium Card -->
                <div class="col-xxl-4 col-md-6">
                    <div class="card info-card revenue-card">
                        <div class="card-body">
                            <h5 class="card-title">CALL Premium</h5>

                            <div class="d-flex align-items-center">
                                <div class="card-icon rounded-circle d-flex align-items-center justify-content-center">
                                    <i class="bi bi-currency-dollar"></i>
                                </div>
                                <div class="ps-3">
                                    <h6 class="currency-usd">${positionDto.getCall()}</h6>
                                    <span class="text-muted small pt-2 ps-1">CALL</span>
                                    <span class="text-success small pt-1 fw-bold currency-usd">${positionDto.getCall()}</span>
                                    <span class="text-muted small pt-2 ps-1">/</span>
                                    <span class="text-success small pt-1 fw-bold currency-usd">${positionDto.getCallMarketPrice()}</span>
                                </div>
                            </div>
                        </div>

                    </div>
                </div>
                <!-- End CALL Premium Card -->

                <!-- DataTable Weekly Buy Obligations -->
                <div class="col-12">
                    <div class="card recent-sales overflow-auto">


                        <div class="card-body">
                            <h5 class="card-title">Weekly Buy Obligations <span></span></h5>

                            <table id="datatableWeeklyOpenPositions" class="table table-striped">
                                <thead>
                                <tr>
                                    <th scope="col">Expiration</th>
                                    <th scope="col">DaysLeft</th>
                                    <th scope="col">Value</th>
                                    <th scope="col">MrktValue</th>
                                    <th scope="col">Price</th>
                                    <th scope="col">MrktPrice</th>
                                    <th scope="col">R.O.I</th>
                                    <th scope="col">P.O.P</th>
                                    <th scope="col">P&L</th>
                                </tr>
                                </thead>
                                <tbody>
                                @for(var option : weeklyOpenPositions )
                                    <tr style="cursor: pointer;">
                                        <td>${option.getExpirationDateString()}</td>
                                        <td>${option.getDaysLeft()}</td>
                                        <td class="currency-usd">${option.getPositionValue()}</td>
                                        <td class="currency-usd">${option.getMarketValue()}</td>
                                        <td class="currency-usd">${option.getTradePrice()}</td>
                                        <td class="currency-usd">${option.getMarketPrice()}</td>
                                        <td>${option.getAnnualizedRoiPercent()}</td>
                                        <td>${option.getProbability()}</td>
                                        <td class="currency-usd">${option.getProfitOrLoss()}</td>
                                    </tr>
                                @endfor
                                </tbody>
                            </table>

                        </div>

                    </div>
                </div>
                <!-- End DataTable weekly Open Positions -->


                <!-- DataTable Open Positions-->
                <div class="col-12">
                    <div class="card recent-sales overflow-auto">

                        <div class="filter">
                            <a class="icon" href="#" data-bs-toggle="dropdown"><i class="bi bi-three-dots"></i></a>
                            <ul class="dropdown-menu dropdown-menu-end dropdown-menu-arrow">
                                <li class="dropdown-header text-start">
                                    <h6>Filter</h6>
                                </li>

                                <li><a class="dropdown-item" href="#">This Week</a></li>
                                <li><a class="dropdown-item" href="#">This Month</a></li>
                                <li><a class="dropdown-item" href="#">This Year</a></li>
                                <li><a class="dropdown-item" href="#">All</a></li>
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
                                    <th scope="col">D.Left</th>
                                    <th scope="col">Qty</th>
                                    <th scope="col">Value</th>
                                    <th scope="col">MrktValue</th>
                                    <th scope="col">Price</th>
                                    <th scope="col">MrktPrice</th>
                                    <th scope="col">P&L</th>
                                    <th scope="col">Earning</th>
                                    <th scope="col">Type</th>
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
                                        @if(option.getQuantity() < 0)
                                            <td class="text-uppercase" style="font-weight: bold;">${option.getTicker()}</td>
                                        @else
                                            <td class="text-uppercase">${option.getTicker()}</td>
                                        @endif
                                        <td>${option.getTradeDateString()}</td>
                                        <td>${option.getExpirationDateString()}</td>
                                        <td>${option.getDaysLeft()}</td>
                                        @if(option.getQuantity() < 0)
                                            <td style="font-weight: bold;">${option.getQuantity()}</td>
                                        @else
                                            <td>${option.getQuantity()}</td>
                                        @endif

                                        <td class="currency-usd">${option.getPositionValue()}</td>
                                        <td class="currency-usd">${option.getMarketValue()}</td>
                                        <td class="currency-usd">${option.getTradePrice()}</td>
                                        <td class="currency-usd">${option.getMarketPrice()}</td>
                                        <td class="currency-usd">${option.getUnRealizedProfitOrLoss()}</td>
                                        <td>${option.getEarningDay()}</td>
                                        <td>${option.getType()}</td>
                                    </tr>
                                @endfor
                                </tbody>
                            </table>

                        </div>

                    </div>
                </div>
                <!-- End DataTable Open Positions -->

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
            var groupColumnOpen = 4; // Expiration date column (0-indexed)

            const tableOpen = new DataTable('#datatableOpenPositions', {
                pageLength: -1,
                perPageSelect: [5, 10, 15, ["All", -1]],
                columnDefs: [{target: 1, visible: false, searchable: false}], // code hidden
                order: [[groupColumnOpen, 'asc'], [2, 'asc']], // Expiration first, then Symbol
                drawCallback: function (settings) {
                    var api = this.api();
                    var rows = api.rows({ page: 'current' }).nodes();
                    var last = null;

                    api.column(groupColumnOpen, { page: 'current' })
                        .data()
                        .each(function (group, i) {
                            if (last !== group) {
                                $(rows).eq(i).before(
                                    '<tr class="group"><td colspan="14" style="font-weight:bold; background-color:#d7deea; color:#4154f1; padding:12px 8px; border-top:2px solid #4154f1; border-bottom:2px solid #4154f1;">' +
                                    group +
                                    '</td></tr>'
                                );
                                last = group;
                            }
                        });
                }
            });

            // Interactive group header click handler
            $('#datatableOpenPositions tbody').on('click', 'tr.group', function () {
                var currentOrder = tableOpen.order()[0];
                if (currentOrder[0] === groupColumnOpen && currentOrder[1] === 'asc') {
                    tableOpen.order([[groupColumnOpen, 'desc']]).draw();
                } else {
                    tableOpen.order([[groupColumnOpen, 'asc']]).draw();
                }
            });

            // Keep existing row click handler - exclude group rows
            $('#datatableOpenPositions tbody').on('click', 'tr:not(.group)', function() {
                const ticker = tableOpen.row(this).data()[2];
                console.log('GET  /getPosition/' + ticker);
                htmx.ajax('GET', '/getPosition/' + ticker, '#main');
            });

            tableOpen.rows().eq(0).each( function ( index ) {
                var row = tableOpen.row( index );

                const rowData = row.data();
                for (let i = 0; i < rowData.length; i++ ) {
                    if(i===8) {
                        let cell = rowData[i];
                        console.log(cell);
                    }
                }

                // ... do something with data(), or row.node(), etc
            } );
        }
        datatable();
        totalSeconds = 0;
    });
</script>