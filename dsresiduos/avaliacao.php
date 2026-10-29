<?php
$notaValue = isset($_GET['nota']) ? intval($_GET['nota']) : -1;
$idValue   = isset($_GET['id'])   ? intval($_GET['id'])   : 0;

// Validar intervalo da nota
if ($notaValue < 0 || $notaValue > 10) {
    $notaValue = -1;
}

// Categoria NPS
$categoria = '';
$categoriaColor = '#9e9e9e';
$categoriaText  = '';
if ($notaValue >= 0 && $notaValue <= 6) {
    $categoria      = 'Precisa melhorar';
    $categoriaColor = '#c62828';
    $categoriaText  = 'Você nos indicaria melhorias. Obrigado pela honestidade!';
} elseif ($notaValue >= 7 && $notaValue <= 8) {
    $categoria      = 'Bom';
    $categoriaColor = '#f57f17';
    $categoriaText  = 'Que bom que nosso serviço te agradou! Vamos continuar melhorando.';
} elseif ($notaValue >= 9) {
    $categoria      = 'Excelente';
    $categoriaColor = '#2e7d32';
    $categoriaText  = 'Fico muy feliz com sua avaliação! Sua confiança nos motiva!';
}

// Mapa de cores idêntico ao do e-mail
$cores = [
    0  => '#c62828', 1  => '#d32f2f', 2  => '#e53935', 3 => '#f44336',
    4  => '#ef6c00', 5  => '#f57c00', 6  => '#fb8c00',
    7  => '#f9a825', 8  => '#f9a825',
    9  => '#388e3c', 10 => '#2e7d32'
];

$pageTitle       = 'Avaliação de Serviço – DS Resíduos';
$pageDescription = 'Obrigado por avaliar o serviço da DS Resíduos. Sua opinião é muito importante para nós!';
$pageKeywords    = 'avaliação serviço DS Resíduos, NPS, pesquisa satisfação';
$canonicalUrl    = 'https://www.dsresiduos.com.br/avaliacao';
?>
<!DOCTYPE html>
<html lang="pt-BR">
<head><?php include 'head-common.php'; ?>
<style>
/* ── NPS Buttons ───────────────────────────────────────────────────────── */
.nps-scale { display:flex; flex-wrap:wrap; gap:8px; justify-content:center; margin:16px 0; }
.nps-btn-disp {
    display:inline-flex; align-items:center; justify-content:center;
    width:46px; height:46px; border-radius:50%;
    font-size:15px; font-weight:bold; color:#fff;
    text-decoration:none; transition:transform .2s, box-shadow .2s, opacity .2s;
    cursor:pointer; opacity:.65;
}
.nps-btn-disp:hover {
    opacity:1;
    transform:scale(1.15);
    box-shadow:0 3px 10px rgba(0,0,0,.2);
}
.nps-btn-disp.selected {
    opacity:1;
    transform:scale(1.25);
    box-shadow:0 0 0 3px #fff, 0 0 0 6px currentColor, 0 6px 18px rgba(0,0,0,.28);
}
/* ── Comment card ──────────────────────────────────────────────────────── */
.comment-sent { display:none; }
</style>
</head>
<body>
<?php include 'cabecalho.html'; ?>

<!-- ── Hero ─────────────────────────────────────────────────────────────── -->
<section class="ds-page-hero">
    <div class="container">
        <h1 class="page-title">
            <?php if ($notaValue >= 0): ?>
                Obrigado pela sua avaliação!
            <?php else: ?>
                Pesquisa de Satisfação
            <?php endif; ?>
        </h1>
        <nav aria-label="breadcrumb"><ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="index.php">Início</a></li>
            <li class="breadcrumb-item active">Avaliação</li>
        </ol></nav>
    </div>
</section>

