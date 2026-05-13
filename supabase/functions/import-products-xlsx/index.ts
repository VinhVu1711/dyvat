import { createClient } from "npm:@supabase/supabase-js@2.45.4";
import * as XLSX from "npm:xlsx@0.18.5";

const MAX_FILE_BYTES = 10 * 1024 * 1024;
const MAX_PRODUCTS = 1000;
const MAX_TOTAL_ROWS = 2000;
const MAX_ERRORS = 500;
const TOKEN_TTL_MS = 30 * 60 * 1000;

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

type ImportError = {
  sheet: string;
  rowNumber: number;
  column: string;
  value: string;
  message: string;
  suggestion: string;
};

type ImportSummary = {
  categoriesToCreate: number;
  categoriesToReuse: number;
  unitsToCreate: number;
  unitsToReuse: number;
  suppliersToCreate: number;
  suppliersToReuse: number;
  productsToImport: number;
  errorCount: number;
  hasMoreErrors: boolean;
};

type SupplierInput = {
  name: string;
  phone: string | null;
};

type ProductInput = {
  name: string;
  categoryName: string;
  unitName: string;
  supplierName: string;
  defaultPurchasePriceVnd: number;
  defaultSalePriceVnd: number;
};

type ImportPayload = {
  categories: string[];
  units: string[];
  suppliers: SupplierInput[];
  products: ProductInput[];
};

type ImportTokenPayload = {
  userId: string;
  expiresAt: number;
  payload: ImportPayload;
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    if (req.method !== "POST") {
      return jsonResponse({ success: false, message: "Phương thức không được hỗ trợ" }, 405);
    }

    const url = new URL(req.url);
    const action = url.pathname.split("/").pop();
    const authHeader = req.headers.get("Authorization") ?? "";
    const supabase = createAuthedClient(authHeader);
    const { data: userData, error: userError } = await supabase.auth.getUser();
    const userId = userData.user?.id;

    if (userError || !userId) {
      return jsonResponse({ success: false, message: "Phiên đăng nhập không hợp lệ" }, 401);
    }

    if (action === "validate") {
      return await handleValidate(req, supabase, userId);
    }

    if (action === "commit") {
      return await handleCommit(req, supabase, userId);
    }

    return jsonResponse({ success: false, message: "Endpoint không hợp lệ" }, 404);
  } catch (error) {
    console.error(error);
    return jsonResponse(
      {
        success: false,
        message: error instanceof Error ? error.message : "Không thể xử lý file import",
      },
      500,
    );
  }
});

async function handleValidate(req: Request, supabase: ReturnType<typeof createAuthedClient>, userId: string) {
  const formData = await req.formData();
  const file = formData.get("file");

  if (!(file instanceof File)) {
    return jsonResponse({ success: false, message: "Vui lòng chọn file Excel .xlsx" }, 400);
  }

  if (!file.name.toLowerCase().endsWith(".xlsx")) {
    return jsonResponse({ success: false, message: "Chỉ hỗ trợ file .xlsx" }, 400);
  }

  if (file.size > MAX_FILE_BYTES) {
    return jsonResponse({ success: false, message: "File vượt quá giới hạn 10 MB" }, 400);
  }

  const workbook = XLSX.read(await file.arrayBuffer(), { type: "array", cellDates: false, raw: false });
  const parsed = parseWorkbook(workbook);
  const existing = await loadExistingLookups(supabase);
  const validation = validatePayload(parsed.payload, parsed.rowCounts, existing, parsed.errors);
  const summary = buildSummary(parsed.payload, existing, validation.errors.length, validation.hasMoreErrors);

  if (validation.errors.length > 0) {
    return jsonResponse({
      success: false,
      summary,
      errors: validation.errors,
      message: "File còn lỗi dữ liệu. Chưa có dữ liệu nào được nhập.",
    });
  }

  const importToken = await encodeImportToken({
    userId,
    expiresAt: Date.now() + TOKEN_TTL_MS,
    payload: parsed.payload,
  });

  return jsonResponse({
    success: true,
    summary,
    errors: [],
    importToken,
    message: "File hợp lệ và sẵn sàng import.",
  });
}

