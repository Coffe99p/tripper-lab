# Validação da etapa 3 — versão 0.2.0

Data: 03/11/2026.

## Verificado

- Compilação `:app:assembleDebug`: aprovada.
- Testes `:app:testDebugUnitTest`: 5 aprovados, nenhuma falha ou erro. Cobrem vetor padrão de CRC, pacotes fixos publicados, fronteira 999 m / 1 km, códigos de esquerda/direita, PIN com zeros iniciais e entradas inválidas.
- `:app:lintDebug`: aprovado. Mantida a verificação de permissões na Activity; a classe de serviço documenta a validação de permissões na entrada e o tratamento de revogação durante a sessão. Três falsos positivos do dicionário inglês para “comando(s)” foram suprimidos apenas nos respectivos textos em português.
- Atualização instalada no Pixel 8 Pro com Android 17: `Success`.
- Tela inicial capturada e inspecionada: versão 0.2.0, estado inicial, botões e instruções legíveis.
- Consulta de erros AndroidRuntime do processo após abrir: nenhum erro retornado.
- Assinatura do APK verificada por `apksigner`.
- SHA-256 do instalador 0.2.0: `2AD1430F22E57FDB7A55A6C79EDB576E7F8EBF30E048FE5ED6B1A0799B0183FD`.

## Teste físico relatado pelo usuário

O usuário relatou: “Apareceu o tripper, eu conectei e enviei a instrucao teste, deu certo ate ai”. Isso confirma descoberta, conexão e exibição do primeiro comando no equipamento real. O PIN não foi coletado nesta conversa. Não houve observação direta do mostrador pelo computador.

Após o roteiro de esquerda/50 m, em frente/1,5 km, bloqueio por 60 segundos e atualização para direita/200 m, o usuário confirmou: “Sim, tudo apareceu corretamente, inclusive após bloquear”. Setas, unidades m/km e manutenção/atualização após bloqueio estão, portanto, validadas pelo relato do usuário neste equipamento.

## Encerramento e reconexão

Foi solicitado salvar após confirmação visual, encerrar e conferir a remoção da notificação, religar a ignição, reconectar sem PIN, enviar uma seta e encerrar novamente. O usuário confirmou: “Encerrou e reconectou corretamente, sem novo PIN”. O fluxo de encerramento e reconexão manual está validado pelo relato do usuário.

**Etapa 3 concluída.** A implementação foi compilada e verificada no computador; a exibição no equipamento físico e o comportamento com a tela bloqueada foram confirmados pelo usuário. Não é uma validação de todos os firmwares ou de uso prolongado em trânsito.

## Limitações e testes ainda não realizados

- A rotatória experimental não foi confirmada no teste relatado.
- Expiração automática de 10 minutos, revogação de permissões durante a sessão e todas as falhas de rádio não foram exercitadas fisicamente.
- Não foi medida a economia de energia. O wakelock limitado pertence ao diagnóstico, e sua necessidade/consumo serão reavaliados na etapa 6.
- O teste de tela bloqueada cobriu o roteiro de 60 segundos; não comprova sessões de longa duração.

Não foram implementados captura de notificações, interpretação do Maps, navegação real ou reconexão automática. Nenhum resultado de economia de bateria foi medido ou alegado.
