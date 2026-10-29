-- =====================================================================
-- DADOS DE EXEMPLO — Gestão de Frota (para demonstração ao cliente)
-- =====================================================================
-- Gerado em 2026-09-14 a partir do dump de produção enviado pelo usuário.
-- Reescrito depois que o usuário zerou manualmente, pelo próprio sistema,
-- o odômetro (kmAtual) de todos os veículos — por isso os lançamentos de
-- exemplo abaixo cobrem os 5 veículos já cadastrados, com o km de cada
-- um começando baixo (perto de zero) e crescendo ao longo do tempo.
--
-- ATENÇÃO — ESTE SCRIPT APAGA DADO REAL ANTES DE INSERIR A DEMONSTRAÇÃO.
-- A pedido do usuário, ele limpa TUDO que hoje existe nas tabelas do
-- módulo de frota, EXCETO `veiculo` e `usuario`:
--   - anexo, leitura_odometro, documento_veiculo, documento_motorista,
--     lancamento_veiculo, fornecedor, categoria_despesa, tipo_documento
-- `veiculo` e `usuario` NÃO são tocados (nenhum DELETE — só um UPDATE de
-- kmAtual nos 5 veículos ao final, refletindo os lançamentos de exemplo).
--
-- IMPORTANTE: este script NÃO cadastra veículo nem motorista novo. Usa
-- os 5 veículos e 5 motoristas que já existem em produção hoje:
--
--   Veículo id 1 (placa BDZ-7E20, "Sprinter 314 - 2019") — motorista id 17 (THIAGO PARAIBA)
--   Veículo id 2 (placa BCW-3C49, "Sprinter 515 - 2018") — motorista id 4  (DANIEL SORTI)
--   Veículo id 3 (placa DBW-0G38, "Sprinter 311 - 2003") — motorista id 9  (FERNANDO HENRIQUE)
--   Veículo id 4 (placa SFM-9F64, "Sprinter 517 - 2024") — motorista id 10 (WELLINGTON DE ASSIS NASCIMENTO)
--   Veículo id 5 (placa AWD-1I37, "VW 5150")              — motorista id 11 (BRUNO CAMILO)
--
-- Observação: o veículo id 1 está com status = inativo no cadastro atual.
-- Os lançamentos de exemplo dele são criados normalmente, mas ele só vai
-- aparecer nas listagens/filtros padrão (que exigem status ativo) se for
-- reativado — o script não mexe no status do veículo.
--
-- O que este script faz, em ordem:
--   0) Apaga anexo, leitura_odometro, documento_veiculo, documento_motorista,
--      lancamento_veiculo, fornecedor, categoria_despesa e tipo_documento
--      (respeitando as chaves estrangeiras: filhos antes dos pais).
--   1) Cadastra 2 fornecedores fictícios (1 posto + 1 oficina).
--   2) Popula `categoria_despesa` e `tipo_documento` com os valores
--      padrão que o próprio sistema cadastraria sozinho na primeira vez
--      que alguém abrisse aquelas telas (CategoriaDespesaMB/TipoDocumentoMB).
--   3) Cria lançamentos (abastecimento, despesa, serviço) e as leituras
--      de odômetro correspondentes para os 5 veículos, com km sempre
--      crescente por veículo (o sistema bloqueia km decrescente).
--   4) Cria documentos (veículo/motorista): 3 deles com vencimentos
--      variados (vencido, vencendo em breve, em dia) pra mostrar o
--      painel de alertas, e mais alguns só "em dia" pra não deixar os
--      outros veículos/motoristas sem nenhum documento cadastrado.
--   5) Atualiza o kmAtual dos 5 veículos com o maior km lançado.
-- =====================================================================

START TRANSACTION;

-- ---------------------------------------------------------------------
-- 0) LIMPEZA — apaga tudo do módulo de frota, exceto veiculo/usuario
-- ---------------------------------------------------------------------

DELETE FROM anexo WHERE lancamento_id IS NOT NULL;
DELETE FROM leitura_odometro;
DELETE FROM documento_veiculo;
DELETE FROM documento_motorista;
DELETE FROM lancamento_veiculo;
DELETE FROM fornecedor;
DELETE FROM categoria_despesa;
DELETE FROM tipo_documento;

-- ---------------------------------------------------------------------
-- 1) FORNECEDORES FICTÍCIOS
-- ---------------------------------------------------------------------

INSERT INTO fornecedor (nome, tipo, bandeira, cnpj, telefone, avaliacao, status)
SELECT 'Posto Demonstração LTDA', 'Posto', NULL, '99.999.999/0001-01',
       '(44) 99999-0001', 4.5, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM fornecedor WHERE cnpj = '99.999.999/0001-01');

INSERT INTO fornecedor (nome, tipo, bandeira, cnpj, telefone, avaliacao, status)
SELECT 'Oficina Demonstração LTDA', 'Oficina', NULL, '99.999.999/0001-02',
       '(44) 99999-0002', 4.0, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM fornecedor WHERE cnpj = '99.999.999/0001-02');

