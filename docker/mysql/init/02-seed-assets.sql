-- Seed FII assets for local development
SET NAMES utf8mb4;
INSERT INTO asset (ticker, name, current_price, dividend_yield, p_vp, updated_at) VALUES
('MXRF11', 'Maxi Renda',                                          9.5000, 12.5000, 1.0500, NOW(6)),
('KISU11', 'KILIMA FIC FDO. IMOB. SUNO 30',                       8.0000, 11.5000, 0.8600, NOW(6)),
('SNCI11', 'SUNO RECEBÍVEIS IMOBILIÁRIOS FDO DE INV IMOB',        9.0000, 13.0000, 0.9300, NOW(6)),
('SNAG11', 'SUNO AGRO - FIAGRO-IMOBILIÁRIO',                      9.5000, 14.0000, 1.0500, NOW(6)),
('RECR11', 'FII REC Recebíveis Imobiliários',                    80.0000, 13.0000, 0.9300, NOW(6)),
('HGLG11', 'PÁTRIA LOG',                                        160.0000,  9.5000, 1.0000, NOW(6)),
('XPML11', 'XP Malls',                                          110.0000,  9.5000, 1.0000, NOW(6)),
('GARE11', 'Guardian Real Estate',                                 9.0000, 12.5000, 0.9500, NOW(6)),
('RBRR11', 'RBR Rendimento High Grade',                          90.0000, 11.5000, 0.9800, NOW(6)),
('VGHF11', 'VALORA HEDGE FUND',                                   8.0000, 12.5000, 0.8200, NOW(6))
ON DUPLICATE KEY UPDATE
    current_price  = VALUES(current_price),
    dividend_yield = VALUES(dividend_yield),
    p_vp           = VALUES(p_vp),
    updated_at     = VALUES(updated_at);
