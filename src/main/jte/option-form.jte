@import co.grtk.srcprofit.entity.OptionType
@import co.grtk.srcprofit.entity.OptionStatus
@import co.grtk.srcprofit.dto.OptionDto
@import java.util.List

@param co.grtk.srcprofit.dto.OptionDto optionDto
@param String success
@param String error
@param List<OptionDto> optionHistory

<div class="pagetitle">
    <h1>Option Form</h1>
    <nav>
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="/">Home</a></li>
        </ol>
    </nav>
</div><!-- End Page Title -->

<section class="section">
    <div class="row">
        <div class="col-lg-12">
            <div class="card">
                <div class="card-body">
                    @if(optionDto.getId() != null)
                    <h5 class="card-title">Update Option #${optionDto.getId()}</h5>
                    @else
                        <h5 class="card-title">New Option</h5>
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
                          hx-post="/save-option"
                          hx-trigger="submit"
                          hx-target="#main"
                          hx-swap="innerHTML"
                          hx-on::afterRequest="console.log('save-option');">
                        <input hidden name="id" id="id" type="number" value="${optionDto.getId()}" />
                        <input hidden name="parentId" id="parentId" type="number" value="${optionDto.getParentId()}" />

                        <div class="col-md-2">
                            <label for="symbol" class="form-label">Symbol *</label>
                            <input id="symbol" name="symbol" required type="text" class="form-control text-uppercase" value="${optionDto.getSymbol()}">
                        </div>

                        <div class="col-md-2">
                            <label for="type" class="form-label">Type *</label>
                            <select id="type" name="type" required class="form-select">
                                @for(var type : OptionType.values())
                                    @if(type.equals(optionDto.getType()))
                                        <option selected="selected">${type.getCode()}</option>
                                    @else
                                        <option>${type.getCode()}</option>
                                    @endif
                                @endfor
                            </select>
                        </div>
                        <div class="col-md-3">
                            <label for="startDate" class="form-label">Trade Date *</label>
                            <input id="startDate" name="startDate" type="datetime-local" required class="form-control" value="${optionDto.getStartDateTimeString()}">
                        </div>
                        <div class="col-md-1">
                            <label for="fee" class="form-label">Fee</label>
                            <input id="fee" name="fee" type="number" class="form-control" value="${optionDto.getFee()}">
                        </div>
                        <div class="col-md-2">
                            <label for="positionValue" class="form-label">Position Value *</label>
                            <input id="positionValue" name="positionValue" type="number" required class="form-control" value="${optionDto.getPositionValue()}">
                        </div>
                        <div class="col-md-2">
                            <label for="tradePrice" class="form-label">Trade Price</label>
                            <input id="tradePrice" name="tradePrice" type="number" class="form-control" value="${optionDto.getTradePrice()}">
                        </div>

                        <div class="col-md-2">
                            <label for="status" class="form-label">Status *</label>
                            <select id="status" name="status" required class="form-select">
                                @for(var status : OptionStatus.values())
                                    @if(status.equals(optionDto.getStatus()))
                                        <option selected="selected">${status.getCode()}</option>
                                    @else
                                        <option>${status.getCode()}</option>
                                    @endif
                                @endfor
                            </select>
                        </div>
                        <div class="col-md-5">
                            <label for="note" class="form-label">Note</label>
                            <input id="note" name="note" type="text" class="form-control" value="${optionDto.getNote()}">
                        </div>
                        <div class="col-md-1">
                            <label for="color" class="form-label">Color</label>
                            <input type="color" id="color" name="color" class="form-control form-control-color" value="${optionDto.getColor()}">
                        </div>
                        <div class="col-md-2">
                            <label for="quantity" class="form-label">Quantity *</label>
                            <input id="quantity" name="quantity" type="number" required class="form-control" value="${optionDto.getQuantity()}">
                        </div>
                        <div class="col-md-2">
                            <label for="marketValue" class="form-label">Market Value *</label>
                            <input id="marketValue" name="marketValue" type="number" required class="form-control" value="${optionDto.getMarketValue()}">
                        </div>


                        <div class="col-md-2">
                            <label for="expirationDate" class="form-label">Expiration Date *</label>
                            <input id="expirationDate" name="expirationDate" type="date" required class="form-control" value="${optionDto.getExpirationDateString()}">
                        </div>
                        <div class="col-md-2">
                            <label for="daysBetween" class="form-label">Days Between</label>
                            <input id="daysBetween" type="number" disabled class="form-control" value="${optionDto.getDaysBetween()}">
                        </div>
                        <div class="col-md-2">
                            <label for="roi" class="form-label">R.O.I %</label>
                            <input id="roi" type="number" disabled class="form-control" value="${optionDto.getAnnualizedRoiPercent()}">
                        </div>
                        <div class="col-md-2">
                            <label for="probability" class="form-label">P.O.P %</label>
                            <input id="probability" type="number" disabled class="form-control" value="${optionDto.getProbability()}">
                        </div>
                        <div class="col-md-2">
                            <fieldset class="row md-2">
                                <label for="daysLeft" class="form-label">Days Left</label>
                                <input  id="daysLeft" type="number" disabled class="form-control" value="${optionDto.getDaysLeft()}" >
                            </fieldset>
                        </div>
                        <div class="col-md-2">
                            <label for="realizedProfitOrLoss" class="form-label">Realized P&L</label>
                            <input id="realizedProfitOrLoss" name="realizedProfitOrLoss" type="number" class="form-control" value="${optionDto.getRealizedProfitOrLoss()}">
                        </div>

                        <div class="text-center">
                            <button class="btn btn-primary">Submit</button>
                            <button type="reset" class="btn btn-secondary">Reset</button>
                            <button id="chartButton" type="button" class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#ExtralargeModal">
                                Chart
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
                    <h5 class="modal-title">Extra Large Modal</h5>
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


    @if(optionHistory != null)
    <div class="row">
        <div class="col-lg-12">
            <div class="card">
                <div class="card-body">
                    <h5 class="card-title">Positions <span></span></h5>

                    <table id="datatable" class="table table-borderless">
                        <thead>
                        <tr>
                            <th scope="col">#</th>
                            <th scope="col">Symbol</th>
                            <th scope="col">TradeDate</th>
                            <th scope="col">Type</th>
                            <th scope="col">Value</th>
                            <th scope="col">Market Value</th>
                            <th scope="col">TradePrice</th>
                            <th scope="col">Expiration</th>
                            <th scope="col">Days</th>
                            <th scope="col">P.O.P %</th>
                            <th scope="col">R.O.I %</th>
                            <th scope="col">Status</th>
                        </tr>
                        </thead>
                        <tbody>
                        @for(var option : optionHistory )
                            <tr style="cursor: pointer;">

                                <th style="color:${option.getColor()};">${option.getId()}</th>

                                <td class="text-uppercase">${option.getSymbol()}</td>
                                <td>${option.getStartDateString()}</td>
                                <td>${option.getType()}</td>
                                <td>${option.getPositionValue()}</td>
                                <td>${option.getMarketValue()}</td>
                                <td>${option.getTradePrice()}</td>
                                <td>${option.getExpirationDateString()}</td>
                                <td>${option.getDaysBetween()}</td>
                                <td>${option.getProbability()}</td>
                                <td>${option.getAnnualizedRoiPercent()}</td>
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

</section>

<script>
    $( document ).ready(function() {
        console.log( "ready!" );
        $('#success').fadeOut(1500);

        const imgSrc = "https://charts2-node.finviz.com/chart.ashx?cs=l&t=symbol&tf=d&s=linear&ct=candle_stick&tm=d&o[0][ot]=sma&o[0][op]=50&o[0][oc]=FF8F33C6&o[1][ot]=sma&o[1][op]=200&o[1][oc]=DCB3326D&o[2][ot]=sma&o[2][op]=20&o[2][oc]=DC32B363&o[3][ot]=patterns&o[3][op]=&o[3][oc]=000";

        $('#symbol').on("change", function() {
            const newSymbol = $(this).val();
            if(newSymbol) {
                $('#chartImg').attr('src', imgSrc.replace("symbol",newSymbol));
                $('#chartButton').prop("disabled", false);
            } else {
                $("#chartButton").prop("disabled", true);
            }
        });


        const symbol = $("#symbol").val();
        if(!symbol){
            $("#chartButton").prop("disabled", true);
        } else {
            $('#chartImg').attr('src', imgSrc.replace("symbol",symbol));
            $('#chartButton').prop("disabled", false);
        }

    });
</script>
