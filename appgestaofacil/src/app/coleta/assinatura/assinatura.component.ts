import { Component, OnInit, OnDestroy, ViewChild, HostListener, ElementRef, AfterViewInit, NgZone } from '@angular/core';
import { Platform, ModalController, AlertController, ToastController } from '@ionic/angular';
//import SignaturePad from 'signature_pad';
import { Storage } from '@ionic/storage';
import { AtualizarDadosService } from '../../api/atualizar-dados.service';
//import { DatabaseService } from '../../service/database.service';
import { PontoColetaService } from '../../service/ponto-coleta.service';
import { DatePipe } from '@angular/common';
import { SignaturePad } from 'angular2-signaturepad';
import { AngularFirestore, AngularFirestoreCollection } from '@angular/fire/firestore';
import { Observable } from 'rxjs';
import { ConstantesApp } from 'src/app/util/constanteApp';

@Component({
  selector: 'app-assinatura',
  templateUrl: './assinatura.component.html',
  styleUrls: ['./assinatura.component.scss'],
})
export class AssinaturaComponent implements OnInit, OnDestroy, AfterViewInit {
  // TUTORIAL https://medium.com/enappd/implement-signature-pad-in-ionic-apps-fec8775fc1a4
  //@ViewChild('canvas', { static: true }) signaturePadElement;
  //signaturePad: any;
  canvasWidth: number;
  canvasHeight: number;
  coleta;
  emailNovo;
  /** Trava o botão Finalizar após o primeiro toque (evita coleta duplicada com a tela lenta). */
  salvando = false;
  /** Segura a pausa do sync até o ponto atualizado ser gravado, mesmo com o modal já destruído. */
  private mantemPausa = false;
  private readonly colectionFirestore: AngularFirestoreCollection<any>;

  @ViewChild(SignaturePad) signaturePad: SignaturePad;

  private signaturePadOptions: Object = { // passed through to szimek/signature_pad constructor
    'minWidth': 5,
    'canvasWidth': 500,
    'canvasHeight': 200
  };

  constructor(
    private firestore: AngularFirestore,
    private pontoColetaService: PontoColetaService,
    private storage: Storage,
    private elementRef: ElementRef,
    public atualizarDadosService: AtualizarDadosService,
    public modalController: ModalController,
    private alertController: AlertController,
    private toastController: ToastController,
    private constanteApp: ConstantesApp,
    private ngZone: NgZone
  ) {
    this.atualizarDadosService.pausarSync();

    let dt = new Date();
    let dia = dt.getDate(), mes = dt.getMonth() + 1, ano = dt.getFullYear(), hora = dt.getHours(), minuto = dt.getMinutes(), segundo = dt.getSeconds();



    this.colectionFirestore = firestore.collection<any>('coleta').doc('veiculo').collection<any>('' + dia + '-' + mes + '-' + ano);

    let pontoColetaSelecionado = { "gruposColetados": "", "email": "" };
    this.coleta = { "a": "", "pontoColetaSelecionado": pontoColetaSelecionado, "gruposColetados": "" };


    this.storage.set("finalizada", "não");


  }

  ngOnInit(): void {
    this.init();
  }
  ngOnDestroy(): void {
    // Ao finalizar, quem retoma é persistirColeta, depois de gravar o ponto atualizado.
    if (!this.mantemPausa) {
      this.atualizarDadosService.retomarSync();
    }
  }

  // Não chama init(): o teclado do campo de e-mail dispara resize e apagaria a assinatura.
  @HostListener('window:resize')
  onResize() {
    this.ajustarCanvas();
  }

  convertString(value) {
    if (value === null || value === undefined) {
      return 0;
    }

    if (typeof value === 'number') {
      return isNaN(value) ? 0 : value;
    }

    if (typeof value === 'string') {
      value = value.trim();
      if (value === '') {
        return 0;
      }

      value = value.replace(',', '.').replace(/[^0-9.\-]/g, '');

      const parsed = parseFloat(value);

      if (isNaN(parsed)) {        
        return 0;
      }

      return parsed;
    }

    return 0;
  }

