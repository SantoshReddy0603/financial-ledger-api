-- Schema version 1. Physical model documented in docs/design/data-model.md.
-- Statements end with ';' at end of line; trigger bodies end with a line 'END;'.

CREATE TABLE schema_info (
  singleton      INTEGER PRIMARY KEY CHECK (singleton = 1),
  schema_version INTEGER NOT NULL CHECK (schema_version > 0)
);

CREATE TABLE currency (
  code                TEXT PRIMARY KEY CHECK (length(code) = 3 AND code GLOB '[A-Z][A-Z][A-Z]'),
  minor_unit_exponent INTEGER NOT NULL CHECK (typeof(minor_unit_exponent) = 'integer' AND minor_unit_exponent BETWEEN 0 AND 18)
);

CREATE TABLE app_user (
  user_id       TEXT PRIMARY KEY CHECK (length(user_id) = 36),
  login_name    TEXT NOT NULL UNIQUE CHECK (length(login_name) BETWEEN 3 AND 64),
  display_name  TEXT NOT NULL,
  password_hash TEXT NOT NULL,
  role          TEXT NOT NULL DEFAULT 'USER' CHECK (role = 'USER'),
  created_at    TEXT NOT NULL
);

CREATE TABLE user_session (
  session_id TEXT PRIMARY KEY CHECK (length(session_id) = 36),
  user_id    TEXT NOT NULL REFERENCES app_user (user_id),
  token_hash TEXT NOT NULL UNIQUE CHECK (length(token_hash) = 64),
  created_at TEXT NOT NULL,
  expires_at TEXT NOT NULL,
  revoked_at TEXT
);

CREATE INDEX ix_user_session_user ON user_session (user_id);

CREATE TABLE account (
  account_id TEXT PRIMARY KEY CHECK (length(account_id) = 36),
  kind       TEXT NOT NULL CHECK (kind IN ('WALLET', 'SYSTEM')),
  currency   TEXT NOT NULL REFERENCES currency (code),
  balance    INTEGER NOT NULL DEFAULT 0 CHECK (typeof(balance) = 'integer'),
  created_at TEXT NOT NULL,
  CHECK (kind = 'SYSTEM' OR balance BETWEEN 0 AND 9007199254740991),
  UNIQUE (account_id, currency)
);

CREATE UNIQUE INDEX ux_account_one_system_per_currency ON account (currency) WHERE kind = 'SYSTEM';

CREATE TABLE wallet (
  wallet_id  TEXT PRIMARY KEY,
  user_id    TEXT NOT NULL REFERENCES app_user (user_id),
  currency   TEXT NOT NULL,
  created_at TEXT NOT NULL,
  UNIQUE (user_id, currency),
  FOREIGN KEY (wallet_id, currency) REFERENCES account (account_id, currency)
);

CREATE TABLE ledger_transaction (
  transaction_id         TEXT PRIMARY KEY CHECK (length(transaction_id) = 36),
  sequence               INTEGER NOT NULL UNIQUE CHECK (typeof(sequence) = 'integer' AND sequence > 0),
  type                   TEXT NOT NULL CHECK (type IN ('FUNDING', 'TRANSFER', 'REVERSAL')),
  source_account_id      TEXT NOT NULL,
  destination_account_id TEXT NOT NULL,
  amount                 INTEGER NOT NULL CHECK (typeof(amount) = 'integer' AND amount BETWEEN 1 AND 1000000000000),
  currency               TEXT NOT NULL REFERENCES currency (code),
  reference              TEXT,
  reason                 TEXT,
  reversal_of            TEXT UNIQUE REFERENCES ledger_transaction (transaction_id),
  created_at             TEXT NOT NULL,
  previous_hash          TEXT NOT NULL CHECK (length(previous_hash) = 64),
  hash                   TEXT NOT NULL UNIQUE CHECK (length(hash) = 64),
  CHECK (source_account_id <> destination_account_id),
  CHECK ((type = 'REVERSAL') = (reversal_of IS NOT NULL)),
  CHECK (type = 'TRANSFER' OR reference IS NULL),
  CHECK (type = 'REVERSAL' OR reason IS NULL),
  FOREIGN KEY (source_account_id, currency) REFERENCES account (account_id, currency),
  FOREIGN KEY (destination_account_id, currency) REFERENCES account (account_id, currency)
);

CREATE TABLE ledger_entry (
  entry_number   INTEGER PRIMARY KEY CHECK (entry_number > 0),
  transaction_id TEXT NOT NULL REFERENCES ledger_transaction (transaction_id),
  account_id     TEXT NOT NULL,
  direction      TEXT NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
  amount         INTEGER NOT NULL CHECK (typeof(amount) = 'integer' AND amount BETWEEN 1 AND 1000000000000),
  currency       TEXT NOT NULL,
  created_at     TEXT NOT NULL,
  UNIQUE (transaction_id, direction),
  FOREIGN KEY (account_id, currency) REFERENCES account (account_id, currency)
);

CREATE INDEX ix_ledger_entry_account ON ledger_entry (account_id, entry_number DESC);

