---
name: tela-cliente
description: Padrão visual das telas da área do cliente do GestãoFácil (WebContent/cliente/*.xhtml) — hero banner, cards, paleta de cores, botões e CSS compartilhado. Use SEMPRE que for criar uma nova tela em WebContent/cliente/, editar uma existente, ou o usuário pedir para "padronizar", "deixar consistente" ou "no mesmo estilo" das telas do cliente (contratos, coletas, romaneios, etc.), mesmo que ele não cite o nome desta skill.
---

# Padrão de tela — Área do Cliente (GestãoFácil)

Guia de estilo para `WebContent/cliente/*.xhtml`. Existe porque as 4 telas atuais (`inicio`, `coletasCliente`, `emissaoRomaneiosLaudos`, `dadosContratos`) já divergiram uma vez — cores trocadas, padding extra, `<style>` duplicado por página. Siga isto para não repetir.

**Regra de ouro:** CSS compartilhado, nunca `<style>` dentro da página. Tudo que é reutilizável mora em `WebContent/resources/css/gf-admin.css` (comentário no topo do arquivo já diz "CSS compartilhado para todas as páginas"). Se a página precisa de um visual novo, crie classes genéricas prefixadas `gf-` nesse arquivo — não improvise inline.

## Esqueleto de toda página

```xml
<ui:composition template="../resources/template/template-cliente.xhtml">
	<ui:define name="mensagem">
		<ui:include src="../componentes/mensagem.xhtml" />
	</ui:define>
	<ui:define name="modal-confirmacao">
		<ui:include src="../componentes/dialog-confirmacao.xhtml" />
	</ui:define>
	<ui:define name="conteudo">
		<div class="gf-page">
			<!-- hero, depois o conteúdo -->
		</div>
	</ui:define>
</ui:composition>
```

- `<div class="gf-page">` sem `style` inline — o layout base já dá o espaçamento. Adicionar `padding` aqui foi o erro que deixou a `inicio.xhtml` com mais respiro que as outras telas.
- Hero: `<div class="gf-hero hero-compact">` em toda tela de conteúdo. Só a `inicio.xhtml` (a home da área do cliente) usa `.gf-hero` sem `hero-compact` — é a única com banner grande, de propósito.
  ```xml
  <div class="gf-hero hero-compact">
      <div>
          <h2><i class="fa fa-ICONE" style="margin-right:10px;"></i>Título da Tela</h2>
          <p>Subtítulo de uma linha explicando a tela</p>
      </div>
      <i class="fa fa-ICONE-decorativo hero-icon"></i> <!-- opcional -->
  </div>
  ```

## Escolha o layout de listagem certo

Duas opções já prontas em `gf-admin.css`. Não invente uma terceira.

**1. Tabela clássica** — quando a lista é só para consulta/filtro (é o padrão mais comum, usado por `coletasCliente.xhtml` e `emissaoRomaneiosLaudos.xhtml`):
```xml
<div class="gf-card">
    <div class="gf-card-header">
        <i class="fa fa-list icon-COR"></i>
        <div><h4>Título</h4><small>Subtítulo</small></div>
    </div>
    <p:dataTable ...>...</p:dataTable>
</div>
```

**2. Grade de cards** — quando cada item precisa de ações próprias em destaque (ex: imprimir) e uma tabela ficaria poluída. Usado por `dadosContratos.xhtml`:
```xml
<div class="gf-info-grid">
    <ui:repeat value="#{bean.lista}" var="item">
        <div class="gf-info-card">
            <div class="gf-info-card-header">
                <div class="gf-info-card-icon nc-COR"><i class="fa fa-ICONE"></i></div>
                <div>
                    <h4>Título do item</h4>
                    <span class="gf-status-badge success">Ativo</span>
                </div>
            </div>
            <div class="gf-info-card-body">
                <div class="gf-info-item">
                    <i class="fa fa-ICONE"></i>
                    <div><small>Rótulo</small><strong>Valor</strong></div>
                </div>
                <!-- mais gf-info-item conforme necessário -->
            </div>
            <div class="gf-info-card-footer">
                <span class="btn-COR-modern"><p:commandButton .../></span>
            </div>
        </div>
    </ui:repeat>
</div>
<!-- estado vazio -->
<h:panelGroup layout="block" styleClass="gf-info-empty" rendered="#{empty bean.lista}">
    <i class="fa fa-ICONE"></i>
    <p>Nenhum item encontrado.</p>
</h:panelGroup>
```
`.gf-info-card` já é `display:flex; flex-direction:column` com o body em `flex:1 1 auto` — o rodapé (`gf-info-card-footer`) sempre fica alinhado na base do card, mesmo quando o conteúdo dos cards ao lado tem alturas diferentes.

`.gf-status-badge` aceita os modificadores `success` (verde, "Ativo"), `warning` (laranja, atenção/pendente), `info` (azul, neutro) e `danger` (vermelho, "Inativo"/erro) — são cores **semânticas**, não a cor do módulo.

## Loading spinner (quando os dados vêm de uma chamada AJAX)

Se a lista é carregada via `action` do bean (não populada direto no `@PostConstruct`), use `p:remoteCommand` disparado no load da página — é o padrão de `emissaoRomaneiosLaudos.xhtml` e `dadosContratos.xhtml`:
```xml
<p:ajaxStatus onstart="PF('statusDialog').show()" onsuccess="PF('statusDialog').hide()" />
<p:dialog widgetVar="statusDialog" modal="true" draggable="false" closable="false"
    resizable="false" showHeader="false" styleClass="gf-loading-dialog">
    <span class="gf-loading-text">Aguarde, carregando...</span>
    <p:graphicImage name="img/aguarde.gif" />
</p:dialog>
<p:remoteCommand name="carregarX" action="#{bean.buscarDados}" update="idDoContainer"
    onstart="PF('statusDialog').show()" oncomplete="PF('statusDialog').hide()" />
<script type="text/javascript">$(document).ready(function(){ carregarX(); });</script>
```

## Home (`inicio.xhtml`)

A home tem só **uma** superfície de navegação: uma linha de `.nav-card` (ícone + título + descrição + rodapé "Acessar X") para cada seção. Não duplique os mesmos links em outro lugar da página — já existiu um bug em que os mesmos 3 links apareciam tanto num carrossel compacto no hero quanto nos cards abaixo.

## Cor: azul em toda tela de conteúdo, cores só nos cards da home

Primeira versão desta skill tentava dar uma cor diferente por módulo (azul/verde/roxo), aplicada de ponta a ponta em cada tela. Na prática isso deu errado: o hero é **sempre** azul (é a cor de marca do sistema, fixa em `gf-admin.css`), então cada página trocando de cor por baixo de um cabeçalho que nunca muda dava a sensação de estar em apps diferentes a cada clique. Foi revertido.

Regra atual, mais simples:
- **Dentro de qualquer tela de conteúdo** (`coletasCliente`, `emissaoRomaneiosLaudos`, `dadosContratos`, e qualquer tela nova): use azul — `icon-blue` / `nc-blue` nos ícones, `btn-primary-modern` no botão de ação principal. É a mesma cor do hero, então a tela inteira lê como uma coisa só.
- **Só os `nav-card` da `inicio.xhtml`** (a home) podem variar de cor — azul/verde/roxo/etc. Ali eles ficam lado a lado na mesma tela, então cores diferentes ajudam a achar a seção rapidamente; é uma lista de menu, não a identidade da página. Ao adicionar uma seção nova na home, pode puxar uma cor livre da paleta abaixo pro card dela.
- **Botões de ação lado a lado que fazem coisas diferentes** (ex.: "Romaneio Mensal" azul, "Certificado Mensal" verde, "Certificado por Período" teal, todos na mesma linha de uma `p:dataTable`) podem ter cores distintas — isso é diferenciação por *tipo de ação*, não tema de página, e ajuda o usuário a escolher o botão certo. Não confundir com o item anterior.
- **Badges de status** (`gf-status-badge`) também continuam semânticos (verde=Ativo, laranja=atenção, vermelho=Inativo), independente da cor da tela.

Paleta disponível (todas já em `gf-admin.css`, não invente hex novo):

| Cor    | `nc-*` / `icon-*`        | Botão               | Hex                        |
|--------|---------------------------|----------------------|-----------------------------|
| Azul   | `nc-blue` / `icon-blue`   | `btn-primary-modern` ou `btn-blue-modern` | `#1565C0 → #42A5F5` |
| Verde  | `nc-green` / `icon-green` | `btn-green-modern` (= `btn-success-modern`) | `#2E7D32 → #66BB6A` |
| Roxo   | `nc-purple` / `icon-purple` | `btn-purple-modern` | `#6A1B9A → #AB47BC` |
| Laranja| `nc-orange` / `icon-orange` | `btn-warning-modern` | `#E65100 → #FFA726`* |
| Teal   | `nc-teal` / `icon-teal`   | `btn-teal-modern`    | `#00695C → #26A69A` |
| Vermelho | `icon-red`               | `btn-danger-modern`  | `#C62828 → #EF5350` |

\* `btn-warning-modern` usa `#FF9800` como segundo tom em vez de `#FFA726` — variação pequena já existente no CSS, não precisa corrigir.

## Arquivos de referência

- `WebContent/resources/css/gf-admin.css` — todas as classes citadas acima (`gf-hero`, `gf-card`, `gf-info-*`, `nav-card`, `nc-*`, `icon-*`, `btn-*-modern`, `gf-status-badge`, `welcome-msg`).
- `WebContent/resources/css/gf-template.css` — chrome do layout (sidebar, header, cores de tema admin/cliente).
- `WebContent/resources/template/template-cliente.xhtml` — o template que toda página client-area estende.
- Exemplos completos: `WebContent/cliente/inicio.xhtml` (home + nav-cards), `WebContent/cliente/coletasCliente.xhtml` e `emissaoRomaneiosLaudos.xhtml` (padrão tabela), `WebContent/cliente/dadosContratos.xhtml` (padrão grade de cards).
