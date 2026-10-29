import { Component, OnInit } from '@angular/core';
import { AngularFirestore, AngularFirestoreCollection } from '@angular/fire/firestore';
import { formatDate } from '@angular/common';

@Component({
  selector: 'app-relatorio-coletas',
  templateUrl: './relatorio-coletas.page.html',
  styleUrls: ['./relatorio-coletas.page.scss'],
})
export class RelatorioColetasPage implements OnInit {

  private colectionFirestore: AngularFirestoreCollection<any>;

  dataInicial: string;
  dataFinal: string;
  veiculoSelecionado = { placaVeiculo: 'Todos' };
  buscando = false;
  dadosFirebase = [];
  dadosFirebaseOriginal = [];
  veiculos = [];
  totalDiaObj={ a: 0, e: 0, b: 0, a2: 0, a3: 0, a4: 0, total: 0 };

  constructor(private firestore: AngularFirestore,) {
    this.setDataInicialFinal();
  }

  ngOnInit() {

  }

  setDataInicialFinal() {
    const dataAtual = new Date();
    this.dataInicial = formatDate(dataAtual, 'yyyy-MM-dd', 'pt-BR');
    this.dataFinal = formatDate(dataAtual, 'yyyy-MM-dd', 'pt-BR');
  }

  totalizadorGeral() {
    this.totalDiaObj = { a: 0, e: 0, b: 0, a2: 0, a3: 0, a4: 0, total: 0 };

    this.dadosFirebase.forEach(objeto => {
      if (objeto.tipo === "totalDia") {
        this.totalDiaObj.a += objeto.a?objeto.a:0;
        this.totalDiaObj.e += objeto.e?objeto.e:0;
        this.totalDiaObj.b += objeto.b?objeto.b:0;
        this.totalDiaObj.a2 += objeto.a2?objeto.a2:0;
        this.totalDiaObj.a3 += objeto.a3?objeto.a3:0;
        this.totalDiaObj.a4 += objeto.a4?objeto.a4:0;
        this.totalDiaObj.total += objeto.total?objeto.total:0;
      }
    });

   
  }

  selecionarVeiculo(placaVeiculoSelecionada: string) {
    let lista = [];
    if (placaVeiculoSelecionada === 'Todos') {
      this.dadosFirebase = this.dadosFirebaseOriginal;
    } else {
      lista = this.dadosFirebaseOriginal.filter(item => item.placaVeiculo === placaVeiculoSelecionada);
      this.totalizarAposFiltro(lista);
    }
    this.totalizadorGeral();
  }

  totalizarAposFiltro(listaOriginal) {
    const totalizado = listaOriginal.reduce((acc, cur) => {
      if (cur.tipo === 'totalDia') {
        return acc;
      }

      acc.a += cur.a || 0;
      acc.b += cur.b || 0;
      acc.a3 += cur.a3 || 0;
      acc.a4 += cur.a4 || 0;
      acc.a2 += cur.a2 || 0;
      acc.e += cur.e || 0;
      acc.total += cur.total || 0;

      return acc;
    }, {
      data: listaOriginal[0].data,
      placaVeiculo: '',
      a: 0,
      b: 0,
      a3: 0,
      a4: 0,
      a2: 0,
      e: 0,
      total: 0,
      tipo: 'totalDia'
    });

    this.dadosFirebase = [...listaOriginal, totalizado];

  }

  async pesquisar() {
    this.veiculos = [{ placaVeiculo: 'Todos' }];
    this.dadosFirebase = [];
    this.dadosFirebaseOriginal = [];

    this.buscando = true;

    let dates = this.getDaysBetweenDates(this.dataInicial.split('T')[0], this.dataFinal.split('T')[0]);

    // Um dia que falha na consulta não derruba os outros; todos rodam juntos, mas o
    // resultado é montado na ordem das datas (e não na ordem em que cada um responde).
    const consultas = dates.map(date => {
      const dia = parseInt(date.split("-")[2]);
      const mes = parseInt(date.split("-")[1]);
      const ano = parseInt(date.split("-")[0]);

      return this.firestore.collection<any>('coleta').doc('veiculo').collection<any>('' + dia + '-' + mes + '-' + ano)
        .get()
        .toPromise()
        .then((querySnapshot) => {
          const dados = [];
          querySnapshot.forEach((doc) => {
            const dadoDoc = doc.data();
            if (dadoDoc.status === false) {
              // Coleta excluída pelo app (soft delete no Firestore) — não entra no relatório.
              return;
            }
            dados.push(dadoDoc);
          });
          return { date, dados, erro: null };
        })
        .catch((erro) => ({ date, dados: [], erro }));
    });

    const resultados = await Promise.all(consultas);

    const diasComErro: string[] = [];
    for (const resultado of resultados) {
      if (resultado.erro) {
        console.log('Error getting documents: ', resultado.date, resultado.erro);
        diasComErro.push(resultado.date);
        continue;
      }
      try {
        this.totalizar(resultado.dados, resultado.date);
        this.buscarVeiculos(resultado.dados);
      } catch (erro) {
        console.log('Error totalizando documentos: ', resultado.date, erro);
        diasComErro.push(resultado.date);
      }
    }

    this.totalizadorGeral();
    this.buscando = false;

    if (diasComErro.length > 0) {
      alert("Erro ao realizar a consulta dos dias: " + diasComErro.map(d => d.split('-').reverse().join('/')).join(', '));
    }
  }

