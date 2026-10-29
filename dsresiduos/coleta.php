<?php
$pageTitle       = 'Coleta de Resíduos Hospitalares em Maringá, Paranavaí e Região';
$pageDescription = 'DS Resíduos realiza coleta, transporte e destinação final de RSS (Resíduos de Serviços de Saúde) em Maringá, Paranavaí, Sarandi, Nova Esperança, Nova Londrina, Paiçandu e mais de 55 municípios do norte e noroeste do Paraná e oeste de São Paulo.';
$pageKeywords    = 'coleta resíduos saúde maringá, coleta lixo hospitalar paranavaí, coleta RSS sarandi, coleta resíduos nova esperança, coleta resíduos nova londrina, coleta resíduos paiçandu, coleta resíduos hospitalar noroeste paraná, lixo hospitalar, perfurocortantes, destinação final RSS, DS Resíduos';
$canonicalUrl    = 'https://www.dsresiduos.com.br/coleta.php';
?>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<?php include 'head-common.php'; ?>
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "Service",
  "serviceType": "Coleta de Resíduos de Serviços de Saúde",
  "name": "Coleta de RSS – DS Resíduos",
  "description": "Serviço regular de coleta, transporte e destinação final de Resíduos de Serviços de Saúde (RSS) classes A, A2, A3, A4, B e E, atendendo clínicas, hospitais, laboratórios, farmácias, veterinárias, tatuagens e fazendas em mais de 55 cidades do noroeste do Paraná e oeste de São Paulo.",
  "provider": {
    "@type": "LocalBusiness",
    "name": "DS Resíduos",
    "url": "https://www.dsresiduos.com.br"
  },
  "areaServed": [
    {"@type": "City", "name": "Maringá"},
    {"@type": "City", "name": "Paranavaí"},
    {"@type": "City", "name": "Sarandi"},
    {"@type": "City", "name": "Nova Esperança"},
    {"@type": "City", "name": "Paiçandu"},
    {"@type": "City", "name": "Nova Londrina"}
  ],
  "hasOfferCatalog": {
    "@type": "OfferCatalog",
    "name": "Classes de RSS coletadas",
    "itemListElement": [
      {"@type": "Offer", "itemOffered": {"@type": "Service", "name": "Coleta Classe A – Infectantes"}},
      {"@type": "Offer", "itemOffered": {"@type": "Service", "name": "Coleta Classe A2 – Animais Contaminados"}},
      {"@type": "Offer", "itemOffered": {"@type": "Service", "name": "Coleta Classe A3 – Peças Anatômicas Humanas"}},
      {"@type": "Offer", "itemOffered": {"@type": "Service", "name": "Coleta Classe A4 – Infectantes Não Especificados"}},
      {"@type": "Offer", "itemOffered": {"@type": "Service", "name": "Coleta Classe B – Químicos"}},
      {"@type": "Offer", "itemOffered": {"@type": "Service", "name": "Coleta Classe E – Perfurocortantes"}}
    ]
  }
}
</script>
</head>
<body>
<?php include 'cabecalho.html'; ?>

<section class="ds-page-hero">
    <div class="container">
        <h1 class="page-title">Coleta de Lixo Hospitalar</h1>
        <nav aria-label="breadcrumb"><ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="index.php">Início</a></li>
            <li class="breadcrumb-item active">Coleta</li>
        </ol></nav>
    </div>
</section>

<section class="ds-section">
    <div class="container">
        <div class="row align-items-center g-5">
            <div class="col-lg-6 fade-in-up">
                <span class="badge-green"><i class="fas fa-truck-medical me-1"></i> Coleta Especializada</span>
                <h2 class="section-title">Coleta Regular e Segura de RSS</h2>
                <div class="divider-green left"></div>
                <p class="ds-about-text">
                    Soluções práticas e inteligentes, proporcionando uma melhoria na saúde pública, na qualidade de vida e bem-estar dos seus clientes, colaboradores e comunidade em geral.
                </p>
                <p class="ds-about-text">
                    A <strong>DS Resíduos</strong> é especializada em coleta de Resíduos de Serviços de Saúde, com equipe de profissionais qualificados e oferece um eficiente serviço de coleta, transporte e encaminhamento para destinação final de resíduos de serviços de saúde (lixo hospitalar).
                </p>
                <div class="mt-4 d-flex gap-3 flex-wrap">
                    <a href="contato.php" class="btn-ds-primary"><i class="fas fa-clipboard-list"></i> Solicitar Coleta</a>
                    <a href="https://api.whatsapp.com/send?phone=5544999323410&text=Olá! Preciso de coleta de resíduos hospitalares." target="_blank" rel="noopener" class="btn-whatsapp"><i class="fab fa-whatsapp"></i> WhatsApp</a>
                </div>
            </div>
            <div class="col-lg-6 fade-in-up" style="transition-delay:.15s">
                <img src="img/coleta.jpg" alt="Coleta de resíduos hospitalares" class="ds-content-img" loading="lazy" width="600" height="420"/>
            </div>
        </div>
    </div>
