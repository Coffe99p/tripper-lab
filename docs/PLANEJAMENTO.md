# Plano de ação — Tripper Lab

Atualizado em 19/11/2026.

## Decisões do usuário

- Uso pessoal no Pixel 8 Pro, informado como Android 17.
- Moto: Royal Enfield Meteor 350, ano-modelo ainda não confirmado.
- Pular a etapa 1 (medição com o app Royal). Medir o nosso app depois; não alegar economia em relação à Royal sem uma comparação real.
- Executar uma etapa por vez. Etapas 2, 3 e 4 concluídas. Etapa 5 autorizada em 10/11/2026, etapa 6 em 12/11/2026 e etapa 7 em 19/11/2026. Encerrar a entrega após a etapa 7, mantendo as validações adiadas registradas.
- Em 12/11, o usuário aceitou encerrar a entrega da etapa 5 com validação parcial e adiar o teste em trajeto. As pendências físicas não bloqueiam a próxima etapa, mas não devem ser registradas como testes aprovados.
- Na entrega da etapa 6, o usuário escolheu deixar a coleta comparativa de consumo para depois. Entregar a ferramenta e melhorias, preservando a ausência de resultado medido.

## Etapas

| Etapa | Escopo | Estado / critério |
|---|---|---|
| 1 | Registrar o consumo atual do app Royal | Pulada a pedido do usuário |
| 2 | Preparar projeto Kotlin, gerar instalador e testar no Pixel | Concluída em 03/11/2026. App 0.1.0 instalado, aberto e respondendo no Pixel 8 Pro com Android 17 |
| 3 | Identificar o Tripper, implementar conexão e comandos simulados | Concluída em 03/11/2026. Versão 0.2.0 testada pelo usuário: setas/distâncias, tela bloqueada, encerramento e reconexão sem novo PIN |
| 4 | Inspecionar notificações do Google Maps | Concluída em 10/11/2026 para os formatos reais coletados em inglês. Export analisado, leitura corrigida, 0.3.2 instalada. Limitações documentadas: distância ausente, rotatória sem número e chegada sem confirmação textual definitiva |
| 5 | Integrar Maps e Tripper | Entrega encerrada com validação parcial aceita pelo usuário em 12/11. 0.4.1 instalada; 36 testes aprovados. Conexão, simulação e espera confirmadas. Conversão real do Maps, bloqueio da integração e confirmação visual do encerramento ficam pendentes, sem bloquear a etapa 6 |
| 6 | Tratar falhas e medir consumo | Software entregue em 12/11: 0.5.0 instalada, 50 testes aprovados, encerramento e diagnóstico melhorados, medidor para dois cenários. Coleta real de 20–25 min por cenário adiada pelo usuário; consumo/economia não medidos |
| 7 | Finalizar a versão pessoal | Concluída em 19/11: interface de uso, ajuda local, instalador 0.6.0 e guia. Instalada e conferida no Pixel; 50 testes e lint aprovados. Validações físicas e comparação de consumo continuam pendentes |

## Limite da etapa 2

Aplicativo nativo simples com identificação do aparelho e leitura pontual da bateria. Sem permissões especiais, conexão Bluetooth, leitura de notificações, serviço de navegação ou funcionamento em segundo plano.

A etapa 3 foi concluída com o Tripper físico do usuário. O protocolo comunitário foi revisado e a comunicação básica foi validada nesta combinação de Pixel 8 Pro / Android 17 / Tripper. A etapa 4 está concluída e a etapa 5 foi autorizada pelo usuário.

Resultado e evidências: [VALIDACAO-ETAPA-2.md](VALIDACAO-ETAPA-2.md).

Resultado da etapa 3: [VALIDACAO-ETAPA-3.md](VALIDACAO-ETAPA-3.md). Os testes Bluetooth preservados usam comandos simulados e sessões de até 10 minutos; a captura separada do Maps dura até 20 minutos.

Resultado da etapa 4: [VALIDACAO-ETAPA-4.md](VALIDACAO-ETAPA-4.md) e [análise do export](ANALISE-TRAJETO-2026-11-10.md). A coleta adiada foi entregue e analisada. As atualizações não retomam captura ou envio automaticamente.

Na etapa 5, definir a apresentação de dados incompletos e a correspondência das novas manobras com os símbolos do Tripper, validar a remoção de instruções antigas e testar Maps → mostrador com a moto parada. Não presumir que as manobras novas já foram comprovadas no hardware pelos testes simulados da etapa 3.

Roteiro disponível para retomada: [TESTE-ETAPA-5.md](TESTE-ETAPA-5.md). Resultado e aceite parcial: [VALIDACAO-ETAPA-5.md](VALIDACAO-ETAPA-5.md). Na versão 0.4.1, a sessão era limitada a 10 minutos desde o início da conexão. Por decisão do usuário, o teste em trajeto fica adiado e não é pré-requisito para a etapa 6; as limitações devem acompanhar as próximas entregas.

Na 0.5.0, a etapa 6 passou a permitir 10 minutos por padrão ou 30 minutos por escolha explícita antes de conectar, para viabilizar uma coleta de 20–25 minutos. Resultado: [VALIDACAO-ETAPA-6.md](VALIDACAO-ETAPA-6.md). Coleta futura: [TESTE-ETAPA-6.md](TESTE-ETAPA-6.md). Não declarar economia sem resultados comparáveis. A etapa 7 foi autorizada em 19/11 e concluída no escopo de interface, instalador e instruções; não altera as pendências de coleta.


Entrega da etapa 7: [VALIDACAO-ETAPA-7.md](VALIDACAO-ETAPA-7.md). Instruções atuais: [GUIA-DE-USO.md](GUIA-DE-USO.md). A versão pessoal 0.6.0 abre diretamente a conexão e reúne ferramentas opcionais em uma seção separada. A implementação planejada está entregue; validações físicas adiadas e medição real continuam pendentes.