-- ---------------------------------------------------------------------
-- 2) CATEGORIAS DE DESPESA PADRÃO (tabela está vazia em produção)
--    Mesmos valores que CategoriaDespesaMB.CATEGORIAS_PADRAO auto-cadastra.
-- ---------------------------------------------------------------------

INSERT INTO categoria_despesa (nome, status)
SELECT c.nome, 1 FROM (
  SELECT 'Combustível' AS nome UNION ALL
  SELECT 'Manutenção Preventiva' UNION ALL
  SELECT 'Manutenção Corretiva' UNION ALL
  SELECT 'Seguro' UNION ALL
  SELECT 'Multa' UNION ALL
  SELECT 'Pedágio' UNION ALL
  SELECT 'IPVA' UNION ALL
  SELECT 'Lavagem' UNION ALL
  SELECT 'Insumos de Biossegurança' UNION ALL
  SELECT 'Outros'
) c
WHERE NOT EXISTS (SELECT 1 FROM categoria_despesa cd WHERE cd.nome = c.nome);

-- ---------------------------------------------------------------------
-- 3) TIPOS DE DOCUMENTO PADRÃO (tabela está vazia em produção)
--    Mesmos valores que TipoDocumentoMB.TIPOS_PADRAO auto-cadastra.
-- ---------------------------------------------------------------------

INSERT INTO tipo_documento (nome, aplicavelA, status)
SELECT t.nome, t.aplicavelA, 1 FROM (
  SELECT 'Licenciamento' AS nome, 'Veiculo' AS aplicavelA UNION ALL
  SELECT 'Seguro Obrigatório', 'Veiculo' UNION ALL
  SELECT 'IPVA', 'Veiculo' UNION ALL
  SELECT 'Vistoria', 'Veiculo' UNION ALL
  SELECT 'Licença Ambiental', 'Veiculo' UNION ALL
  SELECT 'NBR 12810', 'Veiculo' UNION ALL
  SELECT 'CNH', 'Motorista' UNION ALL
  SELECT 'NR-32', 'Motorista'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM tipo_documento td WHERE td.nome = t.nome AND td.aplicavelA = t.aplicavelA
);

-- ---------------------------------------------------------------------
-- 4) LANÇAMENTOS — Veículo id 1 (BDZ-7E20) / Motorista id 17 (Thiago Paraiba)
-- ---------------------------------------------------------------------

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       fornecedor_id, data, valor, km, observacao, litros, valorLitro, combustivel,
       origemLancamento, status, criadoPor, criadoEm)
SELECT 'Abastecimento', 1, 17, cd.id, f.id, '2026-07-12', 164.44, 320,
       '', 28.4, 5.79, 'Diesel S10', 'Manual', 1, 'Importação Dados Exemplo', '2026-07-12 08:10:00'
FROM categoria_despesa cd, fornecedor f
WHERE cd.nome = 'Combustível' AND f.cnpj = '99.999.999/0001-01'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 1 AND lv.data = '2026-07-12' AND lv.valor = 164.44);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       fornecedor_id, data, valor, km, observacao, litros, valorLitro, combustivel,
       origemLancamento, status, criadoPor, criadoEm)
SELECT 'Abastecimento', 1, 17, cd.id, f.id, '2026-07-28', 163.80, 650,
       '', 28.0, 5.85, 'Diesel S10', 'Manual', 1, 'Importação Dados Exemplo', '2026-07-28 17:25:00'
FROM categoria_despesa cd, fornecedor f
WHERE cd.nome = 'Combustível' AND f.cnpj = '99.999.999/0001-01'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 1 AND lv.data = '2026-07-28' AND lv.valor = 163.80);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       data, valor, observacao, origemLancamento, status, criadoPor, criadoEm)
SELECT 'Despesa', 1, 17, cd.id, '2026-08-05', 950.00,
       'Renovação anual do seguro do veículo', 'Manual', 1, 'Importação Dados Exemplo', '2026-08-05 09:40:00'
FROM categoria_despesa cd
WHERE cd.nome = 'Seguro'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 1 AND lv.data = '2026-08-05' AND lv.valor = 950.00);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       fornecedor_id, data, valor, km, kmProximaRevisao, observacao, tipoManutencao,
       dataEntrada, dataSaida, origemLancamento, status, criadoPor, criadoEm)
SELECT 'Serviço', 1, 17, cd.id, f.id, '2026-08-14', 310.00, 900, 10900,
       'Revisão programada (óleo, filtros, alinhamento)', 'Preventiva',
       '2026-08-14', '2026-08-15', 'Manual', 1, 'Importação Dados Exemplo', '2026-08-14 08:00:00'
