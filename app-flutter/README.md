# Gestão Fácil — Flutter (fluxo de coleta)

Migração do app Ionic/Cordova para Flutter. Já portado: **login, início (veículo/motorista), coleta completa**
(cidade, ponto, pesos, assinatura, envio em segundo plano, pesquisa/exclusão, telefones, coletas do ponto), **dados de
pontos, pontos sem coleta, pontos com avisos, central de alertas, abastecimento, despesa/manutenção, meus lançamentos,
lançamento por IA (foto + voz), relatório de coletas e cobrança por WhatsApp**.

Não portados de propósito: as telas "Cliente", "Contrato" e "Fechamento do mês" do Ionic são só exemplos com dados fixos.

Em celular: o relatório de coletas e os anexos dependem do Firebase (precisa do `env.json`); abastecimento, despesa, IA,
alertas, cobrança e meus lançamentos exigem internet (vão direto ao servidor, como no app antigo).

> **Estado:** `flutter analyze` sem erros, 71 testes passando (incluindo SQLite real e servidor simulado). **Nada disto foi executado
> em aparelho ainda** — as telas novas (lançamentos, IA, relatório, cobrança) só foram compiladas e testadas nas regras puras.

## Como rodar

Crie `env.json` a partir de `env.exemplo.json` (usuário técnico do Firebase; o arquivo está no .gitignore) e:

```bash
cd flutter_app
flutter pub get
flutter analyze
flutter test
flutter run --dart-define-from-file=env.json
flutter build apk --release --dart-define-from-file=env.json
```

Sem `env.json` o app funciona normalmente, só que o backup no Firebase fica desligado (aparece no log).
A pasta `android/` já está no repositório (INTERNET, `<queries>` de telefone/WhatsApp e o nome "DS Resíduos").

O `applicationId` novo (`br.com.dsresiduos.gestaofacil`) é diferente do `io.ionic.starter` do app antigo, então os
dois podem ficar instalados lado a lado durante os testes. Para **substituir** o app antigo, use `--org io.ionic` e
`--project-name starter` e assine com a mesma chave.

Endereço e chave da API podem ser trocados no build: `--dart-define=GF_API_BASE=...` e `--dart-define=GF_API_KEY=...`.

## Decisões de arquitetura (e por que mudaram)

| App Ionic | Flutter | Motivo |
|---|---|---|
| `coletas` e `pontosColeta` como dois arrays JSON gigantes no Ionic Storage (IndexedDB), regravados inteiros a cada alteração | **SQLite** (`sqflite`): uma linha por coleta e por ponto, com índices | Gravar uma coleta mexe numa linha. Listar pontos filtra/pagina no banco (30 por vez) em vez de ler tudo para a memória. Era a origem da lentidão em celular fraco. |
| Fila de gravação (`modificarColetas`), flags de pausa do sync durante a assinatura, "pedido adiado", reserva de envio imediato | Transação SQLite + um `SyncService` serializado | Coleta + atualização do ponto entram na **mesma transação** (ou as duas, ou nenhuma). Não há mais o que pausar nem janela de inconsistência. |
| Assinatura em base64 dentro do storage | **Arquivo PNG** em `documents/assinaturas/`, convertido para base64 só no envio; apagado quando o servidor confirma | Banco leve; assinatura sai do aparelho assim que não é mais necessária. |
| Pesos como texto mascarado (`brmasker`), convertidos com `parseFloat`/`toFixed` | **Inteiros em milésimos** + campo estilo calculadora | Sem erro de ponto flutuante; digitar `5` `2` dá `0,052`. |
| `identificadorApp` = `Date.now() + Math.random()` | Mesmo formato, com `Random.secure()` | Continua sendo a chave de idempotência do reenvio. |
| Flag de login com a chave `"Versao43"` no storage | Tabela `kv` com `manterConectado` | Não depende mais do número da versão. |
| Contador de avisos por cidade como array que só cresce | Uma chave por cidade+dia, as antigas são apagadas | — |
| Timers + `resume` + `online` espalhados (tab1, app.component) | `SyncService` na raiz: a cada 2 min, ao voltar do segundo plano e ao voltar a internet | Continua rodando durante a coleta. |
| Ionic/Angular/RxJS, módulos por página | Flutter + Riverpod; telas sem código gerado (sem `build_runner`) | — |
| WhatsApp/telefone pelo plugin `socialsharing` | `url_launcher` (`wa.me`, `tel:`) | Um plugin a menos. |
| Firebase: Firestore espelhando cada coleta + login técnico com e-mail/senha **fixos no código** | **Mantido** (`FirebaseBackup`): mesmo caminho `coleta/veiculo/<d-m-aaaa>/<idPonto>` e exclusão como `status:false`; login vem do build (`env.json`), não do código | Backup como hoje. Falha no Firebase nunca atrapalha a coleta. |

