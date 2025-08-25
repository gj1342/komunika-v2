package com.example.komunikav2.services

import android.net.Uri

object VideoCatalog {

    private const val ASSET_PREFIX = "asset:///videos/"

    private val categoryToFiles: Map<String, List<String>> = mapOf(
        "questions" to listOf(
            "WH-QUESTIONS/how.mp4",
            "WH-QUESTIONS/how_many.mp4",
            "WH-QUESTIONS/how_much.mp4",
            "WH-QUESTIONS/what.mp4",
            "WH-QUESTIONS/when.mp4",
            "WH-QUESTIONS/where.mp4",
            "WH-QUESTIONS/which.mp4",
            "WH-QUESTIONS/who.mp4",
            "WH-QUESTIONS/why.mp4",
        ),
        "time" to listOf(
            "TIME/always.mp4",
            "TIME/daily.mp4",
            "TIME/early.mp4",
            "TIME/hours.mp4",
            "TIME/late.mp4",
            "TIME/later.mp4",
            "TIME/minutes.mp4",
            "TIME/never.mp4",
            "TIME/now.mp4",
            "TIME/recently.mp4",
            "TIME/seconds.mp4",
            "TIME/soon.mp4",
            "TIME/time.mp4",
            "TIME/today.mp4",
            "TIME/tomorrow.mp4",
            "TIME/year.mp4",
            "TIME/yesterday.mp4",
        ),
        "survival" to listOf(
            "SURVIVAL/again.mp4",
            "SURVIVAL/copy_me.mp4",
            "SURVIVAL/don_t_know.mp4",
            "SURVIVAL/don_t_understand.mp4",
            "SURVIVAL/fingerspell.mp4",
            "SURVIVAL/finished.mp4",
            "SURVIVAL/forget.mp4",
            "SURVIVAL/help.mp4",
            "SURVIVAL/know.mp4",
            "SURVIVAL/later.mp4",
            "SURVIVAL/no.mp4",
            "SURVIVAL/please.mp4",
            "SURVIVAL/question.mp4",
            "SURVIVAL/ready.mp4",
            "SURVIVAL/right.mp4",
            "SURVIVAL/slow.mp4",
            "SURVIVAL/stop.mp4",
            "SURVIVAL/understand.mp4",
            "SURVIVAL/wait.mp4",
            "SURVIVAL/wrong.mp4",
            "SURVIVAL/yes.mp4",
        ),
        "places" to listOf(
            "PLACES/apartment.mp4",
            "PLACES/bank.mp4",
            "PLACES/cemetery.mp4",
            "PLACES/church.mp4",
            "PLACES/condominium.mp4",
            "PLACES/grocery.mp4",
            "PLACES/hospital.mp4",
            "PLACES/hotel.mp4",
            "PLACES/house_home.mp4",
            "PLACES/market.mp4",
            "PLACES/office.mp4",
            "PLACES/restaurant.mp4",
            "PLACES/school.mp4",
            "PLACES/store.mp4",
        ),
        "people" to listOf(
            "PEOPLE/boyfriend.mp4",
            "PEOPLE/girlfriend.mp4",
            "PEOPLE/man.mp4",
            "PEOPLE/old.mp4",
            "PEOPLE/sweetheart.mp4",
            "PEOPLE/woman.mp4",
            "PEOPLE/young.mp4",
        ),
        "gender" to listOf(
            "GENDER/boy.mp4",
            "GENDER/classmates.mp4",
            "GENDER/deaf.mp4",
            "GENDER/gay.mp4",
            "GENDER/girl.mp4",
            "GENDER/hard_of_hearing.mp4",
            "GENDER/hearing.mp4",
            "GENDER/lesbian.mp4",
            "GENDER/man.mp4",
            "GENDER/people.mp4",
            "GENDER/person.mp4",
            "GENDER/student.mp4",
            "GENDER/teacher.mp4",
            "GENDER/woman.mp4",
        ),
        "family" to listOf(
            "FAMILY/auntie.mp4",
            "FAMILY/baby.mp4",
            "FAMILY/brother.mp4",
            "FAMILY/cousin.mp4",
            "FAMILY/daughter.mp4",
            "FAMILY/father.mp4",
            "FAMILY/grandchildren.mp4",
            "FAMILY/grandfather.mp4",
            "FAMILY/grandfather_1.mp4",
            "FAMILY/grandmother.mp4",
            "FAMILY/grandmother_1.mp4",
            "FAMILY/husband.mp4",
            "FAMILY/mother.mp4",
            "FAMILY/nephew.mp4",
            "FAMILY/parent.mp4",
            "FAMILY/sister.mp4",
            "FAMILY/son.mp4",
            "FAMILY/uncle.mp4",
            "FAMILY/wife.mp4",
        ),
        "food" to listOf(
            "FOOD/apple.mp4",
            "FOOD/banana.mp4",
            "FOOD/black_pepper.mp4",
            "FOOD/burger.mp4",
            "FOOD/butter.mp4",
            "FOOD/cheese.mp4",
            "FOOD/coffee.mp4",
            "FOOD/egg.mp4",
            "FOOD/eggplant.mp4",
            "FOOD/fish.mp4",
            "FOOD/french_fries.mp4",
            "FOOD/fried_chicken.mp4",
            "FOOD/fruit.mp4",
            "FOOD/ice.mp4",
            "FOOD/ketchup.mp4",
            "FOOD/mango.mp4",
            "FOOD/mayonnaise.mp4",
            "FOOD/meat.mp4",
            "FOOD/noodles.mp4",
            "FOOD/oil.mp4",
            "FOOD/pandesal.mp4",
            "FOOD/rice.mp4",
            "FOOD/salad.mp4",
            "FOOD/sandwich.mp4",
            "FOOD/spaghetti.mp4",
            "FOOD/vegetable.mp4",
            "FOOD/vinegar.mp4",
            "FOOD/water.mp4",
        ),
        "calendar" to listOf(
            "CALENDAR/friday.mp4",
            "CALENDAR/monday.mp4",
            "CALENDAR/saturday.mp4",
            "CALENDAR/sunday.mp4",
            "CALENDAR/thursday.mp4",
            "CALENDAR/tuesday.mp4",
            "CALENDAR/wednesday.mp4",
        ),
        "colors" to listOf(
            "COLORS/black.mp4",
            "COLORS/blue.mp4",
            "COLORS/brown.mp4",
            "COLORS/dark.mp4",
            "COLORS/gray.mp4",
            "COLORS/green.mp4",
            "COLORS/light.mp4",
            "COLORS/orange.mp4",
            "COLORS/pink.mp4",
            "COLORS/red.mp4",
            "COLORS/tan.mp4",
            "COLORS/violet.mp4",
            "COLORS/white.mp4",
            "COLORS/yellow.mp4",
        ),
        "pronouns" to listOf(
            "PRONOUNS/he_she_him_her.mp4",
            "PRONOUNS/me.mp4",
            "PRONOUNS/that.mp4",
            "PRONOUNS/they_them_those.mp4",
            "PRONOUNS/this.mp4",
            "PRONOUNS/us.mp4",
            "PRONOUNS/we.mp4",
            "PRONOUNS/you.mp4",
        ),
        "alphabets" to ('a'..'z').map { "ALPHABETS/${it}.mp4" },
    )

