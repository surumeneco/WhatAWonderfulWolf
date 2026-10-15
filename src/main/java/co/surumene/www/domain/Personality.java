package co.surumene.www.domain;

public enum Personality {
    SERIOUS("まじめ"),
    HASTY("せっかち"),
    JOLLY("ようき"),
    NIMBLE("すばしっこい"),
    VALIANT("いさましい"),
    HARD_WORKING("はたらきもの"),
    NAUGHTY("わんぱく"),
    STURDY("たくましい"),
    ADAMANT("いじっぱり"),
    RELAXED("のんびり"),
    CAUTIOUS("しんちょう"),
    MIGHTY("ごうけつ"),
    GENTLE("おだやか"),
    ROWDY("あばれんぼう"),
    BRAVE("ゆうかん"),
    POWERFUL("ちからもち"),
    GLUTTONOUS("くいしんぼ");

    private final String displayName;

    Personality(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
