import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Storage } from '@ionic/storage';
import { DatePipe } from '@angular/common';
import { ConstantesService } from './constantes.service';
import { timeout } from 'rxjs/operators';
import * as moment from 'moment';
import { AngularFireStorage } from '@angular/fire/storage';


@Injectable({
  providedIn: 'root'
})
export class AtualizarDadosService {

  // ─── Controle de Sincronização ──────────────────────────

  /** Impede chamadas simultâneas — flags separadas para não deixar uma
   * sincronização travada (ex: pontos) bloquear a outra (ex: coletas/exclusões) */
  private isSyncingColetas = false;
  private isSyncingPontos = false;
  /** Pedido de sincronização de coletas que chegou com outra em andamento (um só, ver syncColetasSafe). */
  private coletasRepetir: { sincronizarPontos: boolean; considerarData: string } | null = null;

  /** Pausa as sincronizações automáticas (ex.: durante a tela de assinatura, para não
   * disputar o Storage com a gravação da coleta). O que for adiado roda em retomarSync(). */
  private syncPausado = false;
  private syncAdiado = false;

  /** Timers controlados */
  private timerColetas: any = null;
  private timerPontos: any = null;

  /** Intervalos em ms */
  private readonly INTERVALO_COLETAS = 2 * 60 * 1000;   // 2 minutos
  private readonly INTERVALO_PONTOS  = 2 * 60 * 1000;   // 2 minutos
  private readonly HTTP_TIMEOUT      = 30 * 1000;        // 30 segundos por request

  constructor(
    private constantes: ConstantesService,
    private storage: Storage,
    private http: HttpClient,
    private storageFire: AngularFireStorage
  ) {}

  // ─── Helpers ────────────────────────────────────────────

  /** URL base da API */
  private get api(): string {
    return this.constantes.enderecoApi();
  }

  /** Busca genérica: GET → salva no Storage */
  private async syncEntity<T>(endpoint: string, storageKey: string): Promise<T> {
    try {
      const data = await this.http.get<T>(`${this.api}${endpoint}`).toPromise();
      await this.storage.set(storageKey, data);
      return data;
    } catch (error) {
      console.error(`[Sync] Falha ao sincronizar "${storageKey}":`, error);
      this.tratarErro(error);
      throw error;
    }
  }

  // ─── Controle de Timers ─────────────────────────────────

  /**
   * Inicia os timers de sincronização automática.
   * Seguro para chamar múltiplas vezes — não duplica timers.
   */
  startAutoSync(): void {
    // Timer de coletas pendentes (safety net)
    if (!this.timerColetas) {
      this.timerColetas = setInterval(async () => {
        console.log('[Sync] Timer: sincronizando coletas...');
        await this.syncColetasSafe(true, 'sim');
      }, this.INTERVALO_COLETAS);
    }

    // Timer de pontos de coleta (mais frequente)
    if (!this.timerPontos) {
      this.timerPontos = setInterval(async () => {
        console.log('[Sync] Timer: sincronizando pontos...');
        await this.syncPontosSafe();
      }, this.INTERVALO_PONTOS);
    }

    console.log('[Sync] Auto-sync iniciado (coletas e pontos a cada 2min)');
  }

  /**
   * Para todos os timers de sincronização.
   */
  stopAutoSync(): void {
    if (this.timerColetas) {
      clearInterval(this.timerColetas);
      this.timerColetas = null;
    }
    if (this.timerPontos) {
      clearInterval(this.timerPontos);
      this.timerPontos = null;
    }
    console.log('[Sync] Auto-sync parado');
  }

  /** Suspende as sincronizações até retomarSync(). Chamadas feitas enquanto pausado são adiadas. */
  pausarSync(): void {
    this.syncPausado = true;
  }

  /**
   * Libera as sincronizações. Com `forcar` (acabou de gravar uma coleta nova) envia só
   * as coletas pendentes + pontos, sem rebaixar os catálogos; sem `forcar`, roda uma
   * sincronização completa apenas se alguma foi adiada durante a pausa.
   */
  retomarSync(forcar = false): void {
    if (!this.syncPausado) {
      return;
    }
    this.syncPausado = false;
    const adiado = this.syncAdiado;
    this.syncAdiado = false;
    if (adiado) {
      this.atualizar(true, 'sim');
    } else if (forcar) {
      this.syncColetasSafe(true, 'sim');
    }
  }