  formatarMensagemConfirmar() {
    let mensagem = "";
    if (this.coleta.pontoColetaSelecionado.gruposColetados.includes('A;')) {
      mensagem += "A: " + this.coleta.a + "<br/><br/>";
    }
    if (this.coleta.pontoColetaSelecionado.gruposColetados.includes('E;')) {
      mensagem += "E: " + this.coleta.e + "<br/><br/>";
    }
    if (this.coleta.pontoColetaSelecionado.gruposColetados.includes('B;')) {
      mensagem += "B: " + this.coleta.b + "<br/><br/>";
    }
    if (this.coleta.pontoColetaSelecionado.gruposColetados.includes('A2;')) {
      mensagem += "A2: " + this.coleta.a2 + "<br/><br/>";
    }
    if (this.coleta.pontoColetaSelecionado.gruposColetados.includes('A3;')) {
      mensagem += "A3: " + this.coleta.a3 + "<br/><br/>";
    }
    if (this.coleta.pontoColetaSelecionado.gruposColetados.includes('A4;')) {
      mensagem += "A4: " + this.coleta.a4;
    }
    return mensagem;
  }



  //mexendo aqui para mostrar uma mensagem para o usuário confirmar
  async continuar() {
    if (this.salvando) {
      return;
    }
    const alert = await this.alertController.create({
      cssClass: 'my-custom-class',
      header: 'Confira os dados!',
      message: this.coleta.pontoColetaSelecionado.nomeRazaoSocial + '<br/><br/><strong>' + this.formatarMensagemConfirmar() + "</strong>",
      buttons: [
        {
          text: 'Cancelar',
          role: 'cancel',
          cssClass: 'secondary',
          handler: (blah) => {
            console.log('Confirm Cancel: blah');
          }
        }, {
          text: 'Confirmar',
          handler: () => {
            this.fechar();
          }
        }
      ]
    });

    await alert.present();


  }

  async fechar() {
    const alert = await this.alertController.create({
      cssClass: 'my-custom-class',
      header: 'Confirmar!',
      message: 'Deseja <strong>SALVAR</strong> a coleta??<br/><br/>' + this.formatarMensagemConfirmar(),
      buttons: [
        {
          text: 'Cancelar',
          role: 'cancel',
          cssClass: 'secondary',
          handler: (blah) => {
            console.log('Confirm Cancel: blah');
          }
        }, {
          text: 'Sim',
          handler: () => {
            this.finalizar();
          }
        }
      ]
    });

    await alert.present();
  }

  adicionarColetaFire(coleta) {
    //coleta.assinatura = "";
    this.colectionFirestore.doc("" + coleta.pontoColetaSelecionado.id).set(coleta)
      .catch((error) => {
        console.error('[Sync] Falha ao gravar coleta no Firebase:', error);
      });

  }

  async finalizar() {
    if (this.salvando) {
      return;
    }
    this.salvando = true;
    // Deixa o overlay "Salvando coleta..." ser pintado antes do trabalho pesado abaixo
    // (gerar o PNG da assinatura e gravar no Storage), senão a tela parece congelada.
    await new Promise(resolve => setTimeout(resolve, 50));
    try {
      await this.montarEPersistir();
    } catch (error) {
      // Sem isto o overlay ficaria na tela para sempre se algo aqui lançasse erro.
      console.error('[Coleta] Falha ao finalizar a coleta:', error);
      this.salvando = false;
      alert("Erro ao salvar a coleta, tente novamente.");
    }
  }

