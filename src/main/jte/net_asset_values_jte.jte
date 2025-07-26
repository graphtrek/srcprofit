@import co.grtk.srcprofit.dto.NetAssetValueDto
@import java.util.List
@param List<NetAssetValueDto> netAssetValues


<div class="pagetitle">
    <h1>Net Asset Value</h1>
    <nav>
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="/">Home</a></li>
            <li class="breadcrumb-item active">Net Asset Value</li>
        </ol>
    </nav>
</div><!-- End Page Title -->

<section class="section dashboard">
    <div class="row">

        <!-- Left side columns -->
        <div class="col-lg-12">
            <div class="row">

                <%--
                                <div class="card">
                                    <div class="card-body">
                                        <h5 class="card-title">Rounded Buttons</h5>

                                        <button type="button" id="watchlist" class="btn btn-primary rounded-pill">Reload Predefined Watchlist</button>

                                        <button type="button" id="marketData" class="btn btn-info rounded-pill">Refres Market Data</button>

                                    </div>
                                </div>
                --%>

                <!-- DataTable -->
                <div class="col-12">
                    <div class="card recent-sales overflow-auto">


                        <div class="card-body">
                            <h5 class="card-title">Net Asset Value<span></span></h5>

                            <table id="datatable" class="table table-striped">
                                <thead>
                                <tr>
                                    <th scope="col">#</th>
                                    <th scope="col">Date</th>
                                    <th scope="col">Cash</th>
                                    <th scope="col">Stock</th>
                                    <th scope="col">Premium</th>
                                    <th scope="col">AvrgCash</th>
                                    <th scope="col">R.O.I</th>
                                    <th scope="col">Options</th>
                                    <th scope="col">Dividend Accruals</th>
                                    <th scope="col">Interest Accruals</th>
                                    <th scope="col">Total</th>
                                </tr>
                                </thead>
                                <tbody>
                                @for(var nav : netAssetValues )
                                    <tr style="cursor: pointer;">
                                        <td>${nav.getId()}</td>
                                        <td>${nav.getReportDateStr()}</td>
                                        <td class="currency-usd">${nav.getCash()}</td>
                                        <td class="currency-usd">${nav.getStock()}</td>
                                        <td class="currency-usd">${nav.getDailyPremium()}</td>
                                        <td class="currency-usd">${nav.getAverageCash()}</td>
                                        <td>${nav.getRoi()}</td>
                                        <td class="currency-usd">${nav.getOptions()}</td>
                                        <td class="currency-usd">${nav.getDividendAccruals()}</td>
                                        <td class="currency-usd">${nav.getInterestAccruals()}</td>
                                        <td class="currency-usd">${nav.getTotal()}</td>
                                    </tr>
                                @endfor
                                </tbody>
                            </table>

                        </div>

                    </div>
                </div>
                <!-- End DataTable Instruments -->

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

            const tableOpen = new DataTable('#datatable', {
                perPageSelect: [5, 10, 15, ["All", -1]],
                pageLength: -1,
                order: [[1, 'desc']] // ticker
            });

            $('#datatable tbody').on( 'mouseenter', 'tr', function (e) {
                $(e.target).closest('tbody').children('tr').children('td').removeClass( 'highlight_row' );
                $(e.target).closest('tr').children('td:gt(1)').addClass( 'highlight_row' );
                //$( table.column( colIdx ).nodes() ).addClass( 'highlight' );
            });
        }

        datatable();
        totalSeconds = 0;
    });
</script>