async function handleCommit(req: Request, supabase: ReturnType<typeof createAuthedClient>, userId: string) {
  const body = await req.json().catch(() => null);
  const importToken = body?.importToken;

  if (typeof importToken !== "string" || importToken.trim().length === 0) {
    return jsonResponse({ success: false, message: "Thiếu mã import. Vui lòng kiểm tra file lại." }, 400);
  }

  const decoded = await decodeImportToken(importToken);
  if (decoded.userId !== userId) {
    return jsonResponse({ success: false, message: "Mã import không thuộc phiên đăng nhập hiện tại" }, 403);
  }

  if (decoded.expiresAt < Date.now()) {
    return jsonResponse({ success: false, message: "Mã import đã hết hạn. Vui lòng kiểm tra lại file." }, 400);
  }

  const { data, error } = await supabase.rpc("import_products_from_payload", {
    import_payload: decoded.payload,
  });

  if (error) {
    console.error(error);
    return jsonResponse({
      success: false,
      message: error.message || "Import thất bại. Chưa có dữ liệu nào được nhập.",
    }, 400);
  }

  return jsonResponse({
    success: true,
    result: data,
    message: "Import sản phẩm thành công.",
  });
}

function parseWorkbook(workbook: XLSX.WorkBook): {
  payload: ImportPayload;
  rowCounts: Record<string, number>;
  errors: ImportError[];
} {
  const errors: ImportError[] = [];
  const requiredSheets = ["LoaiSanPham", "DonViTinh", "NhaCungCap", "SanPham"];

  for (const sheetName of requiredSheets) {
    if (!workbook.SheetNames.includes(sheetName)) {
      pushError(errors, sheetName, 1, "sheet", "", `Thiếu sheet ${sheetName}`, "Tạo đúng sheet theo template import.");
    }
  }

  const categories = workbook.SheetNames.includes("LoaiSanPham")
    ? parseSingleNameSheet(workbook, "LoaiSanPham", "ten_loai", errors)
    : [];
  const units = workbook.SheetNames.includes("DonViTinh")
    ? parseSingleNameSheet(workbook, "DonViTinh", "ten_don_vi", errors)
    : [];
  const suppliers = workbook.SheetNames.includes("NhaCungCap")
    ? parseSuppliersSheet(workbook, errors)
    : [];
  const products = workbook.SheetNames.includes("SanPham")
    ? parseProductsSheet(workbook, errors)
    : [];

  return {
    payload: {
      categories,
      units,
      suppliers,
      products,
    },
    rowCounts: {
      LoaiSanPham: categories.length,
      DonViTinh: units.length,
      NhaCungCap: suppliers.length,
      SanPham: products.length,
    },
    errors,
  };
}

function parseSingleNameSheet(
  workbook: XLSX.WorkBook,
  sheetName: string,
  headerName: string,
  errors: ImportError[],
): string[] {
  const rows = sheetRows(workbook, sheetName);
  const header = headerMap(rows[0] ?? []);
  if (!hasHeader(header, headerName)) {
    pushError(errors, sheetName, 1, headerName, "", `Thiếu cột ${headerName}`, "Đặt header đúng ở dòng đầu tiên.");
    return [];
  }

  const index = header.get(headerName)!;
  return rows.slice(1)
    .map((row, idx) => ({ rowNumber: idx + 2, value: cellText(row[index]) }))
    .filter(({ value }) => value.isNotBlank())
    .map(({ value }) => value);
}

