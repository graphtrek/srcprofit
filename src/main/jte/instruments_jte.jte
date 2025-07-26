@import co.grtk.srcprofit.dto.InstrumentDto
@import java.util.List
@param List<InstrumentDto> instruments


<div class="pagetitle">
    <h1>Instruments</h1>
    <nav>
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="/">Home</a></li>
            <li class="breadcrumb-item active">Instruments</li>
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

                <!-- DataTable Instruments-->
                <div class="col-12">
                    <div class="card recent-sales overflow-auto">


                        <div class="card-body">
                            <h5 class="card-title">Instruments<span></span></h5>

                            <table id="datatableInstruments" class="table table-striped">
                                <thead>
                                <tr>
                                    <th scope="col">#</th>
                                    <th scope="col">Ticker</th>
                                    <th scope="col">Updated</th>
                                    <th scope="col">Price$</th>
                                    <th scope="col">Change$</th>
                                    <th scope="col">Percent</th>
                                    <th scope="col">Premium</th>
                                    <th scope="col">RealP&L</th>
                                    <th scope="col">UnRealP&L</th>
                                    <th scope="col">Earning</th>
                                    <th scope="col">EarningDay</th>
                                </tr>
                                </thead>
                                <tbody>
                                @for(var instrument : instruments )
                                    <tr style="cursor: pointer;" href="http://myspace.com">
                                        <td>${instrument.getId()}</td>

                                        @if(instrument.getChange()  != null && instrument.getChange() < 0)
                                            <td class="text-uppercase" style="background-color: orangered; color: white;">
                                                ${instrument.getTicker()}
                                            </td>
                                        @else
                                            <td class="text-uppercase" style="background-color: green; color: white;">
                                                ${instrument.getTicker()}
                                            </td>
                                        @endif

                                        <td>${instrument.getUpdatedTimeStr()}</td>
                                        <td>${instrument.getPrice()}</td>
                                        <td>${instrument.getChange()}</td>
                                        <td>${instrument.getChangePercent()}</td>
                                        <td>${instrument.getCollectedPremium()}</td>
                                        <td>${instrument.getRealizedProfitOrLoss()}</td>
                                        <td>${instrument.getUnRealizedProfitOrLoss()}</td>
                                        <td>${instrument.getEarningDateStr()}</td>
                                        <td>${instrument.getEarningDay()}</td>
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

            const tableOpen = new DataTable('#datatableInstruments', {
                perPageSelect: [5, 10, 15, ["All", -1]],
                pageLength: -1,
                order: [[1, 'asc']] // ticker
            });

            $('#datatableInstruments tbody').on('click', 'tr', function() {
                const ticker = tableOpen.row(this).data()[1]; // ticker
                console.log('clicked ticker: ' + ticker);
                htmx.ajax('GET', '/getPosition/'+ticker, '#main');
            });

            $('#datatableInstruments tbody').on( 'mouseenter', 'tr', function (e) {
                $(e.target).closest('tbody').children('tr').children('td').removeClass( 'highlight_row' );
                $(e.target).closest('tr').children('td:gt(1)').addClass( 'highlight_row' );
                //$( table.column( colIdx ).nodes() ).addClass( 'highlight' );
            });
        }

        datatable();
        totalSeconds = 0;
    });
</script>