<!-- ── Main Content ──────────────────────────────────────────────────────── -->
<section class="ds-section">
    <div class="container">
        <div class="row justify-content-center">
            <div class="col-lg-7 col-md-9 fade-in-up">

                <!-- ── Score card ─────────────────────────────────────────── -->
                <?php if ($notaValue >= 0): ?>
                <div class="ds-form-card mb-4 text-center">

                    <p style="color:#757575; font-size:.9rem; margin-bottom:6px; text-transform:uppercase; letter-spacing:1px; font-weight:600;">
                        Sua avaliação
                    </p>
                    <p id="notaDisplay" style="font-size:3.5rem; font-weight:800; color:<?= htmlspecialchars($categoriaColor) ?>; line-height:1; margin:0 0 4px;">
                        <span id="notaNum"><?= $notaValue ?></span><span style="font-size:1.2rem; color:#9e9e9e; font-weight:400;">/10</span>
                    </p>
                    <span id="categoriaLabel" style="display:inline-block; background:<?= htmlspecialchars($categoriaColor) ?>; color:#fff; border-radius:20px; padding:4px 18px; font-weight:700; font-size:.9rem; margin-bottom:14px;">
                        <?= htmlspecialchars($categoria) ?>
                    </span>
                    <p id="categoriaTexto" style="color:#616161; font-size:.95rem; margin:0 0 20px;">
                        <?= htmlspecialchars($categoriaText) ?>
                    </p>

                    <!-- Escala com nota destacada -->
                    <div class="nps-scale">
                        <?php foreach (range(0, 10) as $n):
                            $selectedClass = ($n === $notaValue) ? ' selected' : '';
                            $cor = $cores[$n];
                        ?>
                        <span class="nps-btn-disp<?= $selectedClass ?>"
                              data-nota="<?= $n ?>"
                              id="npsBtn<?= $n ?>"
                              style="background:<?= $cor ?>; color:<?= $cor ?>;"
                              title="Nota <?= $n ?>">
                            <span style="color:#fff;"><?= $n ?></span>
                        </span>
                        <?php endforeach; ?>
                    </div>
                    <div class="d-flex justify-content-between" style="font-size:.75rem; color:#9e9e9e; margin-top:4px; padding:0 4px;">
                        <span>Muito Insatisfeito</span>
                        <span>Muito Satisfeito</span>
                    </div>

                </div><!-- /.ds-form-card score -->
                <?php endif; ?>

                <!-- ── Comment form ─────────────────────────────────────── -->
                <div class="ds-form-card" id="formComentario"
                     <?php if ($idValue <= 0): ?>style="opacity:.6; pointer-events:none;"<?php endif; ?>>

                    <h3 style="font-size:1.2rem; margin-bottom:4px;">
                        Deixe um comentário <span style="color:#9e9e9e; font-size:.85rem; font-weight:400;">(opcional)</span>
                    </h3>
                    <p class="text-muted mb-4" style="font-size:.88rem;">
                        Sua mensagem nos ajuda a melhorar. Fique à vontade para escrever.
                    </p>

                    <div class="mb-3">
                        <textarea class="ds-form-control" id="inputComentario" rows="4"
                                  placeholder="Escreva aqui seu comentário..."
                                  maxlength="2000"></textarea>
                        <div style="text-align:right; font-size:.75rem; color:#bbb; margin-top:2px;">
                            <span id="charCount">0</span>/2000
                        </div>
                    </div>

                    <button type="button" id="btnEnviarComentario" class="btn-ds-primary w-100 justify-content-center py-3"
                            <?php if ($idValue <= 0): ?>disabled<?php endif; ?>>
                        <i class="fas fa-paper-plane"></i> Enviar Comentário
                    </button>
                </div>

                <!-- Mensagem pós-envio (oculta inicialmente) -->
                <div class="ds-form-card comment-sent text-center" id="comentarioEnviado">
                    <div style="font-size:3rem; color:#2e7d32; margin-bottom:12px;"><i class="fas fa-circle-check"></i></div>
                    <h4 style="color:#2e7d32;">Comentário enviado!</h4>
                    <p class="text-muted">Muito obrigado pelo retorno. Sua opinião é essencial para nós!</p>
                    <a href="https://www.dsresiduos.com.br" class="btn-ds-primary justify-content-center mt-2">
                        <i class="fas fa-home"></i> Ir para o site
                    </a>
                </div>

            </div><!-- /.col -->
        </div><!-- /.row -->
    </div><!-- /.container -->
</section>

<!-- ── Modal Erro ─────────────────────────────────────────────────────────── -->
<div class="modal fade" id="modalErro" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header border-0">
                <h5 class="modal-title"><i class="fas fa-triangle-exclamation text-danger me-2"></i>Erro ao Enviar</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fechar"></button>
            </div>
            <div class="modal-body text-center py-4">
                <div class="text-danger mb-3" style="font-size:3rem"><i class="fas fa-circle-xmark"></i></div>
                <p class="mb-0">Não foi possível enviar seu comentário. Tente novamente ou entre em contato por WhatsApp.</p>
            </div>
            <div class="modal-footer border-0 justify-content-center">
                <a href="https://api.whatsapp.com/send?phone=5544999323410" target="_blank" class="btn-whatsapp me-2"><i class="fab fa-whatsapp"></i> WhatsApp</a>
                <button type="button" class="btn-ds-outline" data-bs-dismiss="modal">Fechar</button>
            </div>
        </div>
    </div>
</div>