FROM categoria_despesa cd, fornecedor f
WHERE cd.nome = 'Manutenção Preventiva' AND f.cnpj = '99.999.999/0001-02'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 1 AND lv.data = '2026-08-14' AND lv.valor = 310.00);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       fornecedor_id, data, valor, km, observacao, litros, valorLitro, combustivel,
       origemLancamento, status, criadoPor, criadoEm)
SELECT 'Abastecimento', 1, 17, cd.id, f.id, '2026-08-25', 171.68, 1180,
       '', 29.0, 5.92, 'Diesel S10', 'Manual', 1, 'Importação Dados Exemplo', '2026-08-25 07:50:00'
FROM categoria_despesa cd, fornecedor f
WHERE cd.nome = 'Combustível' AND f.cnpj = '99.999.999/0001-01'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 1 AND lv.data = '2026-08-25' AND lv.valor = 171.68);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       fornecedor_id, data, valor, km, observacao, litros, valorLitro, combustivel,
       origemLancamento, status, criadoPor, criadoEm)
SELECT 'Abastecimento', 1, 17, cd.id, f.id, '2026-09-08', 165.32, 1500,
       '', 27.6, 5.99, 'Diesel S10', 'Manual', 1, 'Importação Dados Exemplo', '2026-09-08 18:15:00'
FROM categoria_despesa cd, fornecedor f
WHERE cd.nome = 'Combustível' AND f.cnpj = '99.999.999/0001-01'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 1 AND lv.data = '2026-09-08' AND lv.valor = 165.32);

-- ---------------------------------------------------------------------
-- 5) LANÇAMENTOS — Veículo id 2 (BCW-3C49) / Motorista id 4 (Daniel Sorti)
-- ---------------------------------------------------------------------

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       fornecedor_id, data, valor, km, observacao, litros, valorLitro, combustivel,
       origemLancamento, status, criadoPor, criadoEm)
SELECT 'Abastecimento', 2, 4, cd.id, f.id, '2026-07-10', 153.44, 280,
       '', 26.5, 5.79, 'Diesel S10', 'Manual', 1, 'Importação Dados Exemplo', '2026-07-10 08:05:00'
FROM categoria_despesa cd, fornecedor f
WHERE cd.nome = 'Combustível' AND f.cnpj = '99.999.999/0001-01'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 2 AND lv.data = '2026-07-10' AND lv.valor = 153.44);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       fornecedor_id, data, valor, km, observacao, litros, valorLitro, combustivel,
       origemLancamento, status, criadoPor, criadoEm)
SELECT 'Abastecimento', 2, 4, cd.id, f.id, '2026-07-24', 162.63, 600,
       '', 27.8, 5.85, 'Diesel S10', 'Manual', 1, 'Importação Dados Exemplo', '2026-07-24 17:00:00'
FROM categoria_despesa cd, fornecedor f
WHERE cd.nome = 'Combustível' AND f.cnpj = '99.999.999/0001-01'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 2 AND lv.data = '2026-07-24' AND lv.valor = 162.63);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       data, valor, observacao, origemLancamento, status, criadoPor, criadoEm)
SELECT 'Despesa', 2, 4, cd.id, '2026-08-02', 210.50,
       'Excesso de velocidade - rodovia', 'Manual', 1, 'Importação Dados Exemplo', '2026-08-02 13:30:00'
FROM categoria_despesa cd
WHERE cd.nome = 'Multa'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 2 AND lv.data = '2026-08-02' AND lv.valor = 210.50);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       fornecedor_id, data, valor, km, observacao, litros, valorLitro, combustivel,
       origemLancamento, status, criadoPor, criadoEm)
SELECT 'Abastecimento', 2, 4, cd.id, f.id, '2026-08-15', 176.40, 950,
       '', 30.0, 5.88, 'Diesel S10', 'Manual', 1, 'Importação Dados Exemplo', '2026-08-15 12:20:00'
FROM categoria_despesa cd, fornecedor f
WHERE cd.nome = 'Combustível' AND f.cnpj = '99.999.999/0001-01'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 2 AND lv.data = '2026-08-15' AND lv.valor = 176.40);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       fornecedor_id, data, valor, km, kmProximaRevisao, observacao, tipoManutencao,
       dataEntrada, dataSaida, origemLancamento, status, criadoPor, criadoEm)
SELECT 'Serviço', 2, 4, cd.id, f.id, '2026-08-28', 890.00, 1180, 11180,
       'Troca de amortecedores', 'Corretiva',
       '2026-08-28', '2026-08-29', 'Manual', 1, 'Importação Dados Exemplo', '2026-08-28 08:30:00'
FROM categoria_despesa cd, fornecedor f
WHERE cd.nome = 'Manutenção Corretiva' AND f.cnpj = '99.999.999/0001-02'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 2 AND lv.data = '2026-08-28' AND lv.valor = 890.00);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       fornecedor_id, data, valor, km, observacao, litros, valorLitro, combustivel,
       origemLancamento, status, criadoPor, criadoEm)
