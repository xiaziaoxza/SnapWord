#!/usr/bin/env python3
"""Generate inflected forms for built-in dictionary. Conservative rules."""

import json
import re

# ─── Irregular Verbs (base, -s, -ed/past, -ing, alternate past participle) ───
IRREGULAR_VERBS = [
    ("arise", "arises", "arose", "arising", "arisen"),
    ("awake", "awakes", "awoke", "awaking", "awoken"),
    ("be", "is", "was", "being", "been"),
    ("bear", "bears", "bore", "bearing", "born"),
    ("beat", "beats", "beat", "beating", "beaten"),
    ("become", "becomes", "became", "becoming", "become"),
    ("begin", "begins", "began", "beginning", "begun"),
    ("bend", "bends", "bent", "bending", "bent"),
    ("bet", "bets", "bet", "betting", "bet"),
    ("bid", "bids", "bid", "bidding", "bid"),
    ("bind", "binds", "bound", "binding", "bound"),
    ("bite", "bites", "bit", "biting", "bitten"),
    ("bleed", "bleeds", "bled", "bleeding", "bled"),
    ("blow", "blows", "blew", "blowing", "blown"),
    ("break", "breaks", "broke", "breaking", "broken"),
    ("breed", "breeds", "bred", "breeding", "bred"),
    ("bring", "brings", "brought", "bringing", "brought"),
    ("broadcast", "broadcasts", "broadcast", "broadcasting", "broadcast"),
    ("build", "builds", "built", "building", "built"),
    ("burn", "burns", "burnt", "burning", "burnt"),
    ("burst", "bursts", "burst", "bursting", "burst"),
    ("buy", "buys", "bought", "buying", "bought"),
    ("cast", "casts", "cast", "casting", "cast"),
    ("catch", "catches", "caught", "catching", "caught"),
    ("choose", "chooses", "chose", "choosing", "chosen"),
    ("cling", "clings", "clung", "clinging", "clung"),
    ("come", "comes", "came", "coming", "come"),
    ("cost", "costs", "cost", "costing", "cost"),
    ("creep", "creeps", "crept", "creeping", "crept"),
    ("cut", "cuts", "cut", "cutting", "cut"),
    ("deal", "deals", "dealt", "dealing", "dealt"),
    ("dig", "digs", "dug", "digging", "dug"),
    ("do", "does", "did", "doing", "done"),
    ("draw", "draws", "drew", "drawing", "drawn"),
    ("dream", "dreams", "dreamt", "dreaming", "dreamt"),
    ("drink", "drinks", "drank", "drinking", "drunk"),
    ("drive", "drives", "drove", "driving", "driven"),
    ("dwell", "dwells", "dwelt", "dwelling", "dwelt"),
    ("eat", "eats", "ate", "eating", "eaten"),
    ("fall", "falls", "fell", "falling", "fallen"),
    ("feed", "feeds", "fed", "feeding", "fed"),
    ("feel", "feels", "felt", "feeling", "felt"),
    ("fight", "fights", "fought", "fighting", "fought"),
    ("find", "finds", "found", "finding", "found"),
    ("flee", "flees", "fled", "fleeing", "fled"),
    ("fly", "flies", "flew", "flying", "flown"),
    ("forbid", "forbids", "forbade", "forbidding", "forbidden"),
    ("forecast", "forecasts", "forecast", "forecasting", "forecast"),
    ("forget", "forgets", "forgot", "forgetting", "forgotten"),
    ("forgive", "forgives", "forgave", "forgiving", "forgiven"),
    ("freeze", "freezes", "froze", "freezing", "frozen"),
    ("get", "gets", "got", "getting", "gotten"),
    ("give", "gives", "gave", "giving", "given"),
    ("go", "goes", "went", "going", "gone"),
    ("grind", "grinds", "ground", "grinding", "ground"),
    ("grow", "grows", "grew", "growing", "grown"),
    ("hang", "hangs", "hung", "hanging", "hung"),
    ("have", "has", "had", "having", "had"),
    ("hear", "hears", "heard", "hearing", "heard"),
    ("hide", "hides", "hid", "hiding", "hidden"),
    ("hit", "hits", "hit", "hitting", "hit"),
    ("hold", "holds", "held", "holding", "held"),
    ("hurt", "hurts", "hurt", "hurting", "hurt"),
    ("keep", "keeps", "kept", "keeping", "kept"),
    ("kneel", "kneels", "knelt", "kneeling", "knelt"),
    ("know", "knows", "knew", "knowing", "known"),
    ("lay", "lays", "laid", "laying", "laid"),
    ("lead", "leads", "led", "leading", "led"),
    ("lean", "leans", "leant", "leaning", "leant"),
    ("leap", "leaps", "leapt", "leaping", "leapt"),
    ("learn", "learns", "learnt", "learning", "learnt"),
    ("leave", "leaves", "left", "leaving", "left"),
    ("lend", "lends", "lent", "lending", "lent"),
    ("let", "lets", "let", "letting", "let"),
    ("lie", "lies", "lay", "lying", "lain"),
    ("light", "lights", "lit", "lighting", "lit"),
    ("lose", "loses", "lost", "losing", "lost"),
    ("make", "makes", "made", "making", "made"),
    ("mean", "means", "meant", "meaning", "meant"),
    ("meet", "meets", "met", "meeting", "met"),
    ("mistake", "mistakes", "mistook", "mistaking", "mistaken"),
    ("misunderstand", "misunderstands", "misunderstood", "misunderstanding", "misunderstood"),
    ("overcome", "overcomes", "overcame", "overcoming", "overcome"),
    ("overtake", "overtakes", "overtook", "overtaking", "overtaken"),
    ("pay", "pays", "paid", "paying", "paid"),
    ("prove", "proves", "proved", "proving", "proven"),
    ("put", "puts", "put", "putting", "put"),
    ("quit", "quits", "quit", "quitting", "quit"),
    ("read", "reads", "read", "reading", "read"),
    ("ride", "rides", "rode", "riding", "ridden"),
    ("ring", "rings", "rang", "ringing", "rung"),
    ("rise", "rises", "rose", "rising", "risen"),
    ("run", "runs", "ran", "running", "run"),
    ("say", "says", "said", "saying", "said"),
    ("see", "sees", "saw", "seeing", "seen"),
    ("seek", "seeks", "sought", "seeking", "sought"),
    ("sell", "sells", "sold", "selling", "sold"),
    ("send", "sends", "sent", "sending", "sent"),
    ("set", "sets", "set", "setting", "set"),
    ("sew", "sews", "sewed", "sewing", "sewn"),
    ("shake", "shakes", "shook", "shaking", "shaken"),
    ("shed", "sheds", "shed", "shedding", "shed"),
    ("shine", "shines", "shone", "shining", "shone"),
    ("shoot", "shoots", "shot", "shooting", "shot"),
    ("show", "shows", "showed", "showing", "shown"),
    ("shrink", "shrinks", "shrank", "shrinking", "shrunk"),
    ("shut", "shuts", "shut", "shutting", "shut"),
    ("sing", "sings", "sang", "singing", "sung"),
    ("sink", "sinks", "sank", "sinking", "sunk"),
    ("sit", "sits", "sat", "sitting", "sat"),
    ("sleep", "sleeps", "slept", "sleeping", "slept"),
    ("slide", "slides", "slid", "sliding", "slid"),
    ("smell", "smells", "smelt", "smelling", "smelt"),
    ("sow", "sows", "sowed", "sowing", "sown"),
    ("speak", "speaks", "spoke", "speaking", "spoken"),
    ("speed", "speeds", "sped", "speeding", "sped"),
    ("spell", "spells", "spelt", "spelling", "spelt"),
    ("spend", "spends", "spent", "spending", "spent"),
    ("spill", "spills", "spilt", "spilling", "spilt"),
    ("spin", "spins", "spun", "spinning", "spun"),
    ("spit", "spits", "spat", "spitting", "spat"),
    ("split", "splits", "split", "splitting", "split"),
    ("spoil", "spoils", "spoilt", "spoiling", "spoilt"),
    ("spread", "spreads", "spread", "spreading", "spread"),
    ("spring", "springs", "sprang", "springing", "sprung"),
    ("stand", "stands", "stood", "standing", "stood"),
    ("steal", "steals", "stole", "stealing", "stolen"),
    ("stick", "sticks", "stuck", "sticking", "stuck"),
    ("sting", "stings", "stung", "stinging", "stung"),
    ("stink", "stinks", "stank", "stinking", "stunk"),
    ("strike", "strikes", "struck", "striking", "stricken"),
    ("strive", "strives", "strove", "striving", "striven"),
    ("swear", "swears", "swore", "swearing", "sworn"),
    ("sweep", "sweeps", "swept", "sweeping", "swept"),
    ("swim", "swims", "swam", "swimming", "swum"),
    ("swing", "swings", "swung", "swinging", "swung"),
    ("take", "takes", "took", "taking", "taken"),
    ("teach", "teaches", "taught", "teaching", "taught"),
    ("tear", "tears", "tore", "tearing", "torn"),
    ("tell", "tells", "told", "telling", "told"),
    ("think", "thinks", "thought", "thinking", "thought"),
    ("throw", "throws", "threw", "throwing", "thrown"),
    ("understand", "understands", "understood", "understanding", "understood"),
    ("undertake", "undertakes", "undertook", "undertaking", "undertaken"),
    ("wake", "wakes", "woke", "waking", "woken"),
    ("wear", "wears", "wore", "wearing", "worn"),
    ("weave", "weaves", "wove", "weaving", "woven"),
    ("weep", "weeps", "wept", "weeping", "wept"),
    ("win", "wins", "won", "winning", "won"),
    ("wind", "winds", "wound", "winding", "wound"),
    ("withdraw", "withdraws", "withdrew", "withdrawing", "withdrawn"),
    ("write", "writes", "wrote", "writing", "written"),
]

