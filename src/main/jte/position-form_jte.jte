@import co.grtk.srcprofit.entity.OptionType
@import co.grtk.srcprofit.entity.OptionStatus
@import co.grtk.srcprofit.dto.PositionDto
@import java.util.List

@param co.grtk.srcprofit.dto.PositionDto positionDto
@param String success
@param String error
@param List<PositionDto> optionHistory
@param List<PositionDto> openOptions
@param PositionDto virtualPosition

<div class="pagetitle">
    <h1>Position Calculator</h1>
    <nav>
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="/">Home</a></li>
            <li class="breadcrumb-item active">Position Calculator</li>
        </ol>
    </nav>
</div><!-- End Page Title -->

<section class="section">
    <!-- Calculate Position Card Start -->
    <div class="row">
        <div class="col-lg-12">
            <div class="card">
                <div class="card-body">
                    @if(positionDto.getId() != null)
                    <h5 class="card-title">Position Calculator #${positionDto.getId()}</h5>
                    @else
                        <h5 class="card-title">Position Calculator</h5>
                    @endif

                    @if(success != null)
                    <div id="success" class="alert alert-success alert-dismissible fade show" role="alert">
                        <i class="bi bi-check-circle me-1"></i>
                        A simple success alert with icon—check it out!
                        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
                    </div>
                    @elseif(error != null)
                    <div id="error" class="alert alert-danger alert-dismissible fade show" role="alert">
                        ${error}
                        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
                    </div>
                    @endif

                    <!-- Calculate Position Form Start -->
                    <form class="row g-3" hx-validate="true"
                          hx-post="/calculatePosition"
                          hx-trigger="submit"
                          hx-target="#main"
                          hx-swap="innerHTML"
                          hx-scroll="false"
                          hx-on::afterRequest="console.log('calculatePosition');">
                        <input hidden name="id" id="id" type="number" value="${positionDto.getId()}" />
                        <input hidden name="parentId" id="parentId" type="number" value="${positionDto.getParentId()}" />

                        <div class="col-md-2">
                            <label for="ticker" class="form-label">Ticker *</label>
                            <input id="ticker" name="ticker" required type="text" class="form-control text-uppercase" value="${positionDto.getTicker()}">
                        </div>
                        <div class="col-md-2">
                            <label for="tradeDate" class="form-label">Trade Date *</label>
                            <input id="tradeDate" name="startDate" type="date" required class="form-control" value="${positionDto.getTradeDateString()}">
                        </div>
                        <div class="col-md-2">
                            <label for="expirationDate" class="form-label">Expiration Date *</label>
                            <input id="expirationDate" name="expirationDate" type="date" required class="form-control" value="${positionDto.getExpirationDateString()}">
                        </div>
                        <div class="col-md-2">
                            <label for="tradePrice" class="form-label">Trade Price</label>
                            <input id="tradePrice" name="tradePrice" type="number" class="form-control" step="0.01" value="${positionDto.getTradePrice()}">
                        </div>
                        <div class="col-md-2">
                            <label for="positionValue" class="form-label">Position Value *</label>
                            <input id="positionValue" name="positionValue" type="number" required class="form-control" step="0.01" value="${positionDto.getPositionValue()}">
                        </div>
                        <div class="col-md-2">
                            <label for="marketValue" class="form-label">Market Value *</label>
                            <input id="marketValue" name="marketValue" type="number" required class="form-control" value="${positionDto.getMarketValue()}">
                        </div>


                        <div class="col-md-1">
                            <label for="daysLeft" class="form-label">Days Left</label>
                            <input  id="daysLeft" type="number" disabled class="form-control" value="${positionDto.getDaysLeft()}" >
                        </div>
                        <div class="col-md-1">
                            <label for="daysBetween" class="form-label">Days diff</label>
                            <input id="daysBetween" type="number" disabled class="form-control" value="${positionDto.getDaysBetween()}">
                        </div>
                        <div class="col-md-1">
                            <label for="roi" class="form-label">R.O.I %</label>
                            <input id="roi" type="number" disabled class="form-control" value="${positionDto.getAnnualizedRoiPercent()}">
                        </div>
                        <div class="col-md-1">
                            <label for="probability" class="form-label">P.O.P %</label>
                            <input id="probability" type="number" disabled class="form-control" value="${positionDto.getProbability()}">
                        </div>

                        <div class="col-md-2">
                            <label for="unRealizedProfitOrLoss" class="form-label">UnRealized P&L</label>
                            <input id="unRealizedProfitOrLoss" name="unRealizedProfitOrLoss" type="number" class="form-control" step="0.01" value="${positionDto.getUnRealizedProfitOrLoss()}" readonly>
                        </div>
                        <div class="col-md-2">
                            <label for="realizedProfitOrLoss" class="form-label">Realized P&L</label>
                            <input id="realizedProfitOrLoss" name="realizedProfitOrLoss" type="number" class="form-control" step="0.01" value="${positionDto.getRealizedProfitOrLoss()}" readonly>
                        </div>
                        <div class="col-md-2">
                            <label for="breakEven" class="form-label">Market Price</label>
                            <input id="breakEven" name="breakEven" type="number" class="form-control" step="0.01" value="${positionDto.getMarketPrice()}" readonly>
                        </div>
                        <div class="col-md-1">
                            <label for="profitOrLoss" class="form-label">P&L</label>
                            <input id="profitOrLoss" name="profitOrLoss" type="number" class="form-control" value="${positionDto.getProfitOrLoss()}" readonly>
                        </div>
                        <div class="col-md-1">
                            <label for="earning" class="form-label">Earning</label>
                            <input id="earning" name="earning" type="number" disabled class="form-control" value="${positionDto.getEarningDay()}">
                        </div>

                        <div class="text-center">
                            <button class="btn btn-primary">Calculate</button>
                            <a id="tradingViewButton" href="https://www.tradingview.com/chart/?symbol=VIX" class="btn btn-info" role="button" target="_blank">TradingView</a>
                            <a id="perplexityButton" href="https://www.perplexity.ai/finance/" class="btn btn-secondary" role="button" target="_blank">Perplexity</a>
                            <button id="chartButton" type="button" class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#ExtralargeModal">
                                Chart
                            </button>
                        </div>
                    </form>
                    <!-- Calculate Position Form End-->
                </div>
            </div>
        </div>
    </div>
    <!-- Calculate Position Card End -->

    <!-- Daily Snapshot Extra Large Modal Start -->
    <div class="modal fade" id="ExtralargeModal" tabindex="-1">
        <div class="modal-dialog modal-xl">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Daily Snapshot</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body" style="min-height: 470px;">
                    <div id="tradingview_advanced_chart"
                         data-tradingview-symbol="AAPL"
                         data-use-tradingview="true"
                         data-widget-type="advanced"
                         style="width: 100%; height: 450px;">
                    </div>
                    <!-- Fallback for feature flag disabled -->
                    <img id="chartImg" style="width: 100%; display: none;" src="#" />
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                </div>
            </div>
        </div>
    </div>
    <!-- Daily Snapshot Extra Large Modal End -->

    <!-- What-If Scenario Section (Virtual Position) Start -->
    @if(virtualPosition != null)
    <div class="row">
        <div class="col-lg-12">
            <div class="card" style="border-left: 4px solid #ffc107; background-color: #fffbf0;">
                <div class="card-body">
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                        <h5 class="card-title">
                            <i class="bi bi-lightning-fill" style="color: #ffc107;"></i>
                            What-If Scenario
                            <span class="badge bg-warning">VIRTUAL</span>
                        </h5>
                        <form hx-post="/clearVirtualPosition" hx-target="#main" hx-swap="innerHTML" style="display: inline;">
                            <button type="submit" class="btn btn-sm btn-outline-warning">
                                <i class="bi bi-trash"></i> Clear Scenario
                            </button>
                        </form>
                    </div>

                    <table class="table table-sm" style="margin-top: 15px;">
                        <thead>
                        <tr style="background-color: #fff9e6;">
                            <th scope="col">Code</th>
                            <th scope="col">Symbol</th>
                            <th scope="col">Type</th>
                            <th scope="col">Trade Date</th>
                            <th scope="col">Expiration</th>
                            <th scope="col">Days Left</th>
                            <th scope="col">Qty</th>
                            <th scope="col">Position Value</th>
                            <th scope="col">Market Value</th>
                            <th scope="col">Trade Price</th>
                            <th scope="col">Market Price</th>
                        </tr>
                        </thead>
                        <tbody>
                        <tr style="background-color: #fffef5; font-weight: 500;">
                            <td style="color: #ffc107; font-weight: bold;">VIRTUAL</td>
                            <td class="text-uppercase" style="color: #ffc107; font-weight: bold;">${virtualPosition.getTicker()}</td>
                            <td>
                                @if(OptionType.PUT.equals(virtualPosition.getType()))
                                    <span class="badge bg-danger">PUT</span>
                                @else
                                    <span class="badge bg-success">CALL</span>
                                @endif
                            </td>
                            <td>${virtualPosition.getTradeDateString()}</td>
                            <td>${virtualPosition.getExpirationDateString()}</td>
                            <td style="color: #ff6b6b;">${virtualPosition.getDaysLeft()}</td>
                            <td>${virtualPosition.getQuantity()}</td>
                            <td class="currency-usd">${virtualPosition.getPositionValue()}</td>
                            <td class="currency-usd">${virtualPosition.getMarketValue()}</td>
                            <td class="currency-usd">${virtualPosition.getTradePrice()}</td>
                            <td class="currency-usd">${virtualPosition.getMarketPrice()}</td>
                        </tr>
                        </tbody>
                    </table>

                    <div class="alert alert-info" style="margin-top: 15px; margin-bottom: 0;">
                        <i class="bi bi-info-circle me-2"></i>
                        <strong>Impact on Portfolio:</strong> This virtual position is included in the portfolio calculations above
                        (weighted ROI, probability, total P&L). It will be cleared when you close this session.
                    </div>
                </div>
            </div>
        </div>
    </div>
    @endif
    <!-- What-If Scenario Section (Virtual Position) End -->

    <!-- Open Positions Start -->
    @if(openOptions != null)
        <div class="row">
            <div class="col-lg-12">
                <div class="card">
                    <div class="card-body">
                        <h5 class="card-title">Open Positions <span></span></h5>

                        <table id="datatableOpenPositions" class="table table-striped" style="font-size: 0.85rem;">
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
                                @if(!option.getVirtual())
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
                                    <td>${option.getAnnualizedRoiPercent()}</td>
                                    <td>${option.getProbability()}</td>
                                    <td>${option.getType()}</td>
                                </tr>
                                @endif
                            @endfor
                            </tbody>
                        </table>

                    </div>

                </div>
            </div>
        </div>
    @endif
    <!-- Open Positions End -->

    <!-- Positions History Start -->
    @if(optionHistory != null)
    <div class="row">
        <div class="col-lg-12">
            <div class="card recent-sales overflow-auto">
                <div class="card-body">
                    <h5 class="card-title">Position History <span></span></h5>

                    <table id="datatablePositionHistory" class="table table-striped" style="font-size: 0.85rem;">
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
                            <th scope="col">Type</th>
                            <th scope="col">Status</th>
                        </tr>
                        </thead>
                        <tbody>
                        @for(var option : optionHistory )
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
    </div>
    @endif
    <!-- Positions History End -->
