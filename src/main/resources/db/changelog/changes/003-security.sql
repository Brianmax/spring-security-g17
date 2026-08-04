--liquibase formatted sql

--changeset banking-api:003-security
CREATE TABLE auth_credentials (
                                  user_id UUID PRIMARY KEY,
                                  password_hash VARCHAR(72) NOT NULL,
                                  auth_version BIGINT NOT NULL DEFAULT 1,
                                  password_changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                  CONSTRAINT fk_auth_credentials_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE,
                                  CONSTRAINT ck_auth_credentials_version CHECK (auth_version > 0)
);

CREATE TABLE security_roles (
                                id UUID PRIMARY KEY,
                                code VARCHAR(40) NOT NULL,
                                description VARCHAR(255) NOT NULL,
                                CONSTRAINT uk_security_roles_code UNIQUE (code)
);

CREATE TABLE security_permissions (
                                      id UUID PRIMARY KEY,
                                      code VARCHAR(80) NOT NULL,
                                      description VARCHAR(255) NOT NULL,
                                      CONSTRAINT uk_security_permissions_code UNIQUE (code)
);

CREATE TABLE user_roles (
                            user_id UUID NOT NULL,
                            role_id UUID NOT NULL,
                            PRIMARY KEY (user_id, role_id),
                            CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE,
                            CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES security_roles (id) ON DELETE CASCADE
);

CREATE TABLE role_permissions (
                                  role_id UUID NOT NULL,
                                  permission_id UUID NOT NULL,
                                  PRIMARY KEY (role_id, permission_id),
                                  CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES security_roles (id) ON DELETE CASCADE,
                                  CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES security_permissions (id) ON DELETE CASCADE
);

CREATE TABLE refresh_tokens (
                                id UUID PRIMARY KEY,
                                user_id UUID NOT NULL,
                                token_hash VARCHAR(64) NOT NULL,
                                family_id UUID NOT NULL,
                                expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                revoked_at TIMESTAMP WITH TIME ZONE,
                                replaced_by_id UUID,
                                created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
                                CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE,
                                CONSTRAINT fk_refresh_tokens_replacement FOREIGN KEY (replaced_by_id) REFERENCES refresh_tokens (id)
);

CREATE INDEX idx_user_roles_role ON user_roles (role_id);
CREATE INDEX idx_role_permissions_permission ON role_permissions (permission_id);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens (expires_at);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens (token_hash);

INSERT INTO security_roles (id, code, description) VALUES
                                                       ('10000000-0000-0000-0000-000000000001', 'CUSTOMER', 'Gestiona sus propios datos, cuentas y operaciones'),
                                                       ('10000000-0000-0000-0000-000000000002', 'SUPPORT', 'Consulta recursos y administra bloqueos de cuentas'),
                                                       ('10000000-0000-0000-0000-000000000003', 'ADMIN', 'Administración completa de la banca virtual')
    ON CONFLICT (code) DO NOTHING;

INSERT INTO security_permissions (id, code, description) VALUES
                                                             ('20000000-0000-0000-0000-000000000001', 'user:create:any', 'Crear usuarios'),
                                                             ('20000000-0000-0000-0000-000000000002', 'user:read:self', 'Consultar el perfil propio'),
                                                             ('20000000-0000-0000-0000-000000000003', 'user:read:any', 'Consultar cualquier usuario'),
                                                             ('20000000-0000-0000-0000-000000000004', 'user:update:self', 'Actualizar el perfil propio'),
                                                             ('20000000-0000-0000-0000-000000000005', 'user:update:any', 'Actualizar cualquier usuario'),
                                                             ('20000000-0000-0000-0000-000000000006', 'user:deactivate:any', 'Desactivar usuarios'),
                                                             ('20000000-0000-0000-0000-000000000007', 'account:create:self', 'Abrir cuentas propias'),
                                                             ('20000000-0000-0000-0000-000000000008', 'account:create:any', 'Abrir cuentas para cualquier usuario'),
                                                             ('20000000-0000-0000-0000-000000000009', 'account:read:self', 'Consultar cuentas propias'),
                                                             ('20000000-0000-0000-0000-000000000010', 'account:read:any', 'Consultar cualquier cuenta'),
                                                             ('20000000-0000-0000-0000-000000000011', 'account:freeze:any', 'Congelar cualquier cuenta'),
                                                             ('20000000-0000-0000-0000-000000000012', 'account:unfreeze:any', 'Descongelar cualquier cuenta'),
                                                             ('20000000-0000-0000-0000-000000000013', 'account:close:self', 'Cerrar cuentas propias'),
                                                             ('20000000-0000-0000-0000-000000000014', 'account:close:any', 'Cerrar cualquier cuenta'),
                                                             ('20000000-0000-0000-0000-000000000015', 'deposit:create:self', 'Depositar en cuentas propias'),
                                                             ('20000000-0000-0000-0000-000000000016', 'deposit:create:any', 'Depositar en cualquier cuenta'),
                                                             ('20000000-0000-0000-0000-000000000017', 'withdrawal:create:self', 'Retirar de cuentas propias'),
                                                             ('20000000-0000-0000-0000-000000000018', 'withdrawal:create:any', 'Retirar de cualquier cuenta'),
                                                             ('20000000-0000-0000-0000-000000000019', 'transfer:create:self', 'Transferir desde cuentas propias'),
                                                             ('20000000-0000-0000-0000-000000000020', 'transfer:create:any', 'Transferir desde cualquier cuenta'),
                                                             ('20000000-0000-0000-0000-000000000021', 'transfer:read:self', 'Consultar transferencias propias'),
                                                             ('20000000-0000-0000-0000-000000000022', 'transfer:read:any', 'Consultar cualquier transferencia'),
                                                             ('20000000-0000-0000-0000-000000000023', 'transaction:read:self', 'Consultar movimientos propios'),
                                                             ('20000000-0000-0000-0000-000000000024', 'transaction:read:any', 'Consultar cualquier movimiento'),
                                                             ('20000000-0000-0000-0000-000000000025', 'role:read:any', 'Consultar roles'),
                                                             ('20000000-0000-0000-0000-000000000026', 'role:assign:any', 'Asignar roles'),
                                                             ('20000000-0000-0000-0000-000000000027', 'permission:read:any', 'Consultar permisos')
    ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM security_roles r CROSS JOIN security_permissions p
WHERE (r.code = 'CUSTOMER' AND p.code IN (
                                          'user:read:self', 'user:update:self', 'account:create:self', 'account:read:self', 'account:close:self',
                                          'deposit:create:self', 'withdrawal:create:self', 'transfer:create:self', 'transfer:read:self',
                                          'transaction:read:self'
    ))
   OR (r.code = 'SUPPORT' AND p.code IN (
                                         'user:read:any', 'account:read:any', 'account:freeze:any', 'account:unfreeze:any',
                                         'transfer:read:any', 'transaction:read:any'
    ))
   OR (r.code = 'ADMIN')
    ON CONFLICT DO NOTHING;

