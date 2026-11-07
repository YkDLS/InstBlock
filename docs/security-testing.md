# Testes de segurança do InstBlock

## Escopo automatizado

Os testes de `UpdateSecurityPolicyTest` exercitam URLs com host sufixado, `userinfo`, HTTP, porta inesperada, fragmento, host não autorizado, caminho fora da Release oficial, versão malformada e downgrade de `versionCode`. Os testes do armazenamento verificam que mudança de relógio e reboot não encurtam a espera; `ForegroundEntryTrackerTest` verifica que uma mudança para trás na hora civil não paralisa a detecção de novas entradas. `node --test landing-page/serve.test.mjs` verifica os cabeçalhos HTTP, os métodos aceitos e a contenção de caminhos do servidor local.

O Android Lint cobre manifesto, permissões, APIs, recursos e práticas conhecidas pela versão do Android Gradle Plugin. O relatório HTML deve ser preservado como artefato de CI, não versionado.

## Roteiro manual

1. Confirme no APK as permissões `INTERNET`, `REQUEST_INSTALL_PACKAGES`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `PACKAGE_USAGE_STATS`, `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE` e `FOREGROUND_SERVICE_SPECIAL_USE`. No APK debug, confirme também que `PreviewActivity` não foi incluída.
2. Confirme que `BlockedActivity`, `TimerReceiver` e `FileProvider` não são exportados.
3. Confirme que não há `AccessibilityService` no manifesto e que o monitor é não exportado; verifique que a sobreposição só aparece sobre `com.instagram.android`.
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
- O sistema pode interromper o serviço em primeiro plano; o usuário pode retirar acesso ao uso ou à sobreposição.
- Uma queda abrupta pode acrescentar até um intervalo de checkpoint, mas não encurta a espera.
- Builds debug não são adequados como estratégia permanente de distribuição; releases estáveis devem usar assinatura própria, protegida e reproduzível.