</section>

<script>
    $( document ).ready(function() {
        console.log( "ready!" );
        $('#success').fadeOut(1500);

        const imgSrc = "https://charts2-node.finviz.com/chart.ashx?cs=l&t=ticker&tf=d&s=linear&ct=candle_stick&tm=d&o[0][ot]=sma&o[0][op]=50&o[0][oc]=FF8F33C6&o[1][ot]=sma&o[1][op]=200&o[1][oc]=DCB3326D&o[2][ot]=sma&o[2][op]=20&o[2][oc]=DC32B363&o[3][ot]=patterns&o[3][op]=&o[3][oc]=000";
        const tradingView = "https://www.tradingview.com/chart/?symbol=";
        const perplexity = "https://www.perplexity.ai/finance/";
        $('#ticker').on("change", function() {
            const ticker = $(this).val();
            if(ticker) {
                // Update TradingView Advanced Chart
                updateAdvancedChartSymbol('tradingview_advanced_chart', ticker);
                // Keep fallback FinViz image updated for feature flag disabled case
                $('#chartImg').attr('src', imgSrc.replace("ticker",ticker));
                console.log('GET  /getPosition/' + ticker);
                htmx.ajax('GET', '/getPosition/'+ticker, '#main');
                $('#chartButton').prop("disabled", false);
            } else {
                $("#chartButton").prop("disabled", true);
            }
        });


        const ticker = $("#ticker").val();
        if(!ticker){
            $("#chartButton").prop("disabled", true);
        } else {
            // Initialize TradingView Advanced Chart with default ticker
            updateAdvancedChartSymbol('tradingview_advanced_chart', ticker);
            // Keep fallback FinViz image updated
            $('#chartImg').attr('src', imgSrc.replace("ticker",ticker));
            $('#chartButton').prop("disabled", false);
            $('#tradingViewButton').attr('href', tradingView+ticker);
            $('#perplexityButton').attr('href', perplexity+ticker);
        }

        $('.currency-usd').each(function () {
            var monetary_value = $(this).text();
            var i = new Intl.NumberFormat('en-US', {
                style: 'currency',
                currency: 'USD',
                maximumFractionDigits: 0
            }).format(monetary_value);
            $(this).text(i);
        });

        function initOpenPositionsTable() {
            console.log('Open Positions DataTable init');
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
        }

        function initPositionHistoryTable() {
            console.log('Position History DataTable init');
            var groupColumnHistory = 3; // TradeDate column (0-indexed)

            const tableHistory = new DataTable('#datatablePositionHistory', {
                pageLength: 500,
                perPageSelect: [5, 10, 15, ["All", -1]],
                columnDefs: [{target: 1, visible: false, searchable: false}], // code hidden
                order: [[groupColumnHistory, 'desc'], [2, 'asc']], // TradeDate desc, Symbol asc
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
            $('#datatablePositionHistory tbody').on('click', 'tr.group', function () {
                var currentOrder = tableHistory.order()[0];
                if (currentOrder[0] === groupColumnHistory && currentOrder[1] === 'desc') {
                    tableHistory.order([[groupColumnHistory, 'asc'], [2, 'asc']]).draw();
                } else {
                    tableHistory.order([[groupColumnHistory, 'desc'], [2, 'asc']]).draw();
                }
            });

            // Keep existing row click handler - exclude group rows
            $('#datatablePositionHistory tbody').on('click', 'tr:not(.group)', function() {
                const ticker = tableHistory.row(this).data()[2];
                console.log('GET  /getPosition/' + ticker);
                htmx.ajax('GET', '/getPosition/' + ticker, '#main');
            });
        }

        initOpenPositionsTable();
        initPositionHistoryTable();
        totalSeconds = 0;
    });
</script>