function parseSuppliersSheet(workbook: XLSX.WorkBook, errors: ImportError[]): SupplierInput[] {
  const sheetName = "NhaCungCap";
  const rows = sheetRows(workbook, sheetName);
  const header = headerMap(rows[0] ?? []);
  for (const required of ["ten_nha_cung_cap", "so_dien_thoai"]) {
    if (!hasHeader(header, required)) {
      pushError(errors, sheetName, 1, required, "", `Thiếu cột ${required}`, "Đặt header đúng ở dòng đầu tiên.");
    }
  }
  if (!hasHeader(header, "ten_nha_cung_cap")) return [];

  const nameIndex = header.get("ten_nha_cung_cap")!;
  const phoneIndex = header.get("so_dien_thoai");

  return rows.slice(1)
    .map((row) => ({
      name: cellText(row[nameIndex]),
      phone: phoneIndex == null ? null : nullIfBlank(cellText(row[phoneIndex])),
    }))
    .filter((supplier) => supplier.name.isNotBlank() || supplier.phone?.isNotBlank());
}

function parseProductsSheet(workbook: XLSX.WorkBook, errors: ImportError[]): ProductInput[] {
  const sheetName = "SanPham";
  const rows = sheetRows(workbook, sheetName);
  const header = headerMap(rows[0] ?? []);
  const required = ["ten_san_pham", "loai_san_pham", "don_vi", "nha_cung_cap", "gia_nhap", "gia_ban"];
  for (const column of required) {
    if (!hasHeader(header, column)) {
      pushError(errors, sheetName, 1, column, "", `Thiếu cột ${column}`, "Đặt header đúng ở dòng đầu tiên.");
    }
  }
  if (required.some((column) => !hasHeader(header, column))) return [];

  return rows.slice(1)
    .map((row, index) => {
      const rowNumber = index + 2;
      const name = cellText(row[header.get("ten_san_pham")!]);
      const categoryName = cellText(row[header.get("loai_san_pham")!]);
      const unitName = cellText(row[header.get("don_vi")!]);
      const supplierName = cellText(row[header.get("nha_cung_cap")!]);
      const purchaseValue = cellText(row[header.get("gia_nhap")!]);
      const saleValue = cellText(row[header.get("gia_ban")!]);
      const purchasePrice = parseVnd(purchaseValue);
      const salePrice = parseVnd(saleValue);

      if (purchasePrice == null && purchaseValue.isNotBlank()) {
        pushError(errors, sheetName, rowNumber, "gia_nhap", purchaseValue, "Giá nhập không hợp lệ", "Chỉ nhập số VND lớn hơn 0.");
      }
      if (salePrice == null && saleValue.isNotBlank()) {
        pushError(errors, sheetName, rowNumber, "gia_ban", saleValue, "Giá bán không hợp lệ", "Chỉ nhập số VND lớn hơn 0.");
      }

      return {
        name,
        categoryName,
        unitName,
        supplierName,
        defaultPurchasePriceVnd: purchasePrice ?? -1,
        defaultSalePriceVnd: salePrice ?? -1,
      };
    })
    .filter((product) =>
      product.name.isNotBlank() ||
      product.categoryName.isNotBlank() ||
      product.unitName.isNotBlank() ||
      product.supplierName.isNotBlank()
    );
}

