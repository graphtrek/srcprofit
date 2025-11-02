# Black-Scholes Option Pricing Formulas

## Core Formula

**Call Option Price:**
```
C = S * N(d1) - K * e^(-r*t) * N(d2)
```

**Put Option Price:**
```
P = K * e^(-r*t) * N(-d2) - S * N(-d1)
```

## Where:
- `S` = Current stock price
- `K` = Strike price
- `r` = Risk-free interest rate (annualized)
- `t` = Time to expiration (in years, e.g., 30 days = 0.0822)
- `σ` = Volatility (annualized, e.g., 0.25 = 25%)
- `N(x)` = Cumulative standard normal distribution function
- `e` = Mathematical constant (2.71828...)

## Intermediate Calculations

```
d1 = [ln(S/K) + (r + σ²/2) * t] / (σ * √t)
d2 = d1 - σ * √t
```

## Greeks Formulas

### Delta (Δ)
**Call Delta:**
```
Δ_call = N(d1)
```

**Put Delta:**
```
Δ_put = N(d1) - 1 = -N(-d1)
```

Range: Call delta 0 to 1, Put delta -1 to 0

### Gamma (Γ)
```
Γ = N'(d1) / (S * σ * √t)

Where N'(d1) = e^(-d1²/2) / √(2π)
```

Interpretation: Change in delta for $1 move in stock price

### Theta (Θ)
**Call Theta (daily, divide by 365):**
```
Θ_call = -S * N'(d1) * σ / (2 * √t) - r * K * e^(-r*t) * N(d2)
Daily Theta = Θ / 365
```

**Put Theta (daily, divide by 365):**
```
Θ_put = -S * N'(d1) * σ / (2 * √t) + r * K * e^(-r*t) * N(-d2)
Daily Theta = Θ / 365
```

Interpretation: Option value loss per day due to time decay

### Vega (ν)
```
ν = S * N'(d1) * √t / 100

Where division by 100 represents change per 1% volatility change
```

Interpretation: Change in option price for 1% change in volatility

### Rho (ρ)
**Call Rho:**
```
ρ_call = K * t * e^(-r*t) * N(d2)
```

**Put Rho:**
```
ρ_put = -K * t * e^(-r*t) * N(-d2)
```

Interpretation: Change in option price for 1% change in interest rate

## Implementation Notes for SrcProfit

### Use Apache Commons Math for Normal Distribution
```java
import org.apache.commons.math3.distribution.NormalDistribution;

NormalDistribution normal = new NormalDistribution();
double cumulativeNormal = normal.cumulativeProbability(d1);
double densityNormal = normal.density(d1);
```

### Precision Requirements
- All calculations: Use `BigDecimal` with scale of 10
- Final Greeks: Round to 4 decimal places
- Option prices: Round to 2 decimal places (cents)

### Handle Edge Cases
- **Time to Expiration Close to Zero**: Use small positive value (avoid division by zero)
- **Extreme Moneyness**: Use approximations for very deep in/out of money options
- **Dividend-Paying Stocks**: Adjust S by present value of dividends

### Annual vs Daily Values
- Greeks are calculated annualized
- Daily theta = Annual theta / 365
- Vega change = 1% volatility change
