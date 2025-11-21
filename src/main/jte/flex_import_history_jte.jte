@import co.grtk.srcprofit.dto.FlexImportHistoryDto
@import java.util.List
@param List<FlexImportHistoryDto> flexImports


<div class="pagetitle">
    <h1>FLEX Import History</h1>
    <nav>
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="/">Home</a></li>
            <li class="breadcrumb-item active">FLEX Import History</li>
        </ol>
    </nav>
</div><!-- End Page Title -->

<section class="section dashboard">
    <div class="row">

        <!-- Left side columns -->
        <div class="col-lg-12">
            <div class="row">

                <!-- DataTable -->
                <div class="col-12">
                    <div class="card recent-sales overflow-auto">

                        <div class="card-body">
                            <h5 class="card-title">Import History<span></span></h5>

                            <table id="datatable" class="table table-striped">
                                <thead>
                                <tr>
                                    <th scope="col">#</th>
                                    <th scope="col">Reference Code</th>
                                    <th scope="col">Report Type</th>
                                    <th scope="col">Status</th>
                                    <th scope="col">Updated At</th>
                                    <th scope="col">Successful</th>
                                    <th scope="col">Failed</th>
                                    <th scope="col">Skipped</th>
                                    <th scope="col">Data Fix</th>
                                </tr>
                                </thead>
                                <tbody>
                                @for(var flex : flexImports)
                                    <tr style="cursor: pointer;">
                                        <td>${flex.getId()}</td>
                                        <td>${flex.getReferenceCode()}</td>
                                        <td>${flex.getReportType()}</td>
                                        <td>${flex.getStatus()}</td>
                                        <td>${flex.getUpdatedAtStr()}</td>
                                        <td>${flex.getCsvRecordsCountSafe()}</td>
                                        <td>${flex.getCsvFailedRecordsCountSafe()}</td>
                                        <td>${flex.getCsvSkippedRecordsCountSafe()}</td>
                                        <td>${flex.getDataFixRecordsCountSafe()}</td>
                                    </tr>
                                @endfor
                                </tbody>
                            </table>

                        </div>

                    </div>
                </div>
                <!-- End DataTable -->

            </div>
        </div><!-- End Left side columns -->

    </div>
</section>

<script>
    $( document ).ready(function() {
        console.log( "FLEX Import History page ready!" );

        function datatable() {
            console.log('datatable init');

            const table = new DataTable('#datatable', {
                perPageSelect: [5, 10, 15, ["All", -1]],
                pageLength: -1,
                order: [[4, 'desc']] // Updated At column, descending (most recent first)
            });

            $('#datatable tbody').on( 'mouseenter', 'tr', function (e) {
                $(e.target).closest('tbody').children('tr').children('td').removeClass( 'highlight_row' );
                $(e.target).closest('tr').children('td:gt(1)').addClass( 'highlight_row' );
            });
        }

        datatable();
    });
</script>