function validatePayload(
  payload: ImportPayload,
  rowCounts: Record<string, number>,
  existing: Awaited<ReturnType<typeof loadExistingLookups>>,
  initialErrors: ImportError[],
): { errors: ImportError[]; hasMoreErrors: boolean } {
  const errors = [...initialErrors];

  const totalRows = Object.values(rowCounts).reduce((sum, count) => sum + count, 0);
  if (totalRows > MAX_TOTAL_ROWS) {
    pushError(errors, "Workbook", 1, "total_rows", String(totalRows), "File có quá nhiều dòng", `Tổng số dòng tối đa là ${MAX_TOTAL_ROWS}.`);
  }
  if (payload.products.length > MAX_PRODUCTS) {
    pushError(errors, "SanPham", 1, "total_products", String(payload.products.length), "Có quá nhiều sản phẩm", `Tối đa ${MAX_PRODUCTS} sản phẩm mỗi lần import.`);
  }

  validateUniqueNames(payload.categories, "LoaiSanPham", "ten_loai", errors);
  validateUniqueNames(payload.units, "DonViTinh", "ten_don_vi", errors);
  validateUniqueNames(payload.suppliers.map((it) => it.name), "NhaCungCap", "ten_nha_cung_cap", errors);
  validateUniqueNames(payload.products.map((it) => it.name), "SanPham", "ten_san_pham", errors);

  const categoryKeys = new Set([
    ...payload.categories.map(normalizeKey),
    ...existing.categories.map((it) => normalizeKey(it.name)),
  ]);
  const unitKeys = new Set([
    ...payload.units.map(normalizeKey),
    ...existing.units.map((it) => normalizeKey(it.name)),
  ]);
  const supplierKeys = new Set([
    ...payload.suppliers.map((it) => normalizeKey(it.name)),
    ...existing.suppliers.map((it) => normalizeKey(it.name)),
  ]);
  const existingProductKeys = new Set(existing.products.map((it) => normalizeKey(it.name)));
  const supplierPhoneKeys = new Set<string>();

  payload.suppliers.forEach((supplier, index) => {
    const rowNumber = index + 2;
    if (supplier.name.isBlank()) {
      pushError(errors, "NhaCungCap", rowNumber, "ten_nha_cung_cap", "", "Tên nhà cung cấp không được để trống", "Nhập tên nhà cung cấp.");
    }
    if (supplier.phone?.isNotBlank()) {
      const phoneKey = normalizePhone(supplier.phone);
      const supplierKey = normalizeKey(supplier.name);
      const existingPhoneOwner = existing.supplierPhoneOwners.get(phoneKey);
      const conflictsWithExisting = existingPhoneOwner != null && existingPhoneOwner !== supplierKey;
      if (supplierPhoneKeys.has(phoneKey) || conflictsWithExisting) {
        pushError(errors, "NhaCungCap", rowNumber, "so_dien_thoai", supplier.phone, "Số điện thoại nhà cung cấp bị trùng", "Kiểm tra lại số điện thoại hoặc bỏ trống nếu không cần.");
      }
      supplierPhoneKeys.add(phoneKey);
    }
  });

  payload.products.forEach((product, index) => {
    const rowNumber = index + 2;
    if (product.name.isBlank()) {
      pushError(errors, "SanPham", rowNumber, "ten_san_pham", "", "Tên sản phẩm không được để trống", "Nhập tên sản phẩm.");
    }
    if (product.categoryName.isBlank() || !categoryKeys.has(normalizeKey(product.categoryName))) {
      pushError(errors, "SanPham", rowNumber, "loai_san_pham", product.categoryName, "Loại sản phẩm chưa có trong file import hoặc dữ liệu hiện có", "Thêm loại sản phẩm vào sheet LoaiSanPham hoặc tạo trước trong app.");
    }
    if (product.unitName.isBlank() || !unitKeys.has(normalizeKey(product.unitName))) {
      pushError(errors, "SanPham", rowNumber, "don_vi", product.unitName, "Đơn vị tính chưa có trong file import hoặc dữ liệu hiện có", "Thêm đơn vị vào sheet DonViTinh hoặc tạo trước trong app.");
    }
    if (product.supplierName.isBlank() || !supplierKeys.has(normalizeKey(product.supplierName))) {
      pushError(errors, "SanPham", rowNumber, "nha_cung_cap", product.supplierName, "Nhà cung cấp chưa có trong file import hoặc dữ liệu hiện có", "Thêm nhà cung cấp vào sheet NhaCungCap hoặc tạo trước trong app.");
    }
    if (existingProductKeys.has(normalizeKey(product.name))) {
      pushError(errors, "SanPham", rowNumber, "ten_san_pham", product.name, "Sản phẩm đã tồn tại trong hệ thống", "Đổi tên sản phẩm hoặc bỏ dòng này khỏi file.");
    }
    if (product.defaultPurchasePriceVnd <= 0) {
      pushError(errors, "SanPham", rowNumber, "gia_nhap", String(product.defaultPurchasePriceVnd), "Giá nhập không hợp lệ", "Nhập số VND lớn hơn 0.");
    }
    if (product.defaultSalePriceVnd <= 0) {
      pushError(errors, "SanPham", rowNumber, "gia_ban", String(product.defaultSalePriceVnd), "Giá bán không hợp lệ", "Nhập số VND lớn hơn 0.");
    }
    if (
      product.defaultPurchasePriceVnd > 0 &&
      product.defaultSalePriceVnd > 0 &&
      product.defaultSalePriceVnd < product.defaultPurchasePriceVnd
    ) {
      pushError(errors, "SanPham", rowNumber, "gia_ban", String(product.defaultSalePriceVnd), "Giá bán thấp hơn giá nhập", "Nhập giá bán lớn hơn hoặc bằng giá nhập.");
    }
  });

  return {
    errors: errors.slice(0, MAX_ERRORS),
    hasMoreErrors: errors.length > MAX_ERRORS,
  };
}

