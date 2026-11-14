# App do Anel R09 — Planejamento

> Documento de visão: o que queremos que o app seja e faça. Não é um roteiro de implementação.

## Contexto

O dispositivo é um smart ring **Colmi R09** (provavelmente white label), conectado por Bluetooth Low Energy. Hoje os dados dele chegam ao Android pelo **Gadgetbridge**, que envia tudo para o **Health Connect**. Assim algumas informações aparecem no **Samsung Health**.

O Gadgetbridge só é usado por causa desse anel. O app oficial do fabricante foi abandonado porque exige rodar em segundo plano o tempo todo e porque a detecção de gestos dele era ruim: encostar um dedo no outro ou bater na mesa já disparava ações.

## Objetivo geral

Criar um **app dedicado ao R09** que substitua tanto o app oficial quanto o Gadgetbridge. Ele junta duas coisas num lugar só:

1. **Visualização dos dados de saúde**, com uma interface própria e melhor.
2. **Controle por gestos confiável**, para agir no celular, em casa e em outras interfaces.

## 1. Dados de saúde

O app passa a ser o lugar comum para ver os dados do anel:

- Batimento cardíaco (contínuo e medição manual)
- Sono, com estágios (leve, profundo, REM, acordado)
- Passos, distância e calorias
- SpO2, HRV e estresse
- Temperatura da pele (recurso exclusivo do R09)
- Bateria do anel

Os dados continuam sendo **enviados ao Health Connect**, para seguirem aparecendo no Samsung Health e em qualquer outro app compatível. O app se conecta direto ao anel, sem nuvem do fabricante, e roda localmente.

## 2. Gestos

### O que queremos
Usar os sensores do anel, principalmente o acelerômetro, para reconhecer gestos com **baixa taxa de falsos positivos**. Esse é o principal problema que o app original não resolveu.

### Gesto de ativação
Assim como uma *wake word* na voz, existe um **gesto de ativação** que precisa ser feito antes de um comando. Ele deve ser algo que quase nunca acontece por acaso no dia a dia. Só depois dele o app passa a interpretar o próximo gesto como comando.

### Tipos de comando
- **Toques/batidas** no anel (ritmos, quantidade de toques)
- **Movimentos da mão**, como giros e inclinações, captados pelo acelerômetro

### O que os gestos controlam
- **Celular:** mídia (play, pause, pular faixa) e ações rápidas
- **Home Assistant:** cenas, luzes, rotinas e dispositivos
- **Interfaces:** navegar e agir em telas, no estilo da pulseira de controle da Meta com os óculos Ray-Ban Display, incluindo integração com o app leitor de notificações por voz

### Treinamento
A detecção é treinada com os dados do próprio usuário, em duas frentes:
- **Uso natural:** dados do dia a dia sem gestos intencionais (digitar, cozinhar, andar, bater na mesa sem querer), que ensinam o modelo o que **não** é gesto.
- **Sessões de gestos:** gravações rotuladas de cada gesto feito de propósito.

Divisão de papéis: Angelo usa o anel, coleta e rotula os dados, e toma as decisões. O Claude cuida do processamento de sinal e da modelagem.

## Pontos em aberto

- **Toque no anel vs. toque em outra superfície:** não sabemos se o R09 tem sensor de toque. Se tiver só acelerômetro, talvez seja impossível distinguir as duas coisas, e os gestos teriam que se apoiar em movimentos e ritmos.
- **Sensores disponíveis:** precisamos confirmar se há giroscópio, além do acelerômetro, e qual a taxa de amostragem real.
- **Bateria:** um streaming contínuo do acelerômetro pode drenar o anel rápido. Talvez seja preciso usar os eventos de gesto do próprio firmware como gatilho.
- **Base do app:** a escolha é entre fazer um fork do Gadgetbridge, que já tem conexão e sincronização prontas (licença AGPL), ou escrever um app do zero. Rodar os dois apps ao mesmo tempo no mesmo anel foi descartado, porque eles disputariam o mesmo canal de comunicação.
- **Segundo plano no One UI:** a Samsung encerra apps em segundo plano de forma agressiva, então o app precisa de um serviço em primeiro plano estável.
- **Robustez:** o anel gira no dedo, o que muda a orientação dos eixos. Além disso, atualizações de firmware podem alterar o protocolo.
