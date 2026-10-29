<?php
$pageTitle       = 'Instruções MTR/SINIR (PR) e SIGOR/CETESB (SP) – DS Resíduos';
$pageDescription = 'Veja as instruções completas para preenchimento do MTR no SINIR (Paraná) e SIGOR/CETESB (São Paulo). Faça o download do guia e atualize seu cadastro.';
$pageKeywords    = 'MTR SINIR instruções, SIGOR CETESB, manifesto transporte resíduos, cadastro RSS, DS Resíduos';
$canonicalUrl    = 'https://www.dsresiduos.com.br/mtr-sinir.php';
?>
<!DOCTYPE html>
<html lang="pt-BR">
<head><?php include 'head-common.php'; ?></head>
<body>
<?php include 'cabecalho.html'; ?>

<section class="ds-page-hero">
    <div class="container">
        <h1 class="page-title">MTR / SINIR / SIGOR</h1>
        <nav aria-label="breadcrumb"><ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="index.php">Início</a></li>
            <li class="breadcrumb-item active">MTR / SINIR</li>
        </ol></nav>
    </div>
</section>

<section class="ds-section">
    <div class="container">

        <div class="ds-alert-info mb-4">
            <i class="fas fa-circle-info"></i>
            <div>
                <strong>Atualize seu e-mail para receber os pesos das coletas!</strong>
                <a href="atualizar-email.php" class="ms-2 text-decoration-underline">Clique aqui para atualizar</a>
            </div>
        </div>

        <div class="row align-items-center g-3 mb-4">
            <div class="col-lg-8">
                <h2 class="section-title" style="font-size:1.5rem">Instruções para Cadastro no Ministério do Meio Ambiente</h2>
                <p>Se você é cliente do <strong>Paraná</strong> ou <strong>São Paulo</strong>, confira abaixo as instruções para fazer o seu cadastro no site do Ministério do Meio Ambiente e emitir o MTR corretamente.</p>
            </div>
            <div class="col-lg-4 text-lg-end">
                <a href="docs/DICAS_SET2025.pdf" target="_blank" rel="noopener" class="ds-download-btn">
                    <i class="fas fa-file-pdf"></i> Baixar Guia PDF
                </a>
            </div>
        </div>

        <div class="row g-3 mb-5">
            <div class="col-md-6">
                <div class="ds-info-card">
                    <div class="card-icon"><i class="fas fa-map-location-dot"></i></div>
                    <p class="card-title">Clientes do Paraná – SINIR</p>
                    <p class="card-text">Acesse o SINIR (Sistema Nacional de Informações sobre a Gestão dos Resíduos Sólidos) para registrar seus resíduos e emitir MTR.</p>
                    <a href="docs/DICAS_SET2025.pdf" target="_blank" rel="noopener" class="card-link">Ver instruções <i class="fas fa-arrow-right fa-xs"></i></a>
                </div>
            </div>
            <div class="col-md-6">
                <div class="ds-info-card">
                    <div class="card-icon"><i class="fas fa-map-location-dot"></i></div>
                    <p class="card-title">Clientes de São Paulo – SIGOR/CETESB</p>
                    <p class="card-text">Acesse o SIGOR (Sistema de Gerenciamento Online de Resíduos Sólidos) da CETESB para São Paulo.</p>
                    <a href="mtr-sp.php" class="card-link">Ver instruções SP <i class="fas fa-arrow-right fa-xs"></i></a>
                </div>
            </div>
        </div>

        <div class="text-center mb-3">
            <h3 class="section-title" style="font-size:1.3rem">Visualização do Guia de Dicas</h3>
            <p class="text-muted">Você pode visualizar o guia abaixo ou fazer o download para salvar no seu computador.</p>
        </div>

        <object data="https://docs.google.com/gview?embedded=true&url=https://dsresiduos.com.br/docs/DICAS_SET2025.pdf"
                class="ds-pdf-embed"
                style="height:1100px;"
                type="application/pdf">
            <div class="text-center py-5">
                <p>Seu navegador não consegue exibir o PDF.</p>
                <a href="docs/DICAS_SET2025.pdf" target="_blank" class="ds-download-btn"><i class="fas fa-download"></i> Baixar PDF</a>
            </div>
        </object>

    </div>
</section>

<?php include 'rodape.html'; ?>
<?php include 'scripts-common.php'; ?>
</body>
</html>