function buildSummary(
  payload: ImportPayload,
  existing: Awaited<ReturnType<typeof loadExistingLookups>>,
  errorCount: number,
  hasMoreErrors: boolean,
): ImportSummary {
  const existingCategoryKeys = new Set(existing.categories.map((it) => normalizeKey(it.name)));
  const existingUnitKeys = new Set(existing.units.map((it) => normalizeKey(it.name)));
  const existingSupplierKeys = new Set(existing.suppliers.map((it) => normalizeKey(it.name)));
  const referencedCategoryKeys = new Set(payload.products.map((it) => normalizeKey(it.categoryName)).filter((it) => it.length > 0));
  const referencedUnitKeys = new Set(payload.products.map((it) => normalizeKey(it.unitName)).filter((it) => it.length > 0));
  const referencedSupplierKeys = new Set(payload.products.map((it) => normalizeKey(it.supplierName)).filter((it) => it.length > 0));

  return {
    categoriesToCreate: payload.categories.filter((it) => !existingCategoryKeys.has(normalizeKey(it))).length,
    categoriesToReuse: countReusedKeys(payload.categories.map(normalizeKey), referencedCategoryKeys, existingCategoryKeys),
    unitsToCreate: payload.units.filter((it) => !existingUnitKeys.has(normalizeKey(it))).length,
    unitsToReuse: countReusedKeys(payload.units.map(normalizeKey), referencedUnitKeys, existingUnitKeys),
    suppliersToCreate: payload.suppliers.filter((it) => !existingSupplierKeys.has(normalizeKey(it.name))).length,
    suppliersToReuse: countReusedKeys(payload.suppliers.map((it) => normalizeKey(it.name)), referencedSupplierKeys, existingSupplierKeys),
    productsToImport: payload.products.length,
    errorCount,
    hasMoreErrors,
  };
}

function countReusedKeys(
  sheetKeys: string[],
  referencedKeys: Set<string>,
  existingKeys: Set<string>,
): number {
  const keys = new Set([...sheetKeys, ...referencedKeys]);
  let count = 0;
  keys.forEach((key) => {
    if (key.length > 0 && existingKeys.has(key)) count += 1;
  });
  return count;
}

async function loadExistingLookups(supabase: ReturnType<typeof createAuthedClient>) {
  const [categories, units, suppliers, products] = await Promise.all([
    supabase.from("categories").select("name"),
    supabase.from("units").select("name"),
    supabase.from("suppliers").select("name, phone"),
    supabase.from("products").select("name"),
  ]);

  for (const result of [categories, units, suppliers, products]) {
    if (result.error) throw new Error(result.error.message);
  }

  return {
    categories: categories.data ?? [],
    units: units.data ?? [],
    suppliers: suppliers.data ?? [],
    supplierPhoneOwners: new Map((suppliers.data ?? [])
      .filter((it) => typeof it.phone === "string" && it.phone.trim().length > 0)
      .map((it) => [normalizePhone(it.phone), normalizeKey(it.name)])),
    products: products.data ?? [],
  };
}

function sheetRows(workbook: XLSX.WorkBook, sheetName: string): unknown[][] {
  const sheet = workbook.Sheets[sheetName];
  return XLSX.utils.sheet_to_json(sheet, { header: 1, raw: false, defval: "" }) as unknown[][];
}

