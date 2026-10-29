<?php
$pageTitle       = 'Atualizar Dados de Acesso – DS Resíduos';
$pageDescription = 'Atualize seus dados cadastrais (e-mail, telefone) para receber informações sobre suas coletas e ter acesso ao portal DS Resíduos.';
$pageKeywords    = 'atualizar dados DS Resíduos, primeiro acesso, alterar email coleta RSS, portal cliente';
$canonicalUrl    = 'https://www.dsresiduos.com.br/atualizar-email.php';
?>
<!DOCTYPE html>
<html lang="pt-BR">
<head><?php include 'head-common.php'; ?></head>
<body>
<?php include 'cabecalho.html'; ?>

<section class="ds-page-hero">
    <div class="container">
        <h1 class="page-title">Atualizar Dados de Acesso</h1>
        <nav aria-label="breadcrumb"><ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="index.php">Início</a></li>
            <li class="breadcrumb-item active">Atualizar Dados</li>
        </ol></nav>
    </div>
</section>

<section class="ds-section">
    <div class="container">
        <div class="row justify-content-center">
            <div class="col-lg-8">

                <div class="text-center mb-5 fade-in-up">
                    <span class="badge-green"><i class="fas fa-user-edit me-1"></i> Primeiro Acesso</span>
                    <h2 class="section-title">Atualize seus dados</h2>
                    <div class="divider-green"></div>
                    <p class="text-muted">Informe seus dados para que possamos atualizar seu cadastro e liberar o acesso ao portal. Você receberá uma notificação por e-mail assim que o processo for concluído.</p>
                </div>

                <div class="ds-form-card fade-in-up">
                    <div id="result" class="mb-3"></div>
                    <div class="row g-3">

                        <div class="col-md-6">
                            <label for="nome" class="form-label fw-semibold">Nome / Razão Social *</label>
                            <small class="d-block text-muted mb-1">Conforme consta no contrato</small>
                            <input type="text" class="form-control" id="nome" placeholder="Nome ou razão social">
                        </div>
                        <div class="col-md-6">
                            <label for="cpfCnpj" class="form-label fw-semibold">CPF ou CNPJ *</label>
                            <small class="d-block text-muted mb-1">Conforme consta no contrato</small>
                            <input type="text" class="form-control" id="cpfCnpj" placeholder="000.000.000-00 ou 00.000.000/0001-00">
                        </div>
                        <div class="col-md-6">
                            <label for="telefone1" class="form-label fw-semibold">Telefone / WhatsApp Principal *</label>
                            <small class="d-block text-muted mb-1">Com DDD</small>
                            <input type="text" class="form-control" id="telefone1" placeholder="(44) 9 0000-0000">
                        </div>
                        <div class="col-md-6">
                            <label for="telefone2" class="form-label fw-semibold">Telefone / WhatsApp Alternativo</label>
                            <small class="d-block text-muted mb-1">Opcional – se diferente do principal</small>
                            <input type="text" class="form-control" id="telefone2" placeholder="(44) 9 0000-0000">
                        </div>
                        <div class="col-md-6">
                            <label for="email1" class="form-label fw-semibold">E-mail Principal *</label>
                            <small class="d-block text-muted mb-1"><strong class="text-danger">Somente este e-mail receberá avisos de coleta</strong></small>
                            <input type="email" class="form-control" id="email1" placeholder="seuemail@dominio.com.br">
                        </div>
                        <div class="col-md-6">
                            <label for="email2" class="form-label fw-semibold">E-mail Alternativo</label>
                            <small class="d-block text-muted mb-1">Não obrigatório – apenas para contato</small>
                            <input type="email" class="form-control" id="email2" placeholder="outro@dominio.com.br">
                        </div>

                        <div class="col-12 mt-2">
                            <button type="button" id="submit" class="btn-ds-primary w-100">
                                <i class="fas fa-paper-plane"></i> Enviar Dados
                            </button>
                        </div>

                    </div>
                </div>

                <p class="text-center text-muted mt-4" style="font-size:.85rem">
                    <i class="fas fa-lock me-1"></i> Seus dados são tratados com privacidade e utilizados exclusivamente para gestão do seu contrato.
                </p>

            </div>
        </div>
    </div>
</section>

