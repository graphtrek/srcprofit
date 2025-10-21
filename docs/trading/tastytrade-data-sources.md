# TastyTrade Data Sources - Ground Truth Reference

**Last Updated**: 2025-10-06
**Purpose**: Document available TastyTrade data exports for validation and performance tracking

---

## 📥 Available Data Exports

### 0. Positions CSV Export (Web UI)

**Export Location**: TastyTrade Web → Positions → Export
**Update Frequency**: Real-time
**Limitation**: Only exports visible columns (limited by screen width)

#### Available Columns

```csv
Account, Symbol, Type, Quantity, Exp Date, DTE, Strike Price,
Call/Put, Underlying Last Price, P/L Open, P/L Day, Trade Price,
IV Rank, Delta, Bid (Sell), Ask (Buy), D's Opn, β Delta,
Days To Expiration, Indicators
```

#### Critical Fields

| Field | Description | Priority |
|-------|-------------|----------|
| **P/L Open** | Current unrealized P/L | 🔥 CRITICAL |
| **P/L Day** | Today's P/L change | 🔥 CRITICAL |
| **D's Opn** | Days position open | 🔥 CRITICAL |
| Account | Account number | ✅ Need |
| Symbol | Position symbol | ✅ Need |
| Type | Instrument type | ✅ Need |
| Quantity | Position size | ✅ Need |
| Strike Price | Option strike | ✅ Need |
| Call/Put | Option type | ✅ Need |
| Exp Date | Expiration date | ✅ Need |
| Trade Price | Entry price | ✅ Need |
| Underlying Last Price | Current underlying | ✅ Need |
| Delta | Position delta | ⚠️ Nice to have |
| IV Rank | Implied vol rank | ⚠️ Nice to have |
| β Delta | Beta-weighted delta | ⚠️ Nice to have |

#### Key Insights

✅ **P/L Day exists!** → Daily P/L per position
✅ **D's Opn exists!** → Can calculate entry_date (today - D's Opn)
✅ **P/L Open exists!** → Unrealized P/L per position
⚠️ **Export limitation**: Only visible columns exported (screen width limited)

**Usage**: Can supplement API data with P/L Day and D's Opn if API doesn't have them

---

### 1. Year-to-Date Performance by Symbol

**Export Location**: TastyTrade Web → History → YTD Performance
**File Example**: `tastytrade_year_to_date_history_x5WV58172_251006.csv`
**Update Frequency**: Real-time (download anytime)

#### Data Structure

```csv
Symbol, P/L Realized, P/L Open, P/L YTD, Commissions, Fees, P/L YTD w/ Fees
/GCZ5,  4,250.00,    -6,728.56, -2,478.56, -128.75,    -202.91, -2,810.22
AMD,    -11,733.58,  -71.00,    -11,804.58, -155.00,   -42.59,  -12,002.17
```

#### Fields

| Field | Description | Type | Example |
|-------|-------------|------|---------|
| Symbol | Underlying symbol | String | /GCZ5, AMD, SPY |
| P/L Realized | Closed trades YTD | Decimal | 4,250.00 |
| P/L Open | Current unrealized P/L | Decimal | -6,728.56 |
| P/L YTD | Total (realized + open) | Decimal | -2,478.56 |
| Commissions | YTD commissions | Decimal | -128.75 |
| Fees | YTD fees | Decimal | -202.91 |
| P/L YTD w/ Fees | Net after costs | Decimal | -2,810.22 |

#### Key Insights

✅ **Per Symbol**: Performance grouped by underlying (perfect for our use case!)
✅ **Real Data**: Directly from TastyTrade (no calculations needed)
✅ **Complete Picture**: Realized + Unrealized + Fees
✅ **All Asset Classes**: Stocks, futures, options, crypto

**Usage**: Primary data source for performance dashboard

---

### 2. Daily Activity / Orders

**Export Location**: TastyTrade Web → Activity → Orders
**File Example**: `tastytrade_activity_251006.csv`
**Update Frequency**: Real-time

#### Data Structure

```csv
Symbol, Status, MarketOrFill, Price, TIF, Time, TimeStampAtType, Order #, Description
AMD,    Filled, 10.30 cr,     10.30 cr, Day, 4:13:50p, Fill, #411438277, "-1 Nov 21 46d 170 Put STO
-1 Nov 21 46d 260 Call STO"
/GCZ5,  Filled, 6.60 cr,      6.60 cr,  Day, 10:28:23a, Fill, #411355030, "1 Oct 28 22d 3995 Call BTO
-2 Oct 28 22d 4005 Call STO
1 Oct 28 22d 4035 Call BTO"
```

#### Fields

| Field | Description | Example |
|-------|-------------|---------|
| Symbol | Underlying | /GCZ5, AMD |
| Status | Order status | Filled, Canceled, Working |
| MarketOrFill | Fill price | 10.30 cr, 6.60 cr |
| Order # | Order ID | #411438277 |
| Description | Legs detail | Multi-line leg breakdown |
| Time | Execution time | 4:13:50p |

#### Key Insights

