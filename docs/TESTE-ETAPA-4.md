# Etapa 4 — diagnóstico do Google Maps

Versão atual de diagnóstico: 0.3.2, 10/11/2026. Etapa de inspeção concluída para as amostras entregues; este roteiro fica disponível para ampliar a cobertura. O objetivo é descobrir quais informações reais o Maps entrega, não assumir que toda notificação contém uma instrução completa.

## Coleta

1. Abra o Tripper Lab e use **Configurar acesso às notificações**. Autorize o app no Android e volte.
2. Toque em **Iniciar captura do Maps**. A coleta dura até 20 minutos e só registra `com.google.android.apps.maps`.
3. Abra o Maps e inicie uma rota. Volte ao Tripper Lab para conferir a amostra recebida.
4. Durante uma captura, bloqueie a tela por 60 segundos. Confira depois se existem eventos com `locked: true` e se novas atualizações chegam.
5. Cancele a rota no Maps. A captura deve registrar a remoção ou substituição da notificação. Não confundir cancelamento com chegada.
6. Use **Parar captura**. Uma nova rota posterior não deve aumentar o registro enquanto a captura estiver parada.
7. **Exportar registro do Maps** permite escolher onde salvar um JSON contendo eventos e ícones. **Apagar registro do Maps** encerra e elimina a cópia privada, sem apagar exportações anteriores.

O acesso concedido pelo Android é amplo, mas o código verifica o pacote e a captura ativa antes de ler o conteúdo. O registro pode conter ruas e destinos, fica no armazenamento privado excluído de backup (`noBackupFilesDir`) e não é enviado pela internet. Não há acesso a localização, acessibilidade ou conteúdo de outros apps.

Os ícones são apenas amostras visuais; os IDs de recursos do Maps não são usados como códigos universais de manobra. Textos, falta de conteúdo ou encerramento não são convertidos automaticamente em uma orientação.

## Cobertura necessária para concluir a etapa

- [x] Extrair manobra e distância de uma amostra real, identificando os campos ou ícones de origem (direita/esquerda no título).
- [x] Conferir os formatos observados de conversões à esquerda/direita, manter à direita e rotatórias. Preservar explicitamente a ausência de distância, número de saída ou lado da entrada em via; não completar por suposição.
- [x] Conferir atualizações com a tela bloqueada (678 amostras em 10/11, além das anteriores; ver registro de validação).
- [x] Observar recálculo de rota (`Rerouting...`, 10/11).
- [x] Observar chegada e confrontá-la com cancelamento: `Arriving` seguido de remoção; usuário confirmou encerramento automático ao chegar em 10/11. Remoção isolada não distingue os dois casos no código.
- [x] Remover uma notificação invalida sua amostra atual (cancelamento observado no Pixel).
- [x] Captura encerrada pelo botão; ignorar novos eventos é coberto pelos testes unitários.
- [ ] Verificação adicional: nova rota real iniciada enquanto a captura estiver parada (bloqueio já coberto em teste unitário).

Os casos que não surgirem com o aparelho parado exigem coleta em um trajeto apropriado. Configure antes de se deslocar; não opere o celular dirigindo. Não é necessário usar o Tripper para este diagnóstico.

O trajeto inicialmente adiado foi entregue e analisado em 10/11. Para ampliar a amostra futuramente: com o celular parado, inicie a captura antes de iniciar a rota no Maps; após a chegada, pare a captura e exporte o registro. Se ocorrer recálculo naturalmente, ele poderá ser analisado; não é necessário provocar manobras ou desvios para o teste. A coleta para automaticamente em 20 minutos.

## Limites do diagnóstico

Até 2.000 eventos, 32 ícones distintos de 96×96 e arquivo privado compacto de até 2.000.000 bytes. O JSON exportado tem indentação e pode exceder esse tamanho sem que o limite do arquivo privado tenha falhado. Eventos antigos são descartados com contador; ícones que excedam o limite ficam ausentes. O processo não retoma uma captura automaticamente após ser recriado. O conteúdo mostrado é uma amostra com horário, não orientação válida em tempo real.

## Referências

- [NotificationListenerService](https://developer.android.com/reference/android/service/notification/NotificationListenerService): autorização, conexão do listener, eventos e snapshot inicial.
- [Notification](https://developer.android.com/reference/android/app/Notification): campos textuais e ícones da notificação.

Nenhum dado do Maps é transmitido ao Bluetooth nesta etapa. A integração fica para a etapa 5.
