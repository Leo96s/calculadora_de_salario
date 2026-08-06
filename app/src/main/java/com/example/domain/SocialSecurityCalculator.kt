package com.example.domain

/**
 * Quotização do trabalhador para a Segurança Social — regime geral, 11% sobre
 * a remuneração ilíquida total (Lei n.º 110/2009, Art. 53.º; sem exclusão de
 * base para majorações de domingo/feriado, ver Art. 46.º/48.º). Fonte em
 * docs/irs-tabelas-retencao.md.
 */
object SocialSecurityCalculator {
    private const val TAXA_TRABALHADOR = 0.11

    fun calcular(brutoMensal: Double): Double {
        if (brutoMensal <= 0.0) return 0.0
        return brutoMensal * TAXA_TRABALHADOR
    }
}
