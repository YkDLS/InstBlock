# Segurança e privacidade

O aplicativo não possui conta ou servidor próprio. A permissão de internet é usada exclusivamente pelo verificador opcional de atualizações, que consulta as Releases públicas do repositório oficial no GitHub.

O serviço de acessibilidade observa apenas eventos de mudança de janela e compara o pacote com `com.instagram.android`. Ele não solicita leitura do conteúdo da tela.

## Controles implementados

- O manifesto desativa backup e tráfego HTTP em texto claro.
- O serviço de acessibilidade recebe somente mudanças de janela do pacote oficial do Instagram e não recupera conteúdo.
- Activities, receiver e `FileProvider` que não precisam ser públicos não são exportados; o serviço exportado exige a permissão de assinatura `BIND_ACCESSIBILITY_SERVICE`.
- O `PendingIntent` das notificações é imutável, e o conteúdo do canal é privado na tela bloqueada.
- O `FileProvider` compartilha apenas `cache/updates/`, não todo o cache do aplicativo.
- Metadados de atualização são limitados a 1 MiB e APKs a 200 MiB.
- Cada redirecionamento HTTPS é validado contra uma lista fechada de hosts oficiais do GitHub.
- Antes de abrir o instalador, o APK precisa ter o mesmo identificador e os mesmos certificados de assinatura do aplicativo instalado.
- Downloads incompletos ou inválidos são removidos e nunca são encaminhados ao instalador.
- O cronômetro usa tempo monotônico e checkpoints locais; alterar a hora civil não reduz o bloqueio.

## Modelo de ameaça e limites

O atualizador confia na disponibilidade do GitHub e no TLS do Android, mas não confia apenas no conteúdo de uma Release: pacote e assinatura são verificados localmente. A chave de assinatura usada para distribuir uma versão deve ser preservada; trocar a chave sem uma linhagem de rotação compatível fará a atualização ser recusada.

Aplicativos bancários podem recusar qualquer serviço de acessibilidade habilitado, mesmo quando o serviço não recebe eventos do banco. Essa política externa não pode ser contornada pelo InstBlock.

As preferências locais não contêm credenciais nem conteúdo de tela. Um usuário com root ou controle físico total do dispositivo pode modificar esses dados; esse cenário está fora do objetivo de uma ferramenta de autocontrole local.

## Verificação de segurança

Antes de publicar, execute:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon
```

Revise também o relatório `app/build/reports/lint-results-debug.html`, a árvore de dependências com `:app:dependencies` e o APK com `apkanalyzer`/`aapt2` para confirmar permissões, componentes exportados e versão.

Não publique neste repositório:

- `local.properties`;
- chaves de assinatura ou keystores;
- tokens, senhas ou arquivos de configuração de serviços externos;
- APKs de produção assinados.

Para relatar uma vulnerabilidade, prefira um aviso privado ao mantenedor. Se isso não estiver disponível, abra uma issue apenas para solicitar um canal privado, sem incluir detalhes exploráveis, dados pessoais ou credenciais.