    private val numbers1to10 = listOf(
        "NUMBERS/one.mp4",
        "NUMBERS/two.mp4",
        "NUMBERS/three.mp4",
        "NUMBERS/four.mp4",
        "NUMBERS/five.mp4",
        "NUMBERS/six.mp4",
        "NUMBERS/seven.mp4",
        "NUMBERS/eight.mp4",
        "NUMBERS/nine.mp4",
        "NUMBERS/ten.mp4",
    )

    private val numbers11to19 = listOf(
        "NUMBERS/eleven.mp4",
        "NUMBERS/twelve.mp4",
        "NUMBERS/thirteen.mp4",
        "NUMBERS/fourteen.mp4",
        "NUMBERS/fifteen.mp4",
        "NUMBERS/sixteen.mp4",
        "NUMBERS/seventeen.mp4",
        "NUMBERS/eighteen.mp4",
        "NUMBERS/nineteen.mp4",
    )

    private val numbers20to100 = listOf(
        "NUMBERS/twenty.mp4",
        "NUMBERS/thirty.mp4",
        "NUMBERS/forty.mp4",
        "NUMBERS/fifty.mp4",
        "NUMBERS/sixty.mp4",
        "NUMBERS/seventy.mp4",
        "NUMBERS/eighty.mp4",
        "NUMBERS/ninety.mp4",
        "NUMBERS/one_hundred.mp4",
    )

    fun getAssetUrisForCategory(categoryKey: String): List<Uri> {
        return when (categoryKey) {
            "numbers1-10" -> numbers1to10
            "numbers11-19" -> numbers11to19
            "numbers20-100" -> numbers20to100
            else -> categoryToFiles[categoryKey] ?: emptyList()
        }.map { Uri.parse(ASSET_PREFIX + it) }
    }

