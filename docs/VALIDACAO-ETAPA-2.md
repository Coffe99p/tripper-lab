# Validação da etapa 2

Data: 03/11/2026. Versão: 0.1.0.

## Verificado no computador

- Android Studio, Java e Android SDK disponíveis.
- Projeto Kotlin criado com Gradle Wrapper reproduzível.
- `:app:assembleDebug`: aprovado.
- `:app:lintDebug`: aprovado, nenhum problema encontrado. Avisos também são tratados como erros; notificações de atualização de ferramentas são excluídas dessa verificação.
- APK inspecionado: pacote `com.joaofabijack.tripper`, versão 0.1.0, Android mínimo API 31, alvo API 36, atividade inicial válida e nenhuma permissão solicitada.
- Assinatura do APK verificada com `apksigner` (v2 válida).
- Instalador: `artifacts/Tripper-Lab-0.1.0.apk` (2.452.883 bytes).
- SHA-256: `02A82E4CBF1FB7C4F41437E62056C77CC2CB76E42284C344F5BCA5C537DAFDDA`.

## Verificação no Pixel

- Usuário habilitou a depuração USB e autorizou o computador.
- Pixel 8 Pro reconhecido pelo ADB. Android 17 / API 37 confirmado no aparelho.
- Instalação do APK: `Success`.
- Primeira abertura: `Status: ok`, 578 ms reportados pelo Android.
- Tela capturada e inspecionada: dados do Pixel, bateria, botões e texto da próxima etapa legíveis. Conteúdo final acessível por rolagem.
- Botão **Atualizar diagnóstico** testado: horário mudou de 15:18:33 para 15:18:57.
- Botão **Copiar diagnóstico** acionado sem impedir a continuidade do teste; conteúdo da área de transferência não foi inspecionado automaticamente.
- App encerrado e reaberto: `Status: ok`, 348 ms reportados pelo Android; atividade confirmada em primeiro plano.
- Consulta de erros `AndroidRuntime` do processo reaberto não retornou erros.
- Evidências locais: `artifacts/tripper-stage2.png`, `artifacts/tripper-stage2-ui.xml` e `artifacts/tripper-stage2-refreshed.xml`.

**Etapa 2 concluída.** Os testes são de instalação e funcionamento básico. Não incluem rotação, acessibilidade completa, longa duração ou outros modelos de celular.

Nenhum teste de Bluetooth, Maps ou consumo comparativo foi executado. Esses testes pertencem às etapas seguintes.
