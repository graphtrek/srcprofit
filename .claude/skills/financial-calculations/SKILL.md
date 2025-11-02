---
name: financial-calculations
description: Implement and verify options trading financial calculations following TastyTrade methodology, including Black-Scholes Greeks (delta, gamma, theta, vega), P&L analysis with FIFO cost basis, position sizing, and portfolio risk metrics. Use when implementing pricing models, calculating Greeks, validating P&L, or computing position-level risk.
---

# Financial Calculations Skill

Implement and verify trading financial calculations following TastyTrade methodology for options strategies.

## Instructions

### When to Use This Skill

Use this skill when you need to:
- Implement Black-Scholes option pricing model and Greeks calculation
- Calculate position-level Greeks (delta, gamma, theta, vega, rho)
- Compute P&L accurately with FIFO cost basis tracking
- Determine risk metrics and position sizing
- Validate calculations against broker data (Alpaca, IBKR)
- Create performance reports with accurate financial metrics
- Handle dividends, stock splits, expirations, and assignments

### TastyTrade Methodology Requirements

**Strategies**: Premium selling (cash-secured puts, short calls, spreads)
**Position Sizing**: Risk no more than 2-5% per position, 20% portfolio heat maximum
**Cost Basis**: FIFO (First-In-First-Out) for proper tax reporting and P&L accuracy
**Greeks**: Use as indicators for position management and risk assessment
**Probability of Profit**: Model at entry and track through position lifecycle

### Critical Standards

**Always Use BigDecimal for Monetary Values**: NEVER use `double` for financial calculations
- Currency amounts: 2 decimal places precision
- Greeks: 4 decimal places precision
- Interest rates/volatility: 4 decimal places

**Validation**: Test all calculations against real broker data (Alpaca, IBKR)
**Documentation**: Include formulas and references to sources
**Edge Cases**: Handle dividends, stock splits, expirations, assignments properly

### Key Calculations

1. **Black-Scholes Model**
   - Input: Stock price, strike price, time to expiration, risk-free rate, volatility
   - Output: Call/put option price
   - Formula: C = S*N(d1) - K*e^(-rt)*N(d2)

2. **Greeks Calculations**
   - **Delta**: Rate of change of option price with respect to stock price
   - **Gamma**: Rate of change of delta
   - **Theta**: Time decay (daily theta is most practical)
   - **Vega**: Sensitivity to volatility changes (per 1% change)
   - **Rho**: Sensitivity to interest rate changes

3. **FIFO Cost Basis**
   - Track individual lots: entry date, quantity, cost per share
   - Calculate cost per share using FIFO method
   - Realized gains: (Sale Price - Cost Basis) × Quantity
   - Unrealized gains: (Current Value - Cost Basis)

4. **Position P&L**
   - Unrealized: (Current Value - Cost Basis) for open positions
   - Realized: (Sale Price - Cost Basis) for closed positions
   - Include commissions and fees in cost basis

5. **Portfolio Heat (Buying Power Used)**
   - Sum of margin requirements across all positions
   - For spreads: width × quantity
   - For naked positions: % of portfolio value
   - Typical limit: 20% of portfolio

6. **Max Loss (Risk)**
   - Single position: position size × (strike difference or width)
   - Spreads: width of spread × quantity
   - Naked puts: (strike price - $0.01) × quantity

## Implementation Patterns

### Entity Models
- `Position`: Stores current position data with Greeks
- `Trade`: Historical trades with cost basis, execution details
- `PortfolioMetrics`: Aggregate portfolio statistics
- `Greeks`: Calculated Greeks for each position

### Service Layer
```
CalculationService:
  - calculateBlackScholesPrice(spotPrice, strike, tte, rate, volatility)
  - calculateGreeks(position)
  - calculatePnL(position)

PositionService:
  - updateGreeks(position)
  - calculatePortfolioHeat()

CostBasisService:
  - calculateFifoCostBasis(trades)
  - calculateRealizedGains(closedPosition)
```

### External API Integration
- **Alpaca**: Fetch current prices, Greeks if available, historical data
- **IBKR**: Verify Greeks, margin requirements, portfolio metrics
- **Alpha Vantage**: Volatility and historical pricing data

## Examples

### Calculate Greeks for a Call Option
```
spotPrice: $100
strike: $105
timeToExpiration: 30 days (0.0822 years)
riskFreeRate: 5%
volatility: 25%

Expected Output:
delta: 0.4567
gamma: 0.0245
theta: -0.0432 (per day)
vega: 0.1234
```

### Calculate FIFO Cost Basis
```
Buy 10 shares @ $100 = $1,000
Buy 5 shares @ $102 = $510
Sell 8 shares @ $105

Cost basis for 8 shares (FIFO):
- 8 shares from first lot: 8 × $100 = $800
- Realized gain: (8 × $105) - $800 = $240

Remaining position:
- 2 shares @ $100
- 5 shares @ $102
```

### Calculate Position P&L
```
Position: Buy 10 AAPL Call $150 strike @ $3.50
Current price: $4.25
Current position value: 10 × 4.25 × 100 = $4,250
Cost basis: 10 × 3.50 × 100 = $3,500
Unrealized P&L: $750 (21.4% return)
Daily theta: -$23.45 (daily decay)
```

## Error Prevention Checklist

- [ ] All monetary calculations use BigDecimal (not double/float)
- [ ] Greeks precision is 4 decimal places
- [ ] Cost basis uses FIFO method
- [ ] P&L includes commissions and fees
- [ ] Calculations validated against broker data
- [ ] Edge cases handled (dividends, splits, expirations)
- [ ] Test cases cover both long and short positions
- [ ] Greeks updated with real market volatility data
- [ ] Portfolio heat respects 20% maximum limit