function headerMap(row: unknown[]): Map<string, number> {
  const map = new Map<string, number>();
  row.forEach((cell, index) => {
    const key = normalizeHeader(cellText(cell));
    if (key.length > 0 && !map.has(key)) map.set(key, index);
  });
  return map;
}

function hasHeader(header: Map<string, number>, column: string): boolean {
  return header.has(normalizeHeader(column));
}

function parseVnd(value: string): number | null {
  if (value.isBlank()) return null;
  const cleaned = value.replace(/[.,\s_]/g, "");
  if (!/^\d+$/.test(cleaned)) return null;
  const parsed = Number(cleaned);
  return Number.isSafeInteger(parsed) && parsed >= 0 ? parsed : null;
}

function validateUniqueNames(values: string[], sheet: string, column: string, errors: ImportError[]) {
  const seen = new Set<string>();
  values.forEach((value, index) => {
    const rowNumber = index + 2;
    if (value.isBlank()) {
      pushError(errors, sheet, rowNumber, column, value, "Tên không được để trống", "Nhập giá trị cho cột này.");
      return;
    }
    const key = normalizeKey(value);
    if (seen.has(key)) {
      pushError(errors, sheet, rowNumber, column, value, "Tên bị trùng trong file", "Giữ lại một dòng duy nhất hoặc đổi tên.");
    }
    seen.add(key);
  });
}

function pushError(
  errors: ImportError[],
  sheet: string,
  rowNumber: number,
  column: string,
  value: string,
  message: string,
  suggestion: string,
) {
  errors.push({ sheet, rowNumber, column, value, message, suggestion });
}

function cellText(value: unknown): string {
  return String(value ?? "").trim();
}

function nullIfBlank(value: string): string | null {
  return value.trim().length === 0 ? null : value.trim();
}

function normalizeHeader(value: string): string {
  return value.trim().toLowerCase();
}

function normalizeKey(value: string): string {
  return value.trim().replace(/\s+/g, " ").toLowerCase();
}

function normalizePhone(value: string): string {
  return value.replace(/\s+/g, "");
}

function createAuthedClient(authHeader: string) {
  const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
  const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  return createClient(supabaseUrl, supabaseAnonKey, {
    global: {
      headers: {
        Authorization: authHeader,
      },
    },
  });
}

async function encodeImportToken(payload: ImportTokenPayload): Promise<string> {
  const body = base64UrlEncode(new TextEncoder().encode(JSON.stringify(payload)));
  const signature = await sign(body);
  return `${body}.${signature}`;
}

async function decodeImportToken(token: string): Promise<ImportTokenPayload> {
  const [body, signature] = token.split(".");
  if (!body || !signature || await sign(body) !== signature) {
    throw new Error("Mã import không hợp lệ");
  }
  const json = new TextDecoder().decode(base64UrlDecode(body));
  return JSON.parse(json) as ImportTokenPayload;
}

async function sign(value: string): Promise<string> {
  const secret = Deno.env.get("IMPORT_TOKEN_SECRET") ?? Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!secret) {
    throw new Error("Thiếu IMPORT_TOKEN_SECRET hoặc SUPABASE_SERVICE_ROLE_KEY cho import token.");
  }
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(value));
  return base64UrlEncode(new Uint8Array(signature));
}

function base64UrlEncode(bytes: Uint8Array): string {
  let binary = "";
  bytes.forEach((byte) => binary += String.fromCharCode(byte));
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

function base64UrlDecode(value: string): Uint8Array {
  const padded = value.replace(/-/g, "+").replace(/_/g, "/").padEnd(Math.ceil(value.length / 4) * 4, "=");
  const binary = atob(padded);
  return Uint8Array.from(binary, (char) => char.charCodeAt(0));
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      ...corsHeaders,
      "Content-Type": "application/json",
    },
  });
}

declare global {
  interface String {
    isBlank(): boolean;
    isNotBlank(): boolean;
  }
}

String.prototype.isBlank = function (): boolean {
  return this.trim().length === 0;
};

String.prototype.isNotBlank = function (): boolean {
  return this.trim().length > 0;
};