  /**
   * Limpeza única: o histórico mensal de coletas ('coletasHistorico<mes><ano>', com a
   * assinatura em base64 de cada coleta) deixou de ser usado e só ocupava o Storage.
   */
  async limparHistoricoColetasAntigo(): Promise<void> {
    try {
      if (await this.storage.get('historicoColetasLimpo')) {
        return;
      }
      const chaves: string[] = await this.storage.keys();
      for (const chave of chaves) {
        if (chave.startsWith('coletasHistorico')) {
          await this.storage.remove(chave);
        }
      }
      await this.storage.set('historicoColetasLimpo', true);
    } catch (error) {
      console.warn('[Storage] Falha ao limpar histórico antigo de coletas:', error);
    }
  }

  /**
   * Wrapper seguro para enviar coletas — evita chamadas simultâneas.
   *
   * Um pedido que chega com outra sincronização em andamento não é descartado: a que
   * está rodando já montou sua lista de pendentes antes dessa coleta existir, então
   * ela só seria enviada no próximo ciclo do timer (até 2 min). Fica guardado um único
   * pedido (os que chegarem nesse meio-tempo viram um só) e roda assim que a atual termina.
   */
  async syncColetasSafe(sincronizarPontos: boolean, considerarData: string): Promise<void> {
    if (this.syncPausado) {
      this.syncAdiado = true;
      return;
    }
    if (this.isSyncingColetas) {
      console.log('[Sync] Sincronização de coletas já em andamento, nova rodada ao terminar...');
      const anterior = this.coletasRepetir;
      this.coletasRepetir = {
        sincronizarPontos: sincronizarPontos || !!(anterior && anterior.sincronizarPontos),
        // 'nao' (substituição total dos pontos) prevalece sobre o incremental
        considerarData: considerarData === 'nao' || (anterior && anterior.considerarData === 'nao') ? 'nao' : considerarData,
      };
      return;
    }
    this.isSyncingColetas = true;
    try {
      let pedido = { sincronizarPontos, considerarData };
      while (pedido) {
        this.coletasRepetir = null;
        await this.enviarColetas(pedido.sincronizarPontos, pedido.considerarData);
        pedido = this.coletasRepetir;
        if (pedido && this.syncPausado) {
          // Abriu a tela de assinatura durante a rodada: fica para quando retomar.
          this.syncAdiado = true;
          pedido = null;
        }
      }
    } finally {
      this.coletasRepetir = null;
      this.isSyncingColetas = false;
    }
  }

  /**
   * Wrapper seguro para atualizar pontos — evita chamadas simultâneas.
   */
  private async syncPontosSafe(): Promise<void> {
    if (this.syncPausado) {
      this.syncAdiado = true;
      return;
    }
    if (this.isSyncingPontos) {
      console.log('[Sync] Sincronização de pontos já em andamento, ignorando...');
      return;
    }
    this.isSyncingPontos = true;
    try {
      await this.atualizarPontosColeta('sim');
    } finally {
      this.isSyncingPontos = false;
    }
  }

  // ─── API Pública ────────────────────────────────────────

  /**
   * Sincronização completa (1x ao abrir o app / voltar do background):
   * envia coletas + baixa dados de referência.
   */
  async atualizar(sincronizarPontos = true, considerarData = 'sim'): Promise<void> {
    if (this.syncPausado) {
      this.syncAdiado = true;
      return;
    }

    // Envia coletas pendentes e atualiza pontos
    await this.syncColetasSafe(sincronizarPontos, considerarData);

    // Dados de referência — em paralelo
    const entidades = [
      { endpoint: 'cidades', key: 'cidades' },
      { endpoint: 'veiculos', key: 'veiculos' },
      { endpoint: 'motoristas', key: 'motoristas' },
      { endpoint: 'fornecedores', key: 'fornecedores' },
      { endpoint: 'tiposDocumento', key: 'tiposDocumento' },
      { endpoint: 'categoriasDespesa', key: 'categoriasDespesa' },
    ];

    const promessas = entidades.map(e =>
      this.syncEntity(e.endpoint, e.key).catch(err => {
        console.warn(`[Sync] ${e.key} falhou:`, err);
        return null;
      })
    );

    await Promise.all(promessas);
  }

  /** Testa se a API está acessível */
  async testeSincronizacao(): Promise<void> {
    try {
      await this.http.get(`${this.api}processos`).toPromise();
    } catch (_) { /* silencioso — apenas teste */ }
  }

  // ─── Enviar Coletas ─────────────────────────────────────

