-- Seed assets and indicator values for local development
SET NAMES utf8mb4;

-- FII assets
INSERT INTO asset (ticker, name, asset_type, updated_at) VALUES
('MXRF11', 'Maxi Renda',                                          'FII', NOW(6)),
('KISU11', 'KILIMA FIC FDO. IMOB. SUNO 30',                       'FII', NOW(6)),
('SNCI11', 'SUNO RECEBÍVEIS IMOBILIÁRIOS FDO DE INV IMOB',        'FII', NOW(6)),
('SNAG11', 'SUNO AGRO - FIAGRO-IMOBILIÁRIO',                      'FII', NOW(6)),
('RECR11', 'FII REC Recebíveis Imobiliários',                     'FII', NOW(6)),
('HGLG11', 'PÁTRIA LOG',                                          'FII', NOW(6)),
('XPML11', 'XP Malls',                                            'FII', NOW(6)),
('GARE11', 'Guardian Real Estate',                                 'FII', NOW(6)),
('RBRR11', 'RBR Rendimento High Grade',                           'FII', NOW(6)),
('VGHF11', 'VALORA HEDGE FUND',                                   'FII', NOW(6))
ON DUPLICATE KEY UPDATE
    name       = VALUES(name),
    asset_type = VALUES(asset_type),
    updated_at = VALUES(updated_at);

-- Indicator values for FII assets (PRICE, DIVIDEND_YIELD, PVP)
INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'PRICE', 9.5000 FROM asset WHERE ticker = 'MXRF11'
ON DUPLICATE KEY UPDATE value = VALUES(value);
INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'DIVIDEND_YIELD', 12.5000 FROM asset WHERE ticker = 'MXRF11'
ON DUPLICATE KEY UPDATE value = VALUES(value);
INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'PVP', 1.0500 FROM asset WHERE ticker = 'MXRF11'
ON DUPLICATE KEY UPDATE value = VALUES(value);

INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'PRICE', 8.0000 FROM asset WHERE ticker = 'KISU11'
ON DUPLICATE KEY UPDATE value = VALUES(value);
INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'DIVIDEND_YIELD', 11.5000 FROM asset WHERE ticker = 'KISU11'
ON DUPLICATE KEY UPDATE value = VALUES(value);
INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'PVP', 0.8600 FROM asset WHERE ticker = 'KISU11'
ON DUPLICATE KEY UPDATE value = VALUES(value);

INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'PRICE', 9.0000 FROM asset WHERE ticker = 'SNCI11'
ON DUPLICATE KEY UPDATE value = VALUES(value);
INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'DIVIDEND_YIELD', 13.0000 FROM asset WHERE ticker = 'SNCI11'
ON DUPLICATE KEY UPDATE value = VALUES(value);
INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'PVP', 0.9300 FROM asset WHERE ticker = 'SNCI11'
ON DUPLICATE KEY UPDATE value = VALUES(value);

INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'PRICE', 9.5000 FROM asset WHERE ticker = 'SNAG11'
ON DUPLICATE KEY UPDATE value = VALUES(value);
INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'DIVIDEND_YIELD', 14.0000 FROM asset WHERE ticker = 'SNAG11'
ON DUPLICATE KEY UPDATE value = VALUES(value);
INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'PVP', 1.0500 FROM asset WHERE ticker = 'SNAG11'
ON DUPLICATE KEY UPDATE value = VALUES(value);

INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'PRICE', 80.0000 FROM asset WHERE ticker = 'RECR11'
ON DUPLICATE KEY UPDATE value = VALUES(value);
INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'DIVIDEND_YIELD', 13.0000 FROM asset WHERE ticker = 'RECR11'
ON DUPLICATE KEY UPDATE value = VALUES(value);
INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'PVP', 0.9300 FROM asset WHERE ticker = 'RECR11'
ON DUPLICATE KEY UPDATE value = VALUES(value);

INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'PRICE', 160.0000 FROM asset WHERE ticker = 'HGLG11'
ON DUPLICATE KEY UPDATE value = VALUES(value);
INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'DIVIDEND_YIELD', 9.5000 FROM asset WHERE ticker = 'HGLG11'
ON DUPLICATE KEY UPDATE value = VALUES(value);
INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'PVP', 1.0000 FROM asset WHERE ticker = 'HGLG11'
ON DUPLICATE KEY UPDATE value = VALUES(value);

INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'PRICE', 110.0000 FROM asset WHERE ticker = 'XPML11'
ON DUPLICATE KEY UPDATE value = VALUES(value);
INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'DIVIDEND_YIELD', 9.5000 FROM asset WHERE ticker = 'XPML11'
ON DUPLICATE KEY UPDATE value = VALUES(value);
INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'PVP', 1.0000 FROM asset WHERE ticker = 'XPML11'
ON DUPLICATE KEY UPDATE value = VALUES(value);

INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'PRICE', 9.0000 FROM asset WHERE ticker = 'GARE11'
ON DUPLICATE KEY UPDATE value = VALUES(value);
INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'DIVIDEND_YIELD', 12.5000 FROM asset WHERE ticker = 'GARE11'
ON DUPLICATE KEY UPDATE value = VALUES(value);
INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'PVP', 0.9500 FROM asset WHERE ticker = 'GARE11'
ON DUPLICATE KEY UPDATE value = VALUES(value);

INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'PRICE', 90.0000 FROM asset WHERE ticker = 'RBRR11'
ON DUPLICATE KEY UPDATE value = VALUES(value);
INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'DIVIDEND_YIELD', 11.5000 FROM asset WHERE ticker = 'RBRR11'
ON DUPLICATE KEY UPDATE value = VALUES(value);
INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'PVP', 0.9800 FROM asset WHERE ticker = 'RBRR11'
ON DUPLICATE KEY UPDATE value = VALUES(value);

INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'PRICE', 8.0000 FROM asset WHERE ticker = 'VGHF11'
ON DUPLICATE KEY UPDATE value = VALUES(value);
INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'DIVIDEND_YIELD', 12.5000 FROM asset WHERE ticker = 'VGHF11'
ON DUPLICATE KEY UPDATE value = VALUES(value);
INSERT INTO asset_indicator_value (asset_id, indicator_type, value)
SELECT id, 'PVP', 0.8200 FROM asset WHERE ticker = 'VGHF11'
ON DUPLICATE KEY UPDATE value = VALUES(value);
