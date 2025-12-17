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
                                    <th scope="col">R.O.I</th>
                                    <th scope="col">P.O.P</th>
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
                                        <td>${option.getAnnualizedRoiPercent()}%</td>
                                        <td>${option.getProbability()}%</td>
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
                columnDefs: [{target: 1, visible: false, searchable: false},{target: 4, visible: false, searchable: false}], // code hidden
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