  /**
   * Coletas (identificadorApp) cujo envio imediato — logo ao finalizar — está reservado ou
   * em andamento. A fila normal as pula, para a mesma coleta não ser enviada duas vezes.
   */
  private enviandoAgora = new Set<string>();

  /** Reserva o envio imediato da coleta. Chamar ANTES de gravá-la em 'coletas'. */
  reservarEnvioImediato(identificadorApp: any): void {
    this.enviandoAgora.add(String(identificadorApp));
  }

  /** Desfaz a reserva (ex.: a gravação local falhou); a fila normal volta a enxergá-la. */
  cancelarEnvioImediato(identificadorApp: any): void {
    this.enviandoAgora.delete(String(identificadorApp));
  }

  /**
   * Envia UMA coleta recém-finalizada na hora, sem esperar a fila de sincronização (que
   * pode estar ocupada com outra rodada ou só rodar no próximo ciclo do timer). Se falhar,
   * a coleta continua pendente (`sincronizado: false`) e a fila normal a reenvia.
   * Precisa de reservarEnvioImediato() antes de a coleta ser gravada em 'coletas'.
   */
  async enviarColetaAgora(coleta: any): Promise<void> {
    const id = String(coleta.identificadorApp);
    let excluidaNoMeioDoCaminho = false;
    try {
      const usuario: any = (await this.storage.get('usuario')) || {};
      await this.http
        .post<any[]>(`${this.api}cadastrarColeta`, [{ ...coleta, acessoTotal: usuario.acessoTotal }])
        .pipe(timeout(this.HTTP_TIMEOUT))
        .toPromise();

      await this.modificarColetas(atuais => {
        for (const c of atuais) {
          if (String(c.identificadorApp) !== id) {
            continue;
          }
          if (c.status === false) {
            // Excluída enquanto o envio estava no ar: a exclusão ainda precisa ir ao servidor.
            excluidaNoMeioDoCaminho = true;
          } else {
            c.sincronizado = true;
            // Remove assinatura (base64 pesado) de coletas já enviadas para liberar Storage
            delete c.assinatura;
          }
        }
      });
      console.log(`[Sync] Coleta ${id} enviada na hora`);
    } catch (error) {
      console.error(`[Sync] Envio imediato da coleta ${id} falhou (a fila normal reenvia):`, error);
    } finally {
      this.enviandoAgora.delete(id);
    }

    if (excluidaNoMeioDoCaminho) {
      this.syncColetasSafe(true, 'sim');
    }
  }

  /** Fila das gravações em 'coletas': uma por vez, cada uma relendo o Storage no seu turno. */
  private filaColetas: Promise<void> = Promise.resolve();

  /**
   * Único caminho para ALTERAR a lista 'coletas' (finalizar, excluir, sincronizar).
   * Ler-alterar-gravar sem fila deixava uma sincronização em andamento regravar a
   * lista velha por cima de uma coleta recém-lançada, que "sumia" até o servidor
   * devolvê-la. `alterar` recebe a lista atual e devolve a nova (ou altera no lugar).
   */
  modificarColetas(alterar: (coletas: any[]) => any[] | void): Promise<void> {
    const vez = this.filaColetas.then(async () => {
      const atuais: any[] = (await this.storage.get('coletas')) || [];
      const novas = alterar(atuais) || atuais;
      await this.storage.set('coletas', novas);
    });
    this.filaColetas = vez.catch(() => undefined);
    return vez;
  }