<!-- Modal Sucesso -->
<div class="modal fade" id="modalSucesso" tabindex="-1" aria-labelledby="modalSucessoLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header border-0 pb-0">
                <h5 class="modal-title" id="modalSucessoLabel"><i class="fas fa-circle-check text-success me-2"></i>Dados enviados!</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fechar"></button>
            </div>
            <div class="modal-body">
                <div class="alert alert-success mb-0">Informações enviadas com sucesso! Atualizaremos seu cadastro em breve e você será notificado por e-mail.</div>
            </div>
            <div class="modal-footer border-0">
                <button type="button" class="btn-ds-primary" data-bs-dismiss="modal">Fechar</button>
            </div>
        </div>
    </div>
</div>

<!-- Modal Erro -->
<div class="modal fade" id="modalErro" tabindex="-1" aria-labelledby="modalErroLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header border-0 pb-0">
                <h5 class="modal-title" id="modalErroLabel"><i class="fas fa-circle-exclamation text-danger me-2"></i>Ocorreu um problema</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fechar"></button>
            </div>
            <div class="modal-body">
                <div class="alert alert-danger mb-0" id="mensagemErro">Algo deu errado. Por favor, tente novamente.</div>
            </div>
            <div class="modal-footer border-0">
                <button type="button" class="btn-ds-primary" data-bs-dismiss="modal">Fechar</button>
            </div>
        </div>
    </div>
</div>

<?php include 'rodape.html'; ?>
<?php include 'scripts-common.php'; ?>
<script src="js/constante.js"></script>
<script>
$("#submit").click(function () {
    var nome     = $('#nome').val().trim();
    var cpfCnpj  = $('#cpfCnpj').val().trim();
    var telefone1= $('#telefone1').val().trim();
    var telefone2= $('#telefone2').val().trim();
    var email1   = $('#email1').val().trim();
    var email2   = $('#email2').val().trim();
    var flag     = true;

    $('.form-control').css('border-color', '');

    if (!nome)      { $('#nome').css('border-color','red');      flag=false; }
    if (!cpfCnpj)   { $('#cpfCnpj').css('border-color','red');   flag=false; }
    if (!telefone1) { $('#telefone1').css('border-color','red'); flag=false; }
    if (!email1)    { $('#email1').css('border-color','red');    flag=false; }

    if (!flag) return;

    const payload = { nomeRazaoSocial:nome, cpfCnpj, email:email1, email2, telefones:telefone1, telefones2:telefone2 };
    const btn = $('#submit');
    const resultBox = $('#result');
    const originalBtnHtml = '<i class="fas fa-paper-plane"></i> Enviar Dados';

    function showResult(type, text) {
        var cls = type === 'success' ? 'alert-success' : 'alert-danger';
        resultBox.html('<div class="alert ' + cls + ' mb-0" role="alert"><i class="fas fa-' + (type === 'success' ? 'circle-check' : 'circle-exclamation') + ' me-2"></i>' + text + '</div>');
    }

    function clearForm() {
        $('input').val('');
    }

    $.ajax({
        type: 'POST',
        url: constantes.API_URL + '/atualizarDados',
        contentType: 'application/json',
        dataType: 'json',
        timeout: 20000,
        data: JSON.stringify(payload),
        beforeSend: function () {
            btn.attr('disabled', true).html('<i class="fas fa-spinner fa-spin"></i> Enviando...');
            resultBox.html('');
        },
        complete: function () {
            btn.attr('disabled', false).html(originalBtnHtml);
        },
        success: function (data) {
            var payload = data;
            if (typeof payload === 'string') {
                try {
                    payload = JSON.parse(payload);
                } catch (e) {
                    payload = {};
                }
            }

            var status = payload && payload.status ? String(payload.status).toLowerCase() : '';
            var msg = payload && payload.mensagem ? String(payload.mensagem) : 'Operação concluída.';

            if (status === 'ok') {
                clearForm();
                showResult('success', msg);
                setTimeout(function() {
                    new bootstrap.Modal(document.getElementById('modalSucesso')).show();
                }, 500);
            } else {
                showResult('error', msg || 'Erro ao processar sua solicitação.');
                document.getElementById('mensagemErro').textContent = msg || 'Erro desconhecido.';
                setTimeout(function() {
                    new bootstrap.Modal(document.getElementById('modalErro')).show();
                }, 500);
            }
        },
        error: function (xhr, status, error) {
            var errMsg = 'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.';
            if (status === 'timeout') {
                errMsg = 'A requisição demorou muito. Tente novamente em alguns momentos.';
            }
            showResult('error', errMsg);
            document.getElementById('mensagemErro').textContent = errMsg;
            setTimeout(function() {
                new bootstrap.Modal(document.getElementById('modalErro')).show();
            }, 500);
        }
    });
});

$('.form-control').on('keyup', function () { $(this).css('border-color',''); });
</script>
</body>
</html>