CREATE TABLE idempotency_record (
  principal_id    TEXT NOT NULL,
  idempotency_key TEXT NOT NULL CHECK (length(idempotency_key) BETWEEN 16 AND 64),
  fingerprint     TEXT NOT NULL CHECK (length(fingerprint) = 64),
  response_status INTEGER NOT NULL CHECK (response_status BETWEEN 200 AND 299),
  response_body   TEXT NOT NULL,
  transaction_id  TEXT NOT NULL UNIQUE REFERENCES ledger_transaction (transaction_id),
  created_at      TEXT NOT NULL,
  PRIMARY KEY (principal_id, idempotency_key)
);

-- Append-only ledger records (BR-8, FR-4.1).

CREATE TRIGGER trg_ledger_transaction_no_update BEFORE UPDATE ON ledger_transaction
BEGIN
  SELECT RAISE(ABORT, 'ledger_transaction is append-only');
END;

CREATE TRIGGER trg_ledger_transaction_no_delete BEFORE DELETE ON ledger_transaction
BEGIN
  SELECT RAISE(ABORT, 'ledger_transaction is append-only');
END;

CREATE TRIGGER trg_ledger_entry_no_update BEFORE UPDATE ON ledger_entry
BEGIN
  SELECT RAISE(ABORT, 'ledger_entry is append-only');
END;

CREATE TRIGGER trg_ledger_entry_no_delete BEFORE DELETE ON ledger_entry
BEGIN
  SELECT RAISE(ABORT, 'ledger_entry is append-only');
END;

CREATE TRIGGER trg_idempotency_record_no_update BEFORE UPDATE ON idempotency_record
BEGIN
  SELECT RAISE(ABORT, 'idempotency_record is append-only');
END;

CREATE TRIGGER trg_idempotency_record_no_delete BEFORE DELETE ON idempotency_record
BEGIN
  SELECT RAISE(ABORT, 'idempotency_record is append-only');
END;

-- An entry must repeat its transaction's amount and currency (BR-4).

CREATE TRIGGER trg_ledger_entry_matches_transaction BEFORE INSERT ON ledger_entry
WHEN NOT EXISTS (
  SELECT 1 FROM ledger_transaction t
  WHERE t.transaction_id = NEW.transaction_id
    AND t.amount = NEW.amount
    AND t.currency = NEW.currency
    AND NEW.account_id = CASE NEW.direction WHEN 'DEBIT' THEN t.source_account_id ELSE t.destination_account_id END
)
BEGIN
  SELECT RAISE(ABORT, 'ledger_entry does not match its transaction');
END;

-- Currency metadata and account identity never change (FR-6.4, BR-2, BR-3).

CREATE TRIGGER trg_currency_no_update BEFORE UPDATE ON currency
BEGIN
  SELECT RAISE(ABORT, 'currency metadata is immutable');
END;

CREATE TRIGGER trg_currency_no_delete BEFORE DELETE ON currency
BEGIN
  SELECT RAISE(ABORT, 'currency metadata is immutable');
END;

CREATE TRIGGER trg_account_identity_immutable BEFORE UPDATE OF account_id, kind, currency, created_at ON account
BEGIN
  SELECT RAISE(ABORT, 'account identity is immutable');
END;

CREATE TRIGGER trg_account_no_delete BEFORE DELETE ON account
BEGIN
  SELECT RAISE(ABORT, 'accounts cannot be deleted');
END;

CREATE TRIGGER trg_wallet_no_update BEFORE UPDATE ON wallet
BEGIN
  SELECT RAISE(ABORT, 'wallets are immutable');
END;

CREATE TRIGGER trg_wallet_no_delete BEFORE DELETE ON wallet
BEGIN
  SELECT RAISE(ABORT, 'wallets cannot be deleted');
END;

CREATE TRIGGER trg_wallet_requires_wallet_account BEFORE INSERT ON wallet
WHEN (SELECT kind FROM account WHERE account_id = NEW.wallet_id) IS NOT 'WALLET'
BEGIN
  SELECT RAISE(ABORT, 'wallet must reference a WALLET account');
END;

CREATE TRIGGER trg_app_user_no_update BEFORE UPDATE ON app_user
BEGIN
  SELECT RAISE(ABORT, 'users are immutable');
END;

CREATE TRIGGER trg_app_user_no_delete BEFORE DELETE ON app_user
BEGIN
  SELECT RAISE(ABORT, 'users cannot be deleted');
END;

-- Sessions may only be revoked, once.

CREATE TRIGGER trg_user_session_revoke_only BEFORE UPDATE ON user_session
WHEN NEW.session_id IS NOT OLD.session_id
  OR NEW.user_id IS NOT OLD.user_id
  OR NEW.token_hash IS NOT OLD.token_hash
  OR NEW.created_at IS NOT OLD.created_at
  OR NEW.expires_at IS NOT OLD.expires_at
  OR OLD.revoked_at IS NOT NULL
  OR NEW.revoked_at IS NULL
BEGIN
  SELECT RAISE(ABORT, 'sessions may only be revoked once');
END;

INSERT INTO schema_info (singleton, schema_version) VALUES (1, 1);
