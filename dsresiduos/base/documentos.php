<?php
require_once __DIR__ . '/includes/auth.php';
require_once __DIR__ . '/includes/config.php';

requireAdminLogin();

if (!is_dir(DOCS_DIR) && !@mkdir(DOCS_DIR, 0755, true)) {
    http_response_code(500);
    die('Pasta de documentos não encontrada em ' . htmlspecialchars(DOCS_DIR) . ' e não foi possível criá-la. Verifique as permissões no servidor.');
}

if (!is_writable(DOCS_DIR)) {
    http_response_code(500);
    die('A pasta de documentos (' . htmlspecialchars(DOCS_DIR) . ') não tem permissão de escrita no servidor.');
}

/** Lista os nomes de arquivo atualmente na pasta docs/. */
function listarArquivosDocs(): array
{
    $arquivos = array_diff(scandir(DOCS_DIR), ['.', '..']);
    $arquivos = array_filter($arquivos, function ($f) {
        return is_file(DOCS_DIR . '/' . $f);
    });
    sort($arquivos, SORT_NATURAL | SORT_FLAG_CASE);
    return array_values($arquivos);
}

function extensaoPermitida(string $nome): bool
{
    $ext = strtolower(pathinfo($nome, PATHINFO_EXTENSION));
    return in_array($ext, DOCS_ALLOWED_EXTENSIONS, true);
}

function tamanhoLegivel(int $bytes): string
{
    $unidades = ['B', 'KB', 'MB', 'GB'];
    $i = 0;
    $valor = $bytes;
    while ($valor >= 1024 && $i < count($unidades) - 1) {
        $valor /= 1024;
        $i++;
    }
    return round($valor, $i === 0 ? 0 : 1) . ' ' . $unidades[$i];
}

/**
 * Valida um upload vindo de $_FILES e retorna uma mensagem de erro, ou null se estiver OK.
 */
function validarUpload(array $file, string $nomeFinal): ?string
{
    if (($file['error'] ?? UPLOAD_ERR_NO_FILE) === UPLOAD_ERR_NO_FILE) {
        return 'Selecione um arquivo.';
    }
    if ($file['error'] !== UPLOAD_ERR_OK) {
        return 'Falha no envio do arquivo (código ' . $file['error'] . ').';
    }
    if (!is_uploaded_file($file['tmp_name'])) {
        return 'Upload inválido.';
    }
    if ($file['size'] <= 0 || $file['size'] > DOCS_MAX_FILE_SIZE) {
        return 'Arquivo muito grande (máximo ' . tamanhoLegivel(DOCS_MAX_FILE_SIZE) . ').';
    }
    if ($nomeFinal === '' || !extensaoPermitida($nomeFinal)) {
        return 'Tipo de arquivo não permitido. Extensões aceitas: ' . implode(', ', DOCS_ALLOWED_EXTENSIONS) . '.';
    }
    return null;
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (!verifyCsrfToken($_POST['csrf_token'] ?? null)) {
        flashSet('danger', 'Sessão expirada, tente novamente.');
        header('Location: documentos.php');
        exit;
    }

    $acao = $_POST['acao'] ?? '';
    $arquivosAtuais = listarArquivosDocs();

    if ($acao === 'substituir') {
        $alvo = $_POST['arquivo_alvo'] ?? '';

        if (!in_array($alvo, $arquivosAtuais, true)) {
            flashSet('danger', 'Arquivo alvo inválido.');
        } else {
            $erro = validarUpload($_FILES['arquivo'] ?? [], $alvo);
            if ($erro !== null) {
                flashSet('danger', $erro);
            } else {
                $destino = DOCS_DIR . '/' . $alvo;
                if (move_uploaded_file($_FILES['arquivo']['tmp_name'], $destino)) {
                    flashSet('success', 'Arquivo "' . $alvo . '" atualizado com sucesso.');
                } else {
                    flashSet('danger', 'Não foi possível salvar o arquivo no servidor.');
                }
            }
        }
    }

    header('Location: documentos.php');
    exit;
}