  /**
   * Lê coletas locais não sincronizadas, envia ao servidor UMA POR UMA
   * e depois faz request "lista" para atualizar dados do servidor.
   */
  async enviarColetas(sincronizarPontos: boolean, considerarData: string): Promise<void> {
    const coletas: any[] = (await this.storage.get('coletas')) || [];
    const usuario: any = (await this.storage.get('usuario')) || {};
    const veiculo: any = (await this.storage.get('veiculoSelecionado'))
      || { id: null, descricao: '', placa: '' };

    // Filtra coletas ainda não sincronizadas — inclui as que foram apagadas
    // localmente (status: false): é este envio a "cadastrarColeta" que efetiva
    // a exclusão no backend (relacional). Sem reenviá-las aqui, uma exclusão
    // feita offline nunca é confirmada no servidor quando a internet volta.
    // (menos as que estão sendo enviadas na hora, ao finalizar — ver enviarColetaAgora)
    const pendentes = coletas.filter(c => !c.sincronizado && !this.enviandoAgora.has(String(c.identificadorApp)));

    // Rastreia quais coletas foram enviadas com sucesso pelo identificadorApp
    const enviadosComSucesso = new Set<string>();

    // Envia cada coleta individualmente (evita falha em lote)
    if (pendentes.length > 0) {
      console.log(`[Sync] ${pendentes.length} coleta(s) pendente(s) para enviar`);

      for (const coleta of pendentes) {
        const payload = [{ ...coleta, acessoTotal: usuario.acessoTotal }];
        try {
          await this.http
            .post<any[]>(`${this.api}cadastrarColeta`, payload)
            .pipe(timeout(this.HTTP_TIMEOUT))
            .toPromise();

          enviadosComSucesso.add(String(coleta.identificadorApp));
          console.log(`[Sync] Coleta ${coleta.identificadorApp} enviada com sucesso`);
        } catch (error) {
          console.error(`[Sync] Falha ao enviar coleta ${coleta.identificadorApp}:`, error);
          // Continua tentando as próximas — esta será reenviada no próximo ciclo
        }
      }

      // Relê coletas do storage para não sobrescrever coletas criadas durante o envio
      if (enviadosComSucesso.size > 0) {
        await this.modificarColetas(atuais => {
          for (const c of atuais) {
            if (c.identificadorApp && enviadosComSucesso.has(String(c.identificadorApp))) {
              c.sincronizado = true;
              // Remove assinatura (base64 pesado) de coletas já enviadas para liberar Storage
              if (c.assinatura) {
                delete c.assinatura;
              }
            }
          }
        });
      }
    }

    // Request "lista" para receber as coletas atualizadas do servidor
    const agora = new Date();
    const listaPayload = [{
      acessoTotal: usuario.acessoTotal,
      tipo: 'lista',
      identificadorApp: '',
      status: false,
      veiculo,
      diaUltimaColeta: agora.getDate(),
      mesUltimaColeta: agora.getMonth() + 1,
      anoUltimaColeta: agora.getFullYear(),
      horaUltimaColeta: agora.getHours(),
      minutoUltimaColeta: agora.getMinutes(),
      segundoUltimaColeta: agora.getSeconds(),
    }];

    await this.sincronizarColeta(listaPayload, sincronizarPontos, considerarData);
  }

  // ─── Sincronizar Coleta (POST + merge) ──────────────────

  private async sincronizarColeta(
    payload: any[],
    sincronizarPontos: boolean,
    considerarData: string
  ): Promise<void> {
    try {
      const retornoServidor: any[] = await this.http
        .post<any[]>(`${this.api}cadastrarColeta`, payload)
        .pipe(timeout(this.HTTP_TIMEOUT))
        .toPromise();

      // Relê coletas do storage APÓS a chamada HTTP (dentro da fila) para capturar
      // qualquer coleta adicionada enquanto aguardava a resposta do servidor
      await this.modificarColetas(coletasAtuais => this.mesclarColetas(retornoServidor, coletasAtuais));

      // Atualiza pontos de coleta se solicitado
      if (sincronizarPontos) {
        await this.atualizarPontosColeta(considerarData);
      }

    } catch (error) {
      console.error('[Sync] Falha ao sincronizar coletas:', error);
      this.tratarErro(error);
    }
  }

  /** Junta a resposta "lista" do servidor com as coletas locais (regras nos comentários abaixo). */
  private mesclarColetas(retornoServidor: any[], coletasAtuais: any[]): any[] {
    // Coletas excluídas localmente (soft-delete, status: false) cujo envio de
    // exclusão ainda não foi confirmado pelo backend (retry fica a cargo do
    // loop de "pendentes" acima). Enquanto isso, esta "lista" pode responder
    // ainda com o item ativo — nesses casos a versão local excluída prevalece,
    // para a coleta não "voltar a aparecer" antes da exclusão ser processada.
    const excluidosLocalmente = new Set<string>(
      coletasAtuais
        .filter(c => c.status === false && c.identificadorApp)
        .map(c => String(c.identificadorApp))
    );

    // Merge: usa identificadorApp como chave única
    const mapaServidor = new Map<string, any>();
    retornoServidor.forEach(item => {
      if (item.identificadorApp) {
        mapaServidor.set(String(item.identificadorApp), item);
      }
    });

    // Descarta do retorno do servidor qualquer item já excluído localmente
    const merged = retornoServidor.filter(
      item => !item.identificadorApp || !excluidosLocalmente.has(String(item.identificadorApp))
    );

    // Preserva TODA coleta local que o servidor não devolveu nesta "lista" —
    // pendente, excluída ou já sincronizada. "Não veio na lista do servidor"
    // não é garantia de que ela já foi processada/persistida do lado de lá
    // (pode ser atraso de processamento, uma regra própria de ponto/dia, etc.);
    // já vimos coleta sincronizada "sumir" da listagem por causa disso, então
    // nunca descartamos um registro local só por ausência na resposta.
    for (const local of coletasAtuais) {
      if (!local.identificadorApp) continue;
      const id = String(local.identificadorApp);
      // Sempre repõe o tombstone local (a versão do servidor foi descartada
      // acima) e qualquer registro que a resposta do servidor não trouxe.
      if (excluidosLocalmente.has(id) || !mapaServidor.has(id)) {
        merged.push(local);
      }
    }

    // Remove a assinatura (base64 pesado) de coletas já sincronizadas para liberar Storage
    merged.forEach(c => {
      if (c.sincronizado && c.assinatura) {
        delete c.assinatura;
      }
    });

    return merged;
  }

