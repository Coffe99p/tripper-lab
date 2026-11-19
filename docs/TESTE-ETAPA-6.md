# Medir consumo — coleta posterior

Versão 0.5.0. O usuário adiou a coleta em 12/11/2026. Este roteiro não exige repetir agora o trajeto pendente da etapa 5.

## Preparar as condições

Use duas sessões de 20–25 minutos no mesmo percurso ou em condições equivalentes, com carga inicial e temperatura semelhantes. Mantenha brilho, uso da tela, rede, orientação por voz e outros aplicativos semelhantes. Repita a dupla de sessões, alternando a ordem, antes de tirar conclusões.

O celular deve ficar sem carregador, inclusive USB. A captura de diagnóstico de notificações deve estar parada. Configure tudo antes de se deslocar e opere o celular somente depois de parar. Sem garantia de cobertura completa das manobras: as ressalvas da etapa 5 continuam registradas.

## A — Maps sem Tripper

1. Encerre a sessão do Tripper Lab pelo botão **Encerrar teste** ou pela notificação da sessão.
2. Inicie a rota no Maps.
3. Na tela inicial do Tripper Lab, abra **Medir bateria** e toque em **Iniciar: Maps sem Tripper**.
4. Use o Maps por 20–25 minutos com as condições de tela planejadas. Não conecte carregador nem ative captura ou sessão Bluetooth do app.
5. Com o celular parado, volte ao medidor e toque em **Finalizar medição**.

## B — Maps com Tripper

1. Na tela de conexão, escolha **30 minutos — medir consumo** antes de procurar/reconectar o mostrador.
2. Ative **Usar Maps no Tripper**, inicie a rota e confirme a sessão ativa.
3. Abra **Medir bateria** e toque em **Iniciar: Maps com Tripper**.
4. Use por 20–25 minutos em condições semelhantes à sessão A. Não ative captura de diagnóstico nem conecte carregador.
5. Depois de parar, finalize a medição antes de encerrar a rota. Se o envio ao Tripper terminar antes, o medidor finalizará automaticamente nesse instante; menos de 20 minutos fica marcado como insuficiente.

## Conferir e exportar

Use **Exportar medições** e escolha um destino para `Tripper-bateria.json`. Não use amostras sinalizadas como curtas, interrompidas, carregando, com cenário alterado ou com aumento de carga. A tela mostra os últimos quatro registros; o export inclui até 20.

Compare duração, tempo de tela acesa, temperatura e carga inicial/final. Quando houver contador válido e queda detectável, a tela fornece a taxa em mAh/h do celular inteiro. A diferença entre B e A é uma diferença observada entre cenários, não uma medição isolada do consumo do app. Porcentagem tem resolução grosseira; nenhuma variação percentual não prova ausência de consumo. O app Royal não entrou na comparação, por decisão inicial do usuário.

Se o Android encerrar o processo, a próxima abertura marca a coleta como interrompida. Refazer a amostra nesse caso; o medidor não cria um serviço permanente só para manter a coleta viva.

## Falhas a conferir quando houver disponibilidade

- Desligar o Bluetooth durante uma sessão deve encerrar os envios, retirar a notificação e permitir nova conexão manual depois de religá-lo.
- Encerrar uma sessão deve liberar seus recursos e manter o motivo visível ao reabrir a tela.
- Reconexão nunca deve reutilizar instruções antigas; novas instruções dependem do Maps e de seu prazo de validade.
- Sessão não deve exceder o limite escolhido nem ser retomada automaticamente ao reiniciar o processo.

Essas verificações adicionais e a coleta de bateria não foram apresentadas como realizadas na entrega da etapa 6.
