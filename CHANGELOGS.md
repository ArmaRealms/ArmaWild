# Histórico de alterações

O ArmaWild utiliza o [Versionamento Semântico 2.0.0](https://semver.org/lang/pt-BR/spec/v2.0.0.html) no formato `MAJOR.MINOR.PATCH`: MAJOR para mudanças incompatíveis na API pública, MINOR para funcionalidades retrocompatíveis e PATCH para correções retrocompatíveis.

Este histórico parte da versão **0.15.0**. As seções são organizadas por versões **efetivamente publicadas**; **Não lançado** reúne o que entrou na `master` após a última tag, sem atribuir um número ou data de lançamento inexistente. Uma mudança de compatibilidade deve ser avaliada antes da próxima publicação.

## [Não lançado]

### Adicionado
- Comandos nativos pela **Paper Command API / Brigadier**, com registro em `LifecycleEvents.COMMANDS` para `/rtp`, `/wild`, `/forcertp` e `/rtp-admin`. Inclui sugestões de perfis, jogadores e mundos. [#14](https://github.com/ArmaRealms/ArmaWild/pull/14)
- Atalhos gerados dos arquivos `rtpSettings/*.yml` com `command-enabled: true`: por exemplo, `end.yml` permite `/end`, além de `/rtp end`. Atalhos respeitam `jakesrtp.use` e permissões explícitas do perfil, sem exigir `jakesrtp.usebyname`; a forma `/rtp <perfil>` continua exigindo essa permissão. [#14](https://github.com/ArmaRealms/ArmaWild/pull/14)
- Diálogo nativo do Paper para RTP com custo positivo quando o jogador não informa `confirm`. Exibe perfil e valor formatado pela economia; **Confirmar** executa o comando de confirmação correspondente, **Cancelar** e **ESC** fecham sem teleportar ou cobrar. [#15](https://github.com/ArmaRealms/ArmaWild/pull/15)
- Mensagens configuráveis via MiniMessage para título, corpo e botões do diálogo (`confirm-rtp-dialog-*`), com traduções em português e inglês. [#15](https://github.com/ArmaRealms/ArmaWild/pull/15)

### Alterado
- Substituído o registro antigo via `TabExecutor` e as declarações de comandos em `plugin.yml` pelas árvores Brigadier; as definições de permissões permanecem em `plugin.yml`. [#14](https://github.com/ArmaRealms/ArmaWild/pull/14)
- A confirmação de RTP pago deixou de usar a mensagem clicável no chat e passou ao diálogo. Comandos `/rtp confirm`, `/rtp <perfil> confirm` e `/end confirm` continuam funcionando sem abrir o diálogo; o saldo, cooldown e warmup continuam sendo verificados no fluxo existente. A chave `confirm-paid-rtp` é legada e não é exibida nesse fluxo. [#15](https://github.com/ArmaRealms/ArmaWild/pull/15)
- Simplificada a mensagem padrão do diálogo para **`Deseja teleportar pro <profile> por <value>?`** em português; `<value>` usa a formatação da economia. O placeholder anterior `<cost>` continua reconhecido em mensagens personalizadas. (Este PR)

### Corrigido
- Evitada a criação de atalhos de perfis com nomes inválidos, reservados ou já ocupados por outro comando no momento do registro. [#14](https://github.com/ArmaRealms/ArmaWild/pull/14)
- Ajustado o argumento `#static` em `/rtp-admin status` para a árvore Brigadier. [#14](https://github.com/ArmaRealms/ArmaWild/pull/14)
- Removido um registro Bukkit obsoleto do método de recarga de listeners após a migração. [#15](https://github.com/ArmaRealms/ArmaWild/pull/15)
- Ampliados os testes para aliases, permissões, confirmação paga, diálogo, RTP gratuito e saldo insuficiente. [#14](https://github.com/ArmaRealms/ArmaWild/pull/14) [#15](https://github.com/ArmaRealms/ArmaWild/pull/15)

### Compatibilidade e migração
- O registro Brigadier e os diálogos exigem **Paper**. O projeto utiliza a API **Paper 1.21.11** e Java 25; a migração deixa de depender dos executores de comando tradicionais do Bukkit/Spigot. Avaliar a incompatibilidade de plataforma e API de comandos ao definir a próxima versão MAJOR segundo SemVer.
- A criação, remoção ou renomeação de arquivos de perfil requer reinício para reconstruir os atalhos. `/rtp-admin reload` recarrega as configurações dos perfis existentes.
- As configurações de RTP do servidor permanecem externas ao JAR. Não foram adicionados perfis fixos de mundos ao plugin.

## [1.0.0] - 2026-10-06

### Adicionado
- Publicação Maven de componentes Java e JARs de código-fonte via `maven-publish`, possibilitando distribuição pelo JitPack. [#13](https://github.com/ArmaRealms/ArmaWild/pull/13)
- Configuração `jitpack.yml` para OpenJDK 25. [#13](https://github.com/ArmaRealms/ArmaWild/pull/13)

### Alterado
- Exigido Java 25 em todos os subprojetos: toolchain e `--release 25`. [#12](https://github.com/ArmaRealms/ArmaWild/pull/12)
- Definida a versão de lançamento `1.0.0`, com `group` e `version` configuráveis pelas propriedades Gradle. [#13](https://github.com/ArmaRealms/ArmaWild/pull/13)

### Corrigido
- Ajustada a instalação do JitPack para publicar somente o módulo `api` via `:api:publishToMavenLocal`, com diretório Git seguro configurado. [Commit ab32f57](https://github.com/ArmaRealms/ArmaWild/commit/ab32f57d52f9174b21cdd4514933ef15d995ac5b)

## [0.15.0] - 2026-10-06

Versão de referência deste histórico. Conforme as notas da [release 0.15.0](https://github.com/ArmaRealms/ArmaWild/releases/tag/0.15.0):

### Adicionado
- Evento público `PlayerRtpEvent` disparado no RTP. [#6](https://github.com/ArmaRealms/ArmaWild/pull/6)
- `JakesRtpAPI` e registro como serviço para integração de outros plugins. [#7](https://github.com/ArmaRealms/ArmaWild/pull/7)
- Confirmação configurável para RTP pago, inicialmente por mensagem clicável. [#8](https://github.com/ArmaRealms/ArmaWild/pull/8)
- Suporte a MiniMessage nas mensagens. [#9](https://github.com/ArmaRealms/ArmaWild/pull/9)

### Alterado
- Atualizada a configuração Gradle para Kotlin DSL e ajustada a compilação do plugin. [#1](https://github.com/ArmaRealms/ArmaWild/pull/1) [#2](https://github.com/ArmaRealms/ArmaWild/pull/2)
- Atualizada a notificação de fim de cooldown e a configuração de plugins Gradle. [#5](https://github.com/ArmaRealms/ArmaWild/pull/5)

### Corrigido
- Corrigidos comandos e formatação de código. [#3](https://github.com/ArmaRealms/ArmaWild/pull/3) [#4](https://github.com/ArmaRealms/ArmaWild/pull/4)
- Aplicadas correções adicionais consolidadas na versão. [#5](https://github.com/ArmaRealms/ArmaWild/pull/5) [#10](https://github.com/ArmaRealms/ArmaWild/pull/10)

[Não lançado]: https://github.com/ArmaRealms/ArmaWild/compare/1.0.0...master
[1.0.0]: https://github.com/ArmaRealms/ArmaWild/compare/0.15.0...1.0.0
[0.15.0]: https://github.com/ArmaRealms/ArmaWild/releases/tag/0.15.0
