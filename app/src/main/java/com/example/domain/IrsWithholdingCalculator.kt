package com.example.domain

/**
 * Calcula a retenção mensal de IRS (Categoria A, trabalho dependente) a partir
 * das tabelas oficiais em [IrsWithholdingTables].
 *
 * Simplificação assumida (ver docs/irs-tabelas-retencao.md, secção
 * "Limitações"): trata o bruto mensal total, incluindo majorações de domingo e
 * feriado, como remuneração mensal normal — não aplica a regra especial de
 * "trabalho suplementar" (taxa efetiva a 50%) prevista nos despachos, por não
 * ter sido possível confirmar se estas majorações se enquadram nessa categoria
 * legal. Não substitui aconselhamento de um TOC.
 */
object IrsWithholdingCalculator {

    fun calcular(
        brutoMensal: Double,
        estadoCivil: MaritalStatus,
        dependentes: Int,
        regiao: TaxRegion
    ): Double {
        if (brutoMensal <= 0.0) return 0.0

        val numeroTabela = when (estadoCivil) {
            MaritalStatus.NAO_CASADO -> if (dependentes > 0) 2 else 1
            MaritalStatus.CASADO_DOIS_TITULARES -> 1
            MaritalStatus.CASADO_UNICO_TITULAR -> 3
        }

        val tabela = IrsWithholdingTables.paraRegiaoETabela(regiao, numeroTabela)
        val escalao = tabela.firstOrNull { brutoMensal <= it.limiteSuperior } ?: tabela.last()

        val valor = brutoMensal * escalao.taxaMarginal -
            escalao.parcelaAAbater(brutoMensal) -
            escalao.parcelaPorDependente * dependentes

        return valor.coerceAtLeast(0.0)
    }
}