  // ─── Atualizar Pontos de Coleta ─────────────────────────

  async atualizarPontosColeta(considerarData: string): Promise<void> {
    try {
      const datepipe = new DatePipe('en-US');

      // Determina a data de referência para busca incremental
      let dataParam: string;

      if (considerarData === 'nao') {
        dataParam = 'nao';
      } else {
        const ultimaAtualizacao = await this.storage.get('dataUltimaAtualizacao');
        if (ultimaAtualizacao) {
          const comMargem = (moment as any)(ultimaAtualizacao).subtract(3, 'minutes').toDate();
          dataParam = datepipe.transform(comMargem, 'yyyy-MM-dd HH:mm:ss') || '2000-03-14 22:38:05';
        } else {
          dataParam = '2000-03-14 22:38:05';
        }
      }

      // Busca pontos da API
      const pontos: any[] = await this.http
        .get<any[]>(`${this.api}todosPontosColetaReduzidos?dataUltimaAtualizacao=${dataParam}`)
        .pipe(timeout(this.HTTP_TIMEOUT))
        .toPromise();

      // Incremental sem nenhum ponto novo no servidor (o caso comum a cada 2 min): não há o que
      // mesclar, então não lê nem regrava as listas grandes de coletas e pontos no Storage.
      // Só vale para uma lista vazia de verdade: resposta nula/inválida segue o caminho antigo,
      // que lança erro e não avança o timestamp (para a próxima rodada não perder mudanças).
      const nadaMudou = considerarData !== 'nao' && Array.isArray(pontos) && pontos.length === 0;
      if (!nadaMudou) {
        await this.mesclarPontos(pontos, considerarData);
      } else if (!(await this.storage.keys()).includes('pontosColeta')) {
        // Mantém o efeito de antes: a chave passa a existir (vazia) mesmo se ainda não havia
        // sincronização completa; telas como "pontos com avisos" fazem forEach direto nela.
        await this.storage.set('pontosColeta', []);
      }

      // Registra timestamp da última sincronização
      const agora = datepipe.transform(new Date(), 'yyyy-MM-dd HH:mm:ss');
      await this.storage.set('dataUltimaAtualizacao', agora);

    } catch (error) {
      console.error('[Sync] Falha ao atualizar pontos de coleta:', error);
      this.tratarErro(error);
    }
  }

