package com.example.domain

/**
 * Um escalão de retenção na fonte de IRS.
 *
 * `parcelaAAbater` é uma função de R (remuneração mensal) porque nos dois
 * primeiros escalões de cada tabela o valor a abater não é fixo — resulta de
 * uma fórmula linear em R, tal como publicado nos despachos oficiais.
 */
data class Escalao(
    val limiteSuperior: Double,
    val taxaMarginal: Double,
    val parcelaAAbater: (r: Double) -> Double,
    val parcelaPorDependente: Double
)

/**
 * Tabelas oficiais de retenção na fonte de IRS (Categoria A, trabalho
 * dependente), em vigor desde 01/01/2026. Fonte e vigência de cada tabela em
 * docs/irs-tabelas-retencao.md — não alterar estes valores sem atualizar essa
 * fonte também.
 */
object IrsWithholdingTables {

    private fun fixo(valor: Double): (Double) -> Double = { valor }

    private fun tabelaIIDe(tabelaI: List<Escalao>, parcelaPorDependente: Double): List<Escalao> =
        tabelaI.mapIndexed { index, escalao ->
            if (index == 0) escalao else escalao.copy(parcelaPorDependente = parcelaPorDependente)
        }

    // ---- Continente — Despacho n.º 233-A/2026 (DR 2.ª série, Suplemento, N.º 3, de 06/01/2026) ----

    private val continenteTabelaI = listOf(
        Escalao(920.00, 0.0000, fixo(0.00), 0.00),
        Escalao(1042.00, 0.1250, { r -> 0.1250 * 2.60 * (1273.85 - r) }, 21.43),
        Escalao(1108.00, 0.1570, { r -> 0.1570 * 1.35 * (1554.83 - r) }, 21.43),
        Escalao(1154.00, 0.1570, fixo(94.71), 21.43),
        Escalao(1212.00, 0.2120, fixo(158.18), 21.43),
        Escalao(1819.00, 0.2410, fixo(193.33), 21.43),
        Escalao(2119.00, 0.3110, fixo(320.66), 21.43),
        Escalao(2499.00, 0.3490, fixo(401.19), 21.43),
        Escalao(3305.00, 0.3836, fixo(487.66), 21.43),
        Escalao(5547.00, 0.3969, fixo(531.62), 21.43),
        Escalao(20221.00, 0.4495, fixo(823.40), 21.43),
        Escalao(Double.MAX_VALUE, 0.4717, fixo(1272.31), 21.43)
    )

    private val continenteTabelaII = tabelaIIDe(continenteTabelaI, 34.29)

    private val continenteTabelaIII = listOf(
        Escalao(991.00, 0.0000, fixo(0.00), 0.00),
        Escalao(1042.00, 0.1250, { r -> 0.1250 * 2.6 * (1372.15 - r) }, 42.86),
        Escalao(1108.00, 0.1250, { r -> 0.1250 * 1.35 * (1677.85 - r) }, 42.86),
        Escalao(1119.00, 0.1250, fixo(96.17), 42.86),
        Escalao(1432.00, 0.1272, fixo(98.64), 42.86),
        Escalao(1962.00, 0.1570, fixo(141.32), 42.86),
        Escalao(2240.00, 0.1938, fixo(213.53), 42.86),
        Escalao(2773.00, 0.2277, fixo(289.47), 42.86),
        Escalao(3389.00, 0.2570, fixo(370.72), 42.86),
        Escalao(5965.00, 0.2881, fixo(476.12), 42.86),
        Escalao(20265.00, 0.3843, fixo(1049.96), 42.86),
        Escalao(Double.MAX_VALUE, 0.4717, fixo(2821.13), 42.86)
    )

    // ---- Açores — Despacho n.º 1179/2026 (DR 2.ª série, N.º 23, de 03/02/2026) ----

    private val acoresTabelaI = listOf(
        Escalao(966.00, 0.0000, fixo(0.00), 0.00),
        Escalao(1042.00, 0.0875, { r -> 0.0875 * 2.60 * (1337.54 - r) }, 21.43),
        Escalao(1108.00, 0.1099, { r -> 0.1099 * 1.35 * (1652.49 - r) }, 21.43),
        Escalao(1154.00, 0.1099, fixo(80.79), 21.43),
        Escalao(1212.00, 0.1484, fixo(125.22), 21.43),
        Escalao(1819.00, 0.1687, fixo(149.83), 21.43),
        Escalao(2119.00, 0.2177, fixo(238.97), 21.43),
        Escalao(2499.00, 0.2443, fixo(295.34), 21.43),
        Escalao(3305.00, 0.2685, fixo(355.82), 21.43),
        Escalao(5547.00, 0.2779, fixo(386.89), 21.43),
        Escalao(20221.00, 0.3146, fixo(590.47), 21.43),
        Escalao(Double.MAX_VALUE, 0.3302, fixo(905.92), 21.43)
    )

