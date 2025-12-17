@import co.grtk.srcprofit.dto.OpenPositionViewDto

@import java.util.List
@param List<OpenPositionViewDto> openPositions

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
                    <h5 class="card-title">Open Positions <span></span></h5>

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
                                @if(position.pnl() != null && position.pnl() < 0)
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
        totalSeconds = 0;
    });
</script>