  /** Mescla os pontos vindos do servidor com os guardados no aparelho (regras nos comentários). */
  private async mesclarPontos(pontos: any[], considerarData: string): Promise<void> {
    // Pontos com coleta local pendente (criada ou excluída, ainda não
    // confirmada pelo backend): "diaUltimaColeta/mesUltimaColeta/anoUltimaColeta"
    // desses pontos foram atualizados otimisticamente no app (para a tela
    // "Pesquisar Pontos" refletir na hora) e não podem ser sobrescritos por
    // um snapshot do servidor que ainda não processou essa pendência — senão
    // o ponto volta a mostrar o estado antigo até a próxima sincronização.
    const coletasLocais: any[] = (await this.storage.get('coletas')) || [];
    const pontosProtegidos = new Set<number>(
      coletasLocais
        .filter(c => !c.sincronizado && c.pontoColetaSelecionado && c.pontoColetaSelecionado.id != null)
        .map(c => c.pontoColetaSelecionado.id)
    );

    if (considerarData === 'nao') {
      // Substituição total, preservando os pontos protegidos como estão localmente
      const mapaNovosTotal = new Map<number, any>();
      pontos.forEach(p => mapaNovosTotal.set(p.id, p));
      const armazenadosTotal: any[] = (await this.storage.get('pontosColeta')) || [];
      const mapaAtualTotal = new Map<number, any>(armazenadosTotal.map(p => [p.id, p]));

      const resultado = pontos.map(p => pontosProtegidos.has(p.id) ? (mapaAtualTotal.get(p.id) || p) : p);
      // Mantém localmente pontos protegidos que porventura não vieram nesta resposta
      for (const protegidoId of pontosProtegidos) {
        if (!mapaNovosTotal.has(protegidoId) && mapaAtualTotal.has(protegidoId)) {
          resultado.push(mapaAtualTotal.get(protegidoId));
        }
      }
      await this.storage.set('pontosColeta', resultado);
    } else {
      // Merge incremental: atualiza somente pontos existentes.
      // Novos pontos são adicionados apenas na sincronização completa (considerarData='nao')
      // para garantir que o objeto tenha todos os campos necessários.
      const armazenados: any[] = (await this.storage.get('pontosColeta')) || [];
      const mapaNovos = new Map<number, any>();
      pontos.forEach(p => mapaNovos.set(p.id, p));

      const merged = armazenados.map(item =>
        pontosProtegidos.has(item.id) ? item : (mapaNovos.get(item.id) || item)
      );
      await this.storage.set('pontosColeta', merged);
    }
  }

  // ─── Buscar Coletas (download completo) ─────────────────

  async buscarColetas(): Promise<void> {
    try {
      const data = await this.http.get<any[]>(`${this.api}buscarColetas`).toPromise();
      await this.storage.set('coletas', data);
      // Após receber coletas, atualiza pontos (sem filtro de data)
      await this.atualizarPontosColeta('nao');
    } catch (error) {
      console.error('[Sync] Falha ao buscar coletas:', error);
      this.tratarErro(error);
    }
  }

  // ─── Lançamentos de Veículo (Abastecimento/Despesa/Serviço) ─────

  /**
   * Salva o lançamento DIRETO no backend — sem fila offline, sem "será
   * sincronizado depois" (diferente do padrão de coletas, de propósito:
   * abastecimento/despesa/serviço/captura por IA só fazem sentido com
   * confirmação real e imediata do servidor). Exige conexão — se falhar, a
   * exceção sobe pra quem chamou decidir a mensagem exibida; só é
   * considerado salvo quando o backend responde com o registro (com id).
   */
  async salvarLancamentoAgora(lancamento: any): Promise<any> {
    const retorno: any[] = await this.postComRetryDeConexao<any[]>(`${this.api}lancamentos`, [lancamento]);
    return retorno && retorno[0];
  }

  /** Lançamentos de um motorista, pra tela "Meus Lançamentos" — só os que ele mesmo fez. */
  async buscarMeusLancamentosAgora(motoristaId: number): Promise<any[]> {
    return this.http.get<any[]>(`${this.api}lancamentosMotorista?motoristaId=${motoristaId}`)
      .pipe(timeout(this.HTTP_TIMEOUT)).toPromise();
  }

  /** Exclui (soft-delete no backend) um lançamento feito errado — o backend confere se é do próprio motorista. */
  async excluirLancamentoAgora(id: number, motoristaId: number): Promise<void> {
    await this.http.delete<void>(`${this.api}lancamentos/${id}?motoristaId=${motoristaId}`)
      .pipe(timeout(this.HTTP_TIMEOUT)).toPromise();
  }

  /**
   * POST com uma única retentativa automática, só quando a falha é
   * "status 0" (sem resposta nenhuma do servidor — nem erro HTTP de
   * verdade). Visto na prática na Captura por IA: logo após a chamada longa
   * de IA (foto/áudio, até 170s), a primeira tentativa de salvar às vezes
   * cai com esse erro mesmo com internet normal — provável reconexão de
   * rádio/dados do aparelho ainda em andamento. Não repete em erro HTTP real
   * (4xx/5xx), só nesse caso específico de "sem conexão".
   */
  private async postComRetryDeConexao<T>(url: string, body: any): Promise<T> {
    try {
      return await this.http.post<T>(url, body).pipe(timeout(this.HTTP_TIMEOUT)).toPromise();
    } catch (error) {
      if (!error || error.status !== 0) {
        throw error;
      }
      console.warn(`[API] Falha de conexão em ${url}, tentando novamente em 2s...`);
      await new Promise(resolve => setTimeout(resolve, 2000));
      return await this.http.post<T>(url, body).pipe(timeout(this.HTTP_TIMEOUT)).toPromise();
    }
  }