SELECT 'Abastecimento', 2, 4, cd.id, f.id, '2026-09-09', 161.13, 1480,
       '', 26.9, 5.99, 'Diesel S10', 'Manual', 1, 'Importação Dados Exemplo', '2026-09-09 17:45:00'
FROM categoria_despesa cd, fornecedor f
WHERE cd.nome = 'Combustível' AND f.cnpj = '99.999.999/0001-01'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 2 AND lv.data = '2026-09-09' AND lv.valor = 161.13);

-- ---------------------------------------------------------------------
-- 6) LANÇAMENTOS — Veículo id 3 (DBW-0G38) / Motorista id 9 (Fernando Henrique)
-- ---------------------------------------------------------------------

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       fornecedor_id, data, valor, km, observacao, litros, valorLitro, combustivel,
       origemLancamento, status, criadoPor, criadoEm)
SELECT 'Abastecimento', 3, 9, cd.id, f.id, '2026-07-10', 138.96, 250,
       '', 24.0, 5.79, 'Diesel S10', 'Manual', 1, 'Importação Dados Exemplo', '2026-07-10 08:32:00'
FROM categoria_despesa cd, fornecedor f
WHERE cd.nome = 'Combustível' AND f.cnpj = '99.999.999/0001-01'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 3 AND lv.data = '2026-07-10' AND lv.valor = 138.96);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       fornecedor_id, data, valor, km, observacao, litros, valorLitro, combustivel,
       origemLancamento, status, criadoPor, criadoEm)
SELECT 'Abastecimento', 3, 9, cd.id, f.id, '2026-07-25', 158.54, 540,
       '', 27.1, 5.85, 'Diesel S10', 'Manual', 1, 'Importação Dados Exemplo', '2026-07-25 17:10:00'
FROM categoria_despesa cd, fornecedor f
WHERE cd.nome = 'Combustível' AND f.cnpj = '99.999.999/0001-01'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 3 AND lv.data = '2026-07-25' AND lv.valor = 158.54);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       data, valor, observacao, origemLancamento, status, criadoPor, criadoEm)
SELECT 'Despesa', 3, 9, cd.id, '2026-08-05', 1200.00,
       'Renovação anual do seguro do veículo', 'Manual', 1, 'Importação Dados Exemplo', '2026-08-05 09:15:00'
FROM categoria_despesa cd
WHERE cd.nome = 'Seguro'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 3 AND lv.data = '2026-08-05' AND lv.valor = 1200.00);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       fornecedor_id, data, valor, km, kmProximaRevisao, observacao, tipoManutencao,
       dataEntrada, dataSaida, origemLancamento, status, criadoPor, criadoEm)
SELECT 'Serviço', 3, 9, cd.id, f.id, '2026-08-11', 380.00, 800, 10800,
       'Revisão programada (óleo, filtros, alinhamento)', 'Preventiva',
       '2026-08-11', '2026-08-12', 'Manual', 1, 'Importação Dados Exemplo', '2026-08-11 08:00:00'
FROM categoria_despesa cd, fornecedor f
WHERE cd.nome = 'Manutenção Preventiva' AND f.cnpj = '99.999.999/0001-02'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 3 AND lv.data = '2026-08-11' AND lv.valor = 380.00);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       fornecedor_id, data, valor, km, observacao, litros, valorLitro, combustivel,
       origemLancamento, status, criadoPor, criadoEm)
SELECT 'Abastecimento', 3, 9, cd.id, f.id, '2026-08-20', 153.33, 1050,
       '', 25.9, 5.92, 'Diesel S10', 'Manual', 1, 'Importação Dados Exemplo', '2026-08-20 07:55:00'
FROM categoria_despesa cd, fornecedor f
WHERE cd.nome = 'Combustível' AND f.cnpj = '99.999.999/0001-01'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 3 AND lv.data = '2026-08-20' AND lv.valor = 153.33);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       data, valor, observacao, origemLancamento, status, criadoPor, criadoEm)
SELECT 'Despesa', 3, 9, cd.id, '2026-09-01', 195.23,
       'Excesso de velocidade - via urbana', 'Manual', 1, 'Importação Dados Exemplo', '2026-09-01 14:20:00'
FROM categoria_despesa cd
WHERE cd.nome = 'Multa'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 3 AND lv.data = '2026-09-01' AND lv.valor = 195.23);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       fornecedor_id, data, valor, km, observacao, litros, valorLitro, combustivel,
       origemLancamento, status, criadoPor, criadoEm)
SELECT 'Abastecimento', 3, 9, cd.id, f.id, '2026-09-10', 165.92, 1380,
       '', 27.7, 5.99, 'Diesel S10', 'Manual', 1, 'Importação Dados Exemplo', '2026-09-10 18:05:00'
FROM categoria_despesa cd, fornecedor f
WHERE cd.nome = 'Combustível' AND f.cnpj = '99.999.999/0001-01'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 3 AND lv.data = '2026-09-10' AND lv.valor = 165.92);

