const express = require("express");
const crypto = require("crypto");

const app = express();
app.set("trust proxy", 1);
app.use(express.json());

// ---- Config ----
const API_KEY = process.env.API_KEY || "test_key_123";
const FRAUD_THRESHOLD_CENTS = 100000;   // > 1000.00 => failed
const MAX_AMOUNT_CENTS = 9999999;       // 99,999.99
const PAY_LATENCY_MS = 1000;            // delay before /pay responds
const PROCESSING_MS = 3000;             // pending -> final status
const SLOW_RESPONSE_MS = 15000;         // ".98" trigger
const MAX_TRANSACTIONS = 1000;
const RATE_LIMIT = { windowMs: 60000, max: 60 };
const ALLOWED_CURRENCIES = ["EUR", "USD", "QAR"];

// ---- Storage (in memory) ----
const transactions = new Map();      // id -> transaction
const idempotencyKeys = new Map();   // key -> transaction id
const inFlightKeys = new Set();      // keys whose first request is still running

// ---- Helpers ----
const ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
function generateId() {
  let id = "";
  for (let i = 0; i < 20; i++) id += ALPHABET[crypto.randomInt(ALPHABET.length)];
  return `tr_${id}`;
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

function error(res, httpStatus, code, message) {
  return res.status(httpStatus).json({ error: { code, message } });
}

function saveTransaction(tx) {
  transactions.set(tx.id, tx);
  if (transactions.size > MAX_TRANSACTIONS) {
    const oldestId = transactions.keys().next().value;
    const oldest = transactions.get(oldestId);
    transactions.delete(oldestId);
    if (oldest.idempotencyKey) idempotencyKeys.delete(oldest.idempotencyKey);
  }
}

function publicView(tx) {
  const { idempotencyKey, ...rest } = tx;
  return rest;
}

// ---- Middleware: simple rate limit per IP ----
const hits = new Map();
app.use((req, res, next) => {
  const now = Date.now();
  const entry = hits.get(req.ip);
  if (!entry || now - entry.start > RATE_LIMIT.windowMs) {
    hits.set(req.ip, { start: now, count: 1 });
    return next();
  }
  if (++entry.count > RATE_LIMIT.max) {
    return error(res, 429, "rate_limited", "Too many requests, slow down");
  }
  next();
});
setInterval(() => {
  const now = Date.now();
  for (const [ip, e] of hits) if (now - e.start > RATE_LIMIT.windowMs) hits.delete(ip);
}, RATE_LIMIT.windowMs).unref();

// ---- Public health check ----
app.get("/", (req, res) => res.json({ status: "ok" }));

// ---- Middleware: API key ----
app.use((req, res, next) => {
  if (req.get("X-Api-Key") !== API_KEY) {
    return error(res, 401, "unauthorized", "Missing or invalid X-Api-Key header");
  }
  next();
});

// ---- POST /pay ----
// Body: { "amount": 2550, "currency": "EUR" }  (amount in cents, integer)
// Header (optional): Idempotency-Key
//
// Test triggers:
//   amount > 100000         -> transaction ends "failed" (Fraud detected)
//   amount ending in 99     -> HTTP 500, nothing created
//   amount ending in 98     -> transaction created, response delayed 15s
app.post("/pay", async (req, res) => {
  const { amount, currency = "EUR" } = req.body || {};

  if (!Number.isInteger(amount) || amount <= 0 || amount > MAX_AMOUNT_CENTS) {
    return error(res, 400, "invalid_amount",
      `amount must be an integer in cents between 1 and ${MAX_AMOUNT_CENTS}`);
  }
  if (!ALLOWED_CURRENCIES.includes(currency)) {
    return error(res, 400, "invalid_currency",
      `currency must be one of ${ALLOWED_CURRENCIES.join(", ")}`);
  }

  const idemKey = req.get("Idempotency-Key");
  if (idemKey !== undefined && (idemKey.length < 8 || idemKey.length > 64)) {
    return error(res, 400, "invalid_idempotency_key", "Idempotency-Key must be 8-64 characters");
  }

  // Same key while the first request is still processing -> 409
  if (idemKey && inFlightKeys.has(idemKey)) {
    return error(res, 409, "request_in_progress",
      "A request with this Idempotency-Key is still being processed");
  }

  // Same key -> same transaction (no duplicate charge)
  if (idemKey && idempotencyKeys.has(idemKey)) {
    const existing = transactions.get(idempotencyKeys.get(idemKey));
    if (existing.amount !== amount || existing.currency !== currency) {
      return error(res, 422, "idempotency_mismatch",
        "Idempotency-Key already used with a different amount or currency");
    }
    return res.status(200).json(publicView(existing));
  }

  if (idemKey) inFlightKeys.add(idemKey);
  await sleep(PAY_LATENCY_MS);

  if (amount % 100 === 99) {
    if (idemKey) inFlightKeys.delete(idemKey);
    return error(res, 500, "internal_error", "Payment processor unavailable");
  }

  const now = new Date().toISOString();
  const tx = {
    id: generateId(),
    amount,
    currency,
    status: "pending",
    reason: null,
    createdAt: now,
    updatedAt: now,
    idempotencyKey: idemKey || null,
  };
  saveTransaction(tx);
  if (idemKey) {
    idempotencyKeys.set(idemKey, tx.id);
    inFlightKeys.delete(idemKey);
  }

  setTimeout(() => {
    const fraud = amount > FRAUD_THRESHOLD_CENTS;
    tx.status = fraud ? "failed" : "success";
    tx.reason = fraud ? "Fraud detected" : "Approved";
    tx.updatedAt = new Date().toISOString();
  }, PROCESSING_MS);

  if (amount % 100 === 98) await sleep(SLOW_RESPONSE_MS);

  return res.status(202).json(publicView(tx));
});

// ---- GET /transactions/:id ----
app.get("/transactions/:id", (req, res) => {
  const tx = transactions.get(req.params.id);
  if (!tx) return error(res, 404, "not_found", "Transaction not found");
  return res.json(publicView(tx));
});

// ---- GET /transactions?limit=20 (latest first) ----
app.get("/transactions", (req, res) => {
  const limit = Math.min(Math.max(parseInt(req.query.limit, 10) || 20, 1), 100);
  const items = [...transactions.values()].reverse().slice(0, limit).map(publicView);
  return res.json({ items, count: items.length });
});

// ---- Fallbacks ----
app.use((req, res) => error(res, 404, "not_found", "Route not found"));
app.use((err, req, res, next) => {
  if (err.type === "entity.parse.failed") {
    return error(res, 400, "invalid_json", "Request body is not valid JSON");
  }
  console.error(err);
  return error(res, 500, "internal_error", "Unexpected error");
});

const PORT = process.env.PORT || 8080;
app.listen(PORT, () => console.log(`Server listening on port ${PORT}`));