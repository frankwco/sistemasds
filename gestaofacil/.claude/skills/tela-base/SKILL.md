---
name: tela-base
description: Padrão visual das telas administrativas do GestãoFácil (WebContent/base/*.xhtml) — hero banner, cards, tabela, botões e paleta de cores, no mesmo estilo de base/fechamentoMes.xhtml e base/dadosContratos.xhtml. Use SEMPRE que for criar uma nova tela em WebContent/base/, modernizar/converter uma tela antiga (page-header + panelGrid + btn-block), ou o usuário pedir para "padronizar", "deixar consistente" ou "no mesmo estilo do fechamentoMes", mesmo que não cite o nome desta skill.
---

# Padrão de tela — Área Administrativa (GestãoFácil)

Guia de estilo para `WebContent/base/*.xhtml`. Existem duas gerações de telas nesta pasta: um punhado já modernizado (`fechamentoMes.xhtml`, `dadosContratos.xhtml`, `coletasPorGrupo.xhtml`, `excedentesAbertos.xhtml`, `cobranca.xhtml`, `coletaApp.xhtml`, `painelExecutivo.xhtml`) e muitas ainda no padrão antigo (`page-header` + `container-fluid` + `p:panelGrid columns="2"` com `btn btn-block` + `modal-footer`/`ui-priority-primary`), como `categoriasDespesa.xhtml`, `fornecedores.xhtml`, `tiposDocumento.xhtml`, `usuario.xhtml`, `veiculos.xhtml`. Toda tela nova, e toda tela antiga que for tocada, deve seguir o padrão moderno abaixo.

**Regra de ouro:** CSS compartilhado, nunca `<style>` de layout dentro da página. Tudo reutilizável já existe em `WebContent/resources/css/gf-admin.css`. Um `<style>` por página só é aceitável para uma regra genuinamente específica daquela tela (ex.: as 4 cores de `.btn-ativo`/`.btn-renovado`/`.btn-inativo`/`.btn-obs` em `fechamentoMes.xhtml`, que não se repetem em nenhuma outra tela) — não para reimplementar `gf-card`, botões ou o dialog.

## Esqueleto de toda página

```xml
<ui:composition template="../resources/template/template-administrador.xhtml">
	<ui:define name="mensagem">
		<ui:include src="../componentes/mensagem.xhtml" />
	</ui:define>
	<ui:define name="modal-confirmacao">
		<ui:include src="../componentes/dialog-confirmacao.xhtml" />
	</ui:define>
	<ui:define name="conteudo">
		<div class="gf-page">
			<!-- hero, depois cards -->
		</div>
	</ui:define>
</ui:composition>
```

- Nunca reescreva o `p:confirmDialog global="true"` dentro da página — ele já vem de `componentes/dialog-confirmacao.xhtml` incluído acima, já com botões Sim/Não estilizados (verde/branco) via `gf-admin.css`. (Uma tela antiga como `fechamentoMes.xhtml` tem uma cópia disso no fim do arquivo; é resquício de antes da estilização central existir — não copie esse trecho para telas novas.) O CSS do confirm dialog é `.ui-confirm-dialog ...` **sem** prefixo `.gf-page` — o `<ui:insert name="modal-confirmacao">` do template é irmão do `<ui:insert name="conteudo">`, não filho, então o dialog nunca fica dentro de `.gf-page`; um `.gf-page .ui-confirm-dialog` não teria efeito nenhum (foi um bug real que existia antes desta correção).
- Hero em toda tela de conteúdo:
  ```xml
  <div class="gf-hero hero-compact">
      <div>
          <h2><i class="fa fa-ICONE" style="margin-right:10px;"></i>Título da Tela</h2>
          <p>Subtítulo de uma linha explicando a tela</p>
      </div>
      <i class="fa fa-ICONE-decorativo hero-icon"></i> <!-- opcional -->
  </div>
  ```
  Use `hero-compact` — é o padrão em todas as telas modernizadas atuais. O `.gf-hero` sem `hero-compact` (banner grande) existe no CSS mas hoje só faz sentido para uma eventual home da área administrativa; não use em telas de cadastro/listagem.
- Se a tela tem ações que chamam o backend (buscar, salvar, gerar), inclua o spinner padrão logo no início do conteúdo:
  ```xml
  <p:ajaxStatus onstart="PF('statusDialog').show()" onsuccess="PF('statusDialog').hide()" />
  <p:dialog widgetVar="statusDialog" modal="true" draggable="false" closable="false"
      resizable="false" showHeader="false" styleClass="gf-loading-dialog">
      <span class="gf-loading-text">Aguarde, realizando operação...</span>
      <p:graphicImage name="img/aguarde.gif" />
  </p:dialog>
  ```

## Padrão "CRUD simples" (lista + dialog de cadastro/edição)

É o caso mais comum: uma tabela, um botão para cadastrar, e um dialog para criar/editar. As telas antigas (`categoriasDespesa`, `fornecedores`, `tiposDocumento`, `usuario`, `veiculos`) seguem um padrão pré-`gf-card` que precisa ser convertido assim:

```xml
<div class="gf-page">

	<div class="gf-hero hero-compact">
		<div>
			<h2><i class="fa fa-ICONE" style="margin-right:10px;"></i>Título</h2>
			<p>Subtítulo de uma linha</p>
		</div>
	</div>

	<!-- ═══ CARD: Novo registro ═══ -->
	<div class="gf-card">
		<h:form id="frmCadastrar">
			<span class="gf-new-btn">
				<p:commandButton value="  Cadastrar" icon="fa fa-plus"
					update=":frmModalSalvar"
					actionListener="#{meuMB.criarNovoObjeto()}"
					onclick="PF('dlgMeuObjeto').show(); PF('dlgMeuObjeto').content.scrollTop('0')"
					title="Cadastrar" />
			</span>
		</h:form>
	</div>

	<!-- ═══ CARD: Tabela ═══ -->
	<div class="gf-card">
		<h:form id="frmTabela">
			<p:dataTable id="tabela" value="#{meuMB.listaObjeto}"
				resizableColumns="true" reflow="true" var="list" rows="10"
				paginator="true" paginatorPosition="bottom"
				emptyMessage="Nenhum registro cadastrado"
				rowsPerPageTemplate="5,10,15,30,50,100">

				<p:column headerText="Nome" filterBy="#{list.nome}"
					sortBy="#{list.nome}" filterMatchMode="contains">
					<h:outputText value="#{list.nome}" />
				</p:column>

				<p:column headerText="Ações" width="90">
					<div class="action-btn-icons">
						<span class="btn-primary-modern">
							<p:commandButton icon="fa fa-pencil"
								update=":frmModalSalvar" title="Alterar"
								oncomplete="PF('dlgMeuObjeto').show();"
								actionListener="#{meuMB.preencherLista(list)}" />
						</span>
						<span class="btn-danger-modern">
							<p:commandButton icon="fa fa-trash-o"
								actionListener="#{meuMB.inativar(list)}" title="Excluir"
								update=":frmTabela :dlgInformacao" alt="Excluir">
								<p:confirm header="Confirmação" message="Deseja excluir?" />
							</p:commandButton>
						</span>
					</div>
				</p:column>

			</p:dataTable>
		</h:form>
	</div>

	<!-- ═══ DIALOG: Cadastrar/Editar ═══ -->
	<p:dialog header="Meu Objeto" widgetVar="dlgMeuObjeto" modal="true"
		responsive="true" width="60%" id="dlgMeuObjeto" closable="true"
		closeOnEscape="true" resizable="false" draggable="false">
		<h:form id="frmModalSalvar">
			<div class="gf-form-group" style="flex-direction:column;">
				<div class="gf-form-item">
					<label><i class="fa fa-tag" style="margin-right:6px;color:#1565C0;"></i>Nome *</label>
					<p:inputText style="width:100%" required="true"
						value="#{meuMB.objeto.nome}" />
				</div>
			</div>
			<small>Campos obrigatórios *</small>

			<div class="gf-modal-footer">
				<span class="btn-primary-modern">
					<p:commandButton value="  Salvar" icon="fa fa-save"
						actionListener="#{meuMB.salvar()}"
						update=":dlgInformacao :frmTabela" />
				</span>
				<span class="btn-secondary-modern">
					<p:commandButton value="  Cancelar" icon="fa fa-times"
						onclick="PF('dlgMeuObjeto').hide();" />
				</span>
			</div>
		</h:form>
	</p:dialog>

</div><!-- /.gf-page -->
```

Principais trocas em relação ao padrão antigo:

| Antigo | Novo |
|---|---|
| `<h1 class="page-header">Título</h1>` | `.gf-hero.hero-compact` |
| `<div class="row container-fluid"><div class="col-md-3">` + botão solto | `.gf-card` (sem `gf-card-header`, ver nota abaixo) + `span.gf-new-btn` |
| `<div class="col-md-12" style="min-height: 700px">` envolvendo a tabela | `.gf-card` (sem `gf-card-header`) envolvendo a tabela |
| `p:panelGrid columns="2"` com `styleClass="btn btn-block"` e `icon="ui-icon-pencil"`/`"ui-icon-trash"` | `div.action-btn-icons` com `span.btn-primary-modern`/`btn-danger-modern`, só ícone (`fa fa-pencil`/`fa fa-trash-o`) — sem `value`, o `title` faz de tooltip |
| `<div class="col-md-12"><label>Campo:</label><p:inputText .../></div>` repetido para cada campo | `div.gf-form-group` (com `style="flex-direction:column;"` se os campos devem empilhar) contendo um `div.gf-form-item` por campo |
| `<div class="modal-footer">` com `styleClass="ui-priority-primary"` | `div.gf-modal-footer` com botões em `span.btn-primary-modern` / `span.btn-secondary-modern` |

**No CRUD simples de exatamente 2 cards (Cadastrar + Tabela), os dois ficam sem `gf-card-header`** — o hero já diz o nome da tela, o botão "Cadastrar" já se explica sozinho, e a tabela é óbvia pelo que está logo abaixo do botão. Um header repetindo "Novo Fornecedor / Cadastre um novo fornecedor" e "Fornecedores Cadastrados / Visualize, edite e gerencie..." só toma espaço vertical sem agregar informação nova — foi o ajuste feito depois que a versão com header ficou visualmente pesada nas primeiras conversões (`categoriasDespesa.xhtml`, `fornecedores.xhtml`, `tiposDocumento.xhtml`).

**Assim que a tela tem um terceiro card** (filtro, busca, ação extra — qualquer coisa além de Cadastrar/Tabela), a regra vira tudo ou nada: **todos** os cards da tela passam a ter `gf-card-header`, incluindo Cadastrar e Tabela — não só o card novo. Não existe padrão misto (alguns cards com header, outros sem) em nenhuma tela de referência. Veja `dadosContratos.xhtml`: tem 3 cards — "Novo Contrato", "Filtros", "Tabela" — e **os 3** têm header, mesmo o de Cadastrar e o de Tabela sendo "óbvios" isoladamente; o header entra porque a tela como um todo deixou de ser o CRUD de 2 cards. Aplique o mesmo em qualquer tela nova que ganhe um card de busca/filtro/ação além do Cadastrar+Tabela padrão (ex.: `cidades.xhtml`, que tem "Nova Cidade" + "Buscar Cidades" + "Cidades Cadastradas").

**Todo `required="true"` precisa de `requiredMessage`.** Sem isso, o JSF usa a mensagem padrão "{0}: Erro de validação: o valor é necessário.", onde `{0}` vira o clientId técnico do componente (ex.: `frmModalSalvar:j_idt64`) porque o campo não tem `label` definido — e isso aparece feio no `p:growl`. Sempre escreva algo como `requiredMessage="Nome é obrigatório"` / `requiredMessage="Selecione o Tipo"` em todo campo obrigatório.

O `p:growl` (`componentes/mensagem.xhtml`) já vem com aparência clara/discreta via `gf-admin.css` (fundo branco, borda colorida à esquerda — vermelho para erro, verde para sucesso — nada do vermelho escuro pesado do tema PrimeFaces "bootstrap"). Não precisa de nada extra na página para isso funcionar.

Note que o botão Salvar **não** tem `oncomplete` para esconder o dialog — é proposital: se o `required` falhar, o dialog precisa continuar aberto para o usuário ver a mensagem de erro. Só fecha via `p:commandButton` de Cancelar (`onclick="PF('...').hide();"`), igual às telas de referência.

`gf-form-group` sem `flex-direction:column` deixa os `gf-form-item` lado a lado (bom para pares curtos, ex. "Mês Inicial" + "Ano Inicial"); com `style="flex-direction:column;"` empilha um por linha (bom para formulários simples de cadastro, é o mais comum). Cada `gf-form-item` já estiliza seu `label` e faz o `p:inputText`/`p:selectOneMenu`/`p:calendar` ocupar 100% da largura — não precisa de `col-md-12` do Bootstrap dentro do dialog.

**Cuidado ao combinar pares de campos lado a lado (`gf-form-group` sem `flex-direction:column`) DENTRO de um formulário empilhado (`gf-form-group` com `flex-direction:column`):** a div logo após o `<h:form>` — a que empilha tudo — **nunca deve ter a classe `gf-form-group`**, deve ser só `<div style="flex-direction:column;">`, sem classe nenhuma. Isso é diferente de quando o formulário só tem `gf-form-item` soltos, empilhados um por um (o caso mais comum) — aí sim a div externa mantém `class="gf-form-group" style="flex-direction:column;"` normalmente, porque é o `gap` dela que dá o espaçamento entre os campos (um `gf-form-item` sozinho não tem margem própria).

O problema só aparece quando você aninha `<div class="gf-form-group">` (as linhas em par) dentro dela: a div externa, por ter a classe, também é `display:flex` com `gap:16px` — e cada linha aninhada, por também ter a classe, já carrega seu próprio `margin-top:16px` (pensado para separar linhas *soltas*, não aninhadas). Os dois efeitos se somam e dobram o espaço (peguei isso ao vivo em `configuracaoAjustes.xhtml`: virou ~32px em vez de 16px, e pior, inconsistente por causa de quebras de linha em labels longos). Tirando a classe da div externa, ela deixa de ser flex — as linhas aninhadas (que continuam com a classe, então continuam com seu `margin-top:16px`) simplesmente empilham em fluxo normal de bloco, com espaçamento único e correto.

Se o formulário mistura os dois casos — algum `gf-form-item` solto ANTES ou DEPOIS das linhas em par, como em `veiculos.xhtml` (Descrição solta, depois duas linhas em par, depois Observação solta) — o campo solto que vem *depois* de uma linha em par não tem margem própria e vai colar nela sem espaço; corrija só esse item pontual com `style="margin-top:16px;"` (não mexa na div externa nem reintroduza a classe nela). O primeiro campo do formulário nunca precisa disso, esteja ele solto ou em par.

Para uma tabela com 2-3 ações que cabem só com ícone (Alterar/Excluir é o caso mais comum), use `div.action-btn-icons` — botões quadrados 38×38px lado a lado, sem `value`, com `title` no `p:commandButton` fazendo o tooltip (é o padrão atual de `categoriasDespesa.xhtml`, `fornecedores.xhtml` e `tiposDocumento.xhtml`). Use `div.action-btn-group` (empilhado, com texto) só quando o rótulo for necessário para entender a ação (ex.: "Renovar", "Cancelar Faturamento" — ações menos óbvias que um ícone sozinho não comunica). Só use `action-section` (com `action-section-label` tipo "Documentos"/"Gerenciar") quando a linha tiver bastante ações agrupáveis por categoria — veja `fechamentoMes.xhtml` e `dadosContratos.xhtml` para esse caso mais complexo.

## Inputs, selects e autocomplete

Borda, `border-radius` e anel de foco de `p:inputText`/`p:inputNumber`/`p:inputTextarea`/`p:calendar`/`p:selectOneMenu`/`p:autoComplete` já são globais em `gf-admin.css`, aplicados a qualquer elemento dentro de `.gf-page` (classes `.ui-inputfield`, `.ui-inputtextarea`, `.ui-selectonemenu`, `.ui-autocomplete-input`). Isso inclui campos soltos num dialog que não usam `gf-form-item` (ex.: o `p:inputText` de "Alterar Observação" em `fechamentoMes.xhtml`). **Não redeclare borda, `border-radius` ou `:focus` em `<style>` de página.**

**Cuidado específico com `p:selectOneMenu`:** o tema PrimeFaces "bootstrap" (o configurado em `web.xml`, `primefaces.THEME=bootstrap`) dá ao wrapper (`.ui-selectonemenu`), ao label e ao trigger a classe genérica `ui-state-default`, que carrega um gradiente cinza no `theme.css` do próprio PrimeFaces. Um `p:inputText` não sofre disso porque é um `<input>` de verdade e cai num reset do Bootstrap; o `selectOneMenu` é montado com `<div>`/`<label>`, que o reset do Bootstrap não alcança — por isso o campo de texto fica bonito "de graça" mas o combobox fica com aparência de jQuery UI antigo (gradiente + caixinha da seta destacada) se você não neutralizar `background-image`/`text-shadow`/`box-shadow` explicitamente em CADA estado (`ui-state-default`, `ui-state-hover`, `ui-state-focus`) do wrapper, do label E do trigger — é exatamente isso que o bloco "INPUTS / SELECTS / AUTOCOMPLETE" de `gf-admin.css` já faz; não simplifique esse bloco.

Além do gradiente, o wrapper/label/trigger têm altura travada em `38px`/`36px`/`36px` (largura do trigger fixa em `32px`) — sem isso o trigger, que no PrimeFaces é posicionado `absolute` dentro do wrapper, fica com uma proporção diferente da do texto ao lado e passa a impressão de "botão" separado colado no campo, mesmo já sem gradiente. Se algum dia o padding do `.ui-inputfield` mudar, ajuste essas alturas junto para o select continuar com a mesma altura visual do input de texto vizinho.

Se depois de editar `gf-admin.css` o campo ainda aparecer com a aparência antiga (gradiente, altura diferente), o suspeito nº 1 é cache, não regra CSS faltando — os dois `<link>` para `gf-admin.css` (`template-administrador.xhtml` e `template-cliente.xhtml`) carregam com `?v=N` justamente pra isso: **incremente esse número toda vez que editar `gf-admin.css`**, senão o navegador (e às vezes o próprio servidor de aplicação) continua servindo a versão antiga do arquivo mesmo depois do deploy. Isso já foi verificado batendo o HTML real gerado pelo PrimeFaces (`SelectOneMenuRenderer`) contra o `theme.css`/`components.css` reais do PrimeFaces 6.2 + tema "bootstrap" num sandbox isolado — com o CSS atual, select e input saem com a mesma altura e sem gradiente nas duas ordens possíveis de carregamento das folhas de estilo; se a tela ainda mostrar diferença, é o `?v=` que não subiu, republique e confirme.

- Dentro de `.gf-form-item` ou `.gf-filter-item`, `p:inputText`/`p:selectOneMenu`/`p:calendar`/`p:autoComplete` já ocupam 100% da largura automaticamente — não precisa de `style="width:100%"` no componente.
- Fora desses wrappers (ex.: campo solto), use `style="width:100%;"` (ou uma largura fixa quando fizer sentido, como um campo curto de "Quantidade").

Padrão de `p:autoComplete` para buscar uma entidade (cliente, pessoa, fornecedor) por nome, com conversor:

```xml
<div class="gf-form-item">
	<label><i class="fa fa-user" style="margin-right:6px;color:#1565C0;"></i>Cliente</label>
	<p:autoComplete queryDelay="500"
		value="#{meuMB.clienteSelecionado}"
		completeMethod="#{meuMB.completarCliente}"
		var="cur" itemLabel="#{cur.nomeRazaoSocial}" itemValue="#{cur}"
		converter="#{converterCliente}" forceSelection="true" scrollHeight="250" />
</div>
```

`forceSelection="true"` evita salvar texto livre que não bateu com nenhum item da lista — use sempre que o valor precisar ser uma entidade de verdade (não uma string). Quando a busca + ação de adicionar ficam lado a lado (ex.: autocomplete de pessoa + botão "Adicionar"), agrupe com um flex row inline, no mesmo espírito do `gf-filter-bar`:

```xml
<div style="display:flex; gap:8px; align-items:center;">
	<div style="flex:1; min-width:0;">
		<p:autoComplete ... style="width:100%;" />
	</div>
	<span class="btn-primary-modern">
		<p:commandButton value="  Adicionar" icon="fa fa-plus" .../>
	</span>
</div>
```

## Padrão "tela avançada" (filtros, várias seções, accordion)

Quando a tela tem mais de uma etapa (filtrar → buscar → executar ação), organize em múltiplos `.gf-card` sequenciais, cada um com seu próprio `gf-card-header`, e use `p:accordionPanel`/`p:tab` dentro de um card para agrupar ações relacionadas sem poluir a tela. Veja `WebContent/base/fechamentoMes.xhtml` (accordion com 3 abas) e `WebContent/base/dadosContratos.xhtml` (card de filtro + card de tabela) como referência completa, incluindo dialogs de detalhe, `gf-alert` para avisos, e cards de resumo (`stat-card`).

## Cor: cada card pode ter uma cor própria — ela indica a *função* do card, não o tema da tela

Diferente da área do cliente (onde a regra é "tudo azul"), nas telas administrativas cada `.gf-card-header` já usa uma cor de ícone diferente conforme o que aquele card faz — é assim em todas as telas modernizadas hoje, então siga o mesmo padrão em vez de deixar tudo azul:

- **Verde** (`icon-green`) — card de criar/cadastrar novo registro.
- **Azul** (`icon-blue`) — card de filtro/pesquisa.
- **Teal** (`icon-teal`) — card de tabela/lista de resultados.
- **Roxo** (`icon-purple`) / **Laranja** (`icon-orange`) — cards de ação secundária ou aviso, conforme o contexto.

Botões seguem a mesma lógica *por tipo de ação*, não por página:

| Classe | Uso |
|---|---|
| `btn-primary-modern` (azul) | Ação principal: buscar, editar, salvar |
| `btn-success-modern` / `btn-green-modern` | Ação positiva/confirmação |
| `btn-danger-modern` | Destrutiva: excluir, cancelar |
| `btn-warning-modern` (laranja) | Atenção: atualizar, recalcular |
| `btn-teal-modern` | Ação secundária distinta |
| `btn-purple-modern` | Ação especial (ex.: imprimir, certificado) |
| `btn-secondary-modern` | Neutra: cancelar, fechar, corrigir |
| `gf-new-btn` (verde grande) | Botão "Cadastrar novo" fora de tabela, um por card |

Não invente hex novo — todas as cores acima já estão em `gf-admin.css`.

## Arquivos de referência

- `WebContent/resources/css/gf-admin.css` — todas as classes citadas (`gf-hero`, `gf-card`, `gf-form-group`, `gf-modal-footer`, `action-btn-group`, `action-btn-icons`, `btn-*-modern`, `gf-new-btn`, `stat-card`, `gf-alert`, `gf-loading-dialog`), o bloco "INPUTS / SELECTS / AUTOCOMPLETE" (borda + anel de foco globais para `.ui-inputfield`, `.ui-selectonemenu`, `.ui-autocomplete-input`, `.ui-inputtextarea`) o bloco `.ui-confirm-dialog` (botões Sim/Não do dialog de confirmação global) e o bloco `.ui-growl` (mensagens de `componentes/mensagem.xhtml`).
- `WebContent/resources/template/template-administrador.xhtml` — template que toda página de `base/` estende.
- Exemplo completo de CRUD simples convertido: `WebContent/base/dadosContratos.xhtml` (cards "Novo Contrato" + "Filtros" + "Tabela", dialog de Avisos com `gf-form-group`).
- Exemplo completo de tela avançada: `WebContent/base/fechamentoMes.xhtml` (accordion, múltiplos dialogs, cores de status por linha).
- Telas ainda no padrão antigo, candidatas a conversão: `WebContent/base/categoriasDespesa.xhtml`, `WebContent/base/fornecedores.xhtml`, `WebContent/base/tiposDocumento.xhtml`, `WebContent/base/usuario.xhtml`, `WebContent/base/veiculos.xhtml`.