-- ---------------------------------------------------------------------
-- 7) LANÇAMENTOS — Veículo id 4 (SFM-9F64) / Motorista id 10 (Wellington de Assis Nascimento)
-- ---------------------------------------------------------------------

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       fornecedor_id, data, valor, km, observacao, litros, valorLitro, combustivel,
       origemLancamento, status, criadoPor, criadoEm)
SELECT 'Abastecimento', 4, 10, cd.id, f.id, '2026-07-15', 191.07, 400,
       '', 33.0, 5.79, 'Diesel S10', 'Manual', 1, 'Importação Dados Exemplo', '2026-07-15 07:40:00'
FROM categoria_despesa cd, fornecedor f
WHERE cd.nome = 'Combustível' AND f.cnpj = '99.999.999/0001-01'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 4 AND lv.data = '2026-07-15' AND lv.valor = 191.07);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       fornecedor_id, data, valor, km, kmProximaRevisao, observacao, tipoManutencao,
       dataEntrada, dataSaida, origemLancamento, status, criadoPor, criadoEm)
SELECT 'Serviço', 4, 10, cd.id, f.id, '2026-08-01', 2150.00, 700, 10700,
       'Troca de embreagem e disco de freio', 'Corretiva',
       '2026-08-01', '2026-08-02', 'Manual', 1, 'Importação Dados Exemplo', '2026-08-01 08:30:00'
FROM categoria_despesa cd, fornecedor f
WHERE cd.nome = 'Manutenção Corretiva' AND f.cnpj = '99.999.999/0001-02'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 4 AND lv.data = '2026-08-01' AND lv.valor = 2150.00);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       fornecedor_id, data, valor, km, observacao, litros, valorLitro, combustivel,
       origemLancamento, status, criadoPor, criadoEm)
SELECT 'Abastecimento', 4, 10, cd.id, f.id, '2026-08-18', 168.17, 950,
       '', 28.6, 5.88, 'Diesel S10', 'Manual', 1, 'Importação Dados Exemplo', '2026-08-18 12:15:00'
FROM categoria_despesa cd, fornecedor f
WHERE cd.nome = 'Combustível' AND f.cnpj = '99.999.999/0001-01'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 4 AND lv.data = '2026-08-18' AND lv.valor = 168.17);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       data, valor, observacao, origemLancamento, status, criadoPor, criadoEm)
SELECT 'Despesa', 4, 10, cd.id, '2026-08-25', 68.40,
       'Pedágio rodovia BR-376', 'Manual', 1, 'Importação Dados Exemplo', '2026-08-25 10:05:00'
FROM categoria_despesa cd
WHERE cd.nome = 'Pedágio'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 4 AND lv.data = '2026-08-25' AND lv.valor = 68.40);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       fornecedor_id, data, valor, km, observacao, litros, valorLitro, combustivel,
       origemLancamento, status, criadoPor, criadoEm)
SELECT 'Abastecimento', 4, 10, cd.id, f.id, '2026-09-05', 181.48, 1280,
       '', 30.5, 5.95, 'Diesel S10', 'Manual', 1, 'Importação Dados Exemplo', '2026-09-05 09:20:00'
FROM categoria_despesa cd, fornecedor f
WHERE cd.nome = 'Combustível' AND f.cnpj = '99.999.999/0001-01'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 4 AND lv.data = '2026-09-05' AND lv.valor = 181.48);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       data, valor, observacao, origemLancamento, status, criadoPor, criadoEm)
SELECT 'Despesa', 4, 10, cd.id, '2026-09-12', 980.00,
       'IPVA - parcela única', 'Manual', 1, 'Importação Dados Exemplo', '2026-09-12 11:00:00'
FROM categoria_despesa cd
WHERE cd.nome = 'IPVA'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 4 AND lv.data = '2026-09-12' AND lv.valor = 980.00);

-- ---------------------------------------------------------------------
-- 8) LANÇAMENTOS — Veículo id 5 (AWD-1I37) / Motorista id 11 (Bruno Camilo)
-- ---------------------------------------------------------------------

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       fornecedor_id, data, valor, km, observacao, litros, valorLitro, combustivel,
       origemLancamento, status, criadoPor, criadoEm)
SELECT 'Abastecimento', 5, 11, cd.id, f.id, '2026-07-18', 198.02, 500,
       '', 34.2, 5.79, 'Diesel S10', 'Manual', 1, 'Importação Dados Exemplo', '2026-07-18 07:35:00'
FROM categoria_despesa cd, fornecedor f
WHERE cd.nome = 'Combustível' AND f.cnpj = '99.999.999/0001-01'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 5 AND lv.data = '2026-07-18' AND lv.valor = 198.02);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       fornecedor_id, data, valor, km, observacao, litros, valorLitro, combustivel,
       origemLancamento, status, criadoPor, criadoEm)
