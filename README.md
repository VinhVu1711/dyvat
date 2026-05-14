<div align="center">

# Dyvat

**A modern Android inventory, purchasing, sales, and reporting app for small retail stores.**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.09.00-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Supabase](https://img.shields.io/badge/Supabase%20Kotlin-2.5.0-3ECF8E?logo=supabase&logoColor=white)](https://supabase.com)
[![Android](https://img.shields.io/badge/Android-API%2026%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

</div>

## Overview

Dyvat helps grocery and small retail operators manage daily business workflows from a single Android app: product catalogs, supplier purchases, lot-based inventory, sales tickets, stock validation, and financial reporting.

The app is built with Kotlin, Jetpack Compose, and Supabase. Business-critical inventory rules are enforced in both the client and the database so the UI catches mistakes early while PostgreSQL triggers protect stock consistency.

## Core Features

| Area             | Capabilities                                                                                                                  |
| ---------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| Authentication   | Google Sign-In through Android Credential Manager and Supabase Auth                                                           |
| Products         | Create, edit, search, filter, deactivate, restore, and view product details                                                   |
| Product Import   | Import `.xlsx` catalogs with sheets for categories, units, suppliers, and products                                            |
| Purchases        | Create multi-line purchase tickets, assign suppliers per line, validate prices, expiry dates, and quantities                  |
| Inventory        | Track stock by purchase lot, view lot details, hide empty lots, and surface expired or out-of-stock states                    |
| Sales            | Create multi-line sale tickets, select exact lots, prevent overselling, block expired lots, and restore stock when cancelling |
| Statistics       | Review monthly or yearly revenue, purchase cost, cost of goods sold, profit, and ticket counts                                |
| Excel Export     | Export detailed purchase and sales report data for the selected reporting period                                              |
| Catalog Settings | Manage categories, units, and suppliers with soft deactivate/restore workflows                                                |

## Business Rules

- Each purchase item is treated as an inventory lot.
- A sale item must reference an available purchase lot.
- Sale quantity must be greater than zero and cannot exceed the remaining stock of the selected lot.
- Multiple sale lines using the same lot are validated together to prevent aggregate overselling.
- Expired lots remain visible for transparency but cannot be selected for sale.
- A sale date cannot be earlier than the purchase date of the selected lot.
- Purchase price, sale price, and product default prices must be greater than zero.
- Product sale price must be greater than or equal to product purchase price.
- Product names are checked for duplicates against the database, not only the current UI page.
- Purchase expiry date must be after the purchase date.
- Purchase tickets can be cancelled only when their stock has not been sold.
- Sale ticket cancellation restores stock through database triggers.
- Categories, units, and suppliers are soft-deactivated to preserve historical records.
- Deactivating a catalog item warns when active products still reference it.

## Tech Stack

| Layer                | Technology                                           |
| -------------------- | ---------------------------------------------------- |
| Language             | Kotlin 2.0.21                                        |
| Android              | Compile SDK 36, min SDK 26, target SDK 35            |
| UI                   | Jetpack Compose, Material 3, Compose BOM 2024.09.00  |
| Architecture         | MVVM with repository-based data access               |
| Dependency Injection | Hilt                                                 |
| Navigation           | Navigation Compose                                   |
| Async                | Kotlin Coroutines and Flow                           |
| Backend              | Supabase PostgreSQL, Auth, PostgREST, Edge Functions |
| Networking           | Supabase Kotlin SDK, Ktor Android client             |
| Serialization        | Kotlinx Serialization                                |
| Sign-In              | Android Credential Manager and Google ID             |

## Project Structure

```text
.
+-- app/
|   +-- src/main/java/com/vinh/dyvat/
|       +-- data/
|       |   +-- model/          # DTOs, UI-facing models, import/export models
|       |   +-- remote/         # Supabase table and view constants
|       |   +-- repository/     # Supabase data access and business operations
|       +-- di/                 # Hilt modules
|       +-- ui/
|           +-- components/     # Reusable Compose UI components
|           +-- navigation/     # NavHost, routes, bottom navigation
|           +-- screens/        # Feature screens and ViewModels
|           +-- theme/          # Material 3 theme and colors
+-- supabase/
    +-- schema.sql              # Tables, views, triggers, RLS policies, RPCs
    +-- functions/
        +-- import-products-xlsx/
            +-- index.ts        # Excel import validation and commit function
```

## Database Design

Dyvat stores operational data in Supabase PostgreSQL with Row Level Security enabled for user-owned records.

Main tables:

- `categories`
- `units`
- `suppliers`
- `products`
- `purchase_tickets`
- `purchase_items`
- `sale_tickets`
- `sale_items`

Supporting views:

- `v_purchase_ticket_cards`
- `v_sale_ticket_cards`
- `v_inventory_lot_cards`
- `v_inventory_lot_details`
- `v_daily_business_summary`
- `v_purchase_export_details`
- `v_sale_export_details`

Database logic:

- `handle_sale_item_stock()` deducts and adjusts stock when sale items change.
- `restore_stock_when_sale_cancelled()` restores inventory when a sale ticket is cancelled.
- `prevent_purchase_cancel_if_sold()` blocks cancellation of purchase tickets with sold stock.
- `import_products_from_payload(jsonb)` imports validated catalog data atomically.

## Getting Started

### Prerequisites

- Android Studio with JDK 17
- Android device or emulator running API 26+
- Supabase project
- Google Cloud OAuth client for Google Sign-In
- Supabase CLI, only required when deploying the Edge Function from your machine

### 1. Clone the Repository

```bash
git clone https://github.com/VinhVu1711/dyvat.git
cd dyvat
```

### 2. Create the Supabase Database

Open your Supabase project SQL Editor and run:

```sql
-- Paste and execute the contents of supabase/schema.sql
```

This creates the database tables, views, triggers, RLS policies, and product import RPC.

### 3. Deploy the Product Import Edge Function

Deploy this function if you want the Excel product import screen to work:

```bash
supabase functions deploy import-products-xlsx
```

The Android app calls:

```text
{SUPABASE_URL}/functions/v1/import-products-xlsx/validate
{SUPABASE_URL}/functions/v1/import-products-xlsx/commit
```

### 4. Configure Local Android Secrets

Create or update `local.properties` at the project root:

```properties
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
supabase.url=https://your-project-id.supabase.co
supabase.anonKey=your-anon-or-publishable-key
```

These values are injected into `BuildConfig` at build time. Do not hardcode Supabase secrets in source files.

### 5. Configure Google Sign-In

1. Create OAuth clients in Google Cloud Console.
2. Configure the Android client with the app package and SHA-1 fingerprint.
3. Configure the Web client ID used by Credential Manager.
4. Enable Google provider in Supabase Authentication.
5. Add the Google client ID and secret to Supabase.
6. Update `app/src/main/res/values/strings.xml` if your OAuth client IDs differ.

### 6. Run the App

Open the project in Android Studio, sync Gradle, then run the app on an emulator or physical device.

## Excel Product Import Format

The import file must be an `.xlsx` workbook with these sheets:

| Sheet         | Purpose                                                                |
| ------------- | ---------------------------------------------------------------------- |
| `LoaiSanPham` | Product categories                                                     |
| `DonViTinh`   | Units of measure                                                       |
| `NhaCungCap`  | Suppliers                                                              |
| `SanPham`     | Products with category, unit, supplier, purchase price, and sale price |

The Edge Function validates the workbook before committing data. Invalid files return row-level errors and no data is inserted.

## Validation Checklist

Use this checklist after changes to business validation:

- Product default purchase price and sale price equal to zero must be rejected.
- Product sale price lower than purchase price must be rejected.
- Duplicate product names must be rejected even when the duplicate is not visible on the current paginated screen.
- Purchase lines without a supplier must be rejected.
- Purchase price equal to zero must be rejected.
- Expiry date before or equal to purchase date must be rejected.
- Expired sale lots must appear after valid lots and must not be selectable.
- Sale date earlier than the selected lot purchase date must be rejected.
- Sale price equal to zero must be rejected.
- Multiple sale lines using the same lot must be rejected when their total quantity exceeds remaining stock.
- Deactivating a category, unit, or supplier referenced by active products must show an impact warning.
- Statistics Excel export should include purchase and sale details for the selected reporting period.

## Development Notes

- `local.properties` is machine-specific and should not be committed.
- Inventory consistency is protected by PostgreSQL triggers; avoid duplicating stock mutation logic in the Android client.
- The app uses soft status changes for business records so historical tickets and reports remain stable.
- Supabase RLS policies scope data by `owner_id = auth.uid()`.

## License

MIT License. Copyright (c) 2025 VuNguyenVinh.
