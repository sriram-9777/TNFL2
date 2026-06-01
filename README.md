# TNFL2 — Liquor Management Mobile App

> A comprehensive Android mobile application for managing liquor store operations including sales, purchases, inventory, expenses, accounts, and membership programs.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Features & Screens](#features--screens)
- [Architecture](#architecture)
- [API Reference](#api-reference)
- [Data Models](#data-models)
- [Theming & Dark Mode](#theming--dark-mode)
- [Build & Run](#build--run)
- [Configuration](#configuration)

---

## Overview

**TNFL2** (Tamil Nadu Foreign Liquor 2) is a business management application designed for liquor retail stores. It provides a complete suite of tools for:

- **Daily Sales Tracking** — Record, draft, and confirm daily sales with product-level breakdowns
- **Purchase Management** — Track purchase orders, drafts, and supplier billing
- **Inventory Control** — Monitor stock levels across all product categories (Whisky, Rum, Brandy, Vodka, Wine, Beer)
- **Expense Reporting** — Track and categorize business expenses with date filtering
- **Account Management** — Search sales by invoice and view detailed transaction breakdowns
- **Member Management** — Customer membership program with tier-based tracking
- **Dashboard Analytics** — Overview of investment, stock distribution, and business performance

---

## Tech Stack

| Component | Technology |
|---|---|
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Min SDK** | 24 (Android 7.0) |
| **Target SDK** | 36 (Android 16) |
| **Networking** | Retrofit 2 + OkHttp |
| **JSON Parsing** | Gson |
| **Architecture** | MVVM (ViewModel + Compose State) |
| **Build System** | Gradle (Kotlin DSL) |
| **Backend API** | Heroku (REST) |

---

## Project Structure

```
app/src/main/java/com/tnfl2/v2/
│
├── MainActivity.kt              # Entry point — initializes theme and sets up root composition
├── MainScreen.kt                # Main scaffold with navigation drawer, top bar, and screen routing
├── NavigationDrawer.kt          # Side navigation menu with all screen entries
├── Screens.kt                   # All screen composables (Dashboard, Products, Sales, etc.)
├── LoginScreen.kt               # Authentication screen with email/password login
├── AddSaleScreen.kt             # Add/edit sale screen with product list and expense tracking
├── AddPurchaseScreen.kt         # Add purchase screen with product selection and billing
├── AccountsScreen.kt            # Account search and sales detail viewer
├── PurchasesScreen.kt           # Purchase history with expandable details
├── SaleDetailScreen.kt          # Read-only view of sale details (from Accounts)
├── SessionManager.kt            # Global state: auth token, drafts, global loading counter
├── ThemeManager.kt              # Global theme state (light/dark mode toggle)
│
├── network/
│   ├── ApiService.kt            # Retrofit API interface + all data models
│   ├── AuthRepository.kt        # Repository layer wrapping API calls with loading state
│   └── SharedNetwork.kt         # Singleton OkHttpClient + Retrofit instance with connection pooling
│
├── ui/
│   ├── components/
│   │   ├── GlobalLoader.kt      # Full-screen translucent loading overlay
│   │   └── MonthYearPicker.kt   # Custom month/year date picker dialog
│   └── theme/
│       ├── Color.kt             # Color palette definitions (light & dark)
│       ├── Theme.kt             # Material 3 theme setup (V2Theme)
│       ├── Type.kt              # Typography configuration
│       └── GradientBackground.kt # Gradient background composable
│
└── viewmodel/
    ├── AccountsViewModel.kt     # ViewModel for Accounts screen
    ├── AddPurchaseViewModel.kt  # ViewModel for Add Purchase screen
    └── PurchasesViewModel.kt    # ViewModel for Purchases screen
```

---

## Features & Screens

### 1. Login Screen
- Email/password authentication against the backend API
- JWT token-based session management (access + refresh tokens)
- Light/dark mode toggle icon in the top-right corner
- Branded UI with TNFL2 logo, gradient background, and animated decorative circles
- "Remember Me" checkbox and "Forgot Password" link

### 2. Dashboard
- **Investment overview** — Total capital invested displayed prominently
- **Stock distribution** — Category-wise unit breakdown (Whisky, Rum, Brandy, Vodka, Wine, Beer) shown as colored cards
- **Quick stats** — Last recorded quantity and last expense amount
- **Date range filtering** — Filter dashboard data by This Week, This Month, or custom date range
- **Stock details** — Expandable bottom sheet showing detailed stock information

### 3. Products
- **Product listing** — Grid/list view of all products with SKU, brand, category, stock, and pricing
- **Search & filter** — Filter products by name, category, or stock status
- **Metric cards** — Total products count and total stock value
- **Add Product** — Form to add new products with SKU, brand, category, size, stock, purchase price, sale price, and profit calculation

### 4. Sales
- **Sales history** — List of all recorded sales with invoice number, total amount, profit, and timestamps
- **KPI cards** — Total sales, total profit, digital payments, and cash settlement summaries
- **Date range filtering** — Filter sales by custom date ranges
- **Sale details dialog** — Tap any sale to view full product-level breakdown
- **Add Sale** — Multi-step form:
  - Product list with opening stock, closing stock, and auto-calculated sales
  - Expense entry (reason + amount)
  - Payment breakdown (cash, digital, kitchen sales)
  - Draft save & final confirmation

### 5. Purchases
- **Purchase history** — List of all purchase orders with bill number, date, total amount, and unit count
- **Expandable details** — Tap to view individual product lines (SKU, opening qty, purchase qty, purchase price)
- **Search** — Filter by bill number
- **Add Purchase** — Form with:
  - Product selection from master list
  - Quantity and pricing entry per product
  - Draft save and final submission

### 6. Expenses
- **Expense report** — Categorized list of expenses with total amounts
- **KPI summary** — Total expenses, average per entry, and highest single expense
- **Date range filtering** — Filter by This Week, This Month, or custom dates
- **Visual cards** — Each expense displayed with category icon and formatted amount

### 7. Members
- **Member listing** — Customer membership directory with name, phone, tier, total spend, and points
- **Search** — Filter members by name or phone number
- **Tier badges** — Visual tier indicators (Gold, Silver, Bronze, etc.)
- **Summary stats** — Total members count and aggregate spend

### 8. Accounts
- **Invoice search** — Search sales by invoice number
- **Sale detail view** — Full read-only breakdown of any sale including:
  - Product list with quantities and pricing
  - Payment method breakdown
  - Expense list
  - Settlement details

### 9. Member Sales
- **Sales analytics** — Aggregated view of all sales with period-wise breakdown
- **Search** — Filter by customer name or invoice number
- **Debug logging** — Built-in API endpoint scanner for data discovery

### 10. Settings
- **Theme toggle** — Switch between light and dark mode
- **App info** — Version information and app preferences

---

## Architecture

### Overview

```
┌─────────────────────────────────────────────┐
│                 MainActivity                 │
│         (ThemeManager + V2Theme)             │
├─────────────────────────────────────────────┤
│                 MainScreen                   │
│    (Scaffold + NavigationDrawer + Routing)   │
├─────────┬───────────┬───────────┬───────────┤
│Dashboard│ Products  │  Sales    │  Others   │
│ Screen  │  Screen   │  Screen   │  ...      │
├─────────┴───────────┴───────────┴───────────┤
│              ViewModels (MVVM)               │
│  AccountsVM │ PurchasesVM │ AddPurchaseVM    │
├─────────────────────────────────────────────┤
│            AuthRepository (Singleton)        │
├─────────────────────────────────────────────┤
│         SharedNetwork (OkHttp + Retrofit)    │
│         Connection Pool (5 conns, 2 min)     │
├─────────────────────────────────────────────┤
│          Heroku Backend REST API             │
│   https://tnfl2-cb6ea45c64b3.herokuapp.com  │
└─────────────────────────────────────────────┘
```

### Key Design Patterns

| Pattern | Implementation |
|---|---|
| **Singleton Network** | `SharedNetwork` object holds one `OkHttpClient` + `Retrofit` instance for the entire app |
| **Singleton Repository** | `AuthRepository` uses `operator fun invoke()` to always return the same instance |
| **Global Loading** | `SessionManager.isGlobalLoading` tracks a reference count; `GlobalLoader` composable shows/hides overlay |
| **Global Theme** | `ThemeManager.isDark` is a Compose `mutableStateOf` at object level — any composable reading it auto-recomposes |
| **CompositionLocal** | `LocalThemeIsDark` provides theme state down the composition tree |
| **State Hoisting** | Screen-level state (lists, loading, search) is hoisted in composable scope with `remember` |

### Session Management

- `SessionManager` is a global `object` holding:
  - `authToken` — JWT access token
  - `draftClosingStocks` — Mutable map for in-progress sale draft data
  - `expenseReason` / `expenseAmount` — Temporary expense entry data
  - `loadingCount` — Reference counter for the global loading overlay

---

## API Reference

**Base URL:** `https://tnfl2-cb6ea45c64b3.herokuapp.com`

All authenticated endpoints require the header:
```
Authorization: Bearer <accessToken>
```

---

### 1. Authentication

#### `POST /services/auth/login`

Login with email and password. Returns JWT tokens.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "yourpassword"
}
```

**Response Body:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

| Field | Type | Description |
|---|---|---|
| `accessToken` | `String` | JWT access token for authenticating API requests |
| `refreshToken` | `String` | JWT refresh token for obtaining new access tokens |

---

### 2. Dashboard

#### `GET /services/dashboard`

Get investment amount and stock unit breakdown by category.

**Headers:** `Authorization: Bearer <token>`

**Response Body:**
```json
{
  "investmentAmount": "1500000",
  "totalUnits": {
    "BRANDY": 120.0,
    "VODKA": 85.0,
    "WINE": 45.0,
    "RUM": 200.0,
    "WHISKY": 310.0,
    "BEER": 500.0
  }
}
```

| Field | Type | Description |
|---|---|---|
| `investmentAmount` | `String` | Total capital invested (in INR) |
| `totalUnits.BRANDY` | `Double` | Total brandy units in stock |
| `totalUnits.VODKA` | `Double` | Total vodka units in stock |
| `totalUnits.WINE` | `Double` | Total wine units in stock |
| `totalUnits.RUM` | `Double` | Total rum units in stock |
| `totalUnits.WHISKY` | `Double` | Total whisky units in stock |
| `totalUnits.BEER` | `Double` | Total beer units in stock |

---

#### `GET /services/dashboard/performance`

Get monthly sales performance data for a date range.

**Headers:** `Authorization: Bearer <token>`

**Query Parameters:**

| Param | Type | Description |
|---|---|---|
| `startDate` | `Long` | Start date (Unix epoch seconds) |
| `endDate` | `Long` | End date (Unix epoch seconds) |

**Response Body:**
```json
{
  "monthlySales": [
    {
      "profitAmount": 45000.0,
      "_id": 1717200000
    }
  ]
}
```

| Field | Type | Description |
|---|---|---|
| `monthlySales[].profitAmount` | `Double` | Total profit for the month |
| `monthlySales[]._id` | `Long` | Month identifier (epoch timestamp) |

---

### 3. Sales

#### `GET /services/sales`

Get all sales within a date range.

**Headers:** `Authorization: Bearer <token>`

**Query Parameters:**

| Param | Type | Description |
|---|---|---|
| `startDate` | `Long` | Start date (Unix epoch seconds) |
| `endDate` | `Long` | End date (Unix epoch seconds) |

**Response Body:**
```json
{
  "data": [
    {
      "invoiceNumber": "INV-2024-001",
      "totalSalesAmount": 25000.0,
      "profitAmount": 8500.0,
      "totalExpensesAmount": 1200.0,
      "finalCashSettlement": 15000.0,
      "totalDigitalAmount": 10000.0,
      "basePrice": 16500.0,
      "kitchenSales": 3000.0,
      "timeCreatedAt": 1717200000,
      "_id": "665abc123def456",
      "customerName": "John Doe",
      "memberName": null,
      "customerPhone": "9876543210"
    }
  ]
}
```

| Field | Type | Description |
|---|---|---|
| `invoiceNumber` | `String` | Unique invoice identifier |
| `totalSalesAmount` | `Double` | Total revenue from the sale |
| `profitAmount` | `Double` | Net profit amount |
| `totalExpensesAmount` | `Double` | Total expenses recorded for this sale |
| `finalCashSettlement` | `Double` | Cash settlement amount |
| `totalDigitalAmount` | `Double` | Digital/online payment amount |
| `basePrice` | `Double` | Base cost of goods sold |
| `kitchenSales` | `Double` | Kitchen/food sales amount (default: 0) |
| `timeCreatedAt` | `Long` | Creation timestamp (epoch seconds) |
| `_id` | `Any?` | MongoDB document ID |
| `customerName` | `String?` | Customer name (nullable) |
| `memberName` | `String?` | Member name (nullable) |
| `customerPhone` | `String?` | Customer phone number (nullable) |

---

#### `GET /services/sales/id`

Get a single sale by its ID with full product-level detail.

**Headers:** `Authorization: Bearer <token>`

**Query Parameters:**

| Param | Type | Description |
|---|---|---|
| `id` | `String` | Sale document ID |

**Response Body:**
```json
{
  "status": "success",
  "data": {
    "invoiceNumber": "INV-2024-001",
    "totalSalesAmount": 25000.0,
    "profitAmount": 8500.0,
    "totalExpensesAmount": 1200.0,
    "finalCashSettlement": 15000.0,
    "totalDigitalAmount": 10000.0,
    "basePrice": 16500.0,
    "kitchenSales": 3000.0,
    "cashInHand": 5000.0,
    "saleDate": 1717200000,
    "timeCreatedAt": 1717200000,
    "openingPettyCash": 2000.0,
    "diffSettlement": 0.0,
    "isStoreRoomAvailable": true,
    "shopNumber": "SHOP-01",
    "_id": "665abc123def456",
    "payments": {
      "cash": 15000,
      "upi": 8000,
      "card": 2000
    },
    "expenseList": [],
    "productList": [
      {
        "SKU": "WHISKY-JW-750",
        "openingStock": 50,
        "purchaseStock": 0,
        "stock": 50,
        "closingStock": 45,
        "sales": 5,
        "salePrice": 2500.0,
        "totalSaleAmount": 12500.0,
        "category": "WHISKY"
      }
    ]
  }
}
```

| Field | Type | Description |
|---|---|---|
| `data.cashInHand` | `Double` | Cash in hand at close |
| `data.saleDate` | `Long` | Sale date (epoch seconds) |
| `data.openingPettyCash` | `Double` | Opening petty cash amount |
| `data.diffSettlement` | `Double` | Difference in settlement |
| `data.isStoreRoomAvailable` | `Boolean` | Store room availability flag |
| `data.shopNumber` | `String` | Shop identifier |
| `data.payments` | `Map<String, Any>` | Payment breakdown by method |
| `data.expenseList` | `List<Any>` | List of expense entries |
| `data.productList[]` | `DraftProduct[]` | Product-level sales breakdown |

---

#### `GET /services/sales/draft`

Get the current saved sales draft.

**Headers:** `Authorization: Bearer <token>`

**Response Body:**
```json
{
  "data": {
    "productList": [...],
    "expenseList": [],
    "payments": {},
    "totalSalesAmount": 0.0,
    "totalExpensesAmount": 0.0,
    "totalDigitalAmount": 0.0,
    "finalCashSettlement": 0.0,
    "saleDate": 1717200000,
    "openingPettyCash": 0,
    "kitchenSales": 0,
    "cashInHand": 0,
    "closingPettyCash": null,
    "diffSettlement": 0,
    "isStoreRoomAvailable": false
  }
}
```

---

#### `POST /services/sales/draft`

Save a sales draft (work in progress).

**Headers:** `Authorization: Bearer <token>`

**Request Body:** Same structure as `ConfirmSaleRequest` (see below)

**Response Body:**
```json
{
  "status": "success"
}
```

---

#### `POST /services/sales`

Confirm and submit a finalized sale.

**Headers:** `Authorization: Bearer <token>`

**Request Body (`ConfirmSaleRequest`):**
```json
{
  "productList": [
    {
      "SKU": "WHISKY-JW-750",
      "openingStock": 50,
      "purchaseStock": 0,
      "stock": 50,
      "closingStock": 45,
      "sales": 5,
      "salePrice": 2500.0,
      "totalSaleAmount": 12500.0,
      "category": "WHISKY"
    }
  ],
  "expenseList": [],
  "payments": { "cash": 15000, "upi": 8000 },
  "totalSalesAmount": 25000.0,
  "totalExpensesAmount": 1200.0,
  "totalDigitalAmount": 10000.0,
  "finalCashSettlement": 15000.0,
  "saleDate": 1717200000,
  "openingPettyCash": 2000,
  "kitchenSales": 3000,
  "cashInHand": 5000,
  "closingPettyCash": null,
  "diffSettlement": 0,
  "isStoreRoomAvailable": true,
  "id": null
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `productList` | `DraftProduct[]` | Yes | List of products with stock and sales data |
| `expenseList` | `List<Any>` | Yes | Expense entries for the day |
| `payments` | `Map<String, Any>` | Yes | Payment breakdown by method (cash, upi, card, etc.) |
| `totalSalesAmount` | `Double` | Yes | Total revenue |
| `totalExpensesAmount` | `Double` | Yes | Total expenses |
| `totalDigitalAmount` | `Double` | Yes | Total digital payments |
| `finalCashSettlement` | `Double` | Yes | Final cash settled |
| `saleDate` | `Long` | Yes | Sale date (epoch seconds) |
| `openingPettyCash` | `Long` | Yes | Opening petty cash |
| `kitchenSales` | `Int` | Yes | Kitchen/food sales |
| `cashInHand` | `Int` | Yes | Cash in hand at close |
| `closingPettyCash` | `Int?` | No | Closing petty cash (nullable) |
| `diffSettlement` | `Int` | Yes | Settlement difference |
| `isStoreRoomAvailable` | `Boolean` | Yes | Store room availability |
| `id` | `String?` | No | Sale ID (for updates only) |

---

#### `PUT /services/sales`

Update an existing sale record.

**Headers:** `Authorization: Bearer <token>`

**Request Body:** Same as `ConfirmSaleRequest` above (with `id` field set)

**Response Body:**
```json
{
  "status": "success"
}
```

---

### 4. Products

#### `GET /services/productmaster`

Get the full product master list.

**Headers:** `Authorization: Bearer <token>`

**Response Body:**
```json
{
  "productList": [
    {
      "SKU": "WHISKY-JW-750",
      "brand": "Johnnie Walker",
      "category": "WHISKY",
      "stock": 45,
      "openingStock": 50,
      "closingStock": 45,
      "salePrice": 2500.0,
      "purchasePrice": 1800.0,
      "profitAmount": 700.0
    }
  ]
}
```

| Field | Type | Description |
|---|---|---|
| `SKU` | `String` | Stock Keeping Unit identifier |
| `brand` | `String` | Brand name |
| `category` | `String` | Product category (WHISKY, RUM, BRANDY, VODKA, WINE, BEER) |
| `stock` | `Int` | Current stock quantity |
| `openingStock` | `Int` | Opening stock count |
| `closingStock` | `Int` | Closing stock count |
| `salePrice` | `Double` | Selling price per unit (INR) |
| `purchasePrice` | `Double` | Purchase/cost price per unit (INR) |
| `profitAmount` | `Double` | Profit per unit (salePrice - purchasePrice) |

---

#### `POST /services/productmaster`

Add a new product to the master list.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "SKU": "RUM-OLD-750",
  "brand": "Old Monk",
  "category": "RUM",
  "details": [
    {
      "size": "750ml",
      "stock": 100,
      "purchasePrice": 350.0,
      "salePrice": 500.0,
      "profitAmount": 150.0
    }
  ]
}
```

| Field | Type | Description |
|---|---|---|
| `SKU` | `String` | Unique product identifier |
| `brand` | `String` | Brand name |
| `category` | `String` | Product category |
| `details[]` | `ProductDetailItem[]` | Variant details (size, stock, pricing) |
| `details[].size` | `String` | Bottle/pack size |
| `details[].stock` | `Int` | Initial stock quantity |
| `details[].purchasePrice` | `Double` | Cost price |
| `details[].salePrice` | `Double` | Selling price |
| `details[].profitAmount` | `Double` | Profit per unit |

---

### 5. Purchases

#### `GET /services/purchase`

Get all purchase records.

**Headers:** `Authorization: Bearer <token>`

**Response Body:**
```json
{
  "data": [
    {
      "billNumber": "PUR-2024-001",
      "purchaseDate": 1717200000,
      "billTotalAmount": 50000.0,
      "billTotalUnits": 200,
      "timeCreatedAt": 1717200000,
      "purchaseList": [
        {
          "SKU": "WHISKY-JW-750",
          "openingQty": 45,
          "purchaseQty": 24,
          "purchasePrice": 1800.0,
          "lineNumber": "1"
        }
      ]
    }
  ]
}
```

| Field | Type | Description |
|---|---|---|
| `billNumber` | `String` | Purchase bill number |
| `purchaseDate` | `Long` | Purchase date (epoch seconds) |
| `billTotalAmount` | `Double` | Total bill amount (INR) |
| `billTotalUnits` | `Int` | Total units purchased |
| `timeCreatedAt` | `Long` | Record creation timestamp |
| `purchaseList[]` | `PurchaseProduct[]` | Line-item product details |
| `purchaseList[].openingQty` | `Int` | Stock before purchase |
| `purchaseList[].purchaseQty` | `Int` | Quantity purchased |
| `purchaseList[].purchasePrice` | `Double` | Unit purchase price |
| `purchaseList[].lineNumber` | `String` | Bill line number |

---

#### `GET /services/purchase/draft`

Get the saved purchase draft.

**Headers:** `Authorization: Bearer <token>`

**Response Body:**
```json
{
  "data": [
    {
      "billNo": "PUR-2024-002",
      "purchaseDate": 1717200000,
      "purchaseAmount": "45000",
      "totalQuantity": 150,
      "productList": [
        {
          "SKU": "RUM-OLD-750",
          "openingStock": 20,
          "purchaseStock": 48,
          "stock": 68,
          "purchaseAmount": 16800.0,
          "cases": 4
        }
      ]
    }
  ]
}
```

---

#### `POST /services/purchase/draft`

Save a purchase draft.

**Headers:** `Authorization: Bearer <token>`

**Request Body (`PurchaseDraftRequest`):**
```json
{
  "billNo": "PUR-2024-002",
  "purchaseDate": 1717200000,
  "purchaseAmount": "45000",
  "totalQuantity": 150,
  "productList": [
    {
      "SKU": "RUM-OLD-750",
      "openingStock": 20,
      "purchaseStock": 48,
      "stock": 68,
      "purchaseAmount": 16800.0,
      "cases": 4
    }
  ]
}
```

| Field | Type | Description |
|---|---|---|
| `billNo` | `String` | Bill number |
| `purchaseDate` | `Long` | Purchase date (epoch seconds) |
| `purchaseAmount` | `String` | Total purchase amount |
| `totalQuantity` | `Int` | Total units in purchase |
| `productList[]` | `PurchaseDraftProductItem[]` | Products in the purchase |
| `productList[].SKU` | `String` | Product identifier |
| `productList[].openingStock` | `Int` | Stock before purchase |
| `productList[].purchaseStock` | `Int` | Quantity being purchased |
| `productList[].stock` | `Int` | Stock after purchase |
| `productList[].purchaseAmount` | `Double` | Line total amount |
| `productList[].cases` | `Int` | Number of cases |

**Response Body:**
```json
{
  "status": "success"
}
```

---

#### `PUT /services/productmaster`

Submit a finalized purchase (updates stock in product master).

**Headers:** `Authorization: Bearer <token>`

**Request Body:** Same as `PurchaseDraftRequest` above

**Response:** Raw `ResponseBody`

---

### 6. Expenses

#### `GET /services/expenses/expensesReport`

Get expenses within a date range.

**Headers:** `Authorization: Bearer <token>`

**Query Parameters:**

| Param | Type | Description |
|---|---|---|
| `fromTime` | `Long` | Start date (Unix epoch seconds) |
| `toTime` | `Long` | End date (Unix epoch seconds) |

**Response Body:**
```json
{
  "data": [
    {
      "expenseDetail": "Electricity Bill",
      "totalAmount": "5000"
    },
    {
      "expenseDetail": "Staff Salary",
      "totalAmount": "15000"
    }
  ]
}
```

| Field | Type | Description |
|---|---|---|
| `expenseDetail` | `String` | Expense category/description |
| `totalAmount` | `String` | Total amount for this category (INR) |

---

### 7. Members

#### `GET /services/members`

Get all registered members.

**Headers:** `Authorization: Bearer <token>`

**Response Body:**
```json
{
  "data": [
    {
      "_id": "665abc123def456",
      "name": "Rajesh Kumar",
      "phone": "9876543210",
      "joinedDate": "2024-01-15",
      "tier": "Gold",
      "totalSpend": 125000.0,
      "points": 1250
    }
  ]
}
```

| Field | Type | Description |
|---|---|---|
| `_id` | `String?` | MongoDB document ID |
| `name` | `String` | Member name |
| `phone` | `String` | Phone number |
| `joinedDate` | `String?` | Date joined (nullable) |
| `tier` | `String` | Membership tier (Gold, Silver, Bronze, etc.) |
| `totalSpend` | `Double` | Total amount spent (INR) |
| `points` | `Int` | Loyalty points earned |

---

### 8. Exploratory Endpoints (Member Sales Screen)

These endpoints are used by the **Member Sales** screen to discover aggregated sales data. They return raw `ResponseBody` and are probed sequentially:

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/services/transactions` | Transaction history |
| `GET` | `/services/members/sales` | Sales by members |
| `GET` | `/services/customers/sales` | Sales by customers |
| `GET` | `/services/member-sales` | Member sales (alt) |
| `GET` | `/services/users/sales` | Sales by users |
| `GET` | `/services/clients/sales` | Sales by clients |
| `GET` | `/services/customers` | Customer list |
| `GET` | `/services/customer` | Single customer (alt) |
| `GET` | `/services/users` | User list |
| `GET` | `/services/clients` | Client list |

> **Note:** These endpoints return raw JSON and are parsed dynamically. Not all may be active on every backend deployment.


---

## Data Models

### Core Models

| Model | Fields | Description |
|---|---|---|
| `Product` | `sku`, `brand`, `category`, `stock`, `salePrice`, `purchasePrice`, `profitAmount`, `openingStock`, `closingStock` | Product master data |
| `SaleItem` | `invoiceNumber`, `totalSalesAmount`, `profitAmount`, `totalExpensesAmount`, `finalCashSettlement`, `totalDigitalAmount`, `basePrice`, `kitchenSales`, `timeCreatedAt` | Sale summary record |
| `PurchaseItem` | `billNumber`, `purchaseDate`, `billTotalAmount`, `billTotalUnits`, `purchaseList` | Purchase record |
| `ExpenseItem` | `expenseDetail`, `totalAmount` | Expense line item |
| `Member` | `name`, `phone`, `tier`, `totalSpend`, `points`, `joinedDate` | Customer member |

### Request/Response Models

| Model | Used By | Description |
|---|---|---|
| `LoginRequest` / `LoginResponse` | Login | Email + password → access + refresh tokens |
| `ConfirmSaleRequest` | Add/Edit Sale | Complete sale submission payload |
| `PurchaseDraftRequest` | Add Purchase | Purchase draft/final submission |
| `AddProductRequest` | Add Product | New product with detail variants |
| `DashboardResponse` | Dashboard | Investment amount + stock units by category |

---

## Theming & Dark Mode

### How It Works

1. **`ThemeManager`** — A Kotlin `object` with a `mutableStateOf(false)` field `isDark`
2. **Initialization** — `MainActivity.onCreate()` reads `SharedPreferences("theme_prefs")` and initializes `ThemeManager.isDark`
3. **Toggle** — Calling `ThemeManager.toggle(prefs)` flips the boolean and persists to SharedPreferences
4. **Reactivity** — Any composable reading `ThemeManager.isDark` auto-recomposes when it changes
5. **`V2Theme`** — Wraps `MaterialTheme` with custom light/dark color schemes

### Color Palette

| Token | Light Mode | Dark Mode |
|---|---|---|
| Background | `#F8FAFC` (off-white) | `#0F172A` (deep navy) |
| Surface | `#FFFFFF` (white) | `#1E293B` (dark slate) |
| Primary | `#0F766E` (teal) | `#14B8A6` (bright teal) |
| On Background | `#0F172A` (dark) | `#F1F5F9` (light) |
| Brand Accent | `#B75C1C` (amber/brown) | `#B75C1C` (amber/brown) |

### Toggle Locations

- **Login Screen** — Sun/Moon icon in top-right corner
- **Main App** — Sun/Moon icon in the top app bar (all screens)
- **Settings Screen** — Theme toggle option

---

## Build & Run

### Prerequisites

- **Android Studio** (Latest stable — Ladybug or newer)
- **JDK 11+** (bundled with Android Studio)
- **Android SDK 36**
- **Gradle 9.1**

### Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Clean build
./gradlew clean assembleDebug
```

### Output

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`

### Install on Device

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Configuration

### Backend URL

The API base URL is configured in [`SharedNetwork.kt`](app/src/main/java/com/tnfl2/v2/network/SharedNetwork.kt):

```kotlin
.baseUrl("https://tnfl2-cb6ea45c64b3.herokuapp.com")
```

### Network Timeouts

Configured in [`SharedNetwork.kt`](app/src/main/java/com/tnfl2/v2/network/SharedNetwork.kt):

| Setting | Value |
|---|---|
| Connect Timeout | 15 seconds |
| Read Timeout | 20 seconds |
| Write Timeout | 20 seconds |
| Connection Pool | 5 connections, 2 min keepalive |
| Retry on Failure | Enabled |

### Theme Persistence

Theme preference is stored in `SharedPreferences` with key `"theme_prefs"` → `"is_dark_theme"` (boolean).

---

## License

Private — All rights reserved.