SELECT 'Abastecimento', 5, 11, cd.id, f.id, '2026-08-02', 193.05, 850,
       '', 33.0, 5.85, 'Diesel S10', 'Manual', 1, 'Importação Dados Exemplo', '2026-08-02 17:50:00'
FROM categoria_despesa cd, fornecedor f
WHERE cd.nome = 'Combustível' AND f.cnpj = '99.999.999/0001-01'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 5 AND lv.data = '2026-08-02' AND lv.valor = 193.05);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       data, valor, observacao, origemLancamento, status, criadoPor, criadoEm)
SELECT 'Despesa', 5, 11, cd.id, '2026-08-10', 1050.00,
       'Renovação anual do seguro do veículo', 'Manual', 1, 'Importação Dados Exemplo', '2026-08-10 09:20:00'
FROM categoria_despesa cd
WHERE cd.nome = 'Seguro'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 5 AND lv.data = '2026-08-10' AND lv.valor = 1050.00);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       fornecedor_id, data, valor, km, kmProximaRevisao, observacao, tipoManutencao,
       dataEntrada, dataSaida, origemLancamento, status, criadoPor, criadoEm)
SELECT 'Serviço', 5, 11, cd.id, f.id, '2026-08-22', 340.00, 1100, 11100,
       'Revisão programada (óleo, filtros, alinhamento)', 'Preventiva',
       '2026-08-22', '2026-08-23', 'Manual', 1, 'Importação Dados Exemplo', '2026-08-22 08:10:00'
FROM categoria_despesa cd, fornecedor f
WHERE cd.nome = 'Manutenção Preventiva' AND f.cnpj = '99.999.999/0001-02'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 5 AND lv.data = '2026-08-22' AND lv.valor = 340.00);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       fornecedor_id, data, valor, km, observacao, litros, valorLitro, combustivel,
       origemLancamento, status, criadoPor, criadoEm)
SELECT 'Abastecimento', 5, 11, cd.id, f.id, '2026-09-06', 193.38, 1400,
       '', 32.5, 5.95, 'Diesel S10', 'Manual', 1, 'Importação Dados Exemplo', '2026-09-06 09:00:00'
FROM categoria_despesa cd, fornecedor f
WHERE cd.nome = 'Combustível' AND f.cnpj = '99.999.999/0001-01'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 5 AND lv.data = '2026-09-06' AND lv.valor = 193.38);

INSERT INTO lancamento_veiculo (tipo, veiculo_id, motorista_id, categoria_despesa_id,
       data, valor, observacao, origemLancamento, status, criadoPor, criadoEm)
SELECT 'Despesa', 5, 11, cd.id, '2026-09-13', 145.90,
       'Excesso de velocidade - via urbana', 'Manual', 1, 'Importação Dados Exemplo', '2026-09-13 15:10:00'
FROM categoria_despesa cd
WHERE cd.nome = 'Multa'
  AND NOT EXISTS (SELECT 1 FROM lancamento_veiculo lv WHERE lv.veiculo_id = 5 AND lv.data = '2026-09-13' AND lv.valor = 145.90);

-- ---------------------------------------------------------------------
-- 9) LEITURAS DE ODÔMETRO (uma para cada abastecimento/serviço acima)
-- ---------------------------------------------------------------------

INSERT INTO leitura_odometro (veiculo_id, km, data, origem, lancamento_id, status, criadoPor, criadoEm)
SELECT lv.veiculo_id, lv.km, lv.data, lv.tipo, lv.id, 1, 'Importação Dados Exemplo', lv.criadoEm
FROM lancamento_veiculo lv
WHERE lv.criadoPor = 'Importação Dados Exemplo' AND lv.veiculo_id IN (1, 2, 3, 4, 5) AND lv.km IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM leitura_odometro lo WHERE lo.veiculo_id = lv.veiculo_id AND lo.km = lv.km AND lo.lancamento_id = lv.id
  );

-- Atualiza o km atual dos 5 veículos com o maior km de exemplo lançado
-- (só sobe o valor, nunca reduz um km que já esteja maior)
UPDATE veiculo SET kmAtual = 1500 WHERE id = 1 AND (kmAtual IS NULL OR kmAtual < 1500);
UPDATE veiculo SET kmAtual = 1480 WHERE id = 2 AND (kmAtual IS NULL OR kmAtual < 1480);
UPDATE veiculo SET kmAtual = 1380 WHERE id = 3 AND (kmAtual IS NULL OR kmAtual < 1380);
UPDATE veiculo SET kmAtual = 1280 WHERE id = 4 AND (kmAtual IS NULL OR kmAtual < 1280);
UPDATE veiculo SET kmAtual = 1400 WHERE id = 5 AND (kmAtual IS NULL OR kmAtual < 1400);

