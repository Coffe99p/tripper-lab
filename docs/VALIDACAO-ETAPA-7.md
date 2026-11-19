# Etapa 7 — versão pessoal

Entrega de software em **19/11/2026**, versão **0.6.0** (`versionCode=9`). Escopo autorizado pelo usuário: finalizar interface, instalador e instruções. Atualização instalada no Pixel 8 Pro / Android 17, preservando os dados existentes. A entrega desta etapa não encerra as pendências físicas aceitas nas etapas 5 e 6.

## O que mudou

- O ícone abre diretamente a tela de conexão. Estado do Tripper e estado `Maps:` ficam juntos; **Encerrar sessão** fica logo abaixo.
- Acesso às notificações, duração, reconexão, busca, ativação do Maps e abertura do Google Maps estão na tela principal.
- **Mostrar ferramentas e diagnóstico** reúne captura do Maps, medição de bateria, novo PIN, registros e dados do aparelho. A seção começa recolhida e preserva sua expansão durante a recriação da tela.
- **Como usar e resolver problemas** fornece ajuda local para primeiro pareamento, uso seguinte, espera, desconexão, ferramentas e limitações.
- Simulações do dispositivo já salvo ficam recolhidas por padrão. Um novo pareamento continua mostrando os controles para a confirmação física inicial; a regra de liberação do Maps não foi removida. Simulações ficam indisponíveis durante o envio do Maps.
- Aviso de captura ativa na tela principal; callbacks de atualização acompanham a tela visível. O retorno do diagnóstico e da notificação reaproveita a tela de conexão.
- Botões de ação indisponíveis usam cor distinta. O motivo preservado de uma sessão anterior recebe o prefixo **Última sessão**.

Não foram alterados o protocolo, o parser das manobras, o limite de 10/30 minutos, a validade de 30 segundos das instruções nem a política de reconexão manual. Não há novas permissões.

## Verificações realizadas

- Compilação, **50 testes unitários** e lint aprovados; relatório final de lint sem ocorrências. Mesmas suítes: parser 13, estado do Maps 12, bateria 11, protocolo 7, captura 4 e prazo 3. Não foram adicionados testes que apenas repetem a estrutura dos novos textos/telas.
- Assinatura do APK verificada; atualização por cima da instalação anterior concluída com sucesso. Versão instalada confirmada: `versionName=0.6.0`, `versionCode=9`.
- Resolução do launcher e abertura pelo evento de launcher verificadas: `MainActivity`. A chamada genérica `am start` sem componente não resolveu a intenção neste aparelho; a resolução pelo gerenciador de pacotes e a abertura pelo launcher funcionaram.
- Tela principal conferida por captura visual e hierarquia do Android: acesso às notificações preservado, botão de reconexão ao salvo habilitado, Maps e encerramento desabilitados quando não há sessão, botões indisponíveis visualmente distintos.
- Guia aberto e inspecionado visualmente; rolagem, expansão de ferramentas, abertura de bateria e diagnóstico, retorno à conexão e reabertura pelo launcher conferidos no Pixel.
- Tela de bateria mostrou nenhuma medição ativa e nenhum resultado. Diagnóstico mostrou captura parada; registro histórico permaneceu disponível.
- Nenhum erro `AndroidRuntime` ou aviso `TripperLab` retornou na leitura restrita ao processo do app durante a conferência final. Não foram iniciadas conexão Bluetooth, rota, captura ou medição nesta etapa.

As conferências de ajuda e da primeira expansão de ferramentas ocorreram antes do último ajuste de cores e do prefixo da sessão anterior; esses dois ajustes foram recompilados, testados, reinstalados e conferidos na tela principal final. Atalhos de bateria, diagnóstico e launcher foram conferidos no instalador final.

Evidências locais em `artifacts/tripper-stage7-*`: telas/hierarquias de início, ajuda, ferramentas, bateria, diagnóstico e retorno. Esses arquivos de diagnóstico não integram o código distribuído e podem conter dados locais.

## Instalador e instruções

- APK pessoal: `artifacts/Tripper-Lab-0.6.0.apk`.
- SHA-256: `BE4ACCB865A7551309D896535DE7FE0B551740D24A3613E3217186A054449817`.
- Assinado pela chave de desenvolvimento local compatível com as versões anteriores; não é uma versão publicada na Play Store. Atualizar sem desinstalar. Preservar a chave local para futuras atualizações.
- [Guia de uso e atualização](GUIA-DE-USO.md), também resumido na ajuda do próprio app.

## Limites da entrega

Não foram repetidos o pareamento com novo PIN, a confirmação de simulação nem as manobras na moto nesta etapa. A apresentação condicional desses controles foi revisada no código, sem equivaler a uma nova validação física.

Continuam pendentes os testes em trajeto e da integração com tela bloqueada, o encerramento visual e alguns símbolos, além dos testes físicos adicionais e da comparação real de bateria descritos nas [etapas 5](VALIDACAO-ETAPA-5.md) e [6](VALIDACAO-ETAPA-6.md). Não há resultado de consumo/economia. Fonte ampliada, rotação e instalação manual pelo gerenciador de arquivos não foram exercitadas nesta rodada.

**Etapa 7 concluída no escopo de interface, instalador e instruções.** O planejamento de implementação termina aqui. Retomar as validações adiadas quando o usuário quiser, sem registrá-las como aprovadas antecipadamente.
