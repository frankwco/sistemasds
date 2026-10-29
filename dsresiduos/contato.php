<?php
$pageTitle       = 'Contato e Orçamento – DS Resíduos em Maringá';
$pageDescription = 'Entre em contato com a DS Resíduos para solicitar orçamento de coleta de lixo hospitalar ou tirar dúvidas sobre destinação de resíduos em Maringá e região noroeste do Paraná.';
$pageKeywords    = 'contato DS Resíduos, orçamento lixo hospitalar, fale conosco resíduos, maringá, telefone DS Resíduos';
$canonicalUrl    = 'https://www.dsresiduos.com.br/contato.php';
?>
<!DOCTYPE html>
<html lang="pt-BR">

<head><?php include 'head-common.php'; ?></head>

<body>
    <?php include 'cabecalho.html'; ?>

    <section class="ds-page-hero">
        <div class="container">
            <h1 class="page-title">Fale Conosco</h1>
            <nav aria-label="breadcrumb">
                <ol class="breadcrumb">
                    <li class="breadcrumb-item"><a href="index.php">Início</a></li>
                    <li class="breadcrumb-item active">Contato</li>
                </ol>
            </nav>
        </div>
    </section>

    <section class="ds-section">
        <div class="container">

            <!-- Contact Info Cards -->
            <div class="row g-3 mb-5">
                <div class="col-md-6 col-lg-3 fade-in-up">
                    <div class="ds-contact-info-card">
                        <div class="ci-icon"><i class="fas fa-phone-alt"></i></div>
                        <div class="ci-title">Comercial / Financeiro</div>
                        <div class="ci-value"><a href="tel:4499323410" class="text-decoration-none text-dark">(44) 9.9932-3410</a></div>
                    </div>
                </div>
                <div class="col-md-6 col-lg-3 fade-in-up" style="transition-delay:.08s">
                    <div class="ds-contact-info-card">
                        <div class="ci-icon"><i class="fas fa-phone"></i></div>
                        <div class="ci-title">Atendimento Geral</div>
                        <div class="ci-value"><a href="tel:4499279338" class="text-decoration-none text-dark">(44) 9.9927-9338</a></div>
                    </div>
                </div>
                <div class="col-md-6 col-lg-3 fade-in-up" style="transition-delay:.16s">
                    <div class="ds-contact-info-card">
                        <div class="ci-icon"><i class="fas fa-envelope"></i></div>
                        <div class="ci-title">E-mail</div>
                        <div class="ci-value"><a href="mailto:contato@dsresiduos.com.br" class="text-decoration-none text-dark" style="font-size:.85rem">contato@dsresiduos.com.br</a></div>
                    </div>
                </div>
                <div class="col-md-6 col-lg-3 fade-in-up" style="transition-delay:.24s">
                    <div class="ds-contact-info-card">
                        <div class="ci-icon"><i class="fas fa-map-marker-alt"></i></div>
                        <div class="ci-title">Endereço</div>
                        <div class="ci-value" style="font-size:.85rem">Rodovia BR 376 - KM 130<br>Nova Esperança – PR</div>
                    </div>
                </div>
            </div>

            <!-- WhatsApp quick links -->
            <div class="row g-3 mb-5">
                <div class="col-md-6 fade-in-up">
                    <a href="https://api.whatsapp.com/send?phone=5544999323410&text=Se quiser orçamento para coletas, informe:%0ASua cidade:%0ARamo de atividade:%0AJá possui alguma outra empresa que já te presta esse serviço?%0A%0ASe for outro assunto,%0Aqual seria?" target="_blank" rel="noopener"
                        class="btn-whatsapp w-100 justify-content-center py-3" style="border-radius:12px; font-size:1rem;">
                        <i class="fab fa-whatsapp fa-lg"></i> WhatsApp Comercial — (44) 9.9932-3410
                    </a>
                </div>
                <div class="col-md-6 fade-in-up" style="transition-delay:.1s">
                    <a href="https://api.whatsapp.com/send?phone=5544999279338&text=Olá! Tenho uma dúvida sobre os serviços da DS Resíduos." target="_blank" rel="noopener"
                        class="btn-whatsapp w-100 justify-content-center py-3" style="border-radius:12px; font-size:1rem; background:#1da851;">
                        <i class="fab fa-whatsapp fa-lg"></i> WhatsApp Geral — (44) 9.9927-9338
                    </a>
                </div>
            </div>

            <!-- Form + Map -->
            <div class="row g-5">
                <div class="col-lg-6 fade-in-up">
                    <div class="ds-form-card">
                        <h3 class="mb-1" style="font-size:1.4rem">Envie uma mensagem</h3>
                        <p class="text-muted mb-4" style="font-size:.9rem">Preencha o formulário e retornaremos em breve.</p>

                        <div id="result" class="mb-3"></div>

                        <div class="mb-3">
                            <label class="ds-form-label" for="inputNome">Nome *</label>
                            <input type="text" class="ds-form-control" id="inputNome" placeholder="Seu nome">
                        </div>
                        <div class="mb-3">
                            <label class="ds-form-label" for="inputTelefone">Telefone / WhatsApp</label>
                            <input type="text" class="ds-form-control" id="inputTelefone" placeholder="(44) 9.0000-0000">
                        </div>
                        <div class="mb-3">
                            <label class="ds-form-label" for="inputEmail">E-mail</label>
                            <input type="email" class="ds-form-control" id="inputEmail" placeholder="seu@email.com">
                        </div>
                        <div class="mb-4">
                            <label class="ds-form-label" for="inputMensagem">Mensagem * </label>
                            <textarea class="ds-form-control" id="inputMensagem" rows="6" placeholder="Descreva sua necessidade...">Se quiser orçamento para coletas, informe:
Sua cidade:
Ramo de atividade:
Outra empresa que já te presta esse serviço:
Se for outro assunto, qual seria?</textarea>
                        </div>
                        <button type="button" id="submit" class="btn-ds-primary w-100 justify-content-center py-3">
                            <i class="fas fa-paper-plane"></i> Enviar Mensagem
                        </button>
                    </div>
                </div>
                <div class="col-lg-6 fade-in-up" style="transition-delay:.15s">
                    <iframe src="https://www.google.com/maps/embed?pb=!1m14!1m12!1m3!1d1544.1128226830745!2d-52.245100928880994!3d-23.17144412537229!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!5e1!3m2!1spt-BR!2sbr!4v1771891642039!5m2!1spt-BR!2sbr"
                        width="100%" height="450"
                        style="border:0; border-radius:16px; box-shadow:0 4px 20px rgba(0,0,0,.12);"
                        allowfullscreen loading="lazy"
                        referrerpolicy="no-referrer-when-downgrade"
                        title="Localização DS Resíduos – Nova Esperança PR"></iframe>
                </div>
            </div>

        </div>
    </section>

    <!-- Modal Sucesso -->
    <div class="modal fade" id="modalSucesso" tabindex="-1" aria-labelledby="modalSucessoLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header border-0">
                    <h5 class="modal-title" id="modalSucessoLabel"><i class="fas fa-check-circle text-success me-2"></i>Mensagem Enviada!</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fechar"></button>
                </div>
                <div class="modal-body text-center py-4">
                    <div class="text-success mb-3" style="font-size:3rem"><i class="fas fa-circle-check"></i></div>
                    <p class="mb-0">Mensagem enviada com sucesso! Em breve entraremos em contato.</p>
                </div>
                <div class="modal-footer border-0 justify-content-center">
                    <button type="button" class="btn-ds-primary" data-bs-dismiss="modal">Fechar</button>
                </div>
            </div>
        </div>
    </div>

    <!-- Modal Erro -->
    <div class="modal fade" id="modalErro" tabindex="-1" aria-labelledby="modalErroLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header border-0">
                    <h5 class="modal-title" id="modalErroLabel"><i class="fas fa-triangle-exclamation text-danger me-2"></i>Erro ao Enviar</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fechar"></button>
                </div>
                <div class="modal-body text-center py-4">
                    <div class="text-danger mb-3" style="font-size:3rem"><i class="fas fa-circle-xmark"></i></div>
                    <p class="mb-0">Algo deu errado. Tente novamente ou entre em contato por telefone/WhatsApp.<br>Pedimos desculpas pelo transtorno!</p>
                </div>
                <div class="modal-footer border-0 justify-content-center">
                    <a href="https://api.whatsapp.com/send?phone=5544999323410" target="_blank" class="btn-whatsapp me-2"><i class="fab fa-whatsapp"></i> WhatsApp</a>
                    <button type="button" class="btn-ds-outline" data-bs-dismiss="modal">Fechar</button>
                </div>
            </div>
        </div>
    </div>

    <?php include 'rodape.html'; ?>
    <?php include 'scripts-common.php'; ?>

    <script src="js/constante.js"></script>
    <script>
        (function() {
            const API = constantes.API_URL;
            const DEFAULT_MENSAGEM = 'Se quiser orçamento para coletas, informe:\nSua cidade:\nRamo de atividade:\nE se já possui alguma outra empresa que já te presta esse serviço.\nSe for outro assunto, qual seria?';

            document.getElementById('submit').addEventListener('click', function() {
                var nome = document.getElementById('inputNome').value.trim();
                var telefone = document.getElementById('inputTelefone').value.trim();
                var email = document.getElementById('inputEmail').value.trim();
                var mensagem = document.getElementById('inputMensagem').value.trim();
                var valid = true;

                // reset borders
                ['inputNome', 'inputTelefone', 'inputEmail', 'inputMensagem'].forEach(function(id) {
                    document.getElementById(id).style.borderColor = '';
                });

                if (!nome) {
                    document.getElementById('inputNome').style.borderColor = 'red';
                    valid = false;
                }
                if (!email && !telefone) {
                    if (!email) document.getElementById('inputEmail').style.borderColor = 'red';
                    if (!telefone) document.getElementById('inputTelefone').style.borderColor = 'red';
                    valid = false;
                }
                if (!mensagem) {
                    document.getElementById('inputMensagem').style.borderColor = 'red';
                    valid = false;
                }

                if (!valid) return;

                var btn = document.getElementById('submit');
                var resultBox = document.getElementById('result');
                var originalBtnHtml = '<i class="fas fa-paper-plane"></i> Enviar Mensagem';

                function showResult(type, text) {
                    var cls = type === 'success' ? 'alert-success' : 'alert-danger';
                    resultBox.innerHTML = '<div class="alert ' + cls + ' mb-0" role="alert">' + text + '</div>';
                }

                function clearForm() {
                    document.getElementById('inputNome').value = '';
                    document.getElementById('inputTelefone').value = '';
                    document.getElementById('inputEmail').value = '';
                    document.getElementById('inputMensagem').value = DEFAULT_MENSAGEM;
                }

                btn.disabled = true;
                btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Enviando...';

                $.ajax({
                    type: 'POST',
                    url: API + '/inserirMensagemSite',
                    contentType: 'application/json',
                    dataType: 'json',
                    timeout: 20000,
                    data: JSON.stringify({
                        nome: nome,
                        email: email,
                        telefones: telefone,
                        mensagem: mensagem
                    }),
                    success: function(data) {
                        var payload = data;
                        if (typeof payload === 'string') {
                            try {
                                payload = JSON.parse(payload);
                            } catch (e) {
                                payload = {};
                            }
                        }

                        var situacao = payload && payload.situacao ? String(payload.situacao).toLowerCase() : '';
                        var isError = payload && payload.type === 'error';
                        var isSuccess = situacao === 'feito' || (payload && payload.type === 'message');

                        if (isError || !isSuccess) {
                            btn.disabled = false;
                            btn.innerHTML = originalBtnHtml;
                            showResult('error', 'Não foi possível enviar sua mensagem. Tente novamente ou fale conosco no WhatsApp.');
                            new bootstrap.Modal(document.getElementById('modalErro')).show();
                            return;
                        }

                        btn.disabled = false;
                        btn.innerHTML = originalBtnHtml;
                        clearForm();
                        showResult('success', 'Mensagem enviada com sucesso! Em breve entraremos em contato.');
                        new bootstrap.Modal(document.getElementById('modalSucesso')).show();
                    },
                    error: function() {
                        btn.disabled = false;
                        btn.innerHTML = originalBtnHtml;
                        showResult('error', 'Não foi possível enviar sua mensagem agora. Verifique sua conexão e tente novamente.');
                        new bootstrap.Modal(document.getElementById('modalErro')).show();
                    },
                    complete: function() {
                        btn.disabled = false;
                        btn.innerHTML = originalBtnHtml;
                    }
                });
            });

            // reset border on input
            ['inputNome', 'inputTelefone', 'inputEmail', 'inputMensagem'].forEach(function(id) {
                document.getElementById(id).addEventListener('input', function() {
                    this.style.borderColor = '';
                });
            });
        })();
    </script>
</body>

</html>