$flash = flashGet();
$arquivos = listarArquivosDocs();
$csrf = csrfToken();
?>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Licenças e Modelos — Painel Administrativo | DS Resíduos</title>
    <meta name="robots" content="noindex, nofollow">
    <link rel="shortcut icon" href="../img/icone.png" type="image/x-icon">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
    <link rel="stylesheet" href="../css/ds-modern.css">
    <style>
        body { background: var(--ds-green-bg); font-family: 'Inter', sans-serif; }
        .admin-topbar { background: #fff; border-bottom: 1px solid var(--ds-green-pale); }
        .doc-row { background: #fff; border: 1px solid #eef2f0; border-radius: var(--ds-radius); padding: .9rem 1.1rem; }
        .doc-row + .doc-row { margin-top: .6rem; }
        .doc-name { font-weight: 600; color: var(--ds-green-dark); word-break: break-all; }
        .doc-meta { font-size: .78rem; color: #8b98a3; }
        .card-panel { background: #fff; border-radius: var(--ds-radius-lg); padding: 1.75rem; box-shadow: 0 4px 20px rgba(0,0,0,.04); }
    </style>
</head>
<body>

<div class="admin-topbar py-3 mb-4">
    <div class="container d-flex justify-content-between align-items-center flex-wrap gap-2">
        <div>
            <strong class="text-green-dark"><i class="fas fa-folder-open me-2"></i>Licenças e Modelos</strong>
            <span class="text-muted small ms-2">Painel Administrativo</span>
        </div>
        <div class="d-flex align-items-center gap-3">
            <span class="small text-muted"><i class="fas fa-user-circle me-1"></i><?php echo htmlspecialchars($_SESSION['admin_nome'] ?: $_SESSION['admin_email']); ?></span>
            <a href="logout.php" class="btn btn-sm btn-outline-secondary"><i class="fas fa-sign-out-alt"></i> Sair</a>
        </div>
    </div>
</div>

<div class="container pb-5">

    <?php if ($flash): ?>
        <div class="alert alert-<?php echo htmlspecialchars($flash['type']); ?>"><?php echo htmlspecialchars($flash['message']); ?></div>
    <?php endif; ?>

    <div class="row g-4">
        <div class="col-lg-9 mx-auto">
            <div class="card-panel">
                <h5 class="fw-bold text-green-dark mb-1"><i class="fas fa-list me-2"></i>Arquivos em /docs</h5>
                <p class="small text-muted mb-3">Extensões aceitas: <?php echo implode(', ', DOCS_ALLOWED_EXTENSIONS); ?> · Tamanho máximo: <?php echo tamanhoLegivel(DOCS_MAX_FILE_SIZE); ?></p>

                <?php if (empty($arquivos)): ?>
                    <p class="text-muted small mb-0">Nenhum arquivo encontrado.</p>
                <?php endif; ?>

                <?php foreach ($arquivos as $arquivo):
                    $caminho = DOCS_DIR . '/' . $arquivo;
                    $tamanho = tamanhoLegivel(filesize($caminho));
                    $modificado = date('d/m/Y H:i', filemtime($caminho));
                ?>
                <div class="doc-row d-flex justify-content-between align-items-center flex-wrap gap-2">
                    <div>
                        <div class="doc-name"><i class="fas fa-file me-2"></i><?php echo htmlspecialchars($arquivo); ?></div>
                        <div class="doc-meta"><?php echo $tamanho; ?> · atualizado em <?php echo $modificado; ?></div>
                    </div>
                    <div class="d-flex align-items-center gap-2">
                        <a href="../docs/<?php echo rawurlencode($arquivo); ?>" target="_blank" rel="noopener" class="btn btn-sm btn-outline-secondary" title="Visualizar">
                            <i class="fas fa-eye"></i>
                        </a>
                        <button type="button" class="btn btn-sm btn-outline-primary" data-bs-toggle="collapse" data-bs-target="#sub-<?php echo md5($arquivo); ?>">
                            <i class="fas fa-upload"></i> Substituir
                        </button>
                    </div>
                </div>
                <div class="collapse mt-2" id="sub-<?php echo md5($arquivo); ?>">
                    <form method="post" enctype="multipart/form-data" class="d-flex gap-2 align-items-center flex-wrap ps-2">
                        <input type="hidden" name="csrf_token" value="<?php echo htmlspecialchars($csrf); ?>">
                        <input type="hidden" name="acao" value="substituir">
                        <input type="hidden" name="arquivo_alvo" value="<?php echo htmlspecialchars($arquivo); ?>">
                        <input type="file" name="arquivo" required class="form-control form-control-sm" style="max-width:280px">
                        <button type="submit" class="btn btn-sm btn-ds-primary">Enviar nova versão</button>
                    </form>
                </div>
                <?php endforeach; ?>
            </div>
        </div>
    </div>

</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
