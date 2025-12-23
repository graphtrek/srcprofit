@import co.grtk.srcprofit.dto.OpenPositionViewDto
@import co.grtk.srcprofit.dto.StockPositionViewDto

@import java.util.List
@param List<OpenPositionViewDto> openPositions
@param List<StockPositionViewDto> stockPositions

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
                    <h5 class="card-title">Options <span></span></h5>

                    <table id="datatableOpenPositions" class="table table-striped">
                        <thead>
                        <tr>
                            <th scope="col">ID</th>
                            <th scope="col">Symbol</th>
                            <th scope="col">TradeDate</th>
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
                                <td>${position.tradeDate().toString()}</td>
                                <td>${position.expirationDate().toString()}</td>
                                <td>${position.daysLeft()}</td>
                                <td>${position.qty()}</td>
                                <td class="currency-usd">${position.strikePrice()}</td>
                                <td class="currency-usd">${position.underlyingPrice()}</td>
                                <td>${position.pop()}%</td>
                                <td class="currency-usd">${position.costBasisMoney()}</td>
                                <td class="currency-usd">${position.calculatedPnl()}</td>
                                <td>${position.roi()}%</td>
                                <td>${position.type()}</td>
                            </tr>
                        @endfor
                        </tbody>
                        <tfoot>
                        <tr>
                            <th></th>
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
                    <h5 class="card-title">Stocks</h5>

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

        $('.currency-usd').each(function () {
            var monetary_value = $(this).text();
            var i = new Intl.NumberFormat('en-US', {
                style: 'currency',
                currency: 'USD',
                maximumFractionDigits: 2
            }).format(monetary_value);
            $(this).text(i);
        });

        function datatable() {
            console.log('datatable init');
            var groupColumnOpen = 3; // Expiration date column (0-indexed)

            const tableOpen = new DataTable('#datatableOpenPositions', {
                pageLength: -1,
                perPageSelect: [5, 10, 15, ["All", -1]],
                columnDefs: [{target: 3, visible: false, searchable: false}], // expiration hidden for grouping
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

                    // Sum Qty (column 5) - only visible rows
                    var qtyTotal = api.column(5, {page: 'current', search: 'applied'})
                        .data()
                        .reduce(function (a, b) {
                            return floatVal(a) + floatVal(b);
                        }, 0);

                    // Average Strike (column 6) - only visible rows
                    var strikeData = api.column(6, {page: 'current', search: 'applied'}).data();
                    var strikeSum = 0;
                    var strikeCount = 0;
                    strikeData.each(function(value) {
                        strikeSum += floatVal(value);
                        strikeCount++;
                    });
                    var strikeAvg = strikeCount > 0 ? strikeSum / strikeCount : 0;

                    // Average Price (column 7) - only visible rows
                    var priceData = api.column(7, {page: 'current', search: 'applied'}).data();
                    var priceSum = 0;
                    var priceCount = 0;
                    priceData.each(function(value) {
                        priceSum += floatVal(value);
                        priceCount++;
                    });
                    var priceAvg = priceCount > 0 ? priceSum / priceCount : 0;

                    // Sum CostBase (column 9) - only visible rows
                    var costBaseTotal = api.column(9, {page: 'current', search: 'applied'})
                        .data()
                        .reduce(function (a, b) {
                            return floatVal(a) + floatVal(b);
                        }, 0);

                    // Sum P&L (column 10) - only visible rows
                    var pnlTotal = api.column(10, {page: 'current', search: 'applied'})
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
                    $(api.column(5).footer()).html('<strong>' + qtyTotal + '</strong>');
                    $(api.column(6).footer()).html('<strong>' + formatCurrency(strikeAvg) + '</strong>');
                    $(api.column(7).footer()).html('<strong>' + formatCurrency(priceAvg) + '</strong>');
                    $(api.column(9).footer()).html('<strong>' + formatCurrencyInt(costBaseTotal) + '</strong>');
                    $(api.column(10).footer()).html('<strong>' + formatCurrencyInt(pnlTotal) + '</strong>');
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
                                    '<tr class="group"><td colspan="12" style="font-weight:bold; background-color:#d7deea; color:#4154f1; padding:12px 8px; border-top:2px solid #4154f1; border-bottom:2px solid #4154f1;">' +
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