    private val acoresTabelaII = tabelaIIDe(acoresTabelaI, 34.29)

    private val acoresTabelaIII = listOf(
        Escalao(1226.00, 0.0000, fixo(0.00), 0.00),
        Escalao(1267.00, 0.0728, fixo(89.26), 42.86),
        Escalao(1602.00, 0.0964, fixo(119.17), 42.86),
        Escalao(1962.00, 0.1099, fixo(140.80), 42.86),
        Escalao(2240.00, 0.1357, fixo(191.42), 42.86),
        Escalao(2900.00, 0.1594, fixo(244.51), 42.86),
        Escalao(3389.00, 0.1799, fixo(303.96), 42.86),
        Escalao(5965.00, 0.2017, fixo(377.85), 42.86),
        Escalao(20265.00, 0.2710, fixo(791.23), 42.86),
        Escalao(Double.MAX_VALUE, 0.3302, fixo(1990.92), 42.86)
    )

    // ---- Madeira — Declaração de Retificação n.º 10/2026 (JORAM II Série, N.º 16, 3.º Supl., 23/01/2026) ----

    private val madeiraTabelaI = listOf(
        Escalao(980.00, 0.0000, fixo(0.00), 0.00),
        Escalao(1028.00, 0.0872, { r -> 0.0872 * 2.60 * (1356.92 - r) }, 21.43),
        Escalao(1099.00, 0.1204, { r -> 0.1204 * 1.35 * (1696.78 - r) }, 21.43),
        Escalao(1201.00, 0.1204, fixo(97.17), 21.43),
        Escalao(1623.00, 0.1763, fixo(164.31), 21.43),
        Escalao(2332.00, 0.2230, fixo(240.11), 21.43),
        Escalao(3203.00, 0.2242, fixo(242.91), 21.43),
        Escalao(3614.00, 0.2727, fixo(398.26), 21.43),
        Escalao(6585.00, 0.2778, fixo(416.70), 21.43),
        Escalao(6954.00, 0.2802, fixo(432.51), 21.43),
        Escalao(21411.00, 0.2924, fixo(517.35), 21.43),
        Escalao(Double.MAX_VALUE, 0.3278, fixo(1275.30), 21.43)
    )

    private val madeiraTabelaII = tabelaIIDe(madeiraTabelaI, 34.29)

    private val madeiraTabelaIII = listOf(
        Escalao(997.00, 0.0000, fixo(0.00), 0.00),
        Escalao(1099.00, 0.0872, { r -> 0.0872 * 1.35 * (1819.64 - r) }, 42.86),
        Escalao(1141.00, 0.0872, fixo(84.84), 42.86),
        Escalao(1857.00, 0.1033, fixo(103.22), 42.86),
        Escalao(2485.00, 0.1091, fixo(114.00), 42.86),
        Escalao(3331.00, 0.1236, fixo(150.04), 42.86),
        Escalao(3895.00, 0.1404, fixo(206.01), 42.86),
        Escalao(6673.00, 0.1595, fixo(280.41), 42.86),
        Escalao(6878.00, 0.2213, fixo(692.81), 42.86),
        Escalao(21411.00, 0.2493, fixo(885.40), 42.86),
        Escalao(Double.MAX_VALUE, 0.3278, fixo(2566.17), 42.86)
    )

    /** @param numeroTabela 1 = Tabela I, 2 = Tabela II, 3 = Tabela III */
    fun paraRegiaoETabela(regiao: TaxRegion, numeroTabela: Int): List<Escalao> {
        return when (regiao) {
            TaxRegion.CONTINENTE -> when (numeroTabela) {
                1 -> continenteTabelaI
                2 -> continenteTabelaII
                else -> continenteTabelaIII
            }
            TaxRegion.ACORES -> when (numeroTabela) {
                1 -> acoresTabelaI
                2 -> acoresTabelaII
                else -> acoresTabelaIII
            }
            TaxRegion.MADEIRA -> when (numeroTabela) {
                1 -> madeiraTabelaI
                2 -> madeiraTabelaII
                else -> madeiraTabelaIII
            }
        }
    }
}
