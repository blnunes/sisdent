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
