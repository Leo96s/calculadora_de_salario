package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Valida [IrsWithholdingCalculator] contra a coluna "Taxa efetiva no limite do
 * escalão" documentada em docs/irs-tabelas-retencao.md, no valor-limite (R) de
 * cada escalão testado — é a validação central contra erros de transcrição das
 * tabelas oficiais em [IrsWithholdingTables]. Tolerância de 0.2 pontos
 * percentuais por a fonte publicar a taxa efetiva arredondada a 1 casa decimal.
 *
 * A taxa efetiva documentada não inclui a dedução por dependente (é igual nas
 * Tabelas I e II, que só diferem na parcela/dependente) — por isso estes testes
 * usam sempre dependentes = 0, exceto quando explicitamente a testar essa
 * dedução.
 */
class IrsWithholdingCalculatorTest {

    private fun assertTaxaEfetiva(
        r: Double,
        taxaEfetivaEsperada: Double,
        estadoCivil: MaritalStatus,
        regiao: TaxRegion
    ) {
        val retencao = IrsWithholdingCalculator.calcular(r, estadoCivil, 0, regiao)
        val taxaEfetivaObtida = retencao / r
        assertEquals(taxaEfetivaEsperada, taxaEfetivaObtida, 0.002)
    }

    // ---- Continente ----

    @Test
    fun `continente tabela I`() {
        assertTaxaEfetiva(1042.0, 0.053, MaritalStatus.NAO_CASADO, TaxRegion.CONTINENTE)
        assertTaxaEfetiva(1108.0, 0.072, MaritalStatus.NAO_CASADO, TaxRegion.CONTINENTE)
        assertTaxaEfetiva(1212.0, 0.081, MaritalStatus.NAO_CASADO, TaxRegion.CONTINENTE)
        assertTaxaEfetiva(2499.0, 0.188, MaritalStatus.NAO_CASADO, TaxRegion.CONTINENTE)
    }

    @Test
    fun `continente tabela III (casado unico titular)`() {
        assertTaxaEfetiva(1962.0, 0.085, MaritalStatus.CASADO_UNICO_TITULAR, TaxRegion.CONTINENTE)
        assertTaxaEfetiva(5965.0, 0.208, MaritalStatus.CASADO_UNICO_TITULAR, TaxRegion.CONTINENTE)
    }

    @Test
    fun `casado dois titulares usa a mesma tabela que nao casado sem dependentes`() {
        val naoCasado = IrsWithholdingCalculator.calcular(2499.0, MaritalStatus.NAO_CASADO, 0, TaxRegion.CONTINENTE)
        val casadoDoisTitulares = IrsWithholdingCalculator.calcular(2499.0, MaritalStatus.CASADO_DOIS_TITULARES, 0, TaxRegion.CONTINENTE)
        assertEquals(naoCasado, casadoDoisTitulares, 0.001)
    }

    @Test
    fun `nao casado com dependentes usa tabela II e deduz por dependente`() {
        val semDependentes = IrsWithholdingCalculator.calcular(2499.0, MaritalStatus.NAO_CASADO, 0, TaxRegion.CONTINENTE)
        val comUmDependente = IrsWithholdingCalculator.calcular(2499.0, MaritalStatus.NAO_CASADO, 1, TaxRegion.CONTINENTE)
        // Tabela II tem os mesmos escalões/taxas da Tabela I, só muda a parcela/dependente (34.29 no Continente).
        assertEquals(semDependentes - 34.29, comUmDependente, 0.001)
    }

    // ---- Açores ----

    @Test
    fun `acores tabela I`() {
        assertTaxaEfetiva(1042.0, 0.023, MaritalStatus.NAO_CASADO, TaxRegion.ACORES)
        assertTaxaEfetiva(2119.0, 0.105, MaritalStatus.NAO_CASADO, TaxRegion.ACORES)
    }

    @Test
    fun `acores tabela III`() {
        assertTaxaEfetiva(1267.0, 0.002, MaritalStatus.CASADO_UNICO_TITULAR, TaxRegion.ACORES)
        assertTaxaEfetiva(5965.0, 0.138, MaritalStatus.CASADO_UNICO_TITULAR, TaxRegion.ACORES)
    }

    // ---- Madeira ----

    @Test
    fun `madeira tabela I (valores retificados)`() {
        assertTaxaEfetiva(1028.0, 0.015, MaritalStatus.NAO_CASADO, TaxRegion.MADEIRA)
        assertTaxaEfetiva(3614.0, 0.163, MaritalStatus.NAO_CASADO, TaxRegion.MADEIRA)
        assertTaxaEfetiva(6954.0, 0.218, MaritalStatus.NAO_CASADO, TaxRegion.MADEIRA)
    }

    @Test
    fun `madeira tabela III`() {
        assertTaxaEfetiva(1099.0, 0.010, MaritalStatus.CASADO_UNICO_TITULAR, TaxRegion.MADEIRA)
        assertTaxaEfetiva(6673.0, 0.117, MaritalStatus.CASADO_UNICO_TITULAR, TaxRegion.MADEIRA)
    }

    // ---- Casos-limite ----

    @Test
    fun `bruto abaixo do minimo de isencao devolve zero`() {
        assertEquals(0.0, IrsWithholdingCalculator.calcular(800.0, MaritalStatus.NAO_CASADO, 0, TaxRegion.CONTINENTE), 0.001)
    }

    @Test
    fun `bruto zero ou negativo devolve zero`() {
        assertEquals(0.0, IrsWithholdingCalculator.calcular(0.0, MaritalStatus.NAO_CASADO, 0, TaxRegion.CONTINENTE), 0.001)
        assertEquals(0.0, IrsWithholdingCalculator.calcular(-100.0, MaritalStatus.NAO_CASADO, 0, TaxRegion.CONTINENTE), 0.001)
    }

    @Test
    fun `retencao nunca fica negativa mesmo com muitos dependentes`() {
        val retencao = IrsWithholdingCalculator.calcular(1042.0, MaritalStatus.NAO_CASADO, 20, TaxRegion.CONTINENTE)
        assertEquals(0.0, retencao, 0.001)
    }
}
