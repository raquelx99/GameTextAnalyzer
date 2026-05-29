# 🎮 GameText Analyzer
### Análise Comparativa de Algoritmos com Uso de Paralelismo em Textos Literários para Jogos Narrativos

---

## Resumo

O **GameText Analyzer** é uma ferramenta Java para comparar o desempenho de três abordagens de busca/contagem de palavras em arquivos `.txt`: **SerialCPU**, **ParallelCPU** e **ParallelGPU** com OpenCL/JOCL. A versão final do projeto utiliza obrigatoriamente as amostras fornecidas na atividade: **Don Quixote**, **Dracula** e **Moby Dick**.

Para manter a proposta relacionada a jogos digitais, cada obra foi interpretada como um **mundo narrativo** que poderia servir de base para uma adaptação em jogo: *Don Quixote* como um RPG de cavalaria, *Dracula* como um survival horror gótico e *Moby Dick* como uma aventura naval/boss hunt. A contagem de palavras representa uma análise simples de recorrência de elementos narrativos importantes, enquanto os métodos seriais e paralelos são comparados em desempenho.

---

## Introdução

Jogos narrativos frequentemente se inspiram em obras literárias para criar mundos, personagens, missões, atmosferas e conflitos. Antes de adaptar uma obra para um jogo, uma equipe poderia analisar termos recorrentes para entender quais elementos aparecem com mais força no texto.

Neste trabalho, essa análise textual é usada como contexto para o benchmark de paralelismo. O objetivo técnico continua sendo comparar diferentes formas de contar palavras em arquivos de texto, mas a interpretação dos dados foi gamificada.

### Métodos escolhidos

| Método | Descrição |
|---|---|
| `SerialCPU` | Percorre todas as palavras sequencialmente usando um loop simples. |
| `ParallelCPU-2t` | Divide o vetor de palavras entre 2 threads usando `ExecutorService`. |
| `ParallelCPU-4t` | Divide o vetor de palavras entre 4 threads usando `ExecutorService`. |
| `ParallelCPU-8t` | Divide o vetor de palavras entre 8 threads usando `ExecutorService`. |
| `ParallelGPU` | Tenta executar a comparação na GPU com OpenCL/JOCL. Se JOCL/OpenCL não estiver disponível, usa fallback em CPU identificado como `ParallelGPU-FallbackCPU`. |

---

## Metodologia

### Amostras obrigatórias

As amostras oficiais ficam em:

```text
data/samples/
```

| Arquivo | Obra | Interpretação gamificada | Palavra padrão |
|---|---|---|---|
| `DonQuixote-388208.txt` | Don Quixote | RPG de Cavalaria e Aventura | `quijote` |
| `Dracula-165307.txt` | Dracula | Survival Horror Gótico | `blood` |
| `MobyDick-217452.txt` | Moby Dick | Aventura Naval / Boss Hunt | `whale` |

Também foram adicionadas sugestões de palavras por obra:

```text
Don Quixote: quijote, sancho, caballero, aventura, castillo, dulcinea
Dracula: dracula, vampire, blood, night, fear, castle, helsing
Moby Dick: whale, sea, ship, captain, ahab, harpoon, moby
```

> Observação: a versão atual não usa logs sintéticos no fluxo principal. Todos os benchmarks oficiais são feitos sobre as amostras em `data/samples/`.

### Funcionalidades da interface

A interface gráfica foi organizada em uma experiência mais moderna e próxima de um painel de análise:

- **Dashboard**: visão geral do projeto, atalhos e indicadores rápidos;
- **Benchmark**: execução oficial com SerialCPU, ParallelCPU 2/4/8 threads e ParallelGPU;
- **Explorar Textos**: tabela de frequência, distribuição das palavras e visualização dos tokens;
- **Comparador Narrativo**: comparação de palavras entre as três obras, com densidade por 10 mil palavras;
- **Gráficos**: visualização dos gráficos gerados a partir do CSV;
- **Amostras Oficiais**: documentação visual dos textos obrigatórios e da interpretação gamificada.

O **Comparador Narrativo** é uma funcionalidade adicional para reforçar o lado de jogos: ele permite comparar termos como `blood`, `whale`, `quijote`, `castle`, `ship` e `night` entre as obras, mostrando em qual “mundo narrativo” cada termo é mais representativo.

### Tokenização dos textos

Como as amostras são textos literários completos, e não logs com uma palavra por linha, o projeto agora faz uma etapa de tokenização antes da contagem.

A leitura do arquivo transforma o texto em um vetor de palavras normalizadas:

- converte tudo para minúsculas;
- remove pontuação;
- remove acentos;
- preserva letras e números relevantes para a busca;
- ignora espaços e linhas vazias.

Assim, uma frase como:

```text
The vampire's blood was cold.
```

passa a ser analisada como palavras individuais:

```text
the vampire s blood was cold
```

### Medição do tempo

O tempo é medido usando:

```java
System.nanoTime()
```

O valor é convertido para milissegundos com casas decimais. Essa escolha é mais adequada para benchmark do que `System.currentTimeMillis()`, porque permite medir intervalos pequenos com mais precisão.

A leitura/tokenização do arquivo acontece antes da medição principal. Assim, os tempos registrados representam principalmente o custo da contagem das palavras em memória.

### Configurações testadas

- `SerialCPU` — 1 thread;
- `ParallelCPU-2t` — 2 threads;
- `ParallelCPU-4t` — 4 threads;
- `ParallelCPU-8t` — 8 threads;
- `ParallelGPU` — OpenCL/JOCL, com fallback caso a GPU não esteja disponível.

Cada configuração é executada **3 vezes** após um warmup descartado. A mediana dos tempos é usada nos gráficos para reduzir o impacto de outliers.