  /** Preenche os campos finais da coleta (assinatura, data, pesos) e a grava. */
  private async montarEPersistir() {
    this.storage.set("finalizada", "sim");


    const img = this.signaturePad.toDataURL();
    //console.log(img);
    this.coleta.status = true;
    this.coleta.assinatura = img;
    this.coleta.sincronizado = false;
    this.coleta.identificadorApp = this.getId(this.coleta.pontoColetaSelecionado.id);
    this.coleta.versaoApp = this.constanteApp.chave;
    //console.log(this.coleta.identificadorApp);

    let dt = new Date();
    let dia = dt.getDate(), mes = dt.getMonth() + 1, ano = dt.getFullYear(), hora = dt.getHours(), minuto = dt.getMinutes(), segundo = dt.getSeconds();
    // console.log(dia+" "+mes+" "+ano);
    this.coleta.diaUltimaColeta = dia;
    this.coleta.mesUltimaColeta = mes;
    this.coleta.anoUltimaColeta = ano;
    this.coleta.horaUltimaColeta = hora;
    this.coleta.minutoUltimaColeta = minuto;
    this.coleta.segundoUltimaColeta = segundo;
    this.coleta.emailNovo = this.emailNovo != undefined ? this.emailNovo : "";

    const datepipe: DatePipe = new DatePipe('en-US')
    let formattedDate = datepipe.transform(dt, 'yyyy-MM-dd HH:mm:ss')
    this.coleta.dataUltimaColetaString = formattedDate;

    this.coleta.a = this.convertString(this.coleta.a);
    this.coleta.e = this.convertString(this.coleta.e);
    this.coleta.b = this.convertString(this.coleta.b);
    this.coleta.a2 = this.convertString(this.coleta.a2);
    this.coleta.a3 = this.convertString(this.coleta.a3);
    this.coleta.a4 = this.convertString(this.coleta.a4);


    await this.persistirColeta(dia, mes, ano);
  }

  /**
   * Grava a coleta localmente e atualiza o ponto, tudo em sequência (await),
   * para o envio ao servidor só começar com o Storage já consistente.
   */
  private async persistirColeta(dia: number, mes: number, ano: number) {
    try {
      // Reservado antes de gravar: a fila normal não pode pegar esta coleta no meio
      // do caminho e enviá-la junto com o envio imediato (duplicaria no servidor).
      this.atualizarDadosService.reservarEnvioImediato(this.coleta.identificadorApp);
      await this.atualizarDadosService.modificarColetas(coletas => {
        coletas.push(this.coleta);
      });

      const ponto = this.coleta.pontoColetaSelecionado;
      ponto.diaUltimaColeta = dia;
      ponto.mesUltimaColeta = mes;
      ponto.anoUltimaColeta = ano;
      ponto.quantidadeColetadaMes += 1;

      this.mostrarToast("Coleta inserida com sucesso!");
      // A coleta já está gravada: fecha o modal agora. Regravar a lista inteira de pontos
      // (a parte mais pesada em celular fraco) fica para depois, fora do caminho do usuário.
      // O sync continua pausado até lá, para não disputar 'pontosColeta' com esta gravação.
      this.mantemPausa = true;
      await this.modalController.dismiss({
        'dismissed': true
      });
    } catch (error) {
      console.error('[Coleta] Falha ao salvar coleta localmente:', error);
      // Se a coleta já entrou em 'coletas', a fila normal passa a enviá-la.
      this.atualizarDadosService.cancelarEnvioImediato(this.coleta.identificadorApp);
      this.mantemPausa = false;
      this.salvando = false;
      alert("Erro ao salvar a coleta, tente novamente.");
      return;
    }

    // Daqui em diante a coleta está salva e o modal fechado: falhas só vão para o log.
    try {
      // Esta coleta vai ao servidor na hora, sozinha, sem esperar a fila de sincronização
      // (que pode estar ocupada ou só rodar no próximo ciclo). Sem await: não segura nada.
      // Se falhar, ela segue pendente e a fila normal reenvia.
      this.atualizarDadosService.enviarColetaAgora(this.coleta);
      await this.gravarPontoAtualizado(this.coleta.pontoColetaSelecionado);
      this.adicionarColetaFire(this.coleta);
    } catch (error) {
      console.error('[Coleta] Falha nos passos após fechar a assinatura:', error);
    } finally {
      // A fila normal só roda agora se algo ficou adiado enquanto a tela estava aberta.
      this.atualizarDadosService.retomarSync();
    }
  }