# Multi-syllable verbs that DO double final consonant (stress on last syllable)
MULTI_DOUBLES = {
    "begin", "forget", "occur", "permit", "admit", "commit", "refer",
    "prefer", "regret", "control", "compel", "compel", "expel", "rebel",
    "submit", "transmit", "intermit", "dispel", "impel", "propel", "repel",
    "abet", "acquit", "allot", "defer", "deter", "embed", "incur", "infer",
    "omit", "outbid", "overlap", "overrun", "overstep", "recur", "remit",
    "upset", "withhold",
}

# Suffixes that strongly indicate "not a verb" → skip -ed/-ing
NON_VERB_SUFFIXES = {
    "tion", "sion", "ness", "ity", "ment", "ance", "ence", "hood",
    "ship", "dom", "ism", "ist", "logy", "graphy", "metry", "nomy",
    "cracy", "phobia", "ology", "able", "ible", "less", "ful", "ous",
    "like", "some", "ular", "wise", "al", "rupt", "tial", "cial", "sive",
    "tive", "tude", "ior", "cent", "gent", "olute",
}

# Suffixes that indicate "clearly an adjective, not a noun" → skip -s
ADJ_ONLY_SUFFIXES = {
    "able", "ible", "less", "ful", "ous", "like", "ular", "wise", "ic",
    "al", "ish", "rupt",
}

