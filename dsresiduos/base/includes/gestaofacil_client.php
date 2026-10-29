<?php
/**
 * Cliente para a API REST do sistema GestãoFácil.
 */

require_once __DIR__ . '/config.php';

/**
 * Autentica um usuário no sistema GestãoFácil via ServicoHello#usuarioAu.
 *
 * @return array|null Dados do usuário retornados pela API (com acessoSistema=true),
 *                     ou null em caso de credenciais inválidas / erro de comunicação.
 */
function gestaofacilAutenticar(string $email, string $senha): ?array
{
    $url = GESTAOFACIL_API_BASE . '/usuarioAu';

    $payload = json_encode([
        'email' => $email,
        'senha' => $senha,
    ]);

    $ch = curl_init($url);
    curl_setopt_array($ch, [
        CURLOPT_POST           => true,
        CURLOPT_POSTFIELDS     => $payload,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_HTTPHEADER     => [
            'Content-Type: application/json;charset=UTF-8',
            'X-App-Key: ' . GESTAOFACIL_API_KEY,
        ],
        CURLOPT_CONNECTTIMEOUT => 8,
        CURLOPT_TIMEOUT        => 15,
    ]);

    $result = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlError = curl_error($ch);
    curl_close($ch);

    if ($result === false || $httpCode !== 200) {
        error_log('gestaofacilAutenticar: falha na comunicação com a API (' . $httpCode . ') ' . $curlError);
        return null;
    }

    $data = json_decode($result, true);

    if (!is_array($data) || empty($data['acessoSistema'])) {
        return null;
    }

    return $data;
}
