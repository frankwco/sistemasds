<?php
/**
 * head-common.php — Bloco <head> compartilhado entre todas as páginas DS Resíduos 2026
 *
 * Variáveis esperadas antes do include:
 *   $pageTitle       — título da página (sem sufixo)
 *   $pageDescription — meta description
 *   $pageKeywords    — meta keywords (opcional)
 *   $canonicalUrl    — URL canônica completa (opcional)
 *   $ogImage         — imagem OG (opcional, default: og-default.jpg)
 */
$siteBase    = 'https://www.dsresiduos.com.br';
$siteName    = 'DS Resíduos – Gestão de Resíduos de Saúde';
$titleSuffix = ' | DS Resíduos';
$ogImg       = isset($ogImage) ? $ogImage : 'img/og-default.png';
$canonical   = isset($canonicalUrl) ? $canonicalUrl : ($siteBase . '/' . basename($_SERVER['PHP_SELF']));
?>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title><?php echo htmlspecialchars($pageTitle . $titleSuffix); ?></title>
<meta name="description" content="<?php echo htmlspecialchars($pageDescription); ?>">
<?php if(!empty($pageKeywords)): ?>
<meta name="keywords" content="<?php echo htmlspecialchars($pageKeywords); ?>">
<?php endif; ?>

<!-- Canonical -->
<link rel="canonical" href="<?php echo $canonical; ?>">

<!-- Open Graph -->
<meta property="og:type"        content="website">
<meta property="og:site_name"   content="<?php echo $siteName; ?>">
<meta property="og:title"       content="<?php echo htmlspecialchars($pageTitle . $titleSuffix); ?>">
<meta property="og:description" content="<?php echo htmlspecialchars($pageDescription); ?>">
<meta property="og:url"         content="<?php echo $canonical; ?>">
<meta property="og:image"       content="<?php echo $siteBase . '/' . $ogImg; ?>">
<meta property="og:image:width"  content="1200">
<meta property="og:image:height" content="630">
<meta property="og:locale"      content="pt_BR">

<!-- Twitter Card -->
<meta name="twitter:card"        content="summary_large_image">
<meta name="twitter:title"       content="<?php echo htmlspecialchars($pageTitle . $titleSuffix); ?>">
<meta name="twitter:description" content="<?php echo htmlspecialchars($pageDescription); ?>">
<meta name="twitter:image"       content="<?php echo $siteBase . '/' . $ogImg; ?>">

<!-- Favicon -->
<link rel="shortcut icon" href="img/icone.png" type="image/x-icon">
<link rel="icon" href="img/icone.png" type="image/x-icon">

<!-- Preconnect fonts / CDNs -->
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="preconnect" href="https://cdn.jsdelivr.net">

<!-- Google Fonts: Inter + Poppins -->
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&family=Poppins:wght@600;700;800&display=swap" rel="stylesheet">

<!-- Bootstrap 5.3 -->
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
      integrity="sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH"
      crossorigin="anonymous">

<!-- Font Awesome 6 -->
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css"
      integrity="sha512-DTOQO9RWCH3ppGqcWaEA1BIZOC6xxalwEsw9c2QQeAIftl+Vegovlnee1c9QX4TctnWMn13TZye+giMm8e2LwA=="
      crossorigin="anonymous" referrerpolicy="no-referrer">

<!-- DS Modern CSS -->
<link rel="stylesheet" href="css/ds-modern.css">