  /**
   * Sobe o anexo (RF09) direto pro Firebase Storage e registra os metadados
   * no backend — síncrono, sem fila. Só pode ser chamado depois que o
   * lançamento já tem id (o caminho no Storage depende dele).
   */
  async enviarAnexoAgora(lancamentoId: number, fotoBase64: string, nomeArquivo: string): Promise<void> {
    const caminho = await this.uploadAnexoStorage(lancamentoId, fotoBase64, nomeArquivo);
    await this.http
      .post(`${this.api}anexos`, {
        lancamento: { id: lancamentoId },
        nomeArquivo,
        caminho,
        contentType: 'image/jpeg',
        tamanho: Math.round((fotoBase64.length * 3) / 4),
      })
      .pipe(timeout(this.HTTP_TIMEOUT))
      .toPromise();
  }

  private async uploadAnexoStorage(lancamentoId: number, fotoBase64: string, nomeArquivo: string): Promise<string> {
    const agora = new Date();
    const ano = agora.getFullYear();
    const mes = String(agora.getMonth() + 1).padStart(2, '0');
    const uuid = `${Date.now()}-${Math.random().toString(36).slice(2)}`;
    const caminho = `anexos/${ano}/${mes}/${lancamentoId}/${uuid}-${nomeArquivo}`;

    const dataUrl = fotoBase64.startsWith('data:') ? fotoBase64 : `data:image/jpeg;base64,${fotoBase64}`;
    await this.storageFire.ref(caminho).putString(dataUrl, 'data_url');
    return caminho;
  }

  // ─── Catálogos ao vivo (fornecedor/posto, categoria) ────
  //
  // Diferente do cache de "atualizar()" (que fica desatualizado até o
  // próximo ciclo de sincronização), estes buscam direto do backend toda
  // vez que a tela abre — pro cadastro de Fornecedores/Categorias feito no
  // admin aparecer no app na mesma hora, sem esperar o app "sincronizar".

  async buscarFornecedoresAgora(): Promise<any[]> {
    return this.http.get<any[]>(`${this.api}fornecedores`).pipe(timeout(this.HTTP_TIMEOUT)).toPromise();
  }

  /** Cadastra um fornecedor/posto que o motorista não achou na lista, direto pelo app. */
  async criarFornecedorAgora(nome: string, tipo: string = null): Promise<any> {
    return this.http.post<any>(`${this.api}fornecedores`, { nome, tipo }).pipe(timeout(this.HTTP_TIMEOUT)).toPromise();
  }

  async buscarCategoriasDespesaAgora(): Promise<any[]> {
    return this.http.get<any[]>(`${this.api}categoriasDespesa`).pipe(timeout(this.HTTP_TIMEOUT)).toPromise();
  }

  async buscarVeiculosAgora(): Promise<any[]> {
    return this.http.get<any[]>(`${this.api}veiculos`).pipe(timeout(this.HTTP_TIMEOUT)).toPromise();
  }

  async buscarMotoristasAgora(): Promise<any[]> {
    return this.http.get<any[]>(`${this.api}motoristas`).pipe(timeout(this.HTTP_TIMEOUT)).toPromise();
  }

  /** Central de alertas ao vivo — sem cache, sem fallback offline (a tela decide o que mostrar se falhar). */
  async buscarAlertasAgora(): Promise<any[]> {
    return this.http.get<any[]>(`${this.api}alertas`).pipe(timeout(this.HTTP_TIMEOUT)).toPromise();
  }

  // ─── Cobrança (WhatsApp) ────────────────────────────────
  // Só vale pra usuário com acessoSistema=true (ver tab1.page.html) — o backend também
  // confere isso a cada chamada e devolve 403 se não tiver.

  async buscarCobrancasAgora(usuarioId: number): Promise<any> {
    return this.http.get<any>(`${this.api}cobrancas?usuarioId=${usuarioId}`)
      .pipe(timeout(this.HTTP_TIMEOUT)).toPromise();
  }

  async confirmarWhatsappAgora(usuarioId: number, clienteId: number, numeroTelefone: number): Promise<any> {
    return this.http.post<any>(`${this.api}cobrancas/whatsapp/confirmar`, { usuarioId, clienteId, numeroTelefone })
      .pipe(timeout(this.HTTP_TIMEOUT)).toPromise();
  }

  async desmarcarWhatsappAgora(usuarioId: number, clienteId: number, numeroTelefone: number): Promise<any> {
    return this.http.post<any>(`${this.api}cobrancas/whatsapp/desmarcar`, { usuarioId, clienteId, numeroTelefone })
      .pipe(timeout(this.HTTP_TIMEOUT)).toPromise();
  }

