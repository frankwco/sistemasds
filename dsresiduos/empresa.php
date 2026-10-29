<?php
$pageTitle       = 'Sobre a DS Resíduos – Empresa de Gestão de RSS em Maringá e Região';
$pageDescription = 'Conheça a DS Resíduos: equipe multidisciplinar especializada em gestão, coleta, transporte e destinação adequada de Resíduos de Serviços de Saúde em Maringá e região noroeste do PR.';
$pageKeywords    = 'DS Resíduos empresa, gestão ambiental RSS, coleta resíduos saúde maringá, Nova Esperança, equipe técnica, transporte resíduos perigosos';
$canonicalUrl    = 'https://www.dsresiduos.com.br/empresa.php';
?>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<?php include 'head-common.php'; ?>
</head>
<body>
<?php include 'cabecalho.html'; ?>

<!-- Page Hero -->
<section class="ds-page-hero" aria-label="Sobre a empresa">
    <div class="container">
        <h1 class="page-title">Sobre a DS Resíduos</h1>
        <nav aria-label="breadcrumb">
            <ol class="breadcrumb">
                <li class="breadcrumb-item"><a href="index.php">Início</a></li>
                <li class="breadcrumb-item active">Empresa</li>
            </ol>
        </nav>
    </div>
</section>

<!-- Sobre -->
<section class="ds-section">
    <div class="container">
        <div class="row align-items-center g-5">
            <div class="col-lg-6 fade-in-up">
                <span class="badge-green"><i class="fas fa-building me-1"></i> Quem somos</span>
                <h2 class="section-title">Especialistas em Resíduos de Serviços de Saúde</h2>
                <div class="divider-green left"></div>
                <p class="ds-about-text">
                    Nós possuímos uma equipe técnica multidisciplinar pronta para atender às necessidades de nossos clientes no gerenciamento de resíduos, questões técnicas ambientais, gestão ambiental e transporte de produtos perigosos.
                </p>
                <p class="ds-about-text">
                    Nossos motoristas são capacitados com cursos específicos para garantir a segurança no transporte, enquanto nossos operadores são treinados com equipamentos adequados, em conformidade com as normas da ABNT e legislações em vigor.
                </p>
                <p class="ds-about-text">
                    Aqui na <strong>DS Resíduos</strong>, acreditamos em priorizar a qualidade em tudo o que fazemos. Fornecemos soluções práticas e inteligentes para nossos clientes, com o objetivo de melhorar a saúde pública e a qualidade de vida de todos.
                </p>
                <div class="mt-4 d-flex gap-3 flex-wrap">
                    <a href="contato.php" class="btn-ds-primary">
                        <i class="fas fa-clipboard-list"></i> Solicitar Orçamento
                    </a>
                    <a href="https://api.whatsapp.com/send?phone=5544999323410&text=Se quiser orçamento para coletas, informe:%0ASua cidade:%0ARamo de atividade:%0AJá possui alguma outra empresa que já te presta esse serviço?%0A%0ASe for outro assunto,%0Aqual seria?" target="_blank" rel="noopener" class="btn-whatsapp">
                        <i class="fab fa-whatsapp"></i> WhatsApp
                    </a>
                </div>
            </div>
            <div class="col-lg-6 fade-in-up" style="transition-delay:.15s">
                <img src="img/empresa.jpg"
                     alt="Equipe DS Resíduos em campo"
                     class="ds-content-img"
                     loading="lazy" width="600" height="420" />
            </div>
        </div>
    </div>
</section>