-- ---------------------------------------------------------------------
-- 10) DOCUMENTOS
--     3 com vencimentos variados (vencido / vencendo em breve / em dia)
--     pra demonstrar o painel de alertas, e mais 6 "em dia" simples pra
--     nenhum veículo/motorista ficar sem nenhum documento cadastrado.
-- ---------------------------------------------------------------------

-- Licenciamento do veículo id 1 — em dia
INSERT INTO documento_veiculo (veiculo_id, tipo_documento_id, numero, dataEmissao, dataVencimento, observacao, status)
SELECT 1, td.id, 'DEMO-LIC-0001', '2026-01-15', '2027-01-15', 'Documento de demonstração', 1
FROM tipo_documento td
WHERE td.nome = 'Licenciamento' AND td.aplicavelA = 'Veiculo'
  AND NOT EXISTS (SELECT 1 FROM documento_veiculo dv WHERE dv.veiculo_id = 1 AND dv.tipo_documento_id = td.id AND dv.dataVencimento = '2027-01-15');

-- Licenciamento do veículo id 2 — em dia
INSERT INTO documento_veiculo (veiculo_id, tipo_documento_id, numero, dataEmissao, dataVencimento, observacao, status)
SELECT 2, td.id, 'DEMO-LIC-0002', '2026-02-01', '2027-02-01', 'Documento de demonstração', 1
FROM tipo_documento td
WHERE td.nome = 'Licenciamento' AND td.aplicavelA = 'Veiculo'
  AND NOT EXISTS (SELECT 1 FROM documento_veiculo dv WHERE dv.veiculo_id = 2 AND dv.tipo_documento_id = td.id AND dv.dataVencimento = '2027-02-01');

-- Licenciamento do veículo id 3 — em dia (vence só em 2027)
INSERT INTO documento_veiculo (veiculo_id, tipo_documento_id, numero, dataEmissao, dataVencimento, observacao, status)
SELECT 3, td.id, 'DEMO-LIC-0003', '2026-01-10', '2027-01-10', 'Documento de demonstração', 1
FROM tipo_documento td
WHERE td.nome = 'Licenciamento' AND td.aplicavelA = 'Veiculo'
  AND NOT EXISTS (SELECT 1 FROM documento_veiculo dv WHERE dv.veiculo_id = 3 AND dv.tipo_documento_id = td.id AND dv.dataVencimento = '2027-01-10');

-- Seguro Obrigatório do veículo id 3 — vencendo em breve (~3 semanas)
INSERT INTO documento_veiculo (veiculo_id, tipo_documento_id, numero, dataEmissao, dataVencimento, observacao, status)
SELECT 3, td.id, 'DEMO-SEG-0003', '2025-10-05', '2026-10-05', 'Documento de demonstração', 1
FROM tipo_documento td
WHERE td.nome = 'Seguro Obrigatório' AND td.aplicavelA = 'Veiculo'
  AND NOT EXISTS (SELECT 1 FROM documento_veiculo dv WHERE dv.veiculo_id = 3 AND dv.tipo_documento_id = td.id AND dv.dataVencimento = '2026-10-05');

-- IPVA do veículo id 4 — vencido (13 dias atrás)
INSERT INTO documento_veiculo (veiculo_id, tipo_documento_id, numero, dataEmissao, dataVencimento, observacao, status)
SELECT 4, td.id, 'DEMO-IPVA-0004', '2025-09-01', '2026-09-01', 'Documento de demonstração', 1
FROM tipo_documento td
WHERE td.nome = 'IPVA' AND td.aplicavelA = 'Veiculo'
  AND NOT EXISTS (SELECT 1 FROM documento_veiculo dv WHERE dv.veiculo_id = 4 AND dv.tipo_documento_id = td.id AND dv.dataVencimento = '2026-09-01');

-- Licenciamento do veículo id 5 — em dia
INSERT INTO documento_veiculo (veiculo_id, tipo_documento_id, numero, dataEmissao, dataVencimento, observacao, status)
SELECT 5, td.id, 'DEMO-LIC-0005', '2026-03-01', '2027-03-01', 'Documento de demonstração', 1
FROM tipo_documento td
WHERE td.nome = 'Licenciamento' AND td.aplicavelA = 'Veiculo'
  AND NOT EXISTS (SELECT 1 FROM documento_veiculo dv WHERE dv.veiculo_id = 5 AND dv.tipo_documento_id = td.id AND dv.dataVencimento = '2027-03-01');

-- CNH do motorista id 17 (Thiago Paraiba) — em dia
INSERT INTO documento_motorista (motorista_id, tipo_documento_id, numero, dataEmissao, dataVencimento, observacao, status)
SELECT 17, td.id, 'DEMO-CNH-0017', '2023-03-01', '2028-03-01', 'Documento de demonstração', 1
FROM tipo_documento td
WHERE td.nome = 'CNH' AND td.aplicavelA = 'Motorista'
  AND NOT EXISTS (SELECT 1 FROM documento_motorista dm WHERE dm.motorista_id = 17 AND dm.tipo_documento_id = td.id AND dm.dataVencimento = '2028-03-01');

