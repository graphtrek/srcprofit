# Trading Domain Knowledge - SrcProfit

**Purpose**: Options trading methodology and domain expertise
**Last Updated**: 2025-10-21 (Session 1)

---

## 📚 Overview

SrcProfit follows **TastyTrade methodology** for options trading analysis and tracking. This directory contains all trading-specific knowledge, patterns, and best practices migrated from the contrarian project.

---

## 🎯 Trading Methodology

### Core Principles (TastyTrade)
1. **Premium Selling**: Focus on selling premium (puts/calls) rather than buying
2. **Defined Risk**: Use spreads to define maximum risk
3. **High Probability**: Trade high-probability strategies (60-70% win rate)
4. **Small Position Sizes**: Risk 1-5% per trade
5. **Mechanical Trading**: Rule-based, not emotional

### Position Management
- **Entry**: Sell when IV rank > 50%
- **Exit**: Close at 50% of max profit or 21 DTE
- **Adjustment**: Roll when tested, manage winners
- **Risk**: Max 5% portfolio heat per position

---

## 📊 Financial Calculations

### P&L Calculation (FIFO Cost Basis)
```
Realized P&L = Sell Price - Buy Price (FIFO order)
Unrealized P&L = Current Market Price - Cost Basis
Total P&L = Realized + Unrealized
```

**Critical**: Always use FIFO (First In, First Out) for cost basis

### Annualized ROI (Black-Scholes Based)
```
ROI % = (Premium / Risk) * 100
Annualized ROI = ROI * (365 / Days to Expiration)
```

**Implementation**: Uses Apache Commons Math3 for Black-Scholes pricing

### Greeks Understanding
- **Delta**: Rate of change of option price per $1 move in underlying
- **Theta**: Time decay per day
- **Gamma**: Rate of change of delta
- **Vega**: Sensitivity to IV changes

---

## 🔧 TastyTrade Integration

### Data Sources
See: `tastytrade-data-sources.md`

- **Account Data**: Positions, balances, transactions
- **Market Data**: Quotes, chains, Greeks
- **Historical Data**: Trades, P&L history

### API Integration Patterns
See: `tastytrade-cli-porting-strategy.md`

- Session management (authentication, token renewal)
- Data fetching (REST API, streaming)
- Error handling (retries, fallbacks)

### Feature Gap Analysis
See: `tastytrade-cli-feature-gap-analysis.md`

Comparison between TastyTrade CLI capabilities and SrcProfit implementation

---

## 📐 Options Analysis

### Option Chain Analysis
See: `tastytrade-cli-option-analysis.md`

- Strike selection (delta targeting)
- Expiration selection (30-45 DTE optimal)
- IV analysis (sell when IV rank > 50%)
- Probability of profit calculations

### Position Risk Analysis
- **Max Loss**: Defined by spread width
- **Break-Even**: Strike +/- premium collected
- **Probability of Profit**: Based on delta
- **Portfolio Heat**: Sum of all position risks

---

## 🎯 Ground Truth Validation

### Broker Comparison (Critical)
All calculations MUST be validated against broker data:

**Sources**:
- **IBKR Flex Reports**: CSV exports for trades, positions, NAV
- **Alpaca API**: Real-time quotes and market data
- **TastyTrade**: (if integrated) Reference for P&L calculations

**Process**:
1. Export ground truth data from broker
2. Calculate using SrcProfit logic
3. Compare results (exact match required)
4. If mismatch → investigate and fix
5. Create test case with ground truth data

### Test Fixtures
Store ground truth data in `src/test/resources/ground-truth/`:
- `broker-trades.csv` - Actual trade executions
- `broker-positions.json` - End-of-day positions
- `broker-pl.json` - Expected P&L calculations

---

## 🚨 Critical Rules

### Decimal Precision
```java
// ✅ CORRECT - Use BigDecimal for money
BigDecimal premium = new BigDecimal("125.50");
BigDecimal risk = new BigDecimal("500.00");

// ❌ WRONG - Never use double for money
double premium = 125.50;  // Floating point errors!
```

### FIFO Cost Basis
```java
// ✅ CORRECT - FIFO ordering
List<Trade> sorted = trades.stream()
    .sorted(Comparator.comparing(Trade::getTradeDate))
    .collect(Collectors.toList());

// ❌ WRONG - Random or LIFO ordering
List<Trade> unsorted = trades;  // Order matters!
```

### API Documentation (RTFM)
```
Before implementing ANY broker API integration:
1. Read official API documentation
2. Test with sample requests
3. Validate response formats
4. Document assumptions
5. NEVER guess API behavior
```

---

## 📚 Documentation Files

### Methodology Documents
- **tastytrade-data-sources.md** - Available data and APIs
- **tastytrade-cli-feature-gap-analysis.md** - Feature comparison
- **tastytrade-cli-option-analysis.md** - Options analysis patterns
- **tastytrade-cli-porting-strategy.md** - Integration best practices
- **tastytrade-cli-module-architecture-plan.md** - Module design

### Future Documents (To Be Created)
- **options-pricing-models.md** - Black-Scholes, binomial trees
- **risk-management-rules.md** - Position sizing, portfolio heat
- **tax-optimization.md** - Wash sales, long-term gains
- **trading-glossary.md** - Common trading terms

---

## 🎓 Learning Resources

### Internal
- Agent: `.claude/agents/trading-specialist.md` - Trading domain expert
- Testing: `docs/workflow/testing-strategy.md` - Ground Truth TDD

### External
- [TastyTrade Education](https://www.tastytrade.com/education)
- [Options Playbook](https://www.optionsplaybook.com/)
- [CBOE Options Institute](https://www.cboe.com/education/)
- [The Options Guide](https://www.theoptionsguide.com/)

---

## 🔄 Maintenance

### Update Schedule
- **Weekly**: Review TastyTrade methodology updates
- **Monthly**: Validate calculations against broker data
- **Quarterly**: Review and update documentation

### Version History
- **v1.0** (2025-10-21, Session 1): Initial migration from contrarian project

---

**Version**: 1.0 (Session 1 - Migration)
**Source**: Contrarian Trading Portfolio System (180+ sessions)
**Maintained By**: Claude Code with trading-specialist agent
**Next Review**: After first options trade analysis
