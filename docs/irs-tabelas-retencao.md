# Tabelas de retenção na fonte de IRS e Segurança Social — Portugal (2026)

> Fonte oficial para cada afirmação numérica citada abaixo, com URL exato e período de vigência declarado pela própria fonte. Data de consulta: 06/08/2026.
>
> **Isto não substitui aconselhamento de um Técnico Oficial de Contas (TOC).** Os valores legais mudam por despacho/retificação ao longo do ano (como se documenta abaixo, no caso da Madeira); antes de aplicar estes valores em produção, confirmar que continuam em vigor.

## Segurança Social

### Taxa contributiva do trabalhador por conta de outrem — regime geral

**11%** a cargo do trabalhador (quotização), sobre uma taxa contributiva global de **34,75%** (23,75% entidade empregadora + 11% trabalhador).

- **Fonte:** Lei n.º 110/2009, de 16 de setembro, que aprova o Código dos Regimes Contributivos do Sistema Previdencial de Segurança Social — Diário da República, 1.ª série, N.º 180, de 16/09/2009, Anexo, **Artigo 53.º ("Valor da taxa contributiva global")**.
- **URL:** https://files.dre.pt/1s/2009/09/18000/0649006528.pdf (texto original, publicado em Diário da República)
- **Texto:** *"A taxa contributiva global do regime geral correspondente ao elenco das eventualidades protegidas é de 34,75%, cabendo 23,75% à entidade empregadora e 11% ao trabalhador, sem prejuízo do disposto no artigo seguinte."*
- **Vigência:** a lei entrou em vigor em 01/01/2010 (Artigo 6.º, n.º 1). Não encontrei, no tempo disponível, confirmação direta na "legislação consolidada" do DRE de que os Artigos 44.º–53.º não foram alterados desde 2009 (a página `dre.pt/web/guest/legislacao-consolidada/...` carrega via JavaScript e não devolveu conteúdo estático via WebFetch). Fontes secundárias consultadas nesta mesma data (informador.pt, apotec.pt) confirmam os mesmos valores (34,75%/23,75%/11%) como atualmente em vigor, o que é consistente com o texto original — mas o utilizador deve confirmar com um TOC ou diretamente em seg-social.pt/Segurança Social Direta que não houve alteração legislativa posterior a estes artigos específicos.

### Base de incidência contributiva — o que conta como remuneração sujeita a descontos

- **Fonte:** mesmo diploma, **Artigo 44.º ("Base de incidência contributiva")** e **Artigo 46.º ("Delimitação da base de incidência contributiva")**.
- **Artigo 44.º, n.º 1:** *"Para a determinação do montante das contribuições das entidades empregadoras e das quotizações dos trabalhadores, considera-se base de incidência contributiva a remuneração ilíquida devida em função do exercício da actividade profissional (...)."*
- **Artigo 46.º, n.º 2** lista as prestações que integram a base de incidência, incluindo, entre outras:
  - alínea a) — a remuneração base, em dinheiro ou em espécie;
  - alínea e) — a remuneração pela prestação de trabalho suplementar;
  - alínea f) — a remuneração por trabalho noturno;
  - alínea g) — a remuneração correspondente ao período de férias;
  - alínea h) — subsídios de Natal, férias, Páscoa e análogos.
- **Artigo 48.º ("Valores excluídos da base de incidência")** lista exaustivamente o que fica de fora (compensações por não gozo de férias, indemnizações por despedimento ilícito, subsídios de refeição tomados em refeitório da entidade, etc.) — **não há qualquer exclusão para majorações de trabalho prestado em domingo ou feriado**.