</section>

<section class="ds-section ds-section-alt">
    <div class="container">
        <div class="text-center mb-5">
            <span class="badge-green"><i class="fas fa-recycle me-1"></i> Tipos de Resíduos</span>
            <h2 class="section-title">Classificação dos RSS que coletamos</h2>
            <div class="divider-green"></div>
        </div>
        <div class="row g-4">
            <div class="col-md-6 col-lg-4 fade-in-up">
                <div class="ds-info-card">
                    <div class="card-icon" style="background:#fce4e4;color:#c0392b"><i class="fas fa-biohazard"></i></div>
                    <p class="card-title">Grupo A – Infectantes</p>
                    <p class="card-text">Resíduos com possível presença de agentes biológicos: curativos, gazes, materiais descartáveis contaminados.</p>
                </div>
            </div>
            <div class="col-md-6 col-lg-4 fade-in-up" style="transition-delay:.1s">
                <div class="ds-info-card">
                    <div class="card-icon" style="background:#fff3cd;color:#856404"><i class="fas fa-flask"></i></div>
                    <p class="card-title">Grupo B – Químicos</p>
                    <p class="card-text">Resíduos com substâncias químicas: reagentes, solventes, desinfetantes, medicamentos vencidos.</p>
                </div>
            </div>
           <!--  <div class="col-md-6 col-lg-4 fade-in-up" style="transition-delay:.2s">
                <div class="ds-info-card">
                    <div class="card-icon" style="background:#e6f4ff;color:#1f5aa6"><i class="fas fa-clipboard-check"></i></div>
                    <p class="card-title">Segregação Correta</p>
                    <p class="card-text">Orientação para separação e acondicionamento seguro dos resíduos no ponto de geração.</p>
                </div>
            </div> -->
            <div class="col-md-6 col-lg-4 fade-in-up" style="transition-delay:.1s">
                <div class="ds-info-card">
                    <div class="card-icon" style="background:#fdecea;color:#b42318"><i class="fas fa-dna"></i></div>
                    <p class="card-title">Grupo A2</p>
                    <p class="card-text">Carcaças, peças anatômicas e vísceras de animais submetidos a experimentação com suspeita de contaminação biológica.</p>
                </div>
            </div>
            <div class="col-md-6 col-lg-4 fade-in-up" style="transition-delay:.15s">
                <div class="ds-info-card">
                    <div class="card-icon" style="background:#fdecec;color:#c0392b"><i class="fas fa-procedures"></i></div>
                    <p class="card-title">Grupo A3</p>
                    <p class="card-text">Peças anatômicas humanas, produtos de fecundação sem sinais vitais e outros materiais anatômicos definidos em norma.</p>
                </div>
            </div>
            <div class="col-md-6 col-lg-4 fade-in-up" style="transition-delay:.2s">
                <div class="ds-info-card">
                    <div class="card-icon" style="background:#fff7e6;color:#8a6116"><i class="fas fa-band-aid"></i></div>
                    <p class="card-title">Grupo A4</p>
                    <p class="card-text">Materiais de assistência à saúde sem presença de sangue livre, como kits, descartáveis e sobras de procedimentos.</p>
                </div>
            </div>
            <div class="col-md-6 col-lg-4 fade-in-up" style="transition-delay:.2s">
                <div class="ds-info-card">
                    <div class="card-icon" style="background:#fce4e4;color:#c0392b"><i class="fas fa-syringe"></i></div>
                    <p class="card-title">Grupo E – Perfurocortantes</p>
                    <p class="card-text">Objetos que podem perfurar ou cortar: agulhas, bisturis, lâminas e ampolas de vidro.</p>
                </div>
            </div>
          <!--   <div class="col-md-6 col-lg-4 fade-in-up" style="transition-delay:.3s">
                <div class="ds-info-card">
                    <div class="card-icon"><i class="fas fa-file-contract"></i></div>
                    <p class="card-title">Documentação Completa</p>
                    <p class="card-text">MTR, romaneios e certificados de destinação disponíveis no sistema de gestão online.</p>
                </div>
            </div> -->
        </div>
    </div>
</section>

