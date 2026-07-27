# Memória do frontend Angular

Este documento orienta agentes que alteram o frontend do Sisdent. A aplicação
Angular está em `frontend/` e usa componentes standalone, Angular Material,
SCSS e `@ngx-translate/core`.

## Estrutura atual

```text
frontend/
|-- angular.json              # build, serve, testes e schematics
|-- public/i18n/              # traduções pt-PT, en e nl
`-- src/
    |-- main.ts               # bootstrap da aplicação
    |-- styles.scss           # estilos globais
    `-- app/
        |-- app.ts/html/scss  # shell e router-outlet
        |-- app.config.ts     # providers globais
        |-- app.routes.ts     # rotas
        |-- core/             # serviços, modelos, autenticação e interceptor
        |-- shared/           # componentes reutilizáveis
        `-- features/         # componentes organizados por funcionalidade
            |-- login/
            |-- not-found/
            `-- users/
```

Componentes de feature devem permanecer junto dos seus arquivos de template e
estilo. Por exemplo, uma feature usa `nome.component.ts`,
`nome.component.html` e `nome.component.scss`. Serviços e modelos comuns ficam
em `core`; componentes reutilizáveis ficam em `shared`.

## Temas claro e escuro

O frontend suporta os temas `light` e `dark`. O estado é centralizado no
`ThemeService` (`core/theme.service.ts`), que aplica `data-theme` no elemento
`html`, atualiza `color-scheme` e persiste a escolha em `localStorage` com a
chave `sisdent-theme`. O controle visual reutilizável é
`shared/theme-toggle.component.ts`.

Novas cores de interface devem usar as variáveis/seletores globais de tema ou
ser compatíveis com ambos os temas. Ao criar uma nova página, disponibilizar o
`app-theme-toggle` no cabeçalho ou área de ações. Não duplicar lógica de troca
de tema dentro das features.

A paleta escura é exclusiva do seletor `html[data-theme='dark']`; nunca alterar
as regras do tema claro ao refiná-la. A referência de marca é odontologia e
saúde: fundo azul-marinho (`#081c24`), superfícies azul-petróleo (`#102b35`),
bordas azuladas (`#28515c`), texto aqua claro (`#e4f5f5`) e ações em teal
(`#12a99b`/`#46d7c8`). Azul e verde/teal são associados a confiança, limpeza e
calma em comunicação odontológica; usar vermelho apenas para erros/desativação
e nunca como cor primária.

Em superfícies escuras, chips, etiquetas e metadados não devem usar cinza de
baixo contraste. Usar uma superfície teal distinta, borda teal média e texto
aqua claro; verificar visualmente texto e borda em cada componente Material.
Campos de formulário escuros devem usar uma superfície elevada, placeholder
aqua legível e contorno com contraste suficiente; nunca depender da cor padrão
do placeholder do Angular Material.
Em `mat-form-field`, não usar `mat-label` e `placeholder` para a mesma
instrução: o Material apresenta o label como placeholder quando o campo está
vazio, e a duplicação provoca sobreposição visual.
No tema escuro, aplicar contraste tanto ao `::placeholder` nativo quanto ao
`.mdc-floating-label`, pois este é o texto visível quando o `mat-label` ocupa
o campo vazio.
Controles posicionados sobre elementos decorativos devem ficar em um contêiner
flexível com alinhamento explícito e `z-index` acima da decoração. Elementos
puramente visuais, como bolhas de fundo, devem usar `pointer-events: none` para
nunca bloquear cliques.

## Regra obrigatória de templates

Nunca criar HTML dentro do próprio componente TypeScript. Não usar `template:`
nem template string com markup em `@Component`, mesmo para templates pequenos.
Todo componente deve apontar para um arquivo separado:

```ts
@Component({
  selector: 'app-example',
  templateUrl: './example.component.html',
  styleUrl: './example.component.scss',
})
```

O mesmo princípio vale para estilos: preferir `styleUrl` apontando para um
arquivo `.scss` em vez de `styles: [...]`. O HTML deve conter apenas a view;
regras de negócio, estado e chamadas a serviços pertencem ao `.ts`.

Antes de concluir uma alteração, procurar violações com:

```bash
rg -n "template\\s*:|styles\\s*:" frontend/src --glob '*.ts'
```

O resultado esperado é vazio. Ao converter um componente existente, mover o
conteúdo de `template` para o `.html` e o conteúdo de `styles` para o `.scss`,
preservando bindings, eventos, control flow Angular e estilos encapsulados.

## Convenções de implementação

- Usar componentes standalone e declarar as dependências no array `imports`.
- Usar `inject()` para serviços e dependências de componentes quando esse for o
  padrão já adotado pelo arquivo.
- Usar signals para estado reativo local quando já adotado pela feature.
- Manter chamadas HTTP nos serviços de `core`, e não diretamente nos templates.
- Reutilizar os modelos em `core/models.ts` e o `AuthService`/interceptor para
  autenticação.
- Usar traduções de `public/i18n/` para textos de interface novos, mantendo as
  três localidades sincronizadas.
- O loader de traduções usa o caminho absoluto `/i18n/<idioma>.json`; manter os
  arquivos em `frontend/public/i18n/` para que o build os copie para a raiz dos
  assets publicados. O backend deve manter `/i18n/**` público, pois a tela de
  login precisa carregar traduções antes da autenticação.
- O inglês (`en`) é o idioma padrão e também fica embutido no bundle como
  fallback offline. Assim, `/login` não pode exibir chaves como `LOGIN.TITLE`
  mesmo que o request de `/i18n/en.json` falhe; os idiomas adicionais continuam
  sendo carregados de `/i18n/`.
- `LanguageService` deve tratar falhas no carregamento HTTP de um idioma salvo e
  trocar explicitamente para `en`; nunca deixar a exceção sem handler. O deploy
  de preprod deve validar que `/i18n/en.json` retorna o conteúdo esperado, além
  de apenas validar a página `/login`.
- Para componentes Material, importar explicitamente os módulos usados no
  componente standalone.
- Colocar testes ao lado da implementação quando a funcionalidade tiver
  comportamento não trivial.

## Verificação

Executar a partir de `frontend/` após mudanças:

```bash
npm install       # somente quando as dependências ainda não estiverem instaladas
npm run build
npm test -- --watch=false
```

Se a instalação já estiver preparada, `npm run build` é a verificação mínima.
Falhas de orçamento de CSS, bindings, imports ou templates devem ser corrigidas
antes de entregar a alteração.
