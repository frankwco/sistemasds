/// Configuração fixa do app. Endereço e chave da API podem ser trocados no build
/// (`--dart-define=GF_API_BASE=...` / `--dart-define=GF_API_KEY=...`) sem mexer no código.
class Config {
  const Config._();

  static const apiBase = String.fromEnvironment(
    'GF_API_BASE',
    defaultValue: 'https://alcsistemas.com.br/gestaofacil/rest/app/',
  );

  /// Mesma chave do app Ionic (precisa ser idêntica à do backend, ChaveApiFilter).
  static const apiKey = String.fromEnvironment(
    'GF_API_KEY',
    defaultValue: 'gf-app-7f3a9c2e-51b6-4d8a-9c0f-2e6a8b1d4f77',
  );

  /// Vai em cada coleta (campo `versaoApp`) para o backend saber qual app a enviou.
  static const versaoApp = 'Flutter1';

  static const timeoutHttp = Duration(seconds: 30);
  /// Envio de coleta: o servidor grava (e responde) com calma; 30 s era curto e gerava reenvio à toa.
  static const timeoutEnvioColeta = Duration(seconds: 90);
  static const intervaloSync = Duration(minutes: 2);

  /// Coletas já enviadas somem do aparelho depois deste prazo (o servidor guarda o histórico).
  static const retencaoColetasEnviadas = Duration(days: 90);

  /// Peso acima do qual o app pede confirmação (em kg).
  static const pesoSuspeitoKg = 20;

  static const whatsappSuporte = '554499323410';
}
