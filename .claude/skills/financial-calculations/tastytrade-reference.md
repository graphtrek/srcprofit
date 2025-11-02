# TastyTrade Methodology Reference

## Core Principles

### Premium Selling Strategy
- Sell options above probability of profit threshold (65-75% PoP)
- Collect premium upfront (known profit/loss at entry)
- Risk defined by strike width (for spreads) or buying power (for naked puts)
- Manage early if position moves 50% to max profit
- Let losers take profits (defined risk) or let winners come to expiration

### Position Sizing
- **Max Risk Per Trade**: 2-5% of account
- **Portfolio Heat**: Do not exceed 20% of account buying power
- **Example**: $100,000 account
  - Max heat: $20,000
  - Single trade risk: $2,000-$5,000
  - Number of positions: ~4-10 depending on width

### Cost Basis Tracking (FIFO)
Purpose: Tax reporting and accurate P&L calculation

Steps:
1. Track each trade entry separately (lot tracking)
2. When closing, use first-in lot first (FIFO)
3. Calculate cost per share: total_cost / shares
4. Realized gain/loss: (sale_price - cost_basis) × quantity
5. Unrealized gain/loss: (current_value - cost_basis) × quantity

## Key Metrics

### Probability of Profit (PoP)
- **Definition**: Probability the position is profitable at expiration
- **Typical Target**: 65-75% for premium selling
- **Calculation**: N(-|d2|) for individual options
- **Portfolio Level**: Aggregate across all positions

### Greeks and Their Meanings

| Greek | Formula/Source | Meaning | For Premium Sellers |
|-------|-----------------|---------|-------------------|
| **Delta** | N(d1) | 0.30 delta = ~70% probability OTM at exp | Monitor directional exposure |
| **Gamma** | N'(d1)/(S×σ×√t) | Convexity risk - how fast delta changes | Higher for ATM, risk spikes near exp |
| **Theta** | -S×N'(d1)×σ/(2√t) | Time decay (daily = annual/365) | Your friend - positive for sellers |
| **Vega** | S×N'(d1)×√t | Volatility exposure (per 1% IV change) | Typically short vega (good for sellers) |
| **Rho** | K×t×e^(-rt)×N(d2) | Interest rate sensitivity | Usually small, ignore in options |

### Portfolio Heat (Buying Power Used)
- **Formula for Cash-Secured Puts**: Strike × Quantity × 100
- **Formula for Spreads**: (Strike Width) × Quantity × 100
- **Formula for Short Calls**: Margin requirement (typically 20% of stock value)
- **Target**: ≤20% of account = available room for new positions

### Max Loss Calculation
- **Naked Put**: (Strike - 0.01) × Quantity × 100
- **Put Spread**: Width × Quantity × 100
- **Call Spread**: Width × Quantity × 100
- **Naked Call**: Theoretically unlimited (use as last resort)

## Exit Rules

### Taking Profits
- Close at 50% of max profit (this is key to risk/reward)
- Example: Sell $1 wide spread for $0.50 credit
  - Max profit: $50
  - Take profit at: $25 (50% max profit)
  - Risk/Reward: 1:0.5 (better than 1:1)

### Managing Losses
- Close at 21 DTE if underwater
- Close at -2 standard deviations below max profit
- Allow defined risk to hit max loss if conviction strong

### Time-Based Management
- Sell typically 45-60 days to expiration
- Close/roll at 21 days to expiration
- Avoid theta decay acceleration past 14 DTE unless profitable

## Common Strategies

### Cash-Secured Puts (CSP)
- Sell put below support level
- Collect premium, manage at 50% profit or 21 DTE
- Risk: Forced to buy 100 shares × strike price
- Max loss per contract: (Strike - 0%) × 100
- Portfolio heat: Strike × 100 per contract

### Short Call/Put Spreads
- Sell premium + Buy protection
- Lower max loss, lower max profit
- Better for portfolio heat optimization
- Example: Sell $150 Call, Buy $160 Call
  - Width: $10 × 100 = $1,000 max loss
  - Heat: $1,000 (same as max loss)

### Covered Calls
- Own stock, sell calls against it
- Reduces cost basis by premium collected
- Limits upside (cap at strike)
- Lower risk than naked short calls

## Risk Management Framework

### Daily Monitoring
- Check delta exposure (don't get too directional)
- Monitor gamma risk (spikes near expiration)
- Review portfolio heat (stay ≤20%)
- Check theta decay (verify working in your favor)

### Position Management
- Target 50% profit close (not 100%)
- Let winners run to expiration only if convenient
- Close losers early, never hold to max loss unless planned
- Roll positions when appropriate (extend and adjust)

### Portfolio Rules
- Max 2-5% risk per trade
- Max 20% heat total
- No single underlying > 50% of portfolio
- Diversify across sectors and strategies