**Conclusão para a app:** a remuneração majorada por trabalho em domingo (dobro) ou feriado (triplo) não está listada em nenhuma exclusão do Artigo 48.º, e enquadra-se na remuneração base/contrapartida do trabalho prestado (Artigo 46.º, n.º 1: "prestações pecuniárias (...) devidas (...) como contrapartida do seu trabalho"). Portanto, **a taxa de 11% aplica-se sobre a remuneração bruta total, incluindo as majorações de domingo e feriado** — não há uma base de incidência reduzida para essas majorações especificamente. (Nota: se a majoração for tecnicamente qualificada como "trabalho suplementar" nos termos do Código do Trabalho, cai na alínea e) do Artigo 46.º, que também integra a base de incidência — o resultado prático é o mesmo.)

### Regimes especiais — tempo parcial, turnos, remunerações variáveis

Verifiquei o Código completo (Lei n.º 110/2009, Título I, Capítulo II — "Regimes aplicáveis a trabalhadores integrados em categorias ou situações específicas"). **Não existe secção própria com taxa diferente para trabalho a tempo parcial nem para trabalho por turnos** — estes regimes aplicam-se ao regime geral (11%/23,75%) sem adaptação de taxa. As exceções documentadas no Código dizem respeito a categorias específicas de trabalhadores, não à modalidade de horário:
- Membros de órgãos estatutários (gerentes, administradores): 9,3% trabalhador (Artigo 69.º);
- Trabalhadores no domicílio: 9,3% (Artigo 73.º);
- Praticantes desportivos profissionais: 11% (Artigo 79.º) — igual ao regime geral;
- Trabalhadores de contrato de muito curta duração: 26,1% integralmente a cargo da entidade empregadora, 0% trabalhador (Artigo 83.º);
- Trabalhadores agrícolas e pesca local/costeira: 11% trabalhador (Artigos 96.º, 99.º) — igual ao regime geral;
- Trabalhadores do serviço doméstico: 9,4% ou 11%, consoante inclua desemprego (Artigo 121.º).

Nenhuma destas categorias corresponde a "trabalho a tempo parcial" ou "trabalho por turnos" genérico — para um trabalhador comum com contrato de trabalho dependente (full-time ou part-time, por turnos ou não), aplica-se sempre o regime geral do Artigo 53.º: **11% sobre a remuneração ilíquida total**.

---

## IRS — Retenção na fonte (Categoria A, trabalho dependente)

Modelo em vigor desde o 2.º semestre de 2023: taxas marginais progressivas — a retenção não é uma percentagem fixa por escalão, mas resulta da fórmula:

- **Com dependentes:** `[Remuneração mensal (R) × Taxa marginal máxima] − Parcela a abater − (Parcela adicional a abater por dependente × n.º dependentes)`
- **Sem dependentes:** `Remuneração mensal (R) × Taxa marginal máxima − Parcela a abater`

O montante nunca pode ser inferior a zero. Quando é paga remuneração de trabalho suplementar, aplica-se antes a taxa efetiva mensal de retenção (coluna "Taxa efetiva mensal no limite do escalão") correspondente a 50% da que resultaria para a remuneração mensal do mês em causa.

Correspondência entre os cenários pedidos e as tabelas oficiais (todas as regiões seguem a mesma estrutura I/II/III):
- **Não casado, sem dependentes** → Tabela I, com 0 dependentes (parcela adicional não se aplica).
- **Não casado, com dependentes** → Tabela II (parcela adicional a abater por dependente própria desta tabela).
- **Casado, único titular, sem/com dependentes** → Tabela III, com 0 ou n dependentes.
- **Casado, dois titulares, sem/com dependentes** → Tabela I (o despacho agrupa "não casado sem dependentes" e "casado dois titulares" na mesma Tabela I; a coluna "parcela adicional a abater por dependente" da Tabela I aplica-se quando há dependentes).

Isto está expresso literalmente no ponto 1, alínea a), de cada despacho: *"Tabelas de retenção n.os i (não casado sem dependentes ou casado dois titulares), ii (não casado com um ou mais dependentes) e iii (casado único titular)"*.

### Continente