# Individual words that should not get any inflected forms
NO_INFLECTION_WORDS = {
    "absence", "off", "np", "ok", "etc", "vs", "mr", "mrs", "ms",
    "dr", "st", "th", "nd", "rd",
    "each", "every", "any", "some", "all", "both", "few", "many",
    "most", "several", "such", "same", "other", "own", "very",
    "much", "more", "most", "little", "less", "least", "enough",
}

# Words where specific forms should be excluded (compound nouns etc.)
SKIP_ED_ING = {
    "blouse", "greenhouse", "workhouse", "businesswoman", "characteristic",
    "contradictory", "extraordinary", "granddaughter", "revolutionary",
    "sophisticated", "straightforward", "understanding",
    "town", "queen", "king", "prince", "princess", "absurd",
}

# Preposition/conjunction/article — no inflection at all
FUNCTION_WORDS = {
    "about", "above", "abroad", "aboard", "absence", "across", "after",
    "against", "along", "among", "another", "around",
    "before", "behind", "below", "beneath", "beside", "besides", "between",
    "beyond", "despite", "during", "except", "inside", "into", "onto",
    "outside", "over", "since", "through", "throughout", "toward", "towards",
    "under", "underneath", "until", "upon", "within", "without",
    "and", "but", "nor", "yet", "therefore", "however", "moreover",
    "although", "because", "unless", "whereas", "whether", "while",
    "that", "this", "these", "those", "there", "here", "where", "when",
    "than", "then", "thus", "hence", "also", "just", "only", "still",
    "even", "ever", "never", "always", "often", "seldom",
    "shall", "will", "would", "could", "should", "might", "must",
    "may", "can", "cannot", "whatever", "whichever", "whoever",
    "namely", "furthermore", "meanwhile", "nonetheless", "otherwise",
    "besides", "somewhat", "somehow",
}

