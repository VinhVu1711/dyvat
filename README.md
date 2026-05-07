<div align="center">

# Dyvat

**Android inventory & sales management app for small retail stores**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.09-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Supabase](https://img.shields.io/badge/Supabase-2.5.0-3ECF8E?logo=supabase&logoColor=white)](https://supabase.com)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-API%2026-brightgreen)](https://developer.android.com/about/versions/oreo)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

</div>

---

Dyvat helps small shop owners manage their entire business in one app — track stock by batch, record sales with real-time inventory validation, and view revenue and profit summaries by month or year.

## Features

| Module | Description |
|---|---|
| **Products** | List, search and filter by category/supplier, add/edit, discontinue |
| **Purchase** | Multi-line purchase tickets, each ticket creates an inventory batch, cancel before any sales |
| **Inventory** | View stock by batch, per-product detail per batch, hide empty batches |
| **Sales** | Multi-line sale tickets, select exact batch per product, prevent overselling, auto-restore stock on cancel |
| **Statistics** | Revenue · profit · purchase cost · COGS summary by month or year |
| **Catalog** | CRUD for product categories, units of measure, suppliers |

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.09.00) + Material 3 |
| Backend / Database | Supabase PostgreSQL |
| Auth | Supabase Auth (Google Sign-In) |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Navigation | Jetpack Navigation Compose |
| Async | Kotlin Coroutines + Flow |
| Serialization | Kotlinx Serialization |

## Architecture

```
com.vinh.dyvat/
├── data/
│   ├── model/       # DTOs, domain models, form states
│   ├── repository/  # Data access layer (8 repositories)
│   └── remote/      # SupabaseClient, table/view name constants
├── di/              # Hilt modules
└── ui/
    ├── components/  # Shared composables
    ├── navigation/  # NavHost + Screen routes
    ├── screens/     # One package per business module
    └── theme/       # Material 3 dark theme
```

Key decisions:
- Repositories call Supabase Postgrest directly — no Room or local cache layer.
- ViewModels expose `StateFlow<UiState>` as a sealed class: `Loading / Success / Empty / Error`.
- Inventory has no dedicated table — each `purchase_items` row doubles as a batch-level stock record.
- Stock deduction and restoration on cancel are handled by database triggers, not client code.

## Database

```
categories        → Product categories
units             → Units of measure
suppliers         → Suppliers
products          → Products
purchase_tickets  → Purchase tickets (ticket code = batch ID)
purchase_items    → Purchase lines / batch-level stock
sale_tickets      → Sale tickets
sale_items        → Sale lines (FK → purchase_item batch)
```

Supporting views: `v_purchase_ticket_cards` · `v_sale_ticket_cards` · `v_inventory_lot_cards` · `v_inventory_lot_details` · `v_daily_business_summary`

Row Level Security is enabled on all tables — `owner_id = auth.uid()`.

## Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17+
- A Supabase account (free tier is sufficient)

### Setup

**1. Clone the repository**

```bash
git clone https://github.com/VuNguyenVinh/dyvat.git
cd dyvat
```

**2. Create a Supabase project**

1. Create a new project at [supabase.com](https://supabase.com)
2. Run `supabase/schema.sql` in the SQL Editor to create tables, views, triggers, and RLS policies
3. Copy your `Project URL` and `anon public key` from **Settings → API**

**3. Configure `local.properties`**

```properties
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
SUPABASE_URL=https://your-project-id.supabase.co
SUPABASE_PUBLISHABLE_KEY=your-anon-public-key
```

These values are injected at build time via `BuildConfig`. Do not hardcode them in source.

**4. Configure Google Sign-In**

1. Create an OAuth 2.0 Client ID in the [Google Cloud Console](https://console.cloud.google.com)
2. Add your keystore SHA-1 fingerprint to the Cloud Console
3. Enable the Google provider in Supabase under **Authentication → Providers → Google**
4. Enter the Client ID and Client Secret in Supabase

**5. Build and run** on a device or emulator (API 26+)

## License

MIT © 2025 VuNguyenVinh
