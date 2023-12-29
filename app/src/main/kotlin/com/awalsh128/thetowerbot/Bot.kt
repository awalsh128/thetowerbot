class Bot() {

  data class Skill(val name: String, val weight: Int, val tab: Int, val row: Int, val col: Int)

  fun main() {

    val namesAndWeightsByTab =
        listOf(
            listOf(
                Pair("Damage", 1),
                Pair("Attack speed", 1),
                Pair("Critical chance", 1),
                Pair("Critical factor", 1),
                Pair("Range", 1),
                Pair("Damage / meter", 1),
                Pair("Multishot chance", 1),
                Pair("Multishot targets", 1),
                Pair("Rapid fire chance", 1),
                Pair("Rapid fire duration", 1),
                Pair("Bounce shot chance", 1),
                Pair("Bounce shot targets", 1),
                Pair("Bounce shot range", 1)
            ),
            listOf(
                Pair("Health", 1),
                Pair("Health regen", 1),
                Pair("Defense %", 1),
                Pair("Defense absolute", 1),
                Pair("Thorn damage", 1),
                Pair("Lifesteal", 1),
                Pair("Knockback chance", 1),
                Pair("Knockback force", 1),
                Pair("Orb speed", 1),
                Pair("Orbs", 1),
                Pair("Shockwave size", 1),
                Pair("Shockwave frequency", 1),
                Pair("Landmine chance", 1),
                Pair("Landmine damage", 1),
                Pair("Landmine radius", 1)
            ),
            listOf(
                Pair("Cash bonus", 1),
                Pair("Cash wave", 1),
                Pair("Coins / kill bonus", 1),
                Pair("Coins / wave", 1),
                Pair("Free attack upgrade", 1),
                Pair("Free defense upgrade", 1),
                Pair("Free utility upgrade", 1),
                Pair("Interest / wave", 1),
                Pair("Recovery amount", 1),
                Pair("Max recovery", 1),
                Pair("Package Chance", 1)
            )
        )

    namesAndWeightsByTab.flatMapIndexed { i, g ->
      g.mapIndexed { j, p -> Skill(p.first, p.second, i, j / 2, j % 2) }.map {
        if (it.weight > 0) {
          // it.emit()
        }
      }
    }
  }
}