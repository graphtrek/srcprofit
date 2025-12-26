@import co.grtk.srcprofit.dto.OpenPositionViewDto
@import co.grtk.srcprofit.dto.StockPositionViewDto
@import co.grtk.srcprofit.dto.PositionDto
@import java.util.List
@import java.time.LocalDate

@param List<OpenPositionViewDto> openPositions
@param List<StockPositionViewDto> stockPositions
@param LocalDate reportDate
@param co.grtk.srcprofit.dto.PositionDto positionDto
@param List<PositionDto> weeklyOpenPositions
<div class="pagetitle">
    <h1>Open Positions</h1>
    <nav>
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="/">Home</a></li>
            <li class="breadcrumb-item active">Open Positions</li>
        </ol>
    </nav>
</div><!-- End Page Title -->

<section class="section dashboard">
    <div class="row">
        <div class="col-lg-12">
            <div class="row">

                <!-- Summary Cards Section -->
                <div class="col-12">
                    <div class="row">

                        <!-- Buy Obligation Market Value Card -->
                        <div class="col-xxl-4 col-md-6">
                            <div class="card info-card sales-card">
                                <div class="card-body">
                                    <h5 class="card-title">Buy Obligation Market Value</h5>

                                    <div class="d-flex align-items-center">
                                        <div class="card-icon rounded-circle d-flex align-items-center justify-content-center">
                                            <i class="bi bi-inbox"></i>
                                        </div>
                                        <div class="ps-3">
                                            <h6 class="currency-usd-int">${positionDto.getMarketValue()}</h6>
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
                                            <i class="bi bi-bookmark-check"></i>
                                        </div>
                                        <div class="ps-3">
                                            <h6 class="currency-usd-int">${positionDto.getPositionValue()}</h6>
                                            <span class="text-success small pt-1 fw-bold currency-usd-int">${positionDto.getCash()}</span>
                                            <span class="text-muted small pt-2 ps-1">Cash</span>

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
                                            <h6 class="currency-usd-int">${positionDto.getPut()}</h6>
                                            @if(positionDto.getPut() < positionDto.getPutMarketPrice())
                                                <span class="text-danger small pt-1 fw-bold currency-usd-int">${positionDto.getPutMarketPrice()}</span>
                                            @else
                                                <span class="text-success small pt-1 fw-bold currency-usd-int">${positionDto.getPutMarketPrice()}</span>
                                            @endif
                                            <span class="text-muted small pt-2 ps-1">Market Value</span>
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
                                            <i class="bi bi-arrow-bar-up"></i>
                                        </div>
                                        <div class="ps-3">
                                            <h6 class="currency-usd-int">${positionDto.getCallObligationMarketValue()}</h6>
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
                                            <i class="bi bi-hand-thumbs-up"></i>
                                        </div>
                                        <div class="ps-3">
                                            <h6 class="currency-usd-int">${positionDto.getCallObligationValue()}</h6>
                                            <span class="text-success small pt-1 fw-bold currency-usd-int">${positionDto.getStock()}</span>
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
                                            <h6 class="currency-usd-int">${positionDto.getCall()}</h6>
                                            @if(positionDto.getCall() > positionDto.getCallMarketPrice())
                                                <span class="text-success small pt-1 fw-bold currency-usd-int">${positionDto.getCallMarketPrice()}</span>
                                            @else
                                                <span class="text-danger small pt-1 fw-bold currency-usd-int">${positionDto.getCallMarketPrice()}</span>
                                            @endif
                                            <span class="text-muted small pt-2 ps-1">Market Value</span>
                                        </div>
                                    </div>
                                </div>

                            </div>
                        </div>
                        <!-- End CALL Premium Card -->


                    </div>
                </div>
                <!-- End Summary Cards Section -->

                <!-- Weekly Options Summary Table -->
                <div class="col-12">
                    <div class="card recent-sales overflow-auto">
                        <div class="card-body">
                            <h5 class="card-title">Weekly Options Summary <span>| Report Date: ${reportDate.toString()}</span></h5>
                            <table class="table table-borderless datatable" id="datatableWeeklyOpenPositions">
                                <thead>
                                <tr>
                                    <th scope="col">Expiration</th>
                                    <th scope="col">DaysLeft</th>
                                    <th scope="col">Value</th>
                                    <th scope="col">MrktValue</th>
                                    <th scope="col">Price</th>
                                    <th scope="col">MrktPrice</th>
                                    <th scope="col">P&L</th>
                                    <th scope="col">R.O.I</th>
                                    <th scope="col">P.O.P</th>
                                </tr>
                                </thead>
                                <tbody>
                                @for(var option : weeklyOpenPositions )
                                    <tr style="cursor: pointer;">
                                        <td>${option.getExpirationDateString()}</td>
                                        <td>${option.getDaysLeft()}</td>
                                        <td class="currency-usd-int">${option.getPositionValue()}</td>
                                        <td class="currency-usd-int">${option.getMarketValue()}</td>
                                        <td class="currency-usd-int">${option.getTradePrice()}</td>
                                        <td class="currency-usd-int">${option.getMarketPrice()}</td>
                                        <td class="currency-usd-int">${option.getUnRealizedProfitOrLoss()}</td>
                                        <td>${option.getAnnualizedRoiPercent()}%</td>
                                        <td>${option.getProbability()}%</td>
                                    </tr>
                                @endfor
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
                <!-- Weekly Options Summary Table -->

            </div>
        </div>
    </div>
