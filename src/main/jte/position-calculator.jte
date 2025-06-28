@import co.grtk.srcprofit.entity.OptionType
@import co.grtk.srcprofit.entity.OptionStatus
@import co.grtk.srcprofit.dto.PositionDto
@import java.util.List

@param co.grtk.srcprofit.dto.PositionDto positionDto
@param String success
@param String error
@param List<PositionDto> optionHistory
@param List<PositionDto> openOptions

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
    <div class="row">
        <div class="col-lg-12">
            <div class="card">
                <div class="card-body">
                    @if(positionDto.getId() != null)
                    <h5 class="card-title">Position Calculator</h5>
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

                    <!-- No Labels Form -->
                    <form class="row g-3" hx-validate="true"
                          hx-post="/calculatePosition"
                          hx-trigger="submit"
                          hx-target="#main"
                          hx-swap="innerHTML"
                          hx-on::afterRequest="console.log('calculatePosition');">
                        <input hidden name="id" id="id" type="number" value="${positionDto.getId()}" />
                        <input hidden name="parentId" id="parentId" type="number" value="${positionDto.getParentId()}" />

                        <div class="col-md-2">
                            <label for="ticker" class="form-label">Ticker *</label>
                            <input id="ticker" name="ticker" required type="text" class="form-control text-uppercase" value="${positionDto.getTicker()}">
                        </div>
                        <div class="col-md-2">
                            <label for="type" class="form-label">Type *</label>
                            <select id="type" name="type" required class="form-select">
                                @for(var type : OptionType.values())
                                    @if(type.equals(positionDto.getType()))
                                        <option selected="selected">${type.getCode()}</option>
                                    @else
                                        <option>${type.getCode()}</option>
                                    @endif
                                @endfor
                            </select>
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
                            <label for="positionValue" class="form-label">Position Value *</label>
                            <input id="positionValue" name="positionValue" type="number" required class="form-control" step="0.01" value="${positionDto.getPositionValue()}">
                        </div>
                        <div class="col-md-2">
                            <label for="tradePrice" class="form-label">Trade Price *</label>
                            <input id="tradePrice" name="tradePrice" type="number" class="form-control" step="0.01" value="${positionDto.getTradePrice()}">
                        </div>

                        <div class="col-md-8">
                            <label for="note" class="form-label">Note</label>
                            <input id="note" name="note" type="text" class="form-control" value="${positionDto.getNote()}">
                        </div>
                        <div class="col-md-2">
                            <label for="coveredPositionValue" class="form-label">Covered</label>
                            <input id="coveredPositionValue" name="coveredPositionValue" type="number" class="form-control" value="${positionDto.getCoveredPositionValue()}">
                        </div>
                        <div class="col-md-2">
                            <label for="marketValue" class="form-label">Market Value *</label>
                            <input id="marketValue" name="marketValue" type="number" required class="form-control" value="${positionDto.getMarketValue()}">
                        </div>

                        <div class="col-md-2">
                            <label for="daysBetween" class="form-label">Days Between</label>
                            <input id="daysBetween" type="number" disabled class="form-control" value="${positionDto.getDaysBetween()}">
                        </div>
                        <div class="col-md-2">
                            <label for="roi" class="form-label">R.O.I %</label>
                            <input id="roi" type="number" disabled class="form-control" value="${positionDto.getAnnualizedRoiPercent()}">
                        </div>
                        <div class="col-md-2">
                            <label for="probability" class="form-label">P.O.P %</label>
                            <input id="probability" type="number" disabled class="form-control" value="${positionDto.getProbability()}">
                        </div>
                        <div class="col-md-2">
                            <fieldset class="row md-2">
                                <label for="daysLeft" class="form-label">Days Left</label>
                                <input  id="daysLeft" type="number" disabled class="form-control" value="${positionDto.getDaysLeft()}" >
                            </fieldset>
                        </div>

                        <div class="col-md-2">
                            <label for="breakEven" class="form-label">Break Even</label>
                            <input id="breakEven" name="breakEven" type="number" disabled class="form-control" step="0.01" value="${positionDto.getBreakEven()}">
                        </div>

                        <div class="text-center">
                            <button class="btn btn-primary">Calculate</button>
                            <a id="tradingViewButton" href="https://www.tradingview.com/chart/?symbol=VIX" class="btn btn-info" role="button" target="_blank">TradingView</a>
                            <button id="chartButton" type="button" class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#ExtralargeModal">
                                Snapshot Chart
                            </button>
                        </div>
                    </form>

                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="ExtralargeModal" tabindex="-1">
        <div class="modal-dialog modal-xl">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Daily Snapshot</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <img id="chartImg" style="width: 100%" src="#" />
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                </div>
            </div>
        </div>
    </div><!-- End Extra Large Modal-->
</section>

<script>
    $( document ).ready(function() {
        console.log( "ready!" );
        $('#success').fadeOut(1500);

        const imgSrc = "https://charts2-node.finviz.com/chart.ashx?cs=l&t=ticker&tf=d&s=linear&ct=candle_stick&tm=d&o[0][ot]=sma&o[0][op]=50&o[0][oc]=FF8F33C6&o[1][ot]=sma&o[1][op]=200&o[1][oc]=DCB3326D&o[2][ot]=sma&o[2][op]=20&o[2][oc]=DC32B363&o[3][ot]=patterns&o[3][op]=&o[3][oc]=000";
        const tradingView = "https://www.tradingview.com/chart/?symbol=";
        $('#ticker').on("change", function() {
            const ticker = $(this).val();
            if(ticker) {
                $('#chartImg').attr('src', imgSrc.replace("ticker",ticker));
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
            $('#chartImg').attr('src', imgSrc.replace("ticker",ticker));
            $('#chartButton').prop("disabled", false);
            $('#tradingViewButton').attr('href', tradingView+ticker);
        }

    });
</script>