<!-- Diferenciais -->
<section class="ds-section ds-section-alt" aria-labelledby="diff-heading">
    <div class="container">
        <div class="text-center mb-5">
            <span class="badge-green"><i class="fas fa-star me-1"></i> Nossos Diferenciais</span>
            <h2 id="diff-heading" class="section-title">Por que escolher a DS Resíduos?</h2>
            <div class="divider-green"></div>
        </div>
        <div class="row g-4">
            <div class="col-md-6 col-lg-3 fade-in-up">
                <div class="ds-info-card text-center">
                    <div class="card-icon mx-auto"><i class="fas fa-certificate"></i></div>
                    <p class="card-title">Totalmente Licenciado</p>
                    <p class="card-text">Alvará, Licença Ambiental, Sanitária, ART e CLF – Polícia Federal em dia.</p>
                </div>
            </div>
            <div class="col-md-6 col-lg-3 fade-in-up" style="transition-delay:.1s">
                <div class="ds-info-card text-center">
                    <div class="card-icon mx-auto"><i class="fas fa-hard-hat"></i></div>
                    <p class="card-title">Equipe Treinada</p>
                    <p class="card-text">Motoristas e operadores com cursos específicos para RSS e transporte de produtos perigosos.</p>
                </div>
            </div>
            <div class="col-md-6 col-lg-3 fade-in-up" style="transition-delay:.2s">
                <div class="ds-info-card text-center">
                    <div class="card-icon mx-auto"><i class="fas fa-mobile-screen-button"></i></div>
                    <p class="card-title">Sistema de Gestão</p>
                    <p class="card-text">Acesso online ao sistema para acompanhar coletas, contratos e certificados.</p>
                </div>
            </div>
            <div class="col-md-6 col-lg-3 fade-in-up" style="transition-delay:.3s">
                <div class="ds-info-card text-center">
                    <div class="card-icon mx-auto"><i class="fas fa-leaf"></i></div>
                    <p class="card-title">Responsabilidade Ambiental</p>
                    <p class="card-text">Destinação final 100% adequada, contribuindo com a preservação do meio ambiente.</p>
                </div>
            </div>
        </div>
    </div>
</section>

<!-- Segmentos Atendidos -->
<section class="ds-section" aria-labelledby="seg-heading">
    <div class="container">
        <div class="row g-5 align-items-start">
            <div class="col-lg-5 fade-in-up">
                <span class="badge-green"><i class="fas fa-stethoscope me-1"></i> Segmentos</span>
                <h2 id="seg-heading" class="section-title">Quem atendemos</h2>
                <div class="divider-green left"></div>
                <p>Atendemos qualquer estabelecimento que gere Resíduos de Serviços de Saúde (RSS) que necessitem de coleta, tratamento e destinação adequada.</p>
                <a href="contato.php" class="btn-ds-primary mt-2">
                    <i class="fas fa-phone-alt"></i> Entre em contato
                </a>
            </div>
            <div class="col-lg-7 fade-in-up" style="transition-delay:.12s">
                <div class="ds-segments-grid">
                    <div class="ds-segment-pill"><i class="fas fa-pills me-1"></i> Farmácias</div>
                    <div class="ds-segment-pill"><i class="fas fa-hospital me-1"></i> Hospitais</div>
                    <div class="ds-segment-pill"><i class="fas fa-user-doctor me-1"></i> Clínicas Médicas</div>
                    <div class="ds-segment-pill"><i class="fas fa-tooth me-1"></i> Odontologia</div>
                    <div class="ds-segment-pill"><i class="fas fa-flask me-1"></i> Laboratórios</div>
                    <div class="ds-segment-pill"><i class="fas fa-pen me-1"></i> Tatuagens</div>
                    <div class="ds-segment-pill"><i class="fas fa-spa me-1"></i> Estética</div>
                    <div class="ds-segment-pill"><i class="fas fa-box me-1"></i> Distribuidores</div>
                    <div class="ds-segment-pill"><i class="fas fa-paw me-1"></i> Hospitais Vet.</div>
                    <div class="ds-segment-pill"><i class="fas fa-cat me-1"></i> Clínicas Vet.</div>
                    <div class="ds-segment-pill"><i class="fas fa-shield-dog me-1"></i> Centro de Zoonoses</div>
                    <div class="ds-segment-pill"><i class="fas fa-ellipsis me-1"></i> E outros...</div>
                </div>
            </div>
        </div>
    </div>
</section>

<!-- CTA -->
<section class="ds-cta-section">
    <div class="container text-center">
        <h2 class="cta-title">Pronto para iniciar a parceria?</h2>
        <p class="cta-text">Entre em contato agora mesmo e solicite um orçamento personalizado para o seu estabelecimento.</p>
        <div class="d-flex gap-3 justify-content-center flex-wrap">
            <a href="contato.php" class="btn-ds-white">
                <i class="fas fa-envelope"></i> Formulário de Contato
            </a>
            <a href="https://api.whatsapp.com/send?phone=5544999323410&text=Olá! Quero conhecer os serviços da DS Resíduos." target="_blank" rel="noopener" class="btn-whatsapp">
                <i class="fab fa-whatsapp"></i> WhatsApp
            </a>
        </div>
    </div>
</section>

<?php include 'rodape.html'; ?>
<?php include 'scripts-common.php'; ?>
</body>
</html>