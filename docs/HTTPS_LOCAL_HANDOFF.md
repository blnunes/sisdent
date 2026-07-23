# Memória de continuidade: HTTPS na pré-produção local

Data do registo: 23 de julho de 2026

## Objetivo

Implementar HTTPS no ambiente local de pré-produção do Sisdent sem expor
o serviço à internet e sem comprometer o mecanismo atual de deploy, health
check e rollback.

Este documento é apenas uma memória para retomada do trabalho. Nenhuma
alteração de HTTPS foi implementada até este momento.

## Estado atual

- A aplicação roda numa máquina Ubuntu local por meio de Docker Compose.
- Um GitHub Actions self-hosted runner executa o deploy de imagens publicadas
  no GHCR.
- O Caddy atua como reverse proxy e publica atualmente apenas HTTP na porta 80.
- O `Caddyfile` desativa HTTPS explicitamente com `auto_https off`.
- O endereço local documentado é `sisdent-preprod.local`, anunciado via
  Avahi/mDNS.
- O Compose usa `127.0.0.1` como bind padrão, mas a instalação LAN documentada
  utiliza `0.0.0.0` ou o endereço IPv4 fixo da máquina.
- O deploy e o workflow ainda validam a saúde da aplicação por HTTP.
- O banco H2 persiste no volume `sisdent-preprod-data` e não deve ser removido.

## Decisão recomendada

Usar a autoridade certificadora interna do Caddy:

```caddyfile
sisdent-preprod.local {
	tls internal
	reverse_proxy app:8080
}
```

Essa é a solução indicada porque uma autoridade pública normalmente não
emite certificados para nomes `.local` ou endereços IP privados. A CA raiz
do Caddy precisará ser instalada como confiável nos dispositivos autorizados.

Não usar `curl -k`, `--insecure` ou equivalentes nas validações, pois isso
ocultaria falhas de certificado.

## Plano de implementação

1. Confirmar que `sisdent-preprod.local` resolve de forma consistente em todos
   os dispositivos que precisarão acessar o sistema.
2. Adicionar a publicação da porta 443 ao Compose e manter a porta 80 somente
   para redirecionamento para HTTPS.
3. Criar volumes persistentes para `/data` e `/config` do Caddy. Esses volumes
   devem preservar a CA e os certificados entre recriações dos containers.
4. Remover `auto_https off` e configurar o site
   `sisdent-preprod.local` com `tls internal`.
5. Manter os cabeçalhos defensivos existentes e adicionar HSTS inicialmente
   com duração curta.
6. Extrair somente o certificado público da CA raiz do Caddy. Nunca copiar,
   publicar ou versionar a chave privada da CA.
7. Instalar a CA nos computadores e dispositivos autorizados, começando por
   apenas um equipamento de teste.
8. Fazer o self-hosted runner confiar na CA ou fornecer a CA explicitamente ao
   `curl` com `--cacert`.
9. Atualizar o health check, o script de deploy e a validação da LAN para usar
   HTTPS com verificação real do certificado.
10. Restringir as portas 80 e 443 à rede ou aos IPs autorizados, incluindo uma
    revisão da cadeia `DOCKER-USER`, e manter desativado o port forwarding no
    router.
11. Validar redirecionamento, cadeia de confiança, hostname, Swagger, API,
    health check, persistência da CA e rollback.
12. Atualizar toda a documentação operacional antes de considerar a mudança
    concluída.

## Arquivos que provavelmente serão alterados

- `compose.preprod.yml`
- `deploy/preprod/Caddyfile`
- `deploy/preprod/deploy.sh`
- `deploy/preprod/runtime.env.example`, se for necessária nova configuração
- `.github/workflows/ci.yml`
- `docs/PREPROD.md`
- `docs/PREPROD_MACHINE_SETUP.md`
- `docs/PREPROD_UBUNTU_AGENT_PROMPT.md`
- `docs/PIPELINE.md`, caso contenha fluxos ou URLs HTTP afetados

## Cuidados críticos

- Não remover ou recriar o volume `sisdent-preprod-data`.
- Não guardar a chave privada da CA no Git, GitHub Actions, artefatos ou logs.
- Não depender de uma CA gerada num volume efémero.
- Não abrir as portas 80 ou 443 no router para a internet.
- Tratar o runner com acesso ao Docker como uma identidade privilegiada.
- Usar apenas dados fictícios durante os testes.
- Preparar backup e procedimento de recuperação dos volumes persistentes do
  Caddy antes de distribuir a CA aos utilizadores.
- Evitar HSTS de longa duração até a recuperação e a renovação terem sido
  testadas.

## Decisões a confirmar antes da implementação

- Quais sistemas operativos e dispositivos precisarão confiar na CA.
- Se o bind será feito no IPv4 fixo da LAN ou em `0.0.0.0` com firewall.
- Se a CA do Caddy será instalada na trust store do runner ou fornecida com
  `curl --cacert`.
- Onde será guardado o backup seguro dos volumes do Caddy.
- Se o acesso futuro continuará exclusivamente na LAN ou se será necessário
  um domínio real. Um domínio real mudaria a estratégia de certificados.

## Critérios de aceitação

- `http://sisdent-preprod.local` redireciona para HTTPS.
- `https://sisdent-preprod.local` aparece como confiável nos dispositivos
  autorizados.
- O certificado corresponde ao hostname e é validado sem exceções inseguras.
- O pipeline e o deploy validam HTTPS corretamente.
- A recriação dos containers preserva a mesma CA.
- O rollback permanece funcional.
- As portas não estão acessíveis fora da rede autorizada.
- Nenhuma chave privada ou segredo foi incluído no repositório ou em logs.

## Ponto de retomada

Na próxima sessão, começar revendo as decisões pendentes acima. Depois,
implementar primeiro as mudanças de Compose e Caddy numa branch separada,
validar estaticamente a configuração e somente então preparar o deploy
controlado na máquina Ubuntu.

Pedido sugerido para retomar:

> Continue a partir de `docs/HTTPS_LOCAL_HANDOFF.md`. Primeiro reveja o estado
> atual do repositório e apresente as decisões pendentes. Não faça deploy na
> máquina Ubuntu sem autorização explícita.