### Métricas coletadas

| Métrica | Fórmula / Descrição |
|---|---|
| Ocorrências | Quantidade de vezes que a palavra buscada aparece no texto. |
| Tempo de execução | Tempo da contagem em milissegundos. |
| Speedup | `Tempo Serial / Tempo Método` |
| Eficiência | `Speedup / Nº de Threads` para CPU paralela. |
| Throughput | `Total de palavras / Tempo (ms)` |

---

## Resultados e Discussão

Os resultados são salvos em:

```text
results/resultados.csv
```

O CSV possui as seguintes colunas:

```csv
file,world,total_words,word_searched,method,threads,run,occurrences,time_ms,speedup,efficiency,words_per_ms
```

### Gráficos gerados

| Gráfico | Arquivo |
|---|---|
| Tempo médio por método | `charts/chart_median_time.png` |
| Impacto do número de threads | `charts/chart_thread_impact.png` |
| Speedup em relação ao SerialCPU | `charts/chart_speedup.png` |
| Throughput em palavras por milissegundo | `charts/chart_throughput.png` |

### Discussões esperadas

- Em textos menores, o `SerialCPU` pode ser competitivo porque não possui overhead de criação de threads.
- Em textos maiores, o `ParallelCPU` pode apresentar ganhos por dividir o vetor de palavras entre múltiplas threads.
- Aumentar o número de threads nem sempre melhora o desempenho, pois pode haver overhead de criação, escalonamento e sincronização.
- A GPU pode ter overhead alto por causa da transferência de dados e preparação do kernel OpenCL.
- Caso apareça `ParallelGPU-FallbackCPU`, significa que o ambiente não executou GPU real; nesse caso, os resultados devem ser interpretados como fallback em CPU paralela.

---

## Observação importante sobre GPU

A classe `ParallelGPUCounter` tenta usar OpenCL por meio do JOCL. Para execução real em GPU, é necessário ter:

- `jocl-2.0.4.jar` no classpath;
- bibliotecas nativas do JOCL (`.dll`, `.so` ou equivalente);
- driver OpenCL instalado;
- dispositivo OpenCL disponível.

Se esses requisitos não forem encontrados, o programa utiliza automaticamente um fallback em CPU com `parallelStream`, e o método aparece no CSV como:

```text
ParallelGPU-FallbackCPU
```

Portanto, somente resultados marcados como `ParallelGPU` representam execução real em GPU.

---

## Como executar

### Pré-requisitos

- Java 17 ou superior;
- `javac` no PATH;
- arquivos das amostras dentro de `data/samples/`.

### Compilar e executar no modo console

```bash
chmod +x build.sh
./build.sh
```

O script compila o projeto e abre o menu de console.

### Executar interface gráfica

```bash
find src/main/java -name "*.java" | xargs javac --source 17 --target 17 -d out -cp "out:lib/*"
java -cp "out:lib/*" gamelog.Main
```

### Executar console manualmente

```bash
java -Djava.awt.headless=true -cp "out:lib/*" gamelog.Main --console
```

---

## Estrutura do projeto

```text
GameTextAnalyzer/
├── src/main/java/gamelog/
│   ├── Main.java                   # Ponto de entrada + menu console/GUI
│   ├── SampleConfig.java           # Configuração das amostras oficiais
│   ├── CsvReader.java              # Leitura do CSV para regenerar gráficos
│   ├── benchmark/
│   │   ├── BenchmarkRunner.java    # Orquestra runs, warmup e métricas
│   │   ├── BenchmarkResult.java    # Modelo de dados de um resultado
│   │   └── CsvWriter.java          # Escrita do CSV
│   ├── counters/
│   │   ├── WordCounter.java        # Interface comum
│   │   ├── SerialCPUCounter.java   # Loop sequencial
│   │   ├── ParallelCPUCounter.java # ExecutorService + Future
│   │   └── ParallelGPUCounter.java # OpenCL/JOCL com fallback
│   ├── ui/
│   │   └── ...                     # Interface gráfica e geração de gráficos
│   └── utils/
│       └── FileUtils.java          # Leitura e tokenização de arquivos
├── data/
│   └── samples/                    # Amostras obrigatórias da atividade
├── results/resultados.csv          # Resultados do benchmark
├── charts/                         # Gráficos PNG
├── lib/                            # Bibliotecas opcionais, como JOCL
└── build.sh                        # Script de compilação e execução no console
```

---

## Conclusão

O **GameText Analyzer** cumpre a proposta da atividade ao comparar algoritmos de contagem de palavras em ambientes serial, paralelo em CPU e GPU/OpenCL. A adaptação do projeto para usar as amostras obrigatórias fortalece a aderência ao enunciado, enquanto a camada de gamificação permanece na interpretação dos textos como mundos narrativos de possíveis jogos digitais.

A análise permite discutir não apenas qual método foi mais rápido, mas também quando o paralelismo compensa, como o número de threads afeta o desempenho e quais limitações aparecem no uso de GPU com OpenCL.

---

## Referências

- Java ExecutorService documentation: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ExecutorService.html
- Java System.nanoTime documentation: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/System.html#nanoTime()
- JOCL — Java bindings for OpenCL: https://github.com/gpu/JOCL
- OpenCL Specification — Khronos Group: https://www.khronos.org/opencl/
- Amdahl, G. M. Validity of the Single Processor Approach to Achieving Large Scale Computing Capabilities. AFIPS, 1967.

---

## Anexos

Os códigos-fonte completos estão disponíveis nas pastas `src/main/java/gamelog/`.

> **Link do projeto:** https://github.com/SEU_USUARIO/GameTextAnalyzer *(substituir pelo link real)*
