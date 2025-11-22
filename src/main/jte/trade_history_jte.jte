@import co.grtk.srcprofit.dto.PositionDto
@import co.grtk.srcprofit.entity.OptionStatus

@import java.util.List
@param List<PositionDto> closedPositions

<div class="pagetitle">
    <h1>Trade History</h1>
    <nav>
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="/">Home</a></li>
            <li class="breadcrumb-item active">Trade History</li>
        </ol>
    </nav>
</div><!-- End Page Title -->

<section class="section dashboard">
    <div class="row">

        <!-- Left side columns -->
        <div class="col-lg-12">
            <div class="row">

                <!-- DataTable Closed Positions-->
                <div class="col-12">
                    <div class="card recent-sales overflow-auto">

                        <div class="filter">
                            <a class="icon" href="#" data-bs-toggle="dropdown"><i class="bi bi-three-dots"></i></a>
                            <ul class="dropdown-menu dropdown-menu-end dropdown-menu-arrow" id="tradeHistoryFilter">
                                <li class="dropdown-header text-start">
                                    <h6>Filter</h6>
                                </li>

                                <li><a class="dropdown-item" href="#" data-filter="week">This Week</a></li>
                                <li><a class="dropdown-item" href="#" data-filter="month">This Month</a></li>
                                <li><a class="dropdown-item" href="#" data-filter="year">This Year</a></li>
                                <li><a class="dropdown-item" href="#" data-filter="all">All</a></li>
                            </ul>
                        </div>

                        <div class="card-body">
                            <h5 class="card-title">Closed Positions <span></span></h5>

                            <table id="datatableTradeHistory" class="table table-striped">
                                <thead>
                                <tr>
                                    <th scope="col">#</th>
                                    <th scope="col">Code</th>
                                    <th scope="col">Symbol</th>
                                    <th scope="col">TradeDate</th>
                                    <th scope="col">Expiration</th>
                                    <th scope="col">DaysLeft</th>
                                    <th scope="col">Qty</th>
                                    <th scope="col">Value</th>
                                    <th scope="col">MrktValue</th>
                                    <th scope="col">Price</th>
                                    <th scope="col">P&L</th>
                                    <th scope="col">Type</th>
                                    <th scope="col">Status</th>
                                </tr>
                                </thead>
                                <tbody>
                                @for(var option : closedPositions )
                                    <tr style="cursor: pointer;">
                                        <td style="color:${option.getColor()};">${option.getId()}</td>
                                        <td style="color:${option.getColor()};text-wrap: nowrap;">${option.getCode()}</td>
                                        <td class="text-uppercase">${option.getTicker()}</td>
                                        <td>${option.getTradeDateString()}</td>
                                        <td>${option.getExpirationDateString()}</td>
                                        <td>${option.getDaysLeft()}</td>
                                        <td>${option.getQuantity()}</td>
                                        <td class="currency-usd">${option.getPositionValue()}</td>
                                        <td class="currency-usd">${option.getMarketValue()}</td>
                                        <td class="currency-usd">${option.getTradePrice()}</td>

                                        <td>${option.getRealizedProfitOrLoss()}</td>
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
                <!-- End DataTable Closed Positions -->


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
            var groupColumnHistory = 2; // Symbol column (0-indexed)

            const tableHistory = new DataTable('#datatableTradeHistory', {
                pageLength: 500,
                perPageSelect: [5, 10, 15, ["All", -1]],
                columnDefs: [{target: 1, visible: false, searchable: false}], // code hidden
                order: [[3, 'desc'], [groupColumnHistory, 'asc']], // TradeDate desc, Symbol asc
                drawCallback: function (settings) {
                    var api = this.api();
                    var rows = api.rows({ page: 'current' }).nodes();
                    var last = null;

                    api.column(groupColumnHistory, { page: 'current' })
                        .data()
                        .each(function (group, i) {
                            if (last !== group) {
                                $(rows).eq(i).before(
                                    '<tr class="group"><td colspan="13" style="font-weight:bold; background-color:#d7deea; color:#4154f1; padding:12px 8px; border-top:2px solid #4154f1; border-bottom:2px solid #4154f1;">' +
                                    group +
                                    '</td></tr>'
                                );
                                last = group;
                            }
                        });
                }
            });

            // Interactive group header click handler
            $('#datatableTradeHistory tbody').on('click', 'tr.group', function () {
                var currentOrder = tableHistory.order()[0];
                if (currentOrder[0] === groupColumnHistory && currentOrder[1] === 'asc') {
                    tableHistory.order([[groupColumnHistory, 'desc']]).draw();
                } else {
                    tableHistory.order([[groupColumnHistory, 'asc']]).draw();
                }
            });

            // Keep existing row click handler - exclude group rows
            $('#datatableTradeHistory tbody').on('click', 'tr:not(.group)', function() {
                const ticker = tableHistory.row(this).data()[2];
                console.log('GET  /getPosition/' + ticker);
                htmx.ajax('GET', '/getPosition/' + ticker, '#main');
            });
        }
        datatable();

        // Filter dropdown handler for Trade History
        $('#tradeHistoryFilter').on('click', '.dropdown-item', function(e) {
            e.preventDefault();
            const filter = $(this).data('filter');
            let startDate;
            const today = new Date();

            switch(filter) {
                case 'week':
                    startDate = new Date(today);
                    startDate.setDate(today.getDate() - 7);
                    break;
                case 'month':
                    startDate = new Date(today);
                    startDate.setMonth(today.getMonth() - 1);
                    break;
                case 'year':
                    startDate = new Date(today);
                    startDate.setFullYear(today.getFullYear() - 1);
                    break;
                case 'all':
                    // Load all without date filter
                    htmx.ajax('GET', '/tradehistory', {
                        target: '#main',
                        swap: 'innerHTML'
                    });
                    return;
            }

            if (startDate) {
                htmx.ajax('POST', '/tradehistoryFromDate', {
                    target: '#main',
                    swap: 'innerHTML',
                    values: { tradeDate: startDate.toISOString().split('T')[0] }
                });
            }
        });

        totalSeconds = 0;
    });
</script>
