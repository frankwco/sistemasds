<?php
/**
 * Configuração do painel administrativo de Licenças e Modelos.
 */

// Base da API REST do sistema GestãoFácil (produção).
// /app (não /service): usuarioAu agora exige a chave abaixo (ver
// services.ExigeChaveApp/ChaveApiFilter/ServicoApp no backend do gestaofacil).
define('GESTAOFACIL_API_BASE', 'https://alcsistemas.com.br/gestaofacil/rest/app');

// Precisa ser IDÊNTICA à chave configurada no backend (services.ChaveApiFilter
// / -DappApiKey) e no appgestaofacil (ConstantesService.tokenApi). Trocar os
// três lados juntos.
define('GESTAOFACIL_API_KEY', 'gf-app-7f3a9c2e-51b6-4d8a-9c0f-2e6a8b1d4f77');

// Pasta pública onde os documentos (licenças e modelos) ficam disponíveis para download
$docsDirReal = realpath(__DIR__ . '/../../docs');
define('DOCS_DIR', $docsDirReal !== false ? $docsDirReal : (__DIR__ . '/../../docs'));
unset($docsDirReal);

// Extensões de arquivo permitidas para upload
define('DOCS_ALLOWED_EXTENSIONS', ['pdf', 'doc', 'docx', 'jpg', 'jpeg', 'png']);

// Tamanho máximo de upload aceito pela aplicação (bytes) — 25MB
define('DOCS_MAX_FILE_SIZE', 25 * 1024 * 1024);

// Tempo máximo de sessão inativa (segundos) — 2 horas
define('ADMIN_SESSION_IDLE_TIMEOUT', 2 * 60 * 60);