</section>

<section class="section dashboard">
    <div class="row">
        <div class="col-lg-12">
            <div class="card recent-sales overflow-auto">

                <div class="filter">
                    <a class="icon" href="#" data-bs-toggle="dropdown"><i class="bi bi-three-dots"></i></a>
                    <ul class="dropdown-menu dropdown-menu-end dropdown-menu-arrow">
                        <li class="dropdown-header text-start">
                            <h6>Filter</h6>
                        </li>
                        <li><a class="dropdown-item" href="#">All</a></li>
                    </ul>
                </div>

                <div class="card-body">
                    <h5 class="card-title">Options <span>| Report Date: ${reportDate.toString()}</span></h5>

                    <table id="datatableOpenPositions" class="table table-striped">
                        <thead>
                        <tr>
                            <th scope="col">ID</th>
                            <th scope="col">Symbol</th>
                            <th scope="col">Expiration</th>
                            <th scope="col">D.Left</th>
                            <th scope="col">Qty</th>
                            <th scope="col">Strike</th>
                            <th scope="col">Price</th>
                            <th scope="col">P.O.P</th>
                            <th scope="col">CostBase</th>
                            <th scope="col">P&L</th>
                            <th scope="col">R.O.I</th>
                            <th scope="col">Type</th>
                        </tr>
                        </thead>
                        <tbody>
                        @for(var position : openPositions)
                            <tr style="cursor: pointer;">
                                @if(position.calculatedPnl() != null && position.calculatedPnl() < 0)
                                    <td style="background-color:rgb(220 53 69)">${position.id()}</td>
                                @else
                                    <td>${position.id()}</td>
                                @endif
                                <td class="text-uppercase">${position.symbol()}</td>
                                <td>${position.expirationDate().toString()}</td>
                                <td>${position.daysLeft()}</td>
                                <td>${position.qty()}</td>
                                <td class="currency-usd">${position.strikePrice()}</td>
                                <td class="currency-usd">${position.underlyingPrice()}</td>
                                <td>${position.pop()}%</td>
                                <td class="currency-usd">${position.costBasisMoney()}</td>
                                <td class="currency-usd">${position.calculatedPnl()}</td>
                                <td>${position.roi()}%</td>
                                @if(position.qty() < 0)
                                    <td>${position.type()} SELL</td>
                                @else
                                    <td>${position.type()} BUY</td>
                                @endif
                            </tr>
                        @endfor
                        </tbody>
                        <tfoot>
                        <tr>
                            <th></th>
                            <th></th>
                            <th></th>
                            <th class="text-end">Total:</th>
                            <th></th>
                            <th></th>
                            <th></th>
                            <th></th>
                            <th></th>
                            <th></th>
                            <th></th>
                            <th></th>
                        </tr>
                        </tfoot>
                    </table>

                </div>

            </div>
        </div>
    </div>