- **Fonte:** Despacho n.º 233-A/2026, de 5 de janeiro de 2026 (Secretária de Estado dos Assuntos Fiscais), publicado em **Diário da República, 2.ª série, Suplemento, N.º 3, de 06/01/2026**.
- **URL:** https://files.diariodarepublica.pt/2s/2026/01/003000001/0000200010.pdf
- **Vigência:** "aplicam-se aos rendimentos de trabalho dependente e de pensões pagos ou colocados à disposição a partir de 1 de janeiro de 2026" (n.º 12). Revoga o Despacho n.º 8464-A/2025, de 22 de julho.

#### Tabela I — Não casado sem dependentes ou casado dois titulares

| Remuneração mensal (€) | Taxa marginal máxima | Parcela a abater (€) | Parcela adicional/dependente (€) | Taxa efetiva no limite |
|---|---|---|---|---|
| Até 920,00 | 0,00% | 0,00 | 0,00 | 0,0% |
| Até 1 042,00 | 12,50% | 12,50% × 2,60 × (1 273,85 − R) | 21,43 | 5,3% |
| Até 1 108,00 | 15,70% | 15,70% × 1,35 × (1 554,83 − R) | 21,43 | 7,2% |
| Até 1 154,00 | 15,70% | 94,71 | 21,43 | 7,5% |
| Até 1 212,00 | 21,20% | 158,18 | 21,43 | 8,1% |
| Até 1 819,00 | 24,10% | 193,33 | 21,43 | 13,5% |
| Até 2 119,00 | 31,10% | 320,66 | 21,43 | 16,0% |
| Até 2 499,00 | 34,90% | 401,19 | 21,43 | 18,8% |
| Até 3 305,00 | 38,36% | 487,66 | 21,43 | 23,6% |
| Até 5 547,00 | 39,69% | 531,62 | 21,43 | 30,1% |
| Até 20 221,00 | 44,95% | 823,40 | 21,43 | 40,9% |
| Superior a 20 221,00 | 47,17% | 1 272,31 | 21,43 | n.a. |

#### Tabela II — Não casado com um ou mais dependentes

| Remuneração mensal (€) | Taxa marginal máxima | Parcela a abater (€) | Parcela adicional/dependente (€) | Taxa efetiva no limite |
|---|---|---|---|---|
| Até 920,00 | 0,00% | 0,00 | 0,00 | 0,0% |
| Até 1 042,00 | 12,50% | 12,50% × 2,60 × (1 273,85 − R) | 34,29 | 5,3% |
| Até 1 108,00 | 15,70% | 15,70% × 1,35 × (1 554,83 − R) | 34,29 | 7,2% |
| Até 1 154,00 | 15,70% | 94,71 | 34,29 | 7,5% |
| Até 1 212,00 | 21,20% | 158,18 | 34,29 | 8,1% |
| Até 1 819,00 | 24,10% | 193,33 | 34,29 | 13,5% |
| Até 2 119,00 | 31,10% | 320,66 | 34,29 | 16,0% |
| Até 2 499,00 | 34,90% | 401,19 | 34,29 | 18,8% |
| Até 3 305,00 | 38,36% | 487,66 | 34,29 | 23,6% |
| Até 5 547,00 | 39,69% | 531,62 | 34,29 | 30,1% |
| Até 20 221,00 | 44,95% | 823,40 | 34,29 | 40,9% |
| Superior a 20 221,00 | 47,17% | 1 272,31 | 34,29 | n.a. |

#### Tabela III — Casado, único titular