<section class="ds-section" id="cidades-atendidas">
    <div class="container">
        <div class="text-center mb-5">
            <span class="badge-green"><i class="fas fa-map-marker-alt me-1"></i> Área de Atuação</span>
            <h2 class="section-title">Cidades Atendidas – Coleta de Resíduos de Saúde</h2>
            <div class="divider-green"></div>
            <p class="ds-about-text mx-auto" style="max-width:720px">
                Realizamos <strong>coleta e transporte de resíduos de serviços de saúde (RSS)</strong> nas cidades abaixo, na região norte e noroeste do Paraná e oeste de São Paulo. Não encontrou seu município? <a href="contato.php" class="text-green-dark fw-semibold">Entre em contato</a> e consulte disponibilidade.
            </p>
        </div>

        <div class="row g-4 mb-4">
            <div class="col-12">
                <h3 class="fw-bold mb-3" style="font-size:1.1rem;color:var(--ds-green-dark)"><i class="fas fa-map me-2"></i>Paraná</h3>
                <div class="row row-cols-2 row-cols-sm-3 row-cols-md-4 row-cols-lg-5 g-2">
                    <?php
                    $cidades_pr = [
                        'Alto Paraná','Amaporã','Ângulo','Astorga','Atalaia',
                        'Barbosa Ferraz','Colorado','Cruzeiro do Sul','Diamante do Norte',
                        'Floraí','Flórida','Guairaçá','Iguaraçu','Inajá','Itaguajé',
                        'Itaúna do Sul','Jardim Olinda','Loanda','Lobato','Mandaguaçu',
                        'Mandaguari','Marialva','Marilena','Maringá','Mirador',
                        'Munhoz de Melo','N. Sra. das Graças','Nova Aliança do Ivaí',
                        'Nova Esperança','Nova Londrina','Ourizona','Paiçandu',
                        'Paraíso do Norte','Paranacity','Paranapoema','Paranavaí',
                        'Pitangueiras','Planaltina do Paraná','Porto Rico',
                        'Pres. Castelo Branco','Querência do Norte','Rondon',
                        'Sta. Cruz de M. Castelo','Santa Fé','Santa Isabel do Ivaí',
                        'Santa Mônica','Santo Antônio do Caiuá','Santo Inácio',
                        'São Carlos do Ivaí','São João do Caiuá','São Jorge do Ivaí',
                        'São Pedro do Paraná','Sarandi','Tamboara','Terra Rica','Uniflor'
                    ];
                    $destaques_pr = ['Maringá','Paranavaí','Sarandi'];
                    foreach ($cidades_pr as $cidade):
                        $bold = in_array($cidade, $destaques_pr) ? ' fw-bold' : '';
                    ?>
                    <div class="col">
                        <div class="d-flex align-items-center gap-1" style="font-size:.88rem">
                            <i class="fas fa-circle-check text-success" style="font-size:.75rem;flex-shrink:0"></i>
                            <span class="<?= $bold ?>">
                                <?php if(in_array($cidade, $destaques_pr)): ?>
                                    <a href="contato.php" class="text-green-dark text-decoration-none"><?= $cidade ?></a>
                                <?php else: echo $cidade; endif; ?>
                            </span>
                        </div>
                    </div>
                    <?php endforeach; ?>
                </div>
            </div>

            <div class="col-12 mt-3">
                <h3 class="fw-bold mb-3" style="font-size:1.1rem;color:var(--ds-green-dark)"><i class="fas fa-map me-2"></i>São Paulo</h3>
                <div class="row row-cols-2 row-cols-sm-3 row-cols-md-4 row-cols-lg-5 g-2">
                    <?php
                    $cidades_sp = ['Rosana', 'Primavera','Teodoro Sampaio', 'Euclides da Cunha Paulista'];
                    foreach ($cidades_sp as $cidade): ?>
                    <div class="col">
                        <div class="d-flex align-items-center gap-1" style="font-size:.88rem">
                            <i class="fas fa-circle-check text-success" style="font-size:.75rem;flex-shrink:0"></i>
                            <span><?= $cidade ?></span>
                        </div>
                    </div>
                    <?php endforeach; ?>
                </div>
            </div>
        </div>

        <div class="alert alert-success border-0 mt-4" role="note" style="background:#edfaf1;border-left:4px solid var(--ds-green) !important;border-radius:8px">
            <i class="fas fa-info-circle me-2 text-success"></i>
            <strong>Coleta de resíduos hospitalares em Maringá e região</strong> — atendemos clínicas, consultórios, hospitais, laboratórios, farmácias, clínicas veterinárias, fazendas e demais geradores de RSS com rotas programadas e frequência personalizada.
        </div>
    </div>
</section>

<section class="ds-cta-section">
    <div class="container text-center">
        <h2 class="cta-title">Precisa de coleta regular de RSS?</h2>
        <p class="cta-text">Entre em contato agora e receba uma proposta personalizada para o seu estabelecimento.</p>
        <div class="d-flex gap-3 justify-content-center flex-wrap">
            <a href="contato.php" class="btn-ds-white"><i class="fas fa-clipboard-list"></i> Solicitar Orçamento</a>
            <a href="https://api.whatsapp.com/send?phone=5544999323410&text=Olá! Preciso de coleta de resíduos hospitalares." target="_blank" rel="noopener" class="btn-whatsapp"><i class="fab fa-whatsapp"></i> WhatsApp</a>
        </div>
    </div>
</section>

<?php include 'rodape.html'; ?>
<?php include 'scripts-common.php'; ?>
</body>
</html>