</section>

<!-- Stocks DataTable Section -->
<section class="section dashboard">
    <div class="row">
        <div class="col-lg-12">
            <div class="card recent-sales overflow-auto">
                <div class="card-body">
                    <h5 class="card-title">Stocks <span>| Report Date: ${reportDate.toString()}</span></h5>

                    <table id="datatableStockPositions" class="table table-striped">
                        <thead>
                        <tr>
                            <th scope="col">ID</th>
                            <th scope="col">Symbol</th>
                            <th scope="col">Qty</th>
                            <th scope="col">CostBasis</th>
                            <th scope="col">Price</th>
                            <th scope="col">Value</th>
                            <th scope="col">P&L</th>
                            <th scope="col">% NAV</th>
                        </tr>
                        </thead>
                        <tbody>
                        @for(var stock : stockPositions)
                            <tr style="cursor: pointer;">
                                @if(stock.pnl() != null && stock.pnl() < 0)
                                    <td style="background-color:rgb(220 53 69)">${stock.id()}</td>
                                @else
                                    <td>${stock.id()}</td>
                                @endif
                                <td class="text-uppercase">${stock.symbol()}</td>
                                <td>${stock.quantity()}</td>
                                <td class="currency-usd">${stock.costBasisMoney()}</td>
                                <td class="currency-usd">${stock.markPrice()}</td>
                                <td class="currency-usd">${stock.positionValue()}</td>
                                <td class="currency-usd">${stock.pnl()}</td>
                                <td>${stock.percentOfNAV() != null ? String.format("%.2f%%", stock.percentOfNAV()) : ""}</td>
                            </tr>
                        @endfor
                        </tbody>
                    </table>

                </div>

            </div>
        </div>
    </div>
</section>