    fun getDisplayNameFromPath(path: String): String {
        val fileName = path.substringAfterLast('/')
        val base = fileName.removeSuffix(".mp4")
        return base.replace('_', ' ')
    }
    private val prioritizedPaths: List<String> by lazy {
        val prioritized = mutableListOf<String>()
        prioritized += numbers1to10
        prioritized += numbers11to19
        prioritized += numbers20to100
        listOf(
            "survival",
            "questions",
            "time",
            "places",
            "people",
            "gender",
            "family",
            "food",
            "calendar",
            "colors",
            "pronouns",
            "alphabets"
        ).forEach { key ->
            prioritized += (categoryToFiles[key] ?: emptyList())
        }
        prioritized
    }

    private val phraseKeyToPath: Map<String, String> by lazy {
        val map = LinkedHashMap<String, String>()
        prioritizedPaths.forEach { path ->
            val key = path.substringAfterLast('/').removeSuffix(".mp4").lowercase()
            if (!map.containsKey(key)) map[key] = path
        }
        map
    }

    private val knownPhrasesSorted: List<String> by lazy {
        phraseKeyToPath.keys.sortedByDescending { it.length }
    }

    private fun normalize(input: String): String {
        return input.lowercase().replace("[^a-z0-9]+".toRegex(), "_").trim('_')
    }

    private fun numberToPhraseKeys(value: Int): List<String> {
        if (value <= 0) return emptyList()
        if (value == 100) return listOf("one_hundred")
        if (value in 11..19) {
            return when (value) {
                11 -> listOf("eleven")
                12 -> listOf("twelve")
                13 -> listOf("thirteen")
                14 -> listOf("fourteen")
                15 -> listOf("fifteen")
                16 -> listOf("sixteen")
                17 -> listOf("seventeen")
                18 -> listOf("eighteen")
                else -> listOf("nineteen")
            }
        }
        val tens = value / 10 * 10
        val units = value % 10
        val out = mutableListOf<String>()
        when (tens) {
            10 -> out += "ten"
            20 -> out += "twenty"
            30 -> out += "thirty"
            40 -> out += "forty"
            50 -> out += "fifty"
            60 -> out += "sixty"
            70 -> out += "seventy"
            80 -> out += "eighty"
            90 -> out += "ninety"
        }
        if (units in 1..9) {
            out += when (units) {
                1 -> "one"
                2 -> "two"
                3 -> "three"
                4 -> "four"
                5 -> "five"
                6 -> "six"
                7 -> "seven"
                8 -> "eight"
                else -> "nine"
            }
        }
        return out
    }

    fun getUriForPhrase(phrase: String): Uri? {
        val raw = normalize(phrase)
        val numberMatch = "^\\d+".toRegex().matchEntire(raw)
        if (numberMatch != null) {
            val asInt = raw.toIntOrNull()
            if (asInt != null) {
                val keys = numberToPhraseKeys(asInt)
                if (keys.isEmpty()) return null
                val first = keys.firstOrNull() ?: return null
                val path = phraseKeyToPath[first] ?: return null
                return Uri.parse(ASSET_PREFIX + path)
            }
        }
        val path = phraseKeyToPath[raw]
        if (path != null) return Uri.parse(ASSET_PREFIX + path)
        if (raw.length == 1 && raw[0] in 'a'..'z') return Uri.parse(ASSET_PREFIX + "ALPHABETS/${raw}.mp4")
        return null
    }

    fun splitInputToUris(input: String): List<Uri> {
        val out = mutableListOf<Uri>()
        var remaining = normalize(input)
        while (remaining.isNotEmpty()) {
            val numMatch = "^\\d+".toRegex().find(remaining)
            if (numMatch != null) {
                val value = numMatch.value.toIntOrNull()
                if (value != null) {
                    val keys = numberToPhraseKeys(value)
                    keys.forEach { key ->
                        phraseKeyToPath[key]?.let { out += Uri.parse(ASSET_PREFIX + it) }
                    }
                    remaining = remaining.drop(numMatch.value.length).trimStart('_')
                    continue
                }
            }
            var matched = false
            for (key in knownPhrasesSorted) {
                if (remaining.startsWith(key)) {
                    phraseKeyToPath[key]?.let { out += Uri.parse(ASSET_PREFIX + it) }
                    remaining = remaining.removePrefix(key).trimStart('_')
                    matched = true
                    break
                }
            }
            if (!matched) {
                val ch = remaining.first()
                if (ch in 'a'..'z') out += Uri.parse(ASSET_PREFIX + "ALPHABETS/${ch}.mp4")
                remaining = remaining.drop(1)
                remaining = remaining.trimStart('_')
            }
        }
        return out
    }
}