<!-- Schema.org LocalBusiness structured data -->
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "LocalBusiness",
  "name": "DS Resíduos – Gestão de Resíduos de Saúde",
  "url": "https://www.dsresiduos.com.br",
  "logo": "https://www.dsresiduos.com.br/img/logo_nova.png",
  "image": "https://www.dsresiduos.com.br/img/og-default.jpg",
  "description": "Especialistas em coleta, transporte, tratamento e destinação final de Resíduos de Serviços de Saúde em Maringá e região noroeste do Paraná.",
  "telephone": ["+55 44 9 9932-3410", "+55 44 9 9927-9338"],
  "email": "contato@dsresiduos.com.br",
  "address": {
    "@type": "PostalAddress",
    "streetAddress": "Rodovia BR 376 - KM 130 – Gleba Capelinha",
    "addressLocality": "Nova Esperança",
    "addressRegion": "PR",
    "postalCode": "87600-000",
    "addressCountry": "BR"
  },
  "geo": {
    "@type": "GeoCoordinates",
    "latitude": -23.17904,
    "longitude": -52.22469
  },
  "openingHoursSpecification": [
    {
      "@type": "OpeningHoursSpecification",
      "dayOfWeek": ["Monday","Tuesday","Wednesday","Thursday","Friday"],
      "opens": "08:00",
      "closes": "18:00"
    }
  ],
  "sameAs": [
    "https://www.facebook.com/dsresiduos",
    "https://www.instagram.com/dsresiduos"
  ],
  "areaServed": [
    {"@type":"City","name":"Maringá","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Paranavaí","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Sarandi","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Nova Esperança","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Nova Londrina","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Paiçandu","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Mandaguari","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Loanda","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Terra Rica","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Colorado","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Astorga","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Marialva","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Cruzeiro do Sul","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Querência do Norte","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Tamboara","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Alto Paraná","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Amaporã","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Ângulo","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Atalaia","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Barbosa Ferraz","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Diamante do Norte","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Floraí","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Flórida","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Guairaçá","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Iguaraçu","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Inajá","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Itaguajé","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Itaúna do Sul","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Jardim Olinda","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Lobato","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Mandaguaçu","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Marilena","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Mirador","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Munhoz de Melo","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Nossa Senhora das Graças","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Nova Aliança do Ivaí","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Ourizona","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Paraíso do Norte","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Paranacity","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Paranapoema","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Pitangueiras","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Planaltina do Paraná","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Porto Rico","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Presidente Castelo Branco","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Rondon","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Santa Cruz de Monte Castelo","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Santa Fé","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Santa Isabel do Ivaí","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Santa Mônica","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Santo Antônio do Caiuá","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Santo Inácio","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"São Carlos do Ivaí","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"São João do Caiuá","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"São Jorge do Ivaí","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"São Pedro do Paraná","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Uniflor","containedInPlace":{"@type":"State","name":"Paraná"}},
    {"@type":"City","name":"Rosana","containedInPlace":{"@type":"State","name":"São Paulo"}},
    {"@type":"City","name":"Teodoro Sampaio","containedInPlace":{"@type":"State","name":"São Paulo"}},
    {"@type":"City","name":"Euclides da Cunha Paulista","containedInPlace":{"@type":"State","name":"São Paulo"}},
    {"@type":"City","name":"Primavera","containedInPlace":{"@type":"State","name":"São Paulo"}}
  ],
  "serviceType": ["Coleta de Resíduos de Saúde", "Transporte de Resíduos Perigosos", "Destinação Final de RSS", "Gestão de Resíduos de Serviços de Saúde"]
}
</script>

<!-- Schema.org WebSite -->
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "WebSite",
  "name": "DS Resíduos",
  "url": "https://www.dsresiduos.com.br",
  "potentialAction": {
    "@type": "SearchAction",
    "target": "https://www.dsresiduos.com.br/coleta.php?q={search_term_string}",
    "query-input": "required name=search_term_string"
  }
}
</script>

<?php
// BreadcrumbList schema — gerado automaticamente para páginas internas
$isHomepage = ($canonical === $siteBase . '/' || $canonical === $siteBase . '/index.php');
if (!$isHomepage && isset($pageTitle)):
    // Monta um breadcrumb simples: Início > Título da Página
    $breadcrumbTitle = isset($breadcrumbLabel) ? $breadcrumbLabel : $pageTitle;
    $breadcrumbTitle = preg_replace('/\s*[–|-].*$/', '', $breadcrumbTitle); // remove sufixo "– DS Resíduos" se houver
    $breadcrumbTitle = trim($breadcrumbTitle);
?>
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "BreadcrumbList",
  "itemListElement": [
    {
      "@type": "ListItem",
      "position": 1,
      "name": "Início",
      "item": "https://www.dsresiduos.com.br/"
    },
    {
      "@type": "ListItem",
      "position": 2,
      "name": "<?php echo addslashes($breadcrumbTitle); ?>",
      "item": "<?php echo $canonical; ?>"
    }
  ]
}
</script>
<?php endif; ?>
