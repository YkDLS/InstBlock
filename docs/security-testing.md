# Testes de segurança do InstBlock

## Escopo automatizado

Os testes de `UpdateSecurityPolicyTest` exercitam URLs com host sufixado, `userinfo`, HTTP, porta inesperada, fragmento, host não autorizado, versão malformada e downgrade. Os testes do armazenamento verificam que mudança de relógio e reboot não encurtam a espera.

O Android Lint cobre manifesto, permissões, APIs, recursos e práticas conhecidas pela versão do Android Gradle Plugin. O relatório HTML deve ser preservado como artefato de CI, não versionado.

## Roteiro manual

1. Confirme no APK que somente `INTERNET`, `REQUEST_INSTALL_PACKAGES`, `POST_NOTIFICATIONS` e `RECEIVE_BOOT_COMPLETED` são solicitadas.
2. Confirme que `BlockedActivity`, `TimerReceiver` e `FileProvider` não são exportados.
3. Confirme que o `AccessibilityService` exige `BIND_ACCESSIBILITY_SERVICE`, observa apenas `com.instagram.android` e mantém `canRetrieveWindowContent=false`.
4. Intercepte uma consulta de atualização em ambiente de teste e redirecione para host não autorizado; o download deve falhar.
5. Tente instalar um APK com outro package name ou outra assinatura pelo fluxo de atualização; o arquivo deve ser recusado antes do instalador.
6. Interrompa um download; não deve existir APK final utilizável, apenas o `.part` removido na falha.
7. Mude data e hora, reinicie e desligue durante uma espera; nenhum caso pode reduzir o tempo salvo.
8. Com a tela bloqueada, confirme que a notificação não revela o texto completo quando a política do sistema oculta conteúdo privado.

## Supply chain

- O Gradle Wrapper valida a URL de distribuição e usa HTTPS.
- Plugins e bibliotecas vêm de `google()`, `mavenCentral()` e `gradlePluginPortal()`.
- APKs publicados precisam ser assinados com uma chave controlada pelo mantenedor e verificados por hash.
- Dependências e actions devem ser revisadas antes de cada release; alertas do GitHub/Dependabot devem ser tratados como entrada de triagem, não como prova isolada de vulnerabilidade.

## Riscos residuais

- Não há pinning de certificado; a confiança segue a store de CAs do Android.
- O serviço de acessibilidade pode ser desativado pelo usuário ou pelo sistema.
- Uma queda abrupta pode acrescentar até um intervalo de checkpoint, mas não encurta a espera.
- Builds debug não são adequados como estratégia permanente de distribuição; releases estáveis devem usar assinatura própria, protegida e reproduzível.