Comportamentos mantidos de propósito: payload do `cadastrarColeta` (mesmos campos), exclusão lógica reenviada ao
servidor, "lista" que mescla as coletas do servidor sem sobrescrever pendências locais, proteção dos pontos com coleta
pendente contra snapshot antigo do servidor, ordem dos pontos = ordem do servidor, "Coleta Única" que fica coletada até
renovar o contrato, confirmação para peso > 20 kg, conta de somar para excluir, aviso geral de pontos 2×/dia por cidade.

Melhorias de comportamento (pequenas, vale conferir):

- Situação diferente de "Coletado" **zera os pesos** enviados (antes, o que estivesse digitado ia junto).
- Confirma quando o total é zero, quando não há veículo/motorista e quando a assinatura está em branco.
- Duas confirmações ao finalizar viraram uma.
- Ao excluir, o ponto volta ao estado da coleta anterior que sobrou (e o contador do mês é decrementado), em vez de
  só zerar a data.
- Pontos novos que chegam na sincronização incremental entram na hora (antes só na completa), desde que venham com cidade e grupos.
- Sem internet, o envio para na primeira falha em vez de tentar todas as coletas.
- Coletas já enviadas há mais de 90 dias saem do aparelho.
- O horário da última sincronização de pontos é o do **início** da chamada (antes, o do fim), para não perder alterações feitas durante o download.

## Pontos para você confirmar

1. **Firebase.** Usa as opções web do projeto (sem `google-services.json`). Se o Firestore recusar o app Android, registre o app no console e passe o `appId` com `--dart-define=GF_FB_APP_ID=...`.
2. **Credenciais expostas.** `app.component.ts` e `tab1.page.ts` do app Ionic têm e-mail e senha do Firebase escritos no
   código (e a chave `X-App-Key` está em `constantes.service.ts`). Estão no histórico do git: vale **trocar a senha do
   usuário Firebase** e, se possível, a chave da API.
3. **`versaoApp`.** A coleta passa a enviar `Flutter1` (antes `Versao43`). Se o backend usa esse campo para algo além de
   registro, ajuste `Config.versaoApp`.
4. **`applicationId`** (ver acima) e se o Flutter vai substituir ou conviver com o app Ionic.

## Estrutura

```
lib/
  main.dart, app.dart, providers.dart
  core/       config, conversões tolerantes de JSON, formatação/normalização, widgets comuns
  data/       db (SQLite), repositórios (pontos, coletas, catálogo, sessão), api_client,
              sync_service, coleta_service, coleta_payload (regras puras)
  features/   login, home, coleta (coleta, assinatura, pesquisar pontos/coletas, telefones, coletas do ponto)
test/         regras puras: peso, normalização, payload, ponto, item de lista
```

## Testes do módulo de coleta

`test/fluxo_coleta_test.dart` roda o fluxo com **SQLite real** (em memória, via `sqflite_common_ffi`) e um servidor HTTP
simulado: gravar coleta (transação coleta + ponto), envio (sucesso, erro 500, sem internet, idempotência do
`identificadorApp`, envios simultâneos do app e da tarefa em segundo plano), exclusão (inclusive **excluir durante o
envio**), mescla da "lista" do servidor, sincronização de pontos (completa, incremental, protegidos, resposta vazia),
rodada completa, migração do banco e o **backup no Firebase** (inclusão, exclusão, falha com nova tentativa, corrida).
O Firebase é substituído por um falso (`BackupRemoto`); o contato real com o Firestore só se verifica no aparelho.

## Garantia do backup no Firebase

Cada coleta guarda `fb` (0 em dia, 1 falta gravar, 2 falta gravar a exclusão). Só zera quando o Firestore confirma;
falhou, tenta de novo a cada rodada de sincronização e na tarefa em segundo plano. A assinatura só sai do aparelho
depois de servidor **e** Firebase receberem. Build sem `env.json` desliga o backup e a tela inicial avisa.