| Remuneração mensal (€) | Taxa marginal máxima | Parcela a abater (€) | Parcela adicional/dependente (€) | Taxa efetiva no limite |
|---|---|---|---|---|
| Até 991,00 | 0,00% | 0,00 | 0,00 | 0,0% |
| Até 1 042,00 | 12,50% | 12,50% × 2,6 × (1 372,15 − R) | 42,86 | 2,2% |
| Até 1 108,00 | 12,50% | 12,50% × 1,35 × (1 677,85 − R) | 42,86 | 3,8% |
| Até 1 119,00 | 12,50% | 96,17 | 42,86 | 3,9% |
| Até 1 432,00 | 12,72% | 98,64 | 42,86 | 5,8% |
| Até 1 962,00 | 15,70% | 141,32 | 42,86 | 8,5% |
| Até 2 240,00 | 19,38% | 213,53 | 42,86 | 9,8% |
| Até 2 773,00 | 22,77% | 289,47 | 42,86 | 12,3% |
| Até 3 389,00 | 25,70% | 370,72 | 42,86 | 14,8% |
| Até 5 965,00 | 28,81% | 476,12 | 42,86 | 20,8% |
| Até 20 265,00 | 38,43% | 1 049,96 | 42,86 | 33,2% |
| Superior a 20 265,00 | 47,17% | 2 821,13 | 42,86 | n.a. |

### Açores

- **Fonte:** Despacho n.º 1179/2026, de 27 de janeiro de 2026 (Secretária de Estado dos Assuntos Fiscais, ouvido o Governo Regional dos Açores), publicado em **Diário da República, 2.ª série, N.º 23, de 03/02/2026**.
- **URL:** https://files.diariodarepublica.pt/2s/2026/02/023000000/0005100057.pdf
- **Vigência:** "aplicam-se aos rendimentos de trabalho dependente e de pensões pagos ou colocados à disposição a partir de 1 de janeiro de 2026" (n.º 12). Revoga o Despacho n.º 8464-A/2025, de 22 de julho.

#### Tabela I — Não casado sem dependentes ou casado dois titulares

| Remuneração mensal (€) | Taxa marginal máxima | Parcela a abater (€) | Parcela adicional/dependente (€) | Taxa efetiva no limite |
|---|---|---|---|---|
| Até 966,00 | 0,00% | 0,00 | 0,00 | 0,0% |
| Até 1 042,00 | 8,75% | 8,75% × 2,60 × (1 337,54 − R) | 21,43 | 2,3% |
| Até 1 108,00 | 10,99% | 10,99% × 1,35 × (1 652,49 − R) | 21,43 | 3,7% |
| Até 1 154,00 | 10,99% | 80,79 | 21,43 | 4,0% |
| Até 1 212,00 | 14,84% | 125,22 | 21,43 | 4,5% |
| Até 1 819,00 | 16,87% | 149,83 | 21,43 | 8,6% |
| Até 2 119,00 | 21,77% | 238,97 | 21,43 | 10,5% |
| Até 2 499,00 | 24,43% | 295,34 | 21,43 | 12,6% |
| Até 3 305,00 | 26,85% | 355,82 | 21,43 | 16,1% |
| Até 5 547,00 | 27,79% | 386,89 | 21,43 | 20,8% |
| Até 20 221,00 | 31,46% | 590,47 | 21,43 | 28,5% |
| Superior a 20 221,00 | 33,02% | 905,92 | 21,43 | n.a. |

#### Tabela II — Não casado com um ou mais dependentes

Escalões e taxas idênticos à Tabela I dos Açores, com **parcela adicional a abater por dependente = 34,29 €** em todas as linhas (em vez de 21,43 €).

#### Tabela III — Casado, único titular

| Remuneração mensal (€) | Taxa marginal máxima | Parcela a abater (€) | Parcela adicional/dependente (€) | Taxa efetiva no limite |
|---|---|---|---|---|
| Até 1 226,00 | 0,00% | 0,00 | 0,00 | 0,0% |
| Até 1 267,00 | 7,28% | 89,26 | 42,86 | 0,2% |
| Até 1 602,00 | 9,64% | 119,17 | 42,86 | 2,2% |
| Até 1 962,00 | 10,99% | 140,80 | 42,86 | 3,8% |
| Até 2 240,00 | 13,57% | 191,42 | 42,86 | 5,0% |
| Até 2 900,00 | 15,94% | 244,51 | 42,86 | 7,5% |
| Até 3 389,00 | 17,99% | 303,96 | 42,86 | 9,0% |
| Até 5 965,00 | 20,17% | 377,85 | 42,86 | 13,8% |
| Até 20 265,00 | 27,10% | 791,23 | 42,86 | 23,2% |
| Superior a 20 265,00 | 33,02% | 1 990,92 | 42,86 | n.a. |

