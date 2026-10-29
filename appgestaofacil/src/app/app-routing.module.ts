import { NgModule } from '@angular/core';
import { PreloadAllModules, RouterModule, Routes } from '@angular/router';

const routes: Routes = [

  {
    path: '',
    loadChildren: () => import('./login/login.module').then( m => m.LoginPageModule)
  },
  {
    path: 'coleta',
    loadChildren: () => import('./coleta/coleta.module').then( m => m.ColetaPageModule)
  },
  {
    path: 'cliente',
    loadChildren: () => import('./cliente/cliente.module').then( m => m.ClientePageModule)
  },
  {
    path: 'contrato',
    loadChildren: () => import('./contrato/contrato.module').then( m => m.ContratoPageModule)
  },
  {
    path: 'fechamento-mes',
    loadChildren: () => import('./fechamento-mes/fechamento-mes.module').then( m => m.FechamentoMesPageModule)
  },
  {
    path: 'abastecimento',
    loadChildren: () => import('./abastecimento/abastecimento.module').then( m => m.AbastecimentoPageModule)
  },
  {
    path: 'despesa-manutencao',
    loadChildren: () => import('./despesa-manutencao/despesa-manutencao.module').then( m => m.DespesaManutencaoPageModule)
  },
  {
    path: 'alertas',
    loadChildren: () => import('./alertas/alertas.module').then( m => m.AlertasPageModule)
  },
  {
    path: 'captura-ia',
    loadChildren: () => import('./captura-ia/captura-ia.module').then( m => m.CapturaIaPageModule)
  },
  {
    path: '',
    loadChildren: () => import('./tabs/tabs.module').then(m => m.TabsPageModule)
  },
  {
    path: 'selecionar-veiculo',
    loadChildren: () => import('./selecionar-veiculo/selecionar-veiculo.module').then( m => m.SelecionarVeiculoPageModule)
  },
  {
    path: 'dados-pontos',
    loadChildren: () => import('./dados-pontos/dados-pontos.module').then( m => m.DadosPontosPageModule)
  },
  {
    path: 'pontos-sem-coleta',
    loadChildren: () => import('./pontos-sem-coleta/pontos-sem-coleta.module').then( m => m.PontosSemColetaPageModule)
  },
  {
    path: 'relatorio-coletas',
    loadChildren: () => import('./relatorio-coletas/relatorio-coletas.module').then( m => m.RelatorioColetasPageModule)
  },
  {
    path: 'pontos-com-avisos',
    loadChildren: () => import('./pontos-com-avisos/pontos-com-avisos.module').then( m => m.PontosComAvisosPageModule)
  },  {
    path: 'meus-lancamentos',
    loadChildren: () => import('./meus-lancamentos/meus-lancamentos.module').then( m => m.MeusLancamentosPageModule)
  },
  {
    path: 'cobranca',
    loadChildren: () => import('./cobranca/cobranca.module').then( m => m.CobrancaPageModule)
  }

];
@NgModule({
  imports: [
    RouterModule.forRoot(routes, { preloadingStrategy: PreloadAllModules })
  ],
  exports: [RouterModule]
})
export class AppRoutingModule {}