def count_syllables(word):
    """Rough syllable count by vowel groups."""
    word = word.lower()
    count = 0
    prev_vowel = False
    for c in word:
        is_vowel = c in "aeiou"
        if is_vowel and not prev_vowel:
            count += 1
        prev_vowel = is_vowel
    # Silent 'e' at end doesn't count as separate syllable
    if word.endswith('e') and not word.endswith('le') and count > 1:
        count -= 1
    return max(count, 1)

def should_double(word):
    """Only double final consonant for 1-syllable words or known multi-syllable exceptions."""
    if len(word) < 3:
        return False
    if word[-1] not in "bcdfgklmnprstvz":
        return False
    if word[-2] not in "aeiou":
        return False
    if word[-3] in "aeiou":
        return False  # not CVC
    syl = count_syllables(word)
    if syl == 1:
        return True
    if syl >= 2 and word.lower() in MULTI_DOUBLES:
        return True
    return False

def make_ed(word):
    """Generate -ed/past form with corrected doubling."""
    if word.endswith('e'):
        return word + 'd'
    if word.endswith('y') and len(word) > 2 and word[-2] not in "aeiou":
        return word[:-1] + 'ied'
    if should_double(word):
        return word + word[-1] + 'ed'
    return word + 'ed'

def make_ing(word):
    """Generate -ing form with corrected doubling."""
    if word.endswith('ie'):
        return word[:-2] + 'ying'
    if word.endswith('e') and len(word) > 2 and not word.endswith('ee'):
        return word[:-1] + 'ing'
    if should_double(word):
        return word + word[-1] + 'ing'
    return word + 'ing'

def make_s(word):
    """Generate plural/3rd person singular."""
    if word.endswith(('s', 'x', 'z', 'sh', 'ch')):
        return word + 'es'
    if word.endswith('y') and len(word) > 2 and word[-2] not in "aeiou":
        return word[:-1] + 'ies'
    if word.endswith('o') and word not in {'piano', 'photo', 'kilo', 'memo', 'logo', 'solo', 'zero'}:
        return word + 'es'
    return word + 's'

def make_er(word):
    """Generate comparative."""
    if word.endswith('e'):
        return word + 'r'
    if word.endswith('y') and len(word) > 2 and word[-2] not in "aeiou":
        return word[:-1] + 'ier'
    if should_double(word):
        return word + word[-1] + 'er'
    return word + 'er'

