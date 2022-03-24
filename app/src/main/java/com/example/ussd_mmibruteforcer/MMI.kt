package com.example.ussd_mmibruteforcer

import kotlin.math.pow

/**
Structures of MMI codes :
Activation : *SC*SI#
Deactivation : #SC*SI#
Interrogation : *#SC*SI#
Registration : *SC*SI# and **SC*SI#
Erasure : ##SC*SI#
Structures of *SI#:
 *SIA*SIB*SIC#
 *SIA*SIB#
 *SIA**SIC#
 *SIA#
 **SIb*SIC#
 **SIB#
 ***SIC#
#
Refs: http://www.arib.or.jp/english/html/overview/doc/STD-T63V12_10/5_Appendix/Rel13/22/22030-d00.pdf
 */
class MMI(digits: Int) {
    private val n = alphabet.count().toDouble().pow(digits).toInt()
    val total = 5.toBigDecimal().times(n.toBigDecimal())
        .plus(15.toBigDecimal().times(n.toBigDecimal().pow(2)))
        .plus(15.toBigDecimal().times(n.toBigDecimal().pow(3)))
        .plus(5.toBigDecimal().times(n.toBigDecimal().pow(4)))
        // 5*n + 15*n*n + 15*n*n*n + 5*n*n*n*n
    val sequence = generate(digits)

    companion object {
        private val alphabet = sequenceOf("", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9")        // "*", "#",

        @JvmStatic
        fun iterate(digits: Int): Sequence<String> {    // generates: alphabet.length^digits
            var sequence = alphabet
            for (i in 1 until digits)
                sequence = iterate(sequence)
            return sequence
        }

        @JvmStatic
        fun iterate(sequence: Sequence<String>) = sequence {
            sequence.forEach { c1 ->
                alphabet.forEach { c2 ->
                    yield("$c1$c2")
                }
            }
        }

        // Service code types
        @JvmStatic
        fun serviceCodes(SC: String) = sequenceOf(
            "*$SC",     // activation
            "#$SC",     // deactivation
            "*#$SC",    // interrogation
            "**$SC",    // registration
            "##$SC"     // erasure
        )
    }

    private fun generate(digits: Int) = sequence {
        val n2 = n*n
        var a = 0
        var b = 0
        iterate(digits).forEach { scd ->
            serviceCodes(scd).forEach { sc ->
                yield("$sc#")
                iterate(digits).forEach { sia ->
                    yield("$sc*$sia#")
                    a++
                    iterate(digits).forEach { sib ->
                        yield("$sc*$sia*$sib#")
                        if (a%n == 1)
                            yield("$sc**$sib#")
                        b++
                        iterate(digits).forEach { sic ->
                            yield("$sc*$sia*$sib*$sic#")
                            if (b%n == 1)
                                yield("$sc*$sia**$sic#")
                            if (a%n == 1)
                                yield("$sc**$sib*$sic#")
                            if (b%n2 == 1)
                                yield("$sc***$sic#")
                        }
                    }
                }
            }
        }
    }
}