### Madeira

**Atenção — histórico de retificações:** o despacho original (Despacho n.º 19/2026, de 20/01/2026, JORAM II Série N.º 13, 4.º Suplemento) foi publicado com inexatidão e **integralmente republicado e retificado** pela **Declaração de Retificação n.º 10/2026, de 23/01/2026** (JORAM II Série N.º 16, 3.º Suplemento). Os valores abaixo são os da **versão retificada** ("deve ler-se"), que é a versão em vigor. Verifiquei também um Jornal Oficial da Madeira de 13/03/2026 (N.º 47) que continha, segundo um resumo automático inicial, uma alegada "Declaração de Retificação n.º 11/2026" das tabelas de IRS — ao ler o PDF na íntegra, **essa informação estava errada**: esse número do Jornal Oficial não contém qualquer retificação às tabelas de IRS (trata de nomeações de dirigentes). Não encontrei, portanto, qualquer retificação posterior à de 23/01/2026 no período pesquisado.

- **Fonte (retificação em vigor):** Declaração de Retificação n.º 10/2026, de 23 de janeiro de 2026 (Secretaria Regional das Finanças), publicada em **Jornal Oficial da Região Autónoma da Madeira, II Série, N.º 16, 3.º Suplemento, de 23/01/2026**.
- **URL:** https://joram.madeira.gov.pt/joram/2serie/Ano%20de%202026/IISerie-016-2026-01-23Supl3.pdf
- **Fonte (despacho de base):** Despacho n.º 19/2026, de 20 de janeiro de 2026 — https://joram.madeira.gov.pt/joram/2serie/Ano%20de%202026/IISerie-013-2026-01-20Supl4.pdf
- **Vigência:** "para vigorarem a partir de 1 de janeiro de 2026". Revoga o Despacho n.º 633/2025, de 1 de setembro.

#### Tabela I — Não casado sem dependentes ou casado 2 titulares (versão retificada)

| Remuneração mensal (€) | Taxa marginal máxima | Parcela a abater (€) | Parcela adicional/dependente (€) | Taxa efetiva no limite |
|---|---|---|---|---|
| Até 980,00 | 0,00% | 0,00 | 0,00 | 0,0% |
| Até 1 028,00 | 8,72% | 8,72% × 2,60 × (1 356,92 − R) | 21,43 | 1,5% |
| Até 1 099,00 | 12,04% | 12,04% × 1,35 × (1 696,78 − R) | 21,43 | 3,2% |
| Até 1 201,00 | 12,04% | 97,17 | 21,43 | 3,9% |
| Até 1 623,00 | 17,63% | 164,31 | 21,43 | 7,5% |
| Até 2 332,00 | 22,30% | 240,11 | 21,43 | 12,0% |
| Até 3 203,00 | 22,42% | 242,91 | 21,43 | 14,8% |
| Até 3 614,00 | **27,27%** | **398,26** | 21,43 | 16,3% |
| Até 6 585,00 | **27,78%** | **416,70** | 21,43 | 21,5% |
| Até 6 954,00 | 28,02% | **432,51** | 21,43 | 21,8% |
| Até 21 411,00 | 29,24% | **517,35** | 21,43 | 26,8% |
| Superior a 21 411,00 | 32,78% | **1 275,30** | 21,43 | n.a. |

(Valores a negrito são os que a Declaração de Retificação n.º 10/2026 corrigiu face ao Despacho n.º 19/2026 original.)

#### Tabela II — Não casado com um ou mais dependentes (versão retificada)

Escalões e taxas idênticos à Tabela I da Madeira, com **parcela adicional a abater por dependente = 34,29 €** em todas as linhas.