def make_est(word):
    """Generate superlative."""
    if word.endswith('e'):
        return word + 'st'
    if word.endswith('y') and len(word) > 2 and word[-2] not in "aeiou":
        return word[:-1] + 'iest'
    if should_double(word):
        return word + word[-1] + 'est'
    return word + 'est'

# ─── Irregular noun plurals ────────────────────────────────────
IRREGULAR_PLURALS = {
    "child": "children", "man": "men", "woman": "women",
    "person": "people", "mouse": "mice", "goose": "geese",
    "tooth": "teeth", "foot": "feet", "ox": "oxen",
    "crisis": "crises", "analysis": "analyses", "thesis": "theses",
    "phenomenon": "phenomena", "criterion": "criteria",
    "datum": "data", "medium": "media", "bacterium": "bacteria",
    "curriculum": "curricula", "index": "indices", "appendix": "appendices",
    "focus": "foci", "nucleus": "nuclei",
    "stimulus": "stimuli", "syllabus": "syllabi",
    "life": "lives", "wife": "wives", "knife": "knives",
    "wolf": "wolves", "shelf": "shelves", "leaf": "leaves",
    "loaf": "loaves", "thief": "thieves", "half": "halves",
    "self": "selves", "scarf": "scarves",
    "fish": "fishes", "sheep": "sheep", "deer": "deer",
    "species": "species", "series": "series",
    "alumnus": "alumni", "radius": "radii",
}

def is_function_word(word):
    return word.lower() in FUNCTION_WORDS

def is_likely_verb(word):
    """Conservative: only EXCLUDE words that clearly cannot be verbs.
    This gives some false positives but avoids missing common verbs like 'remove'."""
    w = word.lower()
    if len(w) < 2: return False
    if is_function_word(w): return False
    if w.endswith("ly") and len(w) > 4: return False  # adverbs
    for suf in NON_VERB_SUFFIXES:
        if w.endswith(suf):
            return False
    # Common adjectives that are almost never verbs
    if w.endswith(("ical", "ular", "inal", "ior", "ular")):
        return False
    return True

def can_be_noun(word):
    """Word can take -s form. Only exclude clear non-nouns."""
    w = word.lower()
    if len(w) < 2: return False
    if is_function_word(w): return False
    if w.endswith("ly") and len(w) > 4: return False
    for suf in ADJ_ONLY_SUFFIXES:
        if w.endswith(suf):
            return False
    return True

def is_short_adj(word):
    """Short word likely takes -er/-est."""
    w = word.lower()
    if len(w) < 2: return False
    if is_function_word(w): return False
    if w.endswith("ly"): return False
    if is_likely_verb(w) and len(w) > 5: return False  # long verbs
    # Very short words (≤1 syllable) can be adjectives
    if count_syllables(w) <= 1 and len(w) <= 5:
        return True
    if w.endswith('y') and len(w) <= 6 and w[-2] not in "aeiou":
        return True
    return False