  /**
   * Troca o ponto atualizado na lista 'pontosColeta'. Relê a lista na hora de gravar (e não
   * a cópia da abertura da tela, que podia estar velha) e troca só esse ponto. Se falhar, a
   * coleta já está salva e o ponto se acerta na próxima sincronização.
   */
  private async gravarPontoAtualizado(ponto: any) {
    try {
      const pontos: any[] = (await this.storage.get('pontosColeta')) || [];
      const idx = pontos.findIndex(p => p.id == ponto.id);
      if (idx >= 0) {
        pontos[idx] = ponto;
        await this.storage.set('pontosColeta', pontos);
      }
    } catch (error) {
      console.error('[Coleta] Falha ao gravar o ponto atualizado:', error);
    }
  }

  private async mostrarToast(mensagem: string) {
    const toast = await this.toastController.create({
      message: mensagem,
      duration: 2500,
      position: 'bottom'
    });
    await toast.present();
  }

  getId(ii) {
    //let dt = new Date();
    //let i = dt.getDate() + "" + dt.getMonth() + "" + dt.getFullYear() + "" + dt.getHours() + "" + dt.getMinutes() + "" + dt.getSeconds() + "" + dt.getMilliseconds() + "" + ii;
    //return new Date().valueOf()+""+ii;,
    return (Date.now() + Math.random() + "" + ii);
  }

  // Redimensionar o canvas apaga o desenho; só mexe quando o tamanho realmente mudou.
  private ajustarCanvas() {
    const canvas: any = this.elementRef.nativeElement.querySelector('canvas');
    if (!canvas) {
      return;
    }
    // Largura em que o canvas aparece na tela (o cartão é mais estreito que a janela).
    // Com a largura da janela, o desenho era escalado pelo CSS e o traço saía deslocado
    // do dedo, porque a biblioteca calcula os pontos em pixels de tela.
    const largura = canvas.offsetWidth || window.innerWidth;
    if (canvas.width !== largura) {
      canvas.width = largura;
    }
    //canvas.height = window.innerHeight - 600;
    if (canvas.height !== 150) {
      canvas.height = 150;
    }
  }

  init() {
    this.ajustarCanvas();
    if (this.signaturePad) {
      this.signaturePad.clear(); // Clear the pad on init
    }

    this.storage.get('coleta').then((val) => {
      if (val != null) {
        this.coleta = val;
      }
    });
  }

  /*public ngAfterViewInit(): void {
   // this.signaturePad = new SignaturePad(this.signaturePadElement.nativeElement);
    //this.signaturePad.clear();
    //this.signaturePad.penColor = 'rgb(0,0,0)';
  }*/

  ngAfterViewInit() {
    // this.signaturePad is now available
    this.signaturePad.set('minWidth', 1); // set szimek/signature_pad options at runtime
    this.ajustarCanvas(); // agora o canvas já tem largura na tela (no ngOnInit ainda pode ser 0)
    this.signaturePad.clear(); // invoke functions from szimek/signature_pad API

    // O componente da biblioteca registra os ouvidos de toque dentro do ciclo do Angular:
    // cada touchmove (dezenas por segundo) disparava a checagem de TODO o app, o que em
    // celular fraco deixa o traço atrasado. Religados aqui, fora do Angular.
    this.ngZone.runOutsideAngular(() => {
      this.signaturePad.off();
      this.signaturePad.on();
    });
  }

  save(): void {
    const img = this.signaturePad.toDataURL();
    // this.base64ToGallery.base64ToGallery(img).then(
    //   res => console.log('Saved image to gallery ', res),
    //   err => console.log('Error saving image to gallery ', err)
    // );
  }

  isCanvasBlank(): boolean {
    if (this.signaturePad) {
      return this.signaturePad.isEmpty() ? true : false;
    }
  }

  clear() {
    this.signaturePad.clear();
  }

  undo() {
    const data = this.signaturePad.toData();
    if (data) {
      data.pop(); // remove the last dot or line
      this.signaturePad.fromData(data);
    }
  }
}
