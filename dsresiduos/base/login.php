<?php
require_once __DIR__ . '/includes/auth.php';
require_once __DIR__ . '/includes/gestaofacil_client.php';

if (isAdminLoggedIn()) {
    header('Location: documentos.php');
    exit;
}

$erro = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (!verifyCsrfToken($_POST['csrf_token'] ?? null)) {
        $erro = 'Sessão expirada, tente novamente.';
    } else {
        // Throttle simples contra força bruta
        $tentativas = $_SESSION['login_attempts'] ?? 0;
        $bloqueadoAte = $_SESSION['login_blocked_until'] ?? 0;

        if ($bloqueadoAte > time()) {
            $erro = 'Muitas tentativas. Aguarde alguns minutos e tente novamente.';
        } else {
            $email = trim($_POST['email'] ?? '');
            $senha = (string) ($_POST['senha'] ?? '');

            $usuario = ($email !== '' && $senha !== '') ? gestaofacilAutenticar($email, $senha) : null;

            if ($usuario !== null) {
                session_regenerate_id(true);
                $_SESSION['admin_logged_in']    = true;
                $_SESSION['admin_email']        = $usuario['email'] ?? $email;
                $_SESSION['admin_nome']         = $usuario['nome'] ?? '';
                $_SESSION['admin_last_activity'] = time();
                unset($_SESSION['login_attempts'], $_SESSION['login_blocked_until']);

                header('Location: documentos.php');
                exit;
            }

            $tentativas++;
            $_SESSION['login_attempts'] = $tentativas;
            if ($tentativas >= 5) {
                $_SESSION['login_blocked_until'] = time() + 300; // 5 minutos
            }
            usleep(400000); // dificulta tentativas automatizadas
            $erro = 'E-mail ou senha inválidos.';
        }
    }
}
?>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login Administrativo | DS Resíduos</title>
    <meta name="robots" content="noindex, nofollow">
    <link rel="shortcut icon" href="../img/icone.png" type="image/x-icon">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
    <link rel="stylesheet" href="../css/ds-modern.css">
    <style>
        body { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: var(--ds-green-bg); font-family: 'Inter', sans-serif; }
        .login-card { width: 100%; max-width: 400px; background: #fff; border-radius: var(--ds-radius-lg); padding: 2.5rem; box-shadow: 0 10px 40px rgba(0,0,0,.08); }
        .login-card h1 { font-size: 1.35rem; font-weight: 700; color: var(--ds-green-dark); margin-bottom: .25rem; }
        .login-card p.subtitle { color: #6b7280; font-size: .9rem; margin-bottom: 1.75rem; }
        .form-control:focus { border-color: var(--ds-green-light); box-shadow: 0 0 0 .2rem rgba(45,122,82,.15); }
    </style>
</head>
<body>
    <div class="login-card">
        <h1><i class="fas fa-lock text-green me-2"></i>Área Administrativa</h1>
        <p class="subtitle">Licenças e Modelos de Documentos</p>

        <?php if ($erro): ?>
            <div class="alert alert-danger py-2 small"><?php echo htmlspecialchars($erro); ?></div>
        <?php endif; ?>

        <form method="post" novalidate>
            <input type="hidden" name="csrf_token" value="<?php echo htmlspecialchars(csrfToken()); ?>">
            <div class="mb-3">
                <label class="form-label small fw-semibold">E-mail</label>
                <input type="email" name="email" class="form-control" required autofocus>
            </div>
            <div class="mb-4">
                <label class="form-label small fw-semibold">Senha</label>
                <input type="password" name="senha" class="form-control" required>
            </div>
            <button type="submit" class="btn-ds-primary w-100 justify-content-center">
                <i class="fas fa-sign-in-alt"></i> Entrar
            </button>
        </form>
        <p class="text-center small text-muted mt-4 mb-0">Acesso restrito à equipe DS Resíduos.<br>Autenticação via sistema GestãoFácil.</p>
    </div>
</body>
</html>