-- CNH do motorista id 4 (Daniel Sorti) — em dia
INSERT INTO documento_motorista (motorista_id, tipo_documento_id, numero, dataEmissao, dataVencimento, observacao, status)
SELECT 4, td.id, 'DEMO-CNH-0004', '2023-04-01', '2028-04-01', 'Documento de demonstração', 1
FROM tipo_documento td
WHERE td.nome = 'CNH' AND td.aplicavelA = 'Motorista'
  AND NOT EXISTS (SELECT 1 FROM documento_motorista dm WHERE dm.motorista_id = 4 AND dm.tipo_documento_id = td.id AND dm.dataVencimento = '2028-04-01');

-- CNH do motorista id 9 (Fernando Henrique) — em dia
INSERT INTO documento_motorista (motorista_id, tipo_documento_id, numero, dataEmissao, dataVencimento, observacao, status)
SELECT 9, td.id, 'DEMO-CNH-0009', '2023-05-01', '2028-05-01', 'Documento de demonstração', 1
FROM tipo_documento td
WHERE td.nome = 'CNH' AND td.aplicavelA = 'Motorista'
  AND NOT EXISTS (SELECT 1 FROM documento_motorista dm WHERE dm.motorista_id = 9 AND dm.tipo_documento_id = td.id AND dm.dataVencimento = '2028-05-01');

-- NR-32 do motorista id 10 (Wellington de Assis Nascimento) — vencendo em breve (~6 dias)
INSERT INTO documento_motorista (motorista_id, tipo_documento_id, numero, dataEmissao, dataVencimento, observacao, status)
SELECT 10, td.id, 'DEMO-NR32-0010', '2025-09-20', '2026-09-20', 'Documento de demonstração', 1
FROM tipo_documento td
WHERE td.nome = 'NR-32' AND td.aplicavelA = 'Motorista'
  AND NOT EXISTS (SELECT 1 FROM documento_motorista dm WHERE dm.motorista_id = 10 AND dm.tipo_documento_id = td.id AND dm.dataVencimento = '2026-09-20');

-- CNH do motorista id 11 (Bruno Camilo) — em dia
INSERT INTO documento_motorista (motorista_id, tipo_documento_id, numero, dataEmissao, dataVencimento, observacao, status)
SELECT 11, td.id, 'DEMO-CNH-0011', '2023-06-01', '2028-06-01', 'Documento de demonstração', 1
FROM tipo_documento td
WHERE td.nome = 'CNH' AND td.aplicavelA = 'Motorista'
  AND NOT EXISTS (SELECT 1 FROM documento_motorista dm WHERE dm.motorista_id = 11 AND dm.tipo_documento_id = td.id AND dm.dataVencimento = '2028-06-01');

COMMIT;

-- =====================================================================
-- CONFERÊNCIA RÁPIDA (rode depois do import pra ver o que entrou)
-- =====================================================================
-- SELECT * FROM fornecedor WHERE cnpj LIKE '99.999.999/%';
-- SELECT * FROM lancamento_veiculo WHERE criadoPor = 'Importação Dados Exemplo' ORDER BY veiculo_id, data;
-- SELECT * FROM leitura_odometro WHERE criadoPor = 'Importação Dados Exemplo' ORDER BY veiculo_id, data;
-- SELECT * FROM documento_veiculo WHERE observacao = 'Documento de demonstração';
-- SELECT * FROM documento_motorista WHERE observacao = 'Documento de demonstração';
-- SELECT id, placa, descricao, status, kmAtual FROM veiculo WHERE id IN (1,2,3,4,5);

-- =====================================================================
-- LIMPEZA (caso queira remover os dados de exemplo depois) — NÃO RODAR
-- junto com o import acima. Ordem respeita as chaves estrangeiras.
-- Veículos e motoristas NÃO são apagados (são reais) — só os
-- lançamentos/leituras/documentos/fornecedores de exemplo.
-- =====================================================================
-- START TRANSACTION;
-- DELETE FROM documento_motorista WHERE observacao = 'Documento de demonstração';
-- DELETE FROM documento_veiculo WHERE observacao = 'Documento de demonstração';
-- DELETE FROM leitura_odometro WHERE criadoPor = 'Importação Dados Exemplo';
-- DELETE FROM lancamento_veiculo WHERE criadoPor = 'Importação Dados Exemplo';
-- DELETE f FROM fornecedor f WHERE f.cnpj LIKE '99.999.999/%';
-- -- categoria_despesa e tipo_documento NÃO são removidos: são os
-- -- valores padrão que o próprio sistema usaria de qualquer forma.
-- -- kmAtual dos veículos fica no valor de exemplo — ajuste manualmente
-- -- se quiser voltar pro estado zerado.
-- COMMIT;