def main():
    base = '/data/data/com.termux/files/home/ccd-workspace/SnapWord/app/src/main/assets'
    dic_path = f'{base}/dictionary.json'

    with open(dic_path) as f:
        dic = json.load(f)

    existing_words = {e['w'] for e in dic}
    existing_lower = {w.lower() for w in existing_words}

    # Build irregular lookup
    irregular_map = {}
    for (base_w, s3, ed, ing, pp) in IRREGULAR_VERBS:
        forms = []
        if s3:
            forms.append((s3, f'{base_w} 的第三人称单数'))
        if ed:
            forms.append((ed, f'{base_w} 的过去式'))
        if pp and pp != ed:
            forms.append((pp, f'{base_w} 的过去分词'))
        if ing:
            forms.append((ing, f'{base_w} 的现在分词'))
        irregular_map[base_w] = forms

    new_entries = []

    for entry in dic:
        word = entry['w']
        lower = word.lower()
        if len(word) < 2 or len(word) > 20:
            continue
        if lower in NO_INFLECTION_WORDS:
            continue

        # ── Irregular verb forms ──
        if lower in irregular_map:
            for (form, label) in irregular_map[lower]:
                if form not in existing_lower:
                    new_entries.append({'w': form, 't': f'{label}，原形：{word}'})
                    existing_lower.add(form)

        # ── Irregular plural ──
        if lower in IRREGULAR_PLURALS:
            pl = IRREGULAR_PLURALS[lower]
            if pl not in existing_lower:
                new_entries.append({'w': pl, 't': f'{word} 的复数形式'})
                existing_lower.add(pl)

        is_verb = is_likely_verb(lower)
        is_adj = is_short_adj(lower)
        is_noun = can_be_noun(lower)

        # ── -s form ──
        if is_noun or is_verb:
            s_form = make_s(lower)
            if is_valid_english_form(s_form) and s_form not in existing_lower:
                if is_verb and is_noun:
                    label = f'{word} 的复数或第三人称单数'
                elif is_verb:
                    label = f'{word} 的第三人称单数'
                else:
                    label = f'{word} 的复数形式'
                new_entries.append({'w': s_form, 't': label})
                existing_lower.add(s_form)

        # ── -ed ──
        if is_verb and lower not in SKIP_ED_ING:
            ed_form = make_ed(lower)
            if is_valid_english_form(ed_form) and ed_form not in existing_lower:
                new_entries.append({'w': ed_form, 't': f'{word} 的过去式或过去分词'})
                existing_lower.add(ed_form)

            ing_form = make_ing(lower)
            if is_valid_english_form(ing_form) and ing_form not in existing_lower:
                new_entries.append({'w': ing_form, 't': f'{word} 的现在分词'})
                existing_lower.add(ing_form)

        # ── Comparative / Superlative ──
        if is_adj:
            er_form = make_er(lower)
            if is_valid_english_form(er_form) and er_form not in existing_lower:
                new_entries.append({'w': er_form, 't': f'{word} 的比较级'})
                existing_lower.add(er_form)

            est_form = make_est(lower)
            if is_valid_english_form(est_form) and est_form not in existing_lower:
                new_entries.append({'w': est_form, 't': f'{word} 的最高级'})
                existing_lower.add(est_form)

    new_entries.sort(key=lambda x: x['w'])

    print(f"原词条数: {len(dic)}")
    print(f"新增词条: {len(new_entries)}")

    # Check for known false positives
    bad = ['abilitied', 'abouted', 'aboarded', 'aboriginalled', 'abortioning',
           'abandonned', 'abandonning']
    print("\n误检复查（应为空）:")
    found_bad = [e for e in new_entries if e['w'] in bad]
    for e in found_bad:
        print(f"  ✗ {e['w']}")

    # Verify target words
    test_words = ['removed', 'removes', 'removing', 'carried', 'studied',
                  'bigger', 'biggest', 'abilities', 'problems', 'given',
                  'spoken', 'written', 'children', 'teeth', 'taken', 'abandoned']
    print("\n目标验证:")
    for tw in test_words:
        found = any(e['w'] == tw for e in new_entries)
        already = tw in existing_words
        print(f"  {tw:15s} new={'✓' if found else '✗'}, already={'✓' if already else '-'}")

    # Show samples
    print("\n新增样例（前20个）:")
    for e in new_entries[:20]:
        print(f"  {e['w']:20s} → {e['t']}")

    # Merge and save
    merged = dic + new_entries
    merged.sort(key=lambda x: x['w'].lower())

    with open(dic_path, 'w', encoding='utf-8') as f:
        json.dump(merged, f, ensure_ascii=False, indent=2)

    size_kb = len(json.dumps(merged, ensure_ascii=False)) / 1024
    print(f"\n✓ 已保存: {dic_path}")
    print(f"  最终词条数: {len(merged)}, 文件大小: {size_kb:.0f} KB")


def is_valid_english_form(word):
    return bool(re.match(r'^[a-z]+$', word)) and 2 <= len(word) <= 30


if __name__ == '__main__':
    main()