<!-- Toast confirmação nota -->
<div class="position-fixed top-0 end-0 p-3" style="z-index:9999">
    <div id="toastNotaAtualizada" class="toast align-items-center text-bg-success border-0"
         role="alert" aria-live="assertive" aria-atomic="true" data-bs-delay="2500">
        <div class="d-flex">
            <div class="toast-body"><i class="fas fa-check me-2"></i> Nota atualizada!</div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Fechar"></button>
        </div>
    </div>
</div>

<?php include 'rodape.html'; ?>
<?php include 'scripts-common.php'; ?>

<script>
(function () {
    var avaliacaoId  = <?= intval($idValue) ?>;
    var API_COMENTARIO = 'https://alcsistemas.com.br/gestaofacil/rest/avaliacaoNPS/comentario';
    //var API_COMENTARIO = 'http://localhost:8080/gestaofacil/rest/avaliacaoNPS/comentario';
    var API_NOTA = 'https://alcsistemas.com.br/gestaofacil/rest/avaliacaoNPS/atualizarNota';
    //var API_NOTA = 'http://localhost:8080/gestaofacil/rest/avaliacaoNPS/atualizarNota';

    // Contador de caracteres
    var inputComentario = document.getElementById('inputComentario');
    if (inputComentario) {
        inputComentario.addEventListener('input', function () {
            document.getElementById('charCount').textContent = this.value.length;
        });
    }

    var btnEnviar = document.getElementById('btnEnviarComentario');
    if (!btnEnviar || avaliacaoId <= 0) return;

    btnEnviar.addEventListener('click', function () {
        var comentario = document.getElementById('inputComentario').value.trim();

        btnEnviar.disabled = true;
        btnEnviar.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Enviando...';

        $.ajax({
            type: 'POST',
            url: API_COMENTARIO,
            contentType: 'application/json;charset=UTF-8',
            data: JSON.stringify({ id: avaliacaoId, comentario: comentario }),
            success: function (data) {
                document.getElementById('formComentario').style.display  = 'none';
                document.getElementById('comentarioEnviado').style.display = 'block';
            },
            error: function () {
                new bootstrap.Modal(document.getElementById('modalErro')).show();
                btnEnviar.disabled = false;
                btnEnviar.innerHTML = '<i class="fas fa-paper-plane"></i> Enviar Comentário';
            }
        });
    });

    // ── NPS scale – trocar nota ──────────────────────────────────────────
    var CORES_JS = {
        0:'#c62828', 1:'#d32f2f', 2:'#e53935', 3:'#f44336',
        4:'#ef6c00', 5:'#f57c00', 6:'#fb8c00',
        7:'#f9a825', 8:'#f9a825',
        9:'#388e3c', 10:'#2e7d32'
    };
    var CATS_JS = {
        detrator: { label:'Precisa melhorar', cor:'#c62828', texto:'Você nos indicaria melhorias. Obrigado pela honestidade!' },
        passivo:  { label:'Bom',  cor:'#f57f17', texto:'Que bom que nosso serviço te agradou! Vamos continuar melhorando.' },
        promotor: { label:'Excelente', cor:'#2e7d32', texto:'Fico muy feliz com sua avaliação! Sua confiança nos motiva!' }
    };
    function obterCategoria(nota) {
        if (nota <= 6) return CATS_JS.detrator;
        if (nota <= 8) return CATS_JS.passivo;
        return CATS_JS.promotor;
    }

    if (avaliacaoId > 0) {
        document.querySelectorAll('.nps-btn-disp').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var nota = parseInt(this.dataset.nota);
                var self = this;
                self.style.opacity = '.4';

                $.ajax({
                    type: 'POST',
                    url: API_NOTA,
                    contentType: 'application/json;charset=UTF-8',
                    data: JSON.stringify({ id: avaliacaoId, nota: nota }),
                    success: function () {
                        document.querySelectorAll('.nps-btn-disp').forEach(function (b) {
                            b.classList.remove('selected');
                            b.style.opacity = '';
                        });
                        self.classList.add('selected');

                        var cat = obterCategoria(nota);
                        var numEl = document.getElementById('notaNum');
                        if (numEl) { numEl.textContent = nota; }
                        var dispEl = document.getElementById('notaDisplay');
                        if (dispEl) { dispEl.style.color = cat.cor; }
                        var labelEl = document.getElementById('categoriaLabel');
                        if (labelEl) { labelEl.textContent = cat.label; labelEl.style.background = cat.cor; }
                        var textoEl = document.getElementById('categoriaTexto');
                        if (textoEl) { textoEl.textContent = cat.texto; }

                        new bootstrap.Toast(document.getElementById('toastNotaAtualizada')).show();
                    },
                    error: function () {
                        self.style.opacity = '';
                        new bootstrap.Modal(document.getElementById('modalErro')).show();
                    }
                });
            });
        });
    }
})();
</script>
</body>
</html>