✅ **Order Grouping**: Multi-leg orders grouped by Order #
✅ **Entry Intent**: Shows what you intended to enter
✅ **Timestamps**: Exact entry time for each order
✅ **All Orders**: Filled, Canceled, Working

**Usage**:
- Validate strategy detection (Order # = ground truth)
- Get entry dates for positions
- Test detector on fresh positions

---

### 3. Today's Trades Analysis (From CSV)

**Date**: 2025-10-06
**Account**: 5WV58172 (Margin)

#### Futures Entered Today

| Symbol | Quantity | Price | Order # |
|--------|----------|-------|---------|
| /ZBZ5 | 1 BUY | 116'10 | #411340812 |
| /ZNZ5 | 1 BUY | 112'15 | #411340946 |

#### Futures Options Entered Today

| Symbol | Strategy | Order # | Legs |
|--------|----------|---------|------|
| /GCZ5 | Butterfly | #411354896 | 1-2-1 (4100/4150/4300 calls) |
| /GCZ5 | Butterfly | #411354864 | 1-2-1 (4020/4050/4120 calls) |
| /GCZ5 | Butterfly | #411355030 | 1-2-1 (3995/4005/4035 calls) |
| /GCZ5 | Butterfly | #411356719 | 1-2-1 (4030/4070/4140 calls) |
| /SIZ5 | Adjusted | #411357922 | Roll/adjust |

#### Stock Options Entered Today

| Symbol | Type | Order # | Description |
|--------|------|---------|-------------|
| AMD | Short Strangle | #411438277 | -1 Put 170, -1 Call 260 |
| AMD | Short Call | #411404629 | -1 Call 270 |
| AMD | Multiple | Various | 5 more orders |

#### Key Insights

✅ **Confirmed Bug**: 2 futures entered today (/ZBZ5, /ZNZ5) missing from performance
✅ **Strategy Detection**: 4 butterflies on /GCZ5 (should be grouped)
✅ **Perfect Test Case**: Fresh positions to validate detector

---

## 🎯 Use Cases

### Performance Dashboard
**Data Source**: YTD Performance CSV
**Why**: Has realized + unrealized P/L per symbol
**Implementation**:
```python
# Read YTD CSV
ytd_data = parse_ytd_csv("tastytrade_year_to_date_history_*.csv")

# Group by symbol
for symbol, data in ytd_data.items():
    print(f"{symbol}: Open={data.pl_open}, Realized={data.pl_realized}")
```

### Strategy Detection Validation
**Data Source**: Today's Activity CSV
**Why**: Has Order # (ground truth for what you intended)
**Implementation**:
```python
# Read activity CSV
orders = parse_activity_csv("tastytrade_activity_*.csv")

# Group by Order #
for order_id, legs in orders.items():
    detected = strategy_detector.detect(legs)
    print(f"Order {order_id}: Expected={legs.type}, Detected={detected.type}")
```

### Entry Date Population
**Data Source**: Activity CSV + API
**Why**: Need entry dates for ROI calculations
**Options**:
1. Parse activity CSV for entry timestamps
2. Use TastyTrade API `/transactions` endpoint
3. Store in local database

---

## 📂 File Storage

### Ground Truth Data (Git)
```
docs/ground_truth/
  ├── tastytrade_year_to_date_history_251006.csv  (YTD performance)
  ├── tastytrade_activity_251006.csv             (Today's orders)
  └── README.md                                   (This file)
```

**Why store in Git:**
- ✅ Reproducible tests
- ✅ Validate detector against known-good data
- ✅ Track performance over time
- ✅ Regression testing

### Ignored Files (.gitignore)
```
# Don't commit personal data
*.env
*credentials*
*password*
```

---

## 🔍 Data Validation Checklist

When validating against TastyTrade data:

**Performance:**
- [ ] P/L Open matches current portfolio unrealized P/L
- [ ] P/L Realized matches closed trades YTD
- [ ] All symbols present (futures, stocks, options, crypto)
- [ ] Fees/commissions included

**Strategy Detection:**
- [ ] Multi-leg orders grouped by Order #
- [ ] Detected strategy matches order description
- [ ] All legs accounted for
- [ ] Entry dates match order timestamps

**Completeness:**
- [ ] Futures positions included
- [ ] Futures options included
- [ ] Stock options included
- [ ] Crypto included

---

## 📋 Next Steps

1. ✅ Copy CSV files to `docs/ground_truth/`
2. ✅ Parse YTD CSV for performance dashboard
3. ✅ Parse activity CSV for validation
4. ✅ Test strategy detector against today's orders
5. ✅ Implement performance by underlying view

---

## 🐛 Bugs This Data Helps Fix

**Bug #1**: Entry dates missing
→ Solution: Parse activity CSV for order timestamps

**Bug #2**: Futures not included
→ Confirmed: /ZBZ5, /ZNZ5 entered today, not in performance

**Bug #3**: Performance by strategy (unusable)
→ Solution: Use YTD CSV (already grouped by symbol!)

**Bug #4**: No realized P/L
→ Solution: Use YTD CSV "P/L Realized" column

---

**Perfect data sources!** TastyTrade provides exactly what we need. 🎯
