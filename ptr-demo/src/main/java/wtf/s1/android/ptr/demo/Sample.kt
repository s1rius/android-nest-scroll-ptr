package wtf.s1.android.ptr.demo

import wtf.s1.android.ptr_support_design.R
import java.util.*

data class Post(
    val name: Int,
    val text: Int,
    val avatar: Int,
    val pic: Int? = 0
)

val Undying = Post(R.string.undying, R.string.undying_line, R.drawable.undying)
val BeastMaster = Post(R.string.bm, R.string.bm_line, R.drawable.beastmaster)
val Clinkz = Post(R.string.clinkz, R.string.clinkz_line, R.drawable.clinkz)
val Haskar = Post(R.string.huskar, R.string.huskar_line, R.drawable.huskar)
val BatRider = Post(R.string.batrider, R.string.batrider_line, R.drawable.batrider)
val BrewMaster = Post(R.string.brewmaster, R.string.brewmaster_line, R.drawable.brewmaster)
val NeverMore = Post(R.string.nevermore, R.string.nevermore_line, R.drawable.nevermore)
val Antimage = Post(R.string.am, R.string.am_line, R.drawable.antimage)
val Mirana = Post(R.string.mirana, R.string.mirana_line, R.drawable.mirana)
val Rubick = Post(R.string.rubick, R.string.rubick_line, R.drawable.rubick)
val Invoker = Post(R.string.invoker, R.string.invoker_line, R.drawable.invoker)
val Grimstroke = Post(R.string.grimstroke, R.string.grimstroke_line, R.drawable.grimstroke)
val NA = Post(R.string.na, R.string.na_line, R.drawable.nyx_assassin)
val FV = Post(R.string.fv, R.string.fv_line, R.drawable.faceless_void)
val Zeus = Post(R.string.zeus, R.string.zeus_line, R.drawable.zuus)
val Sladar = Post(R.string.slardar, R.string.slardar_line, R.drawable.slardar)
val TW = Post(R.string.tw, R.string.tw_line, R.drawable.troll_warlord)
val NightStalker = Post(R.string.ns, R.string.ns_line, R.drawable.night_stalker)
val ElderTitan = Post(R.string.eldertitan, R.string.eldertitan_line, R.drawable.elder_titan)
val ChaosKnight = Post(R.string.ck, R.string.ck_line, R.drawable.chaos_knight)
val BountyHunter = Post(R.string.bh, R.string.bh_line, R.drawable.bounty_hunter)
val PL = Post(R.string.pl, R.string.pl_line, R.drawable.phantom_lancer)
val Pudge = Post(R.string.pudge, 0, R.drawable.pudge)

val DotaList = arrayListOf(
    Undying,
    Clinkz,
    ElderTitan,
    NightStalker,
    NA,
    FV,
    BatRider,
    BrewMaster,
    Antimage,
    PL,
    BountyHunter,
    Mirana,
    Rubick,
    Invoker,
    ChaosKnight,
    Grimstroke,
    Haskar,
    BeastMaster,
    Zeus,
    Sladar,
    TW,
    NeverMore,
)

private val RANDOM = Random()

val RandomDrawable: Int
    get() {
        return when (RANDOM.nextInt(5)) {
            0 -> R.drawable.wp1
            1 -> R.drawable.wp2
            2 -> R.drawable.wp3
            3 -> R.drawable.wp4
            4 -> R.drawable.wp5
            else -> R.drawable.wp1
        }
    }






