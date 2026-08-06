package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SocialSecurityCalculatorTest {

    @Test
    fun `aplica 11 por cento ao bruto`() {
        assertEquals(110.00, SocialSecurityCalculator.calcular(1000.0), 0.001)
    }

    @Test
    fun `bruto zero ou negativo devolve zero`() {
        assertEquals(0.0, SocialSecurityCalculator.calcular(0.0), 0.001)
        assertEquals(0.0, SocialSecurityCalculator.calcular(-50.0), 0.001)
    }
}