  async registrarEnvioWhatsappAgora(usuarioId: number, clienteId: number, numeroTelefone: number): Promise<any> {
    return this.http.post<any>(`${this.api}cobrancas/whatsapp/registrarEnvio`, { usuarioId, clienteId, numeroTelefone })
      .pipe(timeout(this.HTTP_TIMEOUT)).toPromise();
  }

  /** Extrai algo legível de um erro HTTP (status, mensagem) pra mostrar na tela sem precisar de debug remoto. */
  descreverErro(error: any): string {
    if (!error) { return 'erro desconhecido'; }
    if (typeof error === 'string') { return error; }
    if (error.status === 0) { return 'sem conexão com a internet'; }
    if (error.status != null) { return `HTTP ${error.status}${error.statusText ? ' ' + error.statusText : ''}`; }
    if (error.message) { return error.message; }
    if (error.code != null) { return `código ${error.code}`; } // ex.: FileError do plugin de arquivo
    try {
      return JSON.stringify(error).slice(0, 120);
    } catch {
      return 'erro desconhecido';
    }
  }

  // ─── Captura assistida por IA (RF11/RF12) ───────────────

  /**
   * Envia o áudio/foto pro backend interpretar (Gemini) e devolve os campos
   * extraídos pro app mostrar a tela de confirmação (RF14) — nada é salvo
   * aqui, é só a interpretação. Timeout de quase 3 minutos (maior que o
   * timeout de leitura do próprio backend pro Gemini, 150s) — na prática já
   * vimos o Gemini demorar mais que os 60s "normais" pra processar áudio.
   */
  async interpretarAudio(base64: string, mimeType: string): Promise<any> {
    return this.http
      .post(`${this.api}ia/audio`, { base64, mimeType })
      .pipe(timeout(170 * 1000))
      .toPromise();
  }

  async interpretarFoto(base64: string, mimeType: string): Promise<any> {
    return this.http
      .post(`${this.api}ia/foto`, { base64, mimeType })
      .pipe(timeout(170 * 1000))
      .toPromise();
  }

  /** Só texto (falado via áudio já transcrito, ou digitado direto) — sem mídia, resposta bem mais rápida. */
  async interpretarTexto(texto: string): Promise<any> {
    return this.http
      .post(`${this.api}ia/texto`, { texto })
      .pipe(timeout(170 * 1000))
      .toPromise();
  }

  /** Foto complementada, opcionalmente, por um texto — mesma chamada única do chat da tela administrativa. */
  async interpretarFotoTexto(base64: string, mimeType: string, texto: string): Promise<any> {
    return this.http
      .post(`${this.api}ia/fotoTexto`, { base64, mimeType, texto })
      .pipe(timeout(170 * 1000))
      .toPromise();
  }

  /**
   * Checagem best-effort de duplicidade (mesmo veículo+odômetro, ou mesmo
   * número de documento) antes de confirmar um lançamento vindo da Captura
   * por IA — só avisa, nunca bloqueia. Se estiver offline (ou o request
   * falhar por qualquer motivo), simplesmente não avisa nada; não é uma
   * regra de negócio obrigatória, só um reforço visual.
   */
  async verificarDuplicidade(veiculoId: number, km: number, numeroDocumento: string): Promise<string> {
    const parametros: string[] = [];
    if (veiculoId) { parametros.push(`veiculoId=${veiculoId}`); }
    if (km) { parametros.push(`km=${km}`); }
    if (numeroDocumento) { parametros.push(`numeroDocumento=${encodeURIComponent(numeroDocumento)}`); }
    if (parametros.length === 0) { return null; }

    try {
      const resposta: any = await this.http
        .get(`${this.api}verificarDuplicidade?${parametros.join('&')}`)
        .pipe(timeout(this.HTTP_TIMEOUT))
        .toPromise();
      return (resposta && resposta.aviso) || null;
    } catch (error) {
      console.warn('[CapturaIA] Não foi possível checar duplicidade (provavelmente offline):', error);
      return null;
    }
  }

  // ─── Tratamento de Erros ────────────────────────────────

  private tratarErro(error: any): void {
    const status = error?.status || 0;
    if (status === 404) {
      console.warn('[Sync] Endpoint não encontrado (404)');
    } else if (status === 0) {
      console.warn('[Sync] Sem conexão com a internet');
    }
  }
}