#### Tabela III — Casado, único titular

| Remuneração mensal (€) | Taxa marginal máxima | Parcela a abater (€) | Parcela adicional/dependente (€) | Taxa efetiva no limite |
|---|---|---|---|---|
| Até 997,00 | 0,00% | 0,00 | 0,00 | 0,0% |
| Até 1 099,00 | 8,72% | 8,72% × 1,35 × (1 819,64 − R) | 42,86 | 1,0% |
| Até 1 141,00 | 8,72% | 84,84 | 42,86 | 1,3% |
| Até 1 857,00 | 10,33% | 103,22 | 42,86 | 4,8% |
| Até 2 485,00 | 10,91% | 114,00 | 42,86 | 6,3% |
| Até 3 331,00 | 12,36% | 150,04 | 42,86 | 7,9% |
| Até 3 895,00 | 14,04% | 206,01 | 42,86 | 8,8% |
| Até 6 673,00 | 15,95% | 280,41 | 42,86 | 11,7% |
| Até 6 878,00 | 22,13% | 692,81 | 42,86 | 12,1% |
| Até 21 411,00 | 24,93% | 885,40 | 42,86 | 20,8% |
| Superior a 21 411,00 | 32,78% | 2 566,17 | 42,86 | n.a. |

(A Tabela III não foi alterada pela Declaração de Retificação n.º 10/2026 — os valores são os do Despacho n.º 19/2026 original, confirmados como corretos pela retificação, que só listou correções às Tabelas I, II e IX.)

---

## Limitações e incerteza

1. **Segurança Social — confirmação de vigência atual do Artigo 53.º.** Li o texto original da Lei n.º 110/2009 (Diário da República, 1.ª série). Não consegui aceder à versão de "legislação consolidada" do DRE (a página é renderizada em JavaScript e o WebFetch devolveu conteúdo vazio), pelo que não confirmei diretamente nessa fonte se os Artigos 44.º a 53.º sofreram alguma alteração desde 2009. Fontes secundárias consultadas nesta mesma sessão (2026) indicam que os valores 34,75%/23,75%/11% continuam corretos, mas isto não substitui a confirmação em seg-social.pt (Segurança Social Direta) ou na legislação consolidada do DRE — recomendo essa verificação adicional antes de publicar a funcionalidade.
2. **Trabalho suplementar vs. majoração de domingo/feriado, para efeitos de IRS (não Segurança Social).** Os despachos de retenção na fonte (Continente, Açores, Madeira) referem uma regra especial no n.º 5, alínea f): quando é paga remuneração de "trabalho suplementar", aplica-se antes uma taxa efetiva de 50% da que resultaria da tabela normal. Não investiguei se as majorações de domingo/feriado da app (que já não são cálculo de "trabalho suplementar" no sentido do Código do Trabalho, mas sim majoração do valor/hora normal) se enquadram nesta regra especial de IRS — isto ficou fora do âmbito desta investigação (focada em SS) e deve ser esclarecido separadamente antes de implementar a lógica de retenção de IRS.
3. **Regiões Autónomas — só Continente, Açores e Madeira foram cobertos**, tal como pedido; não há mais regiões a cobrir.
4. **Tabelas para pessoas com deficiência (Tabelas IV a XI) e tabelas de pensões (VIII a XI)** foram lidas nos PDFs originais mas não são reproduzidas aqui, por estarem fora do âmbito pedido (trabalho dependente, sem deficiência, não pensionista).
5. **Possibilidade de retificações supervenientes.** Como demonstrado pelo caso da Madeira (duas publicações em 15 dias), estes despachos podem ser corrigidos após publicação. Antes de "endurecer" estes valores no código da app, vale a pena voltar a verificar info.portaldasfinancas.gov.pt/pt/atualidades ou o site do JORAM para confirmar que não houve nova retificação entre 06/08/2026 (data desta pesquisa) e a data de implementação.
