@import co.grtk.srcprofit.entity.OptionType
@import co.grtk.srcprofit.entity.OptionStatus

@param co.grtk.srcprofit.dto.OptionDto optionDto
@param String success
@param String error

<div class="pagetitle">
    <h1>New Option</h1>
    <nav>
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="/">Home</a></li>
            <li class="breadcrumb-item active">New Option</li>
        </ol>
    </nav>
</div><!-- End Page Title -->

<section class="section">
    <div class="row">
        <div class="col-lg-12">
            <div class="card">
                <div class="card-body">
                    <h5 class="card-title">Option Form</h5>
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
                            <input id="symbol" name="symbol" required type="text" class="form-control" value="${optionDto.getSymbol()}">
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
                            <label for="tradePrice" class="form-label">Trade Price *</label>
                            <input id="tradePrice" name="tradePrice" type="number" required class="form-control" value="${optionDto.getTradePrice()}">
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
                            <label for="RealizedProfitOrLoss" class="form-label">Realized P&L</label>
                            <input id="RealizedProfitOrLoss" type="number" disabled class="form-control" value="${optionDto.getRealizedProfitOrLoss()}">
                        </div>

                        <div class="text-center">
                            <button class="btn btn-primary">Submit</button>
                            <button type="reset" class="btn btn-secondary">Reset</button>
                        </div>
                    </form>

                </div>
            </div>
        </div>

    </div>
</section>

<script>
    $( document ).ready(function() {
        console.log( "ready!" );
        $('#success').fadeOut(1500);
    });
</script>