<script>
    $( document ).ready(function() {
        console.log( "ready!" );

        // Create formatters once and reuse them for better performance
        const currencyFormatter2 = new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD',
            maximumFractionDigits: 2
        });

        const currencyFormatter0 = new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD',
            maximumFractionDigits: 0
        });

        $('.currency-usd').each(function () {
            $(this).text(currencyFormatter2.format($(this).text()));
        });

        $('.currency-usd-int').each(function () {
            $(this).text(currencyFormatter0.format($(this).text()));
        });

        function datatable() {
            console.log('datatable init');
            var groupColumnOpen = 2; // Expiration date column (0-indexed)

            const tableOpen = new DataTable('#datatableOpenPositions', {
                pageLength: -1,
                perPageSelect: [5, 10, 15, ["All", -1]],
                columnDefs: [{target: 2, visible: false, searchable: false}], // expiration hidden for grouping
                order: [[groupColumnOpen, 'asc'], [1, 'asc']], // Expiration first, then Symbol
                footerCallback: function (row, data, start, end, display) {
                    var api = this.api();

                    // Helper function to parse numeric values
                    var floatVal = function (i) {
                        if (typeof i === 'string') {
                            // Remove currency symbols, commas, percentage signs, and convert to float
                            return parseFloat(i.replace(/[$,%]/g, '')) || 0;
                        }
                        return typeof i === 'number' ? i : 0;
                    };

                    // Sum Qty (column 4) - only visible rows
                    var qtyTotal = api.column(4, {page: 'current', search: 'applied'})
                        .data()
                        .reduce(function (a, b) {
                            return floatVal(a) + floatVal(b);
                        }, 0);

                    // Sum Strike × Qty (column 5 × column 4) - only visible rows
                    var rows = api.rows({page: 'current', search: 'applied'}).data();
                    var strikeSum = 0;
                    rows.each(function(row) {
                        var qty = floatVal(row[4]);
                        var strike = floatVal(row[5]);
                        strikeSum += qty * strike;
                    });

                    // Sum Price × Qty (column 6 × column 4) - only visible rows
                    var priceSum = 0;
                    rows.each(function(row) {
                        var qty = floatVal(row[4]);
                        var price = floatVal(row[6]);
                        priceSum += qty * price;
                    });

                    // Sum CostBase (column 8) - only visible rows
                    var costBaseTotal = api.column(8, {page: 'current', search: 'applied'})
                        .data()
                        .reduce(function (a, b) {
                            return floatVal(a) + floatVal(b);
                        }, 0);

                    // Sum P&L (column 9) - only visible rows
                    var pnlTotal = api.column(9, {page: 'current', search: 'applied'})
                        .data()
                        .reduce(function (a, b) {
                            return floatVal(a) + floatVal(b);
                        }, 0);

                    // Format currency values with 2 decimals (for Strike and Price)
                    var formatCurrency = function(value) {
                        return new Intl.NumberFormat('en-US', {
                            style: 'currency',
                            currency: 'USD',
                            maximumFractionDigits: 2
                        }).format(value);
                    };

                    // Format currency values as integers (for CostBase and P&L)
                    var formatCurrencyInt = function(value) {
                        return new Intl.NumberFormat('en-US', {
                            style: 'currency',
                            currency: 'USD',
                            maximumFractionDigits: 0
                        }).format(value);
                    };

                    // Update footer cells
                    $(api.column(4).footer()).html('<strong>' + qtyTotal + '</strong>');
                    $(api.column(5).footer()).html('<strong>' + formatCurrency(strikeSum) + '</strong>');
                    $(api.column(6).footer()).html('<strong>' + formatCurrency(priceSum) + '</strong>');
                    $(api.column(8).footer()).html('<strong>' + formatCurrencyInt(costBaseTotal) + '</strong>');
                    $(api.column(9).footer()).html('<strong>' + formatCurrencyInt(pnlTotal) + '</strong>');
                },
                drawCallback: function (settings) {
                    var api = this.api();
                    var rows = api.rows({ page: 'current' }).nodes();
                    var last = null;

                    api.column(groupColumnOpen, { page: 'current' })
                        .data()
                        .each(function (group, i) {
                            if (last !== group) {
                                $(rows).eq(i).before(
                                    '<tr class="group"><td colspan="11" style="font-weight:bold; background-color:#d7deea; color:#4154f1; padding:12px 8px; border-top:2px solid #4154f1; border-bottom:2px solid #4154f1;">' +
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

            // Row click handler to navigate to position details - exclude group rows
            $('#datatableOpenPositions tbody').on('click', 'tr:not(.group)', function() {
                const symbol = tableOpen.row(this).data()[1];
                console.log('GET  /getPosition/' + symbol);
                htmx.ajax('GET', '/getPosition/' + symbol, '#main');
            });
        }
        datatable();

        // Initialize Stocks DataTable
        const tableStocks = new DataTable('#datatableStockPositions', {
            pageLength: -1,
            perPageSelect: [5, 10, 15, ["All", -1]],
            order: [[1, 'asc']]  // Sort by Symbol (column 1)
        });

        // Row click handler for stocks
        $('#datatableStockPositions tbody').on('click', 'tr', function() {
            const symbol = tableStocks.row(this).data()[1];
            console.log('GET  /getPosition/' + symbol);
            htmx.ajax('GET', '/getPosition/' + symbol, '#main');
        });

        totalSeconds = 0;
    });
</script>
