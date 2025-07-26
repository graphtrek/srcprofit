@import co.grtk.srcprofit.dto.EarningDto
@import java.util.List
@param List<EarningDto> earnings


<div class="pagetitle">
    <h1>Earnings</h1>
    <nav>
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="/">Home</a></li>
            <li class="breadcrumb-item active">Earnings</li>
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
                            <h5 class="card-title">Earnings<span></span></h5>

                            <table id="datatable" class="table table-striped">
                                <thead>
                                <tr>
                                    <th scope="col">#</th>
                                    <th scope="col">Symbol</th>
                                    <th scope="col">ReportDate</th>
                                    <th scope="col">EarningDay</th>
                                    <th scope="col">Estimate</th>
                                    <th scope="col">Name</th>
                                </tr>
                                </thead>
                                <tbody>
                                @for(var earningDate : earnings )
                                    <tr style="cursor: pointer;">
                                        <td>${earningDate.getId()}</td>
                                        <td>
                                            <a href="https://www.tradingview.com/chart/?symbol=${earningDate.getSymbol()}" target="_blank">${earningDate.getSymbol()}</a>
                                        </td>
                                        <td>${earningDate.getReportDateStr()}</td>
                                        <td>${earningDate.getEarningDay()}</td>
                                        <td>${earningDate.getEstimate()}</td>
                                        <td>${earningDate.getName()}</td>
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


        function datatable() {
            console.log('datatable init');

            const tableOpen = new DataTable('#datatable', {
                perPageSelect: [5, 10, 15, ["All", -1]],
                pageLength: -1,
                order: [[3, 'asc']] // ticker
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


