# Handoff: ações da tabela de pacientes

## Situação atual

O problema visual ainda é relatado pelo usuário: em linhas de pacientes ativos, o botão **Ver detalhes** (`visibility`) fica parcialmente oculto ou não utilizável. A intenção é que pacientes ativos tenham as ações **ver detalhes**, **editar** e **desativar**.

Não considerar o problema resolvido apenas porque os testes atuais passam.

## Fatos confirmados

### Montagem das ações

O componente compartilhado que monta as ações é `frontend/src/app/features/resource-support/resource-list.controller.ts`.

Sua regra é:

1. Inclui `view` quando `definition.canView?.()` é verdadeiro.
2. Inclui `edit` quando o usuário tem a permissão de manutenção.
3. Inclui `delete` quando `definition.canDelete(record)` é verdadeiro.

Em `frontend/src/app/features/patients/patients.component.ts`, a definição é:

```ts
canView: () => true,
canDelete: (record) => record['active'] === true,
```

Logo:

- Paciente ativo: `view`, `edit`, `delete` (a operação é uma desativação via `DELETE`).
- Paciente inativo: `view`, `edit`; não recebe `delete` porque já está inativo.

O usuário inspecionou uma linha ativa no DevTools e enviou o DOM. Ele confirma `Ativo` na célula de estado e **três botões** na célula de ações, inclusive:

```html
button aria-label="Ver detalhes"
mat-icon>visibility</mat-icon>
button aria-label="Editar registo"
mat-icon>edit</mat-icon>
button aria-label="Desativar paciente"
mat-icon>delete_outline</mat-icon>
```

Assim, para pacientes ativos, o botão de detalhes é criado corretamente. O problema restante está na geometria/renderização/área clicável no navegador usado pelo usuário, não na regra de autorização ou na geração da ação.

### Estado e dados

O DTO do backend expõe `active` como booleano (`src/main/java/br/com/itbn/sisdent/dto/PatientResponse.java`). Não foi encontrada conversão conhecida da resposta de lista que mudasse esse valor para string.

Não alterar a regra de pacientes inativos sem uma decisão de produto: o rótulo de excluir no fluxo atual corresponde a desativar o vínculo/paciente no contexto da organização, não a remoção física.

## Alterações feitas nesta investigação

Todas estão no branch `feat/preprod-deployment`:

| Commit | Alteração |
| --- | --- |
| `fc21eaf` | Primeira tentativa de aumentar a coluna/ações do `DataTableComponent`. |
| `36a8c56` | Fixou tamanhos de botões e adicionou uma verificação E2E inicial de contenção. |
| `69d8872` | Adicionou `colgroup` no `DataTableComponent`. |
| `0fb7ef9` | Tentou `table-layout: auto`; esta abordagem foi substituída depois. |
| `6877cfd` | Estado final atual: `table-layout: fixed` e `width: 184px` aplicada diretamente ao elemento `<col>` da coluna de ações. |

Arquivos atualmente relevantes:

- `frontend/src/app/shared/data-table/data-table.component.html`
- `frontend/src/app/shared/data-table/data-table.component.scss`
- `frontend/src/app/shared/data-table/data-table.component.spec.ts`
- `frontend/e2e/patients.spec.ts`
- `frontend/src/app/features/resource-support/resource-list.controller.ts`
- `frontend/src/app/features/patients/patients.component.ts`

## Validações já executadas

- Teste unitário focado de `DataTableComponent`: passou.
- `npm run build` em `frontend`: passou.
- E2E focado em pacientes: passou.

O E2E em `frontend/e2e/patients.spec.ts` verifica para uma linha ativa:

- três botões na coluna de ações;
- célula de ações com largura de pelo menos `184px`;
- retângulos dos botões contidos no retângulo da célula.

Apesar disso, a captura fornecida pelo usuário ainda mostra o ícone `visibility` parcialmente recortado. Portanto esse E2E não reproduz o ambiente/viewport/navegador da captura e não deve ser usado como prova definitiva de resolução.

## Próximo diagnóstico recomendado

Antes de alterar mais CSS, reproduzir no mesmo navegador e na mesma URL usados pelo usuário. No DevTools da linha ativa, registrar:

```js
const cell = document.querySelector('td.mat-column-actions');
const group = cell?.querySelector('.actions');
const table = cell?.closest('table');
const buttons = [...(group?.querySelectorAll('button') ?? [])];

({
  url: location.href,
  viewport: { width: innerWidth, height: innerHeight },
  table: table?.getBoundingClientRect().toJSON(),
  cell: cell?.getBoundingClientRect().toJSON(),
  group: group?.getBoundingClientRect().toJSON(),
  buttons: buttons.map((button) => ({
    label: button.getAttribute('aria-label'),
    rect: button.getBoundingClientRect().toJSON(),
    style: {
      width: getComputedStyle(button).width,
      minWidth: getComputedStyle(button).minWidth,
      flex: getComputedStyle(button).flex,
    },
  })),
  tableLayout: table ? getComputedStyle(table).tableLayout : undefined,
  cellStyle: cell ? {
    width: getComputedStyle(cell).width,
    paddingLeft: getComputedStyle(cell).paddingLeft,
    paddingRight: getComputedStyle(cell).paddingRight,
    overflow: getComputedStyle(cell).overflow,
  } : undefined,
});
```

Também inspecionar o `<colgroup>` no DOM para confirmar que o último `col` contém `style="width: 184px"`.

Com esses valores será possível identificar se a causa é:

1. uma instância/bundle diferente do código atual;
2. uma regra de browser/Material que sobrepõe a largura;
3. clipping por um ancestral (`.table-card`, `.table-scroll` ou outro contêiner);
4. viewport/zoom que o E2E atual não reproduz.

Evitar novas tentativas genéricas de tamanho até ter essas métricas reais.