  /** Valor numérico do documento do Firebase: ausente ou inválido vira 0; "1,5" vira 1.5. */
  private numero(valor: any): number {
    const n = typeof valor === 'number' ? valor : parseFloat(String(valor).replace(',', '.'));
    return isNaN(n) ? 0 : n;
  }

  /** Placa do veículo do documento; documento sem veículo entra como "Sem veículo". */
  private placaDoDocumento(dado: any): string {
    return (dado && dado.veiculo && dado.veiculo.placa) || 'Sem veículo';
  }

  buscarVeiculos(dados) {
    const placasUnicas = {};
    if (dados.length > 0) {
      dados.forEach((objeto) => {
        const placa = this.placaDoDocumento(objeto);
        if (!this.veiculos.find((veiculo) => veiculo.placaVeiculo === placa)) {
          placasUnicas[placa] = { placaVeiculo: placa };
        }
      });

      const resultado = Object.values(placasUnicas);
      this.veiculos = this.veiculos.concat(resultado);
    }
  }


  /** `dataPadrao` (yyyy-MM-dd): dia da consulta, usado quando o documento não traz a data da coleta. */
  totalizar(dados, dataPadrao: string) {
    const resultado = {};

    // percorre os dados originais e agrega as informações
    for (const dado of dados) {

      const data = typeof dado.dataUltimaColetaString === 'string' && dado.dataUltimaColetaString.length >= 10
        ? dado.dataUltimaColetaString.substring(0, 10)
        : dataPadrao;
      const placa = this.placaDoDocumento(dado);
      const a = this.numero(dado.a), b = this.numero(dado.b), a3 = this.numero(dado.a3),
        a4 = this.numero(dado.a4), a2 = this.numero(dado.a2), e = this.numero(dado.e);

      if (!resultado[data]) {
        resultado[data] = {};
      }

      if (!resultado[data][placa]) {
        resultado[data][placa] = {
          a: 0,
          b: 0,
          a3: 0,
          a4: 0,
          a2: 0,
          e: 0,
          total: 0
        };
      }

      resultado[data][placa].a += a;
      resultado[data][placa].b += b;
      resultado[data][placa].a3 += a3;
      resultado[data][placa].a4 += a4;
      resultado[data][placa].a2 += a2;
      resultado[data][placa].e += e;
      resultado[data][placa].total += a + b + a3 + a4 + a2 + e;
    }

    // converte o objeto em um array no formato desejado
    const arrayResultado = [];
    for (const data in resultado) {
      const totalDia = {
        data: data,
        a: 0,
        b: 0,
        a3: 0,
        a4: 0,
        a2: 0,
        e: 0,
        total: 0,
        tipo: 'totalDia'
      };

      for (const placa in resultado[data]) {
        const totalVeiculo = {
          data: data,
          placaVeiculo: placa,
          a: resultado[data][placa].a,
          b: resultado[data][placa].b,
          a3: resultado[data][placa].a3,
          a4: resultado[data][placa].a4,
          a2: resultado[data][placa].a2,
          e: resultado[data][placa].e,
          total: resultado[data][placa].total,
          tipo: 'totalVeiculo'
        };

        arrayResultado.push(totalVeiculo);

        totalDia.a += resultado[data][placa].a;
        totalDia.b += resultado[data][placa].b;
        totalDia.a3 += resultado[data][placa].a3;
        totalDia.a4 += resultado[data][placa].a4;
        totalDia.a2 += resultado[data][placa].a2;
        totalDia.e += resultado[data][placa].e;
        totalDia.total += resultado[data][placa].total;
      }

      arrayResultado.push(totalDia);
    }
    if (dados.length > 0) {
      this.dadosFirebase = this.dadosFirebase.concat(arrayResultado);
      this.dadosFirebaseOriginal = this.dadosFirebase;
    }
  }



  getDaysBetweenDates(startDate, endDate) {
    // Cria um array vazio para armazenar as datas
    let dates = [];

    // Converte as datas de entrada em timestamps
    let currentTimestamp = new Date(startDate).getTime();
    const endTimestamp = new Date(endDate).getTime();

    // Loop enquanto a data atual for menor ou igual à data final
    while (currentTimestamp <= endTimestamp) {
      // Adiciona a data atual à matriz de datas
      dates.push(new Date(currentTimestamp).toISOString().split('T')[0]);

      // Incrementa a data atual em um dia
      currentTimestamp += 24 * 60 * 60 * 1000;
    }

    // Retorna a matriz de datas
    return dates;
  }




}
