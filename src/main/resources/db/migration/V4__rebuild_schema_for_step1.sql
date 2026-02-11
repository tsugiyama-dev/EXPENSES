-- 一時退避
RENAME TABLE expenses TO expenses_old;

CREATE TABLE expenses (
  id BIGINT NOT NULL AUTO_INCREMENT,
  applicant_id BIGINT NOT NULL,
  title VARCHAR(100) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  currency VARCHAR(3) NOT NULL DEFAULT 'JPY',
  status VARCHAR(20) NOT NULL,
  submitted_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

DROP TABLE expenses_old;

RENAME TABLE expense_audit_logs TO expense_audit_logs_old;

CREATE TABLE expense_audit_logs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  expense_id BIGINT NOT NULL,
  actor_id BIGINT NOT NULL,
  action VARCHAR(30) NOT NULL,
  before_status VARCHAR(20) NULL,
  after_status VARCHAR(20) NULL,
  note VARCHAR(255),
  trace_id VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_expense_id (expense_id),
  CONSTRAINT fk_expense_audit
    FOREIGN KEY (expense_id) REFERENCES expenses(id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

DROP TABLE expense_audit_